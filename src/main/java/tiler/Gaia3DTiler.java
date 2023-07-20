package tiler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import converter.AssimpConverter;
import converter.Converter;
import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaUniverse;
import geometry.extension.GeometryOptimizer;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import tiler.tileset.Tileset;
import tiler.tileset.asset.*;
import tiler.tileset.node.BoundingVolume;
import tiler.tileset.node.Content;
import tiler.tileset.node.Node;
import util.GlobeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class Gaia3DTiler implements Tiler {
    private static final int MAX_COUNT = 256;

    private final Converter assimpConverter;
    private final Path inputPath;
    private final Path outputPath;
    private final FormatType inputFormatType;
    private final CoordinateReferenceSystem source;
    private final CommandLine command;

    public Gaia3DTiler(Path inputPath, Path outputPath, FormatType inputFormatType, CoordinateReferenceSystem source, CommandLine command) {
        this.assimpConverter = new AssimpConverter(command);
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.inputFormatType = inputFormatType;
        this.source = source;
        this.command = command;
    }

    public Tileset tile() {
        boolean recursive = command.hasOption("recursive");
        List<GaiaScene> scenes = readInputFile(inputPath, recursive);
        double geometricError = calcGeometricError(scenes);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(scenes);
        Matrix4d transformMatrix = getTransfromMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox, this.source));
        root.setTransformMatrix(transformMatrix);
        root.setGeometricError(geometricError);

        try {
            createNode(root, scenes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        write(outputPath, tileset);

        return tileset;
    }

    private List<GaiaScene> reloadScenes(List<GaiaScene> scenes) {
        //FormatType formatType = this.inputFormatType;
        return scenes.stream()
                .map((scene) -> assimpConverter.load(scene.getOriginalPath()))
                .collect(Collectors.toList());
    }

    private double calcGeometricError(List<GaiaScene> sceneList) {
        // getMaxDistance
        return sceneList.stream().mapToDouble(scene -> {
            double result = scene.getBoundingBox().getLongestDistance();
            if (result > 1000.0d) {
                log.info(result + " is too long distance. check it please.");
            }
            return result;
        }).max().orElse(0.0d);
    }

    private GaiaBoundingBox calcBoundingBox(List<GaiaScene> sceneList) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        sceneList.forEach(scene -> boundingBox.addBoundingBox(scene.getBoundingBox()));
        return boundingBox;
    }

    private void createNode(Node parentNode, List<GaiaScene> scenes) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        if (scenes.size() > MAX_COUNT) {
            List<List<GaiaScene>> childrenScenes = parentBoundingVolume.distributeScene(scenes, this.source);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<GaiaScene> childScenes = childrenScenes.get(index);
                Node childNode = createStructNode(parentNode, childScenes, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childScenes);
                }
            }
        } else if (scenes.size() > 1) {
            int test = 0;
            List<List<GaiaScene>> childrenScenes = parentBoundingVolume.distributeScene(scenes, this.source);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<GaiaScene> childScenes = reloadScenes(childrenScenes.get(index));
                test += childScenes.size();
                Node childNode = createContentNode(parentNode, childScenes, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childScenes);
                }
            }
            if (test != scenes.size()) {
                log.info("test : " + test + ", scenes : " + scenes.size());
            }
        } else if (scenes.size() > 0) {
            Node childNode = createContentNode(parentNode, reloadScenes(scenes), 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, scenes);
            }
        }
    }

    private Node createStructNode(Node parentNode, List<GaiaScene> scenes, int index) {
        if (scenes.size() < 1) {
            return null;
        }
        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[StructNode ][" + nodeCode + "] : {}", scenes.size());

        double geometricError = calcGeometricError(scenes);
        GaiaBoundingBox childBoundingBox = calcBoundingBox(scenes);
        Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
        rotateX90(transformMatrix);

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox, this.source);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(geometricError);
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());
        GaiaUniverse universe = new GaiaUniverse(nodeCode, inputPath, outputPath);
        scenes.forEach(scene -> universe.getScenes().add(scene));
        return childNode;
    }

    private Node createContentNode(Node parentNode, List<GaiaScene> scenes, int index) throws IOException {
        if (scenes.size() < 1) {
            return null;
        }

        GaiaBoundingBox childBoundingBox = calcBoundingBox(scenes);
        Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
        rotateX90(transformMatrix);

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox, this.source);

        String nodeCode = parentNode.getNodeCode();
        LevelOfDetail lod = getLodByNodeCode(nodeCode);
        if (lod == LevelOfDetail.LOD3) {
            nodeCode = nodeCode + "C";
        } else if (lod == LevelOfDetail.NONE) {
            return null;
        }
        nodeCode = nodeCode + index;

        log.info("[ContentNode][" + nodeCode + "] : {}", scenes.size());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lod.getGeometricError());
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());

        GaiaUniverse universe = new GaiaUniverse(nodeCode, inputPath, outputPath);
        scenes.forEach(scene -> universe.getScenes().add(scene));

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setLod(lod);
        batchInfo.setUniverse(universe);
        batchInfo.setBoundingBox(childBoundingBox);
        batchInfo.setNodeCode(nodeCode);

        /*Batched3DModel batched3DModel = new Batched3DModel(tileInfo, lod, this.command);
        if (!batched3DModel.write(nodeCode)) {
            return null;
        }*/

        Content content = new Content();
        content.setUri(nodeCode + ".b3dm");
        content.setBatchInfo(batchInfo);
        childNode.setContent(content);
        return childNode;
    }

    private LevelOfDetail getLodByNodeCode(String nodeCode) {
        LevelOfDetail levelOfDetail = LevelOfDetail.NONE;
        boolean hasContent = nodeCode.contains("C");
        if (hasContent) {
            String contentLevel = nodeCode.split("C")[1];
            int level = contentLevel.length();
            if (level == 1) {
                levelOfDetail = LevelOfDetail.LOD2;
            } else if (level == 2) {
                levelOfDetail = LevelOfDetail.LOD1;
            } else if (level == 3) {
                levelOfDetail = LevelOfDetail.LOD0;
            }
        } else {
            levelOfDetail = LevelOfDetail.LOD3;
        }
        return levelOfDetail;
    }

    private void rotateX90(Matrix4d matrix) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(-90));
        matrix.mul(rotationMatrix, matrix);
    }

    private Matrix4d getTransfromMatrix(GaiaBoundingBox boundingBox) {
        Vector3d center = boundingBox.getCenter();
        Vector3d centerBottom = new Vector3d(center.x, center.y, boundingBox.getMinZ());
        ProjCoordinate centerPoint = new ProjCoordinate(centerBottom.x(), centerBottom.y(), centerBottom.z());
        ProjCoordinate translatedCenterPoint = GlobeUtils.transform(this.source, centerPoint);
        double[] cartesian = GlobeUtils.geographicToCartesianWgs84(translatedCenterPoint.x, translatedCenterPoint.y, centerBottom.z());
        return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        Extras extras = new Extras();
        Cesium cesium = new Cesium();
        Ion ion = new Ion();
        List<Credit> credits = new ArrayList<>();
        Credit credit = new Credit();
        credit.setHtml("<html>");
        credits.add(credit);
        cesium.setCredits(credits);
        extras.setIon(ion);
        extras.setCesium(cesium);
        asset.setExtras(extras);
        return asset;
    }

    private Node createRoot() {
        Node root = new Node();
        root.setParent(root);
        root.setNodeCode("R");
        root.setRefine(Node.RefineType.REPLACE);
        root.setChildren(new ArrayList<>());
        return root;
    }

    private void write(Path output, Tileset tileset) {
        File tilesetFile = output.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            writer.write(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<GaiaScene> readInputFile(Path input, boolean recursive) {
        Objects.requireNonNull(input, "input path is null");
        List<GaiaScene> sceneList = new ArrayList<>();
        String[] extensions = new String[] {this.inputFormatType.getExtension()};
        for (File child : FileUtils.listFiles(input.toFile(), extensions, recursive)) {
            GaiaScene scene = assimpConverter.load(child);
            sceneList.add(scene);
        }
        return sceneList;
    }
}
