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
import org.joml.Matrix4d;
import org.joml.Vector3d;
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
import java.util.stream.Collectors;

@Slf4j
public class Gaia3DTiler {
    private static final AssimpConverter assimpConverter = new AssimpConverter(null);
    private static final int LOD0_GEOMETRIC_ERROR = 0;
    private static final int LOD1_GEOMETRIC_ERROR = 2;
    private static final int LOD2_GEOMETRIC_ERROR = 4;
    private static final int LOD3_GEOMETRIC_ERROR = 8;

    private static final int MAX_COUNT = 36;
    private static final int TEST_COUNT = 2000;

    private final Path inputPath;
    private final Path outputPath;

    public Gaia3DTiler(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public void excute() {
        List<GaiaScene> scenes = read(inputPath);
        GaiaBoundingBox globalBoundingBox = calcBoundingBox(scenes);
        Matrix4d transformMatrix = getTransfromMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix);

        tiling(root, scenes);

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(100f);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        write(outputPath, tileset);
    }

    public List<GaiaScene> containScenes(BoundingVolume boundingVolume, List<GaiaScene> scenes) {
        List<GaiaScene> result = boundingVolume.contains(scenes);
        FormatType formatType = FormatType.MAX_3DS;
        result = result.stream().map((scene) -> assimpConverter.load(scene.getOriginalPath(), formatType.getExtension()))
                .collect(Collectors.toList());
        return result;
    }

    public GaiaBoundingBox calcBoundingBox(List<GaiaScene> sceneList) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        sceneList.forEach(scene -> boundingBox.addBoundingBox(scene.getBoundingBox()));
        return boundingBox;
    }

    private boolean tiling(Node parentNode, List<GaiaScene> scenes) {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        if (scenes.size() > MAX_COUNT) {
            log.info("node : " + parentNode.getNodeCode() +", " + scenes.size() + " scenes");
            BoundingVolume[] childrenBoundingVolume = parentBoundingVolume.divideBoundingVolume();
            for (int i = 0; i < childrenBoundingVolume.length; i++) {
                BoundingVolume childBoundingVolume = childrenBoundingVolume[i];
                List<GaiaScene> childScenes = containScenes(childBoundingVolume, scenes);
                if (childScenes.size() < 1) {
                    continue;
                }
                GaiaBoundingBox childBoundingBox = calcBoundingBox(childScenes);
                childBoundingVolume = new BoundingVolume(childBoundingBox);

                Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
                rotateX90(transformMatrix);
                Node childNode = new Node();
                childNode.setParent(parentNode);
                childNode.setTransformMatrix(transformMatrix);
                childNode.setNodeCode(parentNode.getNodeCode() + i);
                childNode.setGeometricError(50f);
                childNode.setRefine(Node.RefineType.REPLACE);
                childNode.setBoundingVolume(childBoundingVolume);
                childNode.setChildren(new ArrayList<>());
                tiling(childNode, childScenes);
                parentNode.getChildren().add(childNode);
            }
        } else {
            if (scenes.size() > 0) {
                log.info("contents : " + parentNode.getNodeCode() +", " + scenes.size() + " scenes");
                String name = parentNode.getNodeCode();
                int geometricError;
                int lod;
                if (name.contains("C")) {
                    String c = name.split("C")[1];
                    if (c.length() == 0) {
                        geometricError = LOD2_GEOMETRIC_ERROR;
                        lod = 2;
                    } else if (c.length() == 1) {
                        geometricError = LOD1_GEOMETRIC_ERROR;
                        lod = 1;
                    } else if (c.length() == 2) {
                        geometricError = LOD0_GEOMETRIC_ERROR;
                        lod = 0;
                    } else {
                        return false;
                    }
                } else {
                    name = name + "C";
                    geometricError = LOD3_GEOMETRIC_ERROR;
                    lod = 3;
                }

                GaiaUniverse universe = new GaiaUniverse(name, inputPath, outputPath);
                scenes.forEach(scene -> universe.getScenes().add(scene));

                GaiaBoundingBox parentBoundingBox = calcBoundingBox(scenes);
                TileInfo tileInfo = new TileInfo();
                tileInfo.setUniverse(universe);
                tileInfo.setBoundingBox(parentBoundingBox);
                Batched3DModel batched3DModel = new Batched3DModel(tileInfo, lod);
                if (!batched3DModel.write(name)) {
                    return false;
                }

                Content content = new Content();
                content.setUri(name + ".b3dm");
                parentNode.setContent(content);

                BoundingVolume[] childrenBoundingVolume = parentBoundingVolume.divideBoundingVolume();
                for (int i = 0; i < childrenBoundingVolume.length; i++) {
                    BoundingVolume childBoundingVolume = childrenBoundingVolume[i];
                    List<GaiaScene> childScenes = containScenes(childBoundingVolume, scenes);
                    /*if (childScenes.size() < 1) {
                        continue;
                    }*/

                    GaiaBoundingBox childBoundingBox = calcBoundingBox(childScenes);
                    Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
                    rotateX90(transformMatrix);

                    childBoundingVolume = new BoundingVolume(childBoundingBox);

                    Node childNode = new Node();
                    childNode.setParent(parentNode);
                    childNode.setTransformMatrix(transformMatrix);
                    childNode.setNodeCode(name + i);
                    childNode.setGeometricError(geometricError);
                    childNode.setRefine(Node.RefineType.REPLACE);
                    childNode.setBoundingVolume(childBoundingVolume);
                    childNode.setChildren(new ArrayList<>());

                    boolean result = tiling(childNode, childScenes);
                    if (result) {
                        parentNode.getChildren().add(childNode);
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }


    private void rotateNodes(Node node) {
        Matrix4d transformMatrix = node.getTransformMatrix();
        rotateX90(transformMatrix);
        node.setTransform(transformMatrix.get(new float[16]));
        for (Node childNode : node.getChildren()) {
            rotateNodes(childNode);
        }
    }

    private void rotateX90(Matrix4d matrix) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(-90));
        matrix.mul(rotationMatrix, matrix);
    }

    private Matrix4d getTransfromMatrix(BoundingVolume boundingVolume) {
        Vector3d center = new Vector3d(boundingVolume.getCenter());
        Vector3d centerBottom = new Vector3d(center.x, center.y, boundingVolume.getRegion()[4]);
        ProjCoordinate centerPoint = new ProjCoordinate(centerBottom.x(), centerBottom.y(), centerBottom.z());
        //ProjCoordinate centerPoint = new ProjCoordinate(center.x(), center.y(), center.z());

        ProjCoordinate translatedCenterPoint = GlobeUtils.transform(null, centerPoint);
        double[] cartesian = GlobeUtils.geographicToCartesianWgs84(translatedCenterPoint.x, translatedCenterPoint.y, centerBottom.z());
        return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
    }

    private Matrix4d getTransfromMatrix(GaiaBoundingBox boundingBox) {
        Vector3d center = new Vector3d(boundingBox.getCenter());
        Vector3d centerBottom = new Vector3d(center.x, center.y, boundingBox.getMinZ());

        ProjCoordinate centerPoint = new ProjCoordinate(centerBottom.x(), centerBottom.y(), centerBottom.z());
        //ProjCoordinate centerPoint = new ProjCoordinate(center.x(), center.y(), center.z());

        ProjCoordinate translatedCenterPoint = GlobeUtils.transform(null, centerPoint);
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
        root.setGeometricError(100f);
        root.setRefine(Node.RefineType.REPLACE);
        root.setChildren(new ArrayList<>());
        return root;
    }

    private void write(Path output, Tileset tileset) {
        File tilesetFile = output.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String test = objectMapper.writeValueAsString(tileset);
            log.info(test);
            writer.write(test);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<GaiaScene> read(Path input) {
        List<GaiaScene> sceneList = new ArrayList<>();
        readTree(sceneList, input.toFile(), FormatType.MAX_3DS);
        return sceneList;
    }

    private void readTree(List<GaiaScene> sceneList, File inputFile, FormatType formatType) {
        if (inputFile.isFile() && inputFile.getName().endsWith("." + formatType.getExtension())) {
            GaiaScene scene = assimpConverter.load(inputFile.toPath(), formatType.getExtension());
            sceneList.add(scene);
        } else if (inputFile.isDirectory()){
            for (File child : inputFile.listFiles()) {
                if (sceneList.size() <= TEST_COUNT) {
                    readTree(sceneList, child, formatType);
                }
            }
        }
    }
}
