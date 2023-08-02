package tiler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import command.KmlInfo;
import command.KmlReader;
import converter.AssimpConverter;
import converter.Converter;
import geometry.basic.GaiaBoundingBox;
import geometry.batch.GaiaTransfomer;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;
import org.joml.Vector3d;
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
public class Gaia3DTiler implements Tiler {
    private static final int DEFUALT_MAX_COUNT = 256;
    private final CommandLine command;
    private final Converter assimpConverter;

    private final TilerOptions options;

    public Gaia3DTiler(TilerOptions tilerOptions, CommandLine command) {
        this.options = tilerOptions;
        this.assimpConverter = new AssimpConverter(command);
        this.command = command;
    }

    public Tileset tile(List<TileInfo> tileInfos) {
        double geometricError = calcGeometricError(tileInfos);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransfromMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix);
        root.setGeometricError(geometricError);

        try {
            createNode(root, tileInfos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        return tileset;
    }

    public void writeTileset(Path output, Tileset tileset) {
        File tilesetFile = output.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            writer.write(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TileInfo> reloadScenes(List<TileInfo> tileInfos) {
        return tileInfos.stream().map((tileInfo) -> {
            TileInfo tileinfo = TileInfo.builder().kmlInfo(tileInfo.getKmlInfo()).scene(assimpConverter.load(tileInfo.getScene().getOriginalPath())).build();
            GaiaTransfomer.translate(options.getSource(), tileinfo);
            return tileinfo;
            /*return TileInfo.builder()
                    .kmlInfo(tileInfo.getKmlInfo())
                    .scene(assimpConverter.load(tileInfo.getScene().getOriginalPath()))
                    .build();*/
        }).collect(Collectors.toList());
    }

    private double calcGeometricError(List<TileInfo> tileInfos) {
        return tileInfos.stream().mapToDouble(tileInfo -> {
            GaiaBoundingBox boundingBox = tileInfo.getScene().getBoundingBox();
            double result = boundingBox.getLongestDistance();
            if (result > 1000.0d) {
                log.info(result + " is too long distance. check it please.");
            }
            return result;
        }).max().orElse(0.0d);
    }

    private GaiaBoundingBox calcBoundingBox(List<TileInfo> tileInfos) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        tileInfos.forEach(tileInfo -> {
            KmlInfo kmlInfo = tileInfo.getKmlInfo();

            if (kmlInfo == null) {
                log.info("test");
            }

            Vector3d position = kmlInfo.getPosition();
            GaiaBoundingBox localBoundingBox = tileInfo.getScene().getBoundingBox();
            localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(position);
            boundingBox.addBoundingBox(localBoundingBox);


            /*if (FormatType.KML == type) {
                KmlInfo kmlInfo = tileInfo.getKmlInfo();
                Vector3d position = kmlInfo.getPosition();
                GaiaBoundingBox localBoundingBox = tileInfo.getScene().getBoundingBox();
                localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(position);
                boundingBox.addBoundingBox(localBoundingBox);
            } else {
                boundingBox.addBoundingBox(tileInfo.getScene().getBoundingBox());
            }*/
        });
        return boundingBox;
    }

    private void createNode(Node parentNode, List<TileInfo> tileInfos) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        int maxCount = command.hasOption("maxCount") ? Integer.parseInt(command.getOptionValue("maxCount")) : DEFUALT_MAX_COUNT;
        if (tileInfos.size() > maxCount) {
            List<List<TileInfo>> childrenScenes = parentBoundingVolume.distributeScene(tileInfos);

            int test = 0;
            for (List<TileInfo> tileInfoList : childrenScenes) {
                test += tileInfoList.size();
            }

            if (test == tileInfos.size()) {
                log.info("test : " + test + ", scenes : " + tileInfos.size());
            }

            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createStructNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos);
                }
            }
        } else if (tileInfos.size() > 1) {
            int test = 0;
            List<List<TileInfo>> childrenScenes = parentBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = reloadScenes(childrenScenes.get(index));
                //List<TileInfo> childTileInfos = childrenScenes.get(index);

                test += childTileInfos.size();
                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos);
                }
            }
            if (test != tileInfos.size()) {
                log.info("test : " + test + ", scenes : " + tileInfos.size());
            }
        } else if (tileInfos.size() > 0) {
            Node childNode = createContentNode(parentNode, reloadScenes(tileInfos), 0);
            //Node childNode = createContentNode(parentNode, tileInfos, 0);

            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos);
            }
        }
    }

    private Node createStructNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.size() < 1) {
            return null;
        }
        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[StructNode ][" + nodeCode + "] : {}", tileInfos.size());

        double geometricError = calcGeometricError(tileInfos);
        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
        rotateX90(transformMatrix);

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(geometricError);
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());
        /*GaiaUniverse universe = new GaiaUniverse(nodeCode, tilerOptions.getInputPath(), tilerOptions.getOutputPath());
        tileInfos.forEach(tileInfo -> universe.getScenes().add(tileInfo.getScene()));*/
        return childNode;
    }

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.size() < 1) {
            return null;
        }

        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransfromMatrix(childBoundingBox);
        rotateX90(transformMatrix);

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        String nodeCode = parentNode.getNodeCode();
        LevelOfDetail lod = getLodByNodeCode(nodeCode);
        if (lod == LevelOfDetail.LOD3) {
            nodeCode = nodeCode + "C";
        } else if (lod == LevelOfDetail.NONE) {
            return null;
        }
        nodeCode = nodeCode + index;

        log.info("[ContentNode][" + nodeCode + "] : {}", tileInfos.size());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lod.getGeometricError());
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());

        GaiaUniverse universe = new GaiaUniverse(nodeCode, options.getInputPath(), options.getOutputPath());
        tileInfos.forEach(tileInfo -> universe.getScenes().add(tileInfo.getScene()));

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setLod(lod);
        contentInfo.setUniverse(universe);
        contentInfo.setTileInfos(tileInfos);
        contentInfo.setBoundingBox(childBoundingBox);
        contentInfo.setNodeCode(nodeCode);

        Content content = new Content();
        content.setUri(nodeCode + ".b3dm");
        content.setContentInfo(contentInfo);
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
        double[] cartesian = GlobeUtils.geographicToCartesianWgs84(center.x, center.y, center.z);
        return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);


        /*if (FormatType.KML == formatType) {
            double[] cartesian = GlobeUtils.geographicToCartesianWgs84(center.x, center.y, center.z);
            return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
        } else {
            Vector3d centerBottom = new Vector3d(center.x, center.y, boundingBox.getMinZ());
            ProjCoordinate centerPoint = new ProjCoordinate(centerBottom.x(), centerBottom.y(), centerBottom.z());
            ProjCoordinate translatedCenterPoint = GlobeUtils.transform(tilerOptions.getSource(), centerPoint);
            double[] cartesian = GlobeUtils.geographicToCartesianWgs84(translatedCenterPoint.x, translatedCenterPoint.y, centerBottom.z());
            return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
        }*/
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        Extras extras = new Extras();
        Cesium cesium = new Cesium();
        Ion ion = new Ion();
        List<Credit> credits = new ArrayList<>();
        Credit credit = new Credit();
        credit.setHtml("<html>TEST</html>");
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
}
