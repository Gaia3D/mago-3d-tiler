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
    private static AssimpConverter assimpConverter = new AssimpConverter(null);
    private static final int MAX_COUNT = 12;
    private static final int TEST_COUNT = 100;

    private final Path inputPath;
    private final Path outputPath;

    public Gaia3DTiler(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public List<GaiaScene> containScenes(GaiaBoundingBox searchBoundingBox, List<GaiaScene> sceneList) {
        List<GaiaScene> resultList = sceneList.stream().filter((scene) -> {
            GaiaBoundingBox boundingBox = scene.getBoundingBox();
            Vector3d center = boundingBox.getCenter();
            return searchBoundingBox.contains(center);
        }).collect(Collectors.toList());

        FormatType formatType = FormatType.MAX_3DS;
        resultList = resultList.stream().map((scene) -> {
            GaiaScene reloadScene = assimpConverter.load(scene.getOriginalPath(), formatType.getExtension());
            return reloadScene;
        }).collect(Collectors.toList());

        return resultList;
    }

    // child idx.***
    //       +-----+-----+
    //       |  3  |  2  |
    //       +-----+-----+
    //       |  0  |  1  |
    //       +-----+-----+

    public GaiaBoundingBox[] divideBoundingBox(GaiaBoundingBox globalBoundingBox) {
        Vector3d center = globalBoundingBox.getCenter();
        double minX = globalBoundingBox.getMinX();
        double minY = globalBoundingBox.getMinY();
        double minZ = globalBoundingBox.getMinZ();
        double maxX = globalBoundingBox.getMaxX();
        double maxY = globalBoundingBox.getMaxY();
        double maxZ = globalBoundingBox.getMaxZ();
        double centerX = center.x();
        double centerY = center.y();
        GaiaBoundingBox[] divideBoundingBox = new GaiaBoundingBox[4];
        GaiaBoundingBox boundingBox0 = new GaiaBoundingBox();
        boundingBox0.addPoint(new Vector3d(minX, minY, minZ));
        boundingBox0.addPoint(new Vector3d(centerX, centerY, maxZ));
        divideBoundingBox[0] = boundingBox0;

        GaiaBoundingBox boundingBox1 = new GaiaBoundingBox();
        boundingBox1.addPoint(new Vector3d(centerX, minY, minZ));
        boundingBox1.addPoint(new Vector3d(maxX, centerY, maxZ));
        divideBoundingBox[1] = boundingBox1;

        GaiaBoundingBox boundingBox2 = new GaiaBoundingBox();
        boundingBox2.addPoint(new Vector3d(centerX, centerY, minZ));
        boundingBox2.addPoint(new Vector3d(maxX, maxY, maxZ));
        divideBoundingBox[2] = boundingBox2;

        GaiaBoundingBox boundingBox3 = new GaiaBoundingBox();
        boundingBox3.addPoint(new Vector3d(minX, centerY, minZ));
        boundingBox3.addPoint(new Vector3d(centerX, maxY, maxZ));
        divideBoundingBox[3] = boundingBox3;
        return divideBoundingBox;
    }

    public BoundingVolume getBoundingVolume(GaiaBoundingBox boundingBox) {
        ProjCoordinate minPoint = new ProjCoordinate(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
        ProjCoordinate maxPoint = new ProjCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        ProjCoordinate translatedMinPoint = GlobeUtils.transform(null, minPoint);
        ProjCoordinate translatedMaxPoint = GlobeUtils.transform(null, maxPoint);

        BoundingVolume rootBoundingVolume = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        double[] rootRegion = new double[6];
        rootRegion[0] = Math.toRadians(translatedMinPoint.x);
        rootRegion[1] = Math.toRadians(translatedMinPoint.y);
        rootRegion[2] = Math.toRadians(translatedMaxPoint.x);
        rootRegion[3] = Math.toRadians(translatedMaxPoint.y);
        rootRegion[4] = boundingBox.getMinZ();
        rootRegion[5] = boundingBox.getMaxZ();
        rootBoundingVolume.setRegion(rootRegion);
        return rootBoundingVolume;
    }

    public GaiaBoundingBox getGlobalBoundingBox(List<GaiaScene> sceneList) {
        GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
        for (GaiaScene scene : sceneList) {
            GaiaBoundingBox localBoundingBox = scene.getBoundingBox();
            globalBoundingBox.addBoundingBox(localBoundingBox);
        }
        return globalBoundingBox;
    }

    private boolean tiling(Node parentNode, GaiaBoundingBox boundingBox, List<GaiaScene> scenes) {
        if (scenes.size() > MAX_COUNT) {
            GaiaBoundingBox[] childrenBoundingBox = divideBoundingBox(boundingBox);
            for (int i = 0; i < childrenBoundingBox.length; i++) {
                GaiaBoundingBox childBoundingBox = childrenBoundingBox[i];
                Node childNode = new Node();
                BoundingVolume rootBoundingVolume = getBoundingVolume(childBoundingBox);

                Matrix4d transformMatrix = new Matrix4d();
                transformMatrix.identity();
                float[] childTransform = transformMatrix.get(new float[16]);

                childNode.setTransform(childTransform);
                childNode.setNodeCode(parentNode.getNodeCode() + i);
                childNode.setGeometricError(276.41843f);
                childNode.setRefine(Node.RefineType.REPLACE);
                childNode.setBoundingVolume(rootBoundingVolume);
                childNode.setChildren(new ArrayList<>());
                List<GaiaScene> childScenes = containScenes(childBoundingBox, scenes);
                tiling(childNode, childBoundingBox, childScenes);
                parentNode.getChildren().add(childNode);
            }
        } else {
            if (scenes.size() > 0) {
                String name = parentNode.getNodeCode();
                GaiaUniverse universe = new GaiaUniverse(name, inputPath, outputPath);
                scenes.forEach((scene) -> {
                    universe.getScenes().add(scene);
                });
                Batched3DModel batched3DModel = new Batched3DModel(universe);
                batched3DModel.write(name);
                Content content = new Content();
                content.setUri(name + ".b3dm");
                parentNode.setContent(content);

                if (scenes.size() > 1) {
                    GaiaBoundingBox[] childrenBoundingBox = divideBoundingBox(boundingBox);
                    for (int i = 0; i < childrenBoundingBox.length; i++) {
                        GaiaBoundingBox childBoundingBox = childrenBoundingBox[i];
                        Node childNode = new Node();
                        BoundingVolume rootBoundingVolume = getBoundingVolume(childBoundingBox);
                        childNode.setNodeCode(parentNode.getNodeCode() + i);
                        childNode.setGeometricError(100.0f);
                        childNode.setRefine(Node.RefineType.REPLACE);
                        childNode.setBoundingVolume(rootBoundingVolume);
                        childNode.setChildren(new ArrayList<>());

                        float[] parentTransform = parentNode.getTransform();
                        Vector3d center = childBoundingBox.getCenter();
                        Matrix4d transformMatrix = new Matrix4d();
                        transformMatrix.identity();
                        float[] childTransform = transformMatrix.get(new float[16]);
                        childTransform[13] = parentTransform[13] - childTransform[13];
                        childTransform[14] = parentTransform[14] - childTransform[14];
                        childTransform[15] = parentTransform[15] - childTransform[15];

                        childNode.setTransform(childTransform);
                        List<GaiaScene> childScenes = containScenes(childBoundingBox, scenes);
                        boolean result = tiling(childNode, childBoundingBox, childScenes);
                        if (result) {
                            parentNode.getChildren().add(childNode);
                        }
                    }
                }
            } else {
                log.info("ended");
                return false;
            }
        }
        return true;
    }

    public void excute() {
        //System.setProperty("org.geotools.referencing.forceXY", "true");
        List<GaiaScene> sceneList = read(inputPath);
        GaiaBoundingBox globalBoundingBox = getGlobalBoundingBox(sceneList);

        Vector3d center = globalBoundingBox.getCenter();
        Matrix4d transfromMatrix = getTransfromMatrix(center);
        rotateX90(transfromMatrix);
        float[] transfrom = transfromMatrix.get(new float[16]);

        Node root = createRoot();
        root.setNodeCode("R");
        root.setGeometricError(276.41843f);
        root.setRefine(Node.RefineType.REPLACE);
        root.setChildren(new ArrayList<>());
        root.setBoundingVolume(getBoundingVolume(globalBoundingBox));
        root.setTransform(transfrom);
        //rootTest(root, globalBoundingBox);

        tiling(root, globalBoundingBox, sceneList);

        Tileset tileset = new Tileset();
        tileset.setGeometricError(276.41843f);
        tileset.setAsset(createAsset());
        tileset.setRoot(root);
        write(outputPath, tileset);
    }

    private void rotateX90(Matrix4d matrix) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(-90));
        matrix.mul(rotationMatrix, matrix);
    }

    private Matrix4d getTransfromMatrix(Vector3d center) {
        ProjCoordinate centerPoint = new ProjCoordinate(center.x(), center.y(), center.z());
        ProjCoordinate translatedCenterPoint = GlobeUtils.transform(null, centerPoint);
        double[] cartesian = GlobeUtils.geographicToCartesianWgs84(translatedCenterPoint.x, translatedCenterPoint.y, center.z());
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

    private void rootTest(Node root, GaiaBoundingBox boundingBox) {
        Node child = new Node();
        List<Node> children = new ArrayList<>();
        BoundingVolume boundingVolume = getBoundingVolume(boundingBox);
        child.setBoundingVolume(boundingVolume);

        Content content = new Content();
        content.setUri("result.b3dm");
        content.setBoundingVolume(boundingVolume);

        child.setContent(content);
        child.setGeometricError(276.41843f);
        child.setRefine(Node.RefineType.REPLACE);
        children.add(child);
        root.setChildren(children);
    }

    private Node createRoot() {
        Node root = new Node();
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
