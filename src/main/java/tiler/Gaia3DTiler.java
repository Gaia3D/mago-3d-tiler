package tiler;

import assimp.AssimpConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import gltf.Batched3DModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
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
public class Gaia3DTiler {
    private static final int MAX_COUNT = 128;

    private AssimpConverter assimpConverter;
    private final Path inputPath;
    private final Path outputPath;
    private final FormatType inputFormatType;
    private final CoordinateReferenceSystem source;

    public Gaia3DTiler(Path inputPath, Path outputPath, FormatType inputFormatType, CoordinateReferenceSystem source, CommandLine command) {
        this.assimpConverter = new AssimpConverter(command);
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.inputFormatType = inputFormatType;
        this.source = source;
    }

    public void execute() {
        List<GaiaScene> scenes = readInputFile(inputPath);
        double geometricError = calcGeometricError(scenes);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(scenes);
        Matrix4d transformMatrix = getTransfromMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox, this.source));
        root.setTransformMatrix(transformMatrix);
        root.setGeometricError(geometricError);

        tiling(root, scenes);

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        write(outputPath, tileset);
    }

    public List<GaiaScene> containScenes(BoundingVolume boundingVolume, List<GaiaScene> scenes) {
        List<GaiaScene> result = boundingVolume.contains(scenes, this.source);
        return reloadScenes(result);
    }

    public List<GaiaScene> reloadScenes(List<GaiaScene> scenes) {
        FormatType formatType = this.inputFormatType;
        return scenes.stream()
                .map((scene) -> assimpConverter.load(scene.getOriginalPath(), formatType.getExtension()))
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

    private void tiling(Node parentNode, List<GaiaScene> scenes) {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        if (scenes.size() > MAX_COUNT) {
            BoundingVolume[] childrenBoundingVolume = parentBoundingVolume.divideBoundingVolume();
            for (int index = 0; index < childrenBoundingVolume.length; index++) {
                BoundingVolume childBoundingVolume = childrenBoundingVolume[index];
                List<GaiaScene> childScenes = containScenes(childBoundingVolume, scenes);
                Node childNode = createStructNode(parentNode, childScenes, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    tiling(childNode, childScenes);
                }
            }
        } else if (scenes.size() > 1) {
            BoundingVolume[] childrenBoundingVolume = parentBoundingVolume.divideBoundingVolume();
            for (int index = 0; index < childrenBoundingVolume.length; index++) {
                BoundingVolume childBoundingVolume = childrenBoundingVolume[index];
                List<GaiaScene> childScenes = containScenes(childBoundingVolume, scenes);
                Node childNode = createContentNode(parentNode, childScenes, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    tiling(childNode, childScenes);
                }
            }
        } else if (scenes.size() > 0) {
            Node childNode = createContentNode(parentNode, reloadScenes(scenes), 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                tiling(childNode, scenes);
            }
        }
    }

    private Node createStructNode(Node parentNode, List<GaiaScene> scenes, int index) {
        if (scenes.size() < 1) {
            return null;
        }
        double geometricError = calcGeometricError(scenes);
        GaiaBoundingBox childBoundingBox = calcBoundingBox(scenes);
        Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
        rotateX90(transformMatrix);

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox, this.source);
        boundingVolume.square();

        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[" + nodeCode + "] : " + scenes.size());

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

    private Node createContentNode(Node parentNode, List<GaiaScene> scenes, int index) {
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
        TileInfo tileInfo = new TileInfo();
        tileInfo.setUniverse(universe);
        tileInfo.setBoundingBox(childBoundingBox);

        long start = System.currentTimeMillis();
        Batched3DModel batched3DModel = new Batched3DModel(tileInfo, lod);
        if (!batched3DModel.write(nodeCode)) {
            return null;
        }
        log.info("┕ " + nodeCode + " : " + scenes.size() + " : " + (System.currentTimeMillis() - start));

        Content content = new Content();
        content.setUri(nodeCode + ".b3dm");
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

    private List<GaiaScene> readInputFile(Path input) {
        Objects.requireNonNull(input, "input path is null");
        List<GaiaScene> sceneList = new ArrayList<>();
        readTree(sceneList, input.toFile(), this.inputFormatType);
        return sceneList;
    }

    private void readTree(List<GaiaScene> sceneList, File inputFile, FormatType formatType) {
        if (inputFile.isFile() && inputFile.getName().endsWith("." + formatType.getExtension())) {
            GaiaScene scene = assimpConverter.load(inputFile.toPath(), formatType.getExtension());
            sceneList.add(scene);
        } else if (inputFile.isDirectory()){
            File[] childFiles = inputFile.listFiles();
            if (childFiles != null) {
                for (File child : childFiles) {
                    readTree(sceneList, child, formatType);
                }
            }
        }
    }
}
