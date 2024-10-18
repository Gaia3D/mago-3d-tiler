package com.gaia3d.processPhR.tileProcessPhR;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.DefaultTiler;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.asset.Asset;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class Batched3DModelTilerPhR extends DefaultTiler implements Tiler {
    public final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public Tileset run(List<TileInfo> tileInfos) throws FileNotFoundException {
        //**************************************************************
        // In photoRealistic, 1rst make a empty quadTree.
        // then use rectangleCakeCutter to fill the quadTree.
        //**************************************************************
        double geometricError = calcGeometricError(tileInfos);
        geometricError = DecimalUtils.cut(geometricError);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        Node root = createRoot();
        root.setDepth(0);
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(geometricError);
        double minLatLength = 200.0; // test value
        makeQuadTree(root, minLatLength);

        int lod = 0;
        try {
            cutRectangleCake(tileInfos, lod, root);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }


//        //Old**************************************************************
//        try {
//            createNode(root, tileInfos, 0);
//        } catch (IOException e) {
//            log.error("Error : {}", e.getMessage());
//            throw new RuntimeException(e);
//        }
//        //End Old.---------------------------------------------------------

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        return tileset;
    }

    private void cutRectangleCake(List<TileInfo> tileInfos, int lod, Node rootNode) throws FileNotFoundException {
        int maxDepth = rootNode.getMaxDepth();
        int currDepth = maxDepth - lod;

        // the maxDepth corresponds to lod0.***
        List<Node> nodes = new ArrayList<>();
        rootNode.getNodesByDepth(maxDepth, nodes);
        BoundingVolume boundingVolume = rootNode.getBoundingVolume();
        double minLonDeg = Math.toDegrees(boundingVolume.getRegion()[0]);
        double minLatDeg = Math.toDegrees(boundingVolume.getRegion()[1]);
        double maxLonDeg = Math.toDegrees(boundingVolume.getRegion()[2]);
        double maxLatDeg = Math.toDegrees(boundingVolume.getRegion()[3]);

//        List<Node> nodesByDepth = new ArrayList<>();
//        rootNode.getNodesByDepth(currDepth, nodesByDepth);

        double divisionsCount = Math.pow(2, currDepth);

        List<Double> lonDivisions = new ArrayList<>();
        List<Double> latDivisions = new ArrayList<>();

        double lonStep = (maxLonDeg - minLonDeg) / divisionsCount;
        double latStep = (maxLatDeg - minLatDeg) / divisionsCount;

        // exclude the first and last divisions, so i = 1 and i < divisionsCount.***
        for (int i = 1; i < divisionsCount; i++)
        {
            lonDivisions.add(minLonDeg + i * lonStep);
            latDivisions.add(minLatDeg + i * latStep);
        }

        // 1rst cut by latitudes.***
        int latitudesCount = latDivisions.size();
        for(int i = 0; i < latitudesCount; i++)
        {
            double latDeg = latDivisions.get(i);
            cutRectangleCakeByLatitudeDeg(tileInfos, lod, latDeg);
        }

        int longitudesCount = lonDivisions.size();

        int hola = 0;

    }

    private void cutRectangleCakeByLatitudeDeg(List<TileInfo> tileInfos, int lod, double latDeg) throws FileNotFoundException {
        int tileInfosCount = tileInfos.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            List<Path> paths = tileInfo.getTempPathLod();
            Path path = paths.get(lod);
            GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d geoCoordPosition = kmlInfo.getPosition();
            Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
            Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
            Matrix4d transformMatrixInv = new Matrix4d(transformMatrix);
            transformMatrixInv.invert();

            // load the file.***
            GaiaSet gaiaSet = GaiaSet.readFile(path);
            if(gaiaSet == null)
                continue;
            GaiaScene scene = new GaiaScene(gaiaSet);
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            // create a point with geoCoordPosition.x, latDeg, 0.0.***
            Vector3d samplePointGeoCoord = new Vector3d(geoCoordPosition.x, latDeg, 0.0);
            Vector3d samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            Vector3d samplePointLC = new Vector3d();
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);


            int hola = 0;
        }
    }

    public void getRenderTexture(GaiaScene scene)
    {
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        tilerExtensionModule.executePhotorealistic(scene, null);

    }

    private void createQuadTreeChildrenForNode(Node node)
    {
        if (node == null)
            return;

        BoundingVolume boundingVolume = node.getBoundingVolume();
        double[] region = boundingVolume.getRegion();
        double minLonDeg = Math.toDegrees(region[0]);
        double minLatDeg = Math.toDegrees(region[1]);
        double maxLonDeg = Math.toDegrees(region[2]);
        double maxLatDeg = Math.toDegrees(region[3]);
        double minAltitude = region[4];
        double maxAltitude = region[5];

        // must descend as quadtree.***
        double midLonDeg = (minLonDeg + maxLonDeg) / 2.0;
        double midLatDeg = (minLatDeg + maxLatDeg) / 2.0;

        double parentGeometricError = node.getGeometricError();
        double childGeometricError = parentGeometricError / 2.0;

        //
        List<Node> children = node.getChildren();
        if (children == null)
        {
            children = new ArrayList<>();
            node.setChildren(children);
        }

        //
        //        +------------+------------+
        //        |            |            |
        //        |     3      |     2      |
        //        |            |            |
        //        +------------+------------+
        //        |            |            |
        //        |     0      |     1      |
        //        |            |            |
        //        +------------+------------+


        // 0. left - down.***
        Node child0 = new Node();
        children.add(child0);
        child0.setParent(node);
        child0.setDepth(node.getDepth() + 1);
        child0.setGeometricError(childGeometricError);
        GaiaBoundingBox child0BoundingBox = new GaiaBoundingBox(minLonDeg, minLatDeg, minAltitude, midLonDeg, midLatDeg, maxAltitude, false);
        child0.setBoundingVolume(new BoundingVolume(child0BoundingBox));

        // 1. right - down.***
        Node child1 = new Node();
        children.add(child1);
        child1.setParent(node);
        child1.setDepth(node.getDepth() + 1);
        child1.setGeometricError(childGeometricError);
        GaiaBoundingBox child1BoundingBox = new GaiaBoundingBox(midLonDeg, minLatDeg, minAltitude, maxLonDeg, midLatDeg, maxAltitude, false);
        child1.setBoundingVolume(new BoundingVolume(child1BoundingBox));

        // 2. right - up.***
        Node child2 = new Node();
        children.add(child2);
        child2.setParent(node);
        child2.setDepth(node.getDepth() + 1);
        child2.setGeometricError(childGeometricError);
        GaiaBoundingBox child2BoundingBox = new GaiaBoundingBox(midLonDeg, midLatDeg, minAltitude, maxLonDeg, maxLatDeg, maxAltitude, false);
        child2.setBoundingVolume(new BoundingVolume(child2BoundingBox));

        // 3. left - up.***
        Node child3 = new Node();
        children.add(child3);
        child3.setParent(node);
        child3.setDepth(node.getDepth() + 1);
        child3.setGeometricError(childGeometricError);
        GaiaBoundingBox child3BoundingBox = new GaiaBoundingBox(minLonDeg, midLatDeg, minAltitude, midLonDeg, maxLatDeg, maxAltitude, false);
        child3.setBoundingVolume(new BoundingVolume(child3BoundingBox));

    }

    public void makeQuadTree(Node node, double minLatLength)
    {
        if (node == null)
            return;

        BoundingVolume boundingVolume = node.getBoundingVolume();
        double[] region = boundingVolume.getRegion();
        double minLatRad = region[1];
        double maxLatRad = region[3];
        double distanceBetweenLat = GlobeUtils.distanceBetweenLatitudesRad(minLatRad, maxLatRad);

        if(distanceBetweenLat < minLatLength)
        {
            return;
        }

        // must descend as quadtree.***
        createQuadTreeChildrenForNode(node);

        List<Node> children = node.getChildren();

        int childrenCount = children.size();
        for (int i = 0; i < childrenCount; i++)
        {
            Node child = children.get(i);
            makeQuadTree(child, minLatLength);
        }

    }

    public void splitMeshesByLod(List<TileInfo> tileInfos) {
        int minLod = globalOptions.getMinLod(); // most detailed level
        int maxLod = globalOptions.getMaxLod(); // least detailed level
    }

    public void writeTileset(Tileset tileset) {
        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        File tilesetFile = outputPath.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            log.info("[Tile][Tileset] write 'tileset.json' file.");
            writer.write(result);
            globalOptions.setTilesetSize(result.length());
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new TileProcessingException(e.getMessage());
        }
    }

    private void createNode(Node parentNode, List<TileInfo> tileInfos, int nodeDepth) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        BoundingVolume squareBoundingVolume = parentBoundingVolume.createSqureBoundingVolume();

        boolean refineAdd = globalOptions.isRefineAdd();
        long triangleLimit = globalOptions.getMaxTriangles();
        long totalTriangleCount = tileInfos.stream().mapToLong(TileInfo::getTriangleCount).sum();
        log.debug("[TriangleCount] Total : {}", totalTriangleCount);
        log.debug("[Tile][ContentNode][OBJECT] : {}", tileInfos.size());

        if (nodeDepth > globalOptions.getMaxNodeDepth()) {
            log.warn("[Tile] Node depth limit exceeded : {}", nodeDepth);
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
            }
            return;
        }

        if (tileInfos.size() <= 1) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos, nodeDepth + 1);
            }
        } else if (totalTriangleCount > triangleLimit) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createLogicalNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos, nodeDepth + 1);
                }
            }
        } else if (totalTriangleCount > 1) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);

                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    Content content = childNode.getContent();
                    if (content != null && refineAdd) {
                        ContentInfo contentInfo = content.getContentInfo();
                        createNode(childNode, contentInfo.getRemainTileInfos(), nodeDepth + 1);
                    } else {
                        createNode(childNode, childTileInfos, nodeDepth + 1);
                    }
                }
            }
        } else if (tileInfos.size() <= 4 || !tileInfos.isEmpty()) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos, nodeDepth + 1);
            }
        }
    }

    private Node createLogicalNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[Tile][LogicalNode][" + nodeCode + "][OBJECT{}]", tileInfos.size());

        double geometricError = calcGeometricError(tileInfos);
        GaiaBoundingBox boundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(boundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }
        BoundingVolume boundingVolume = new BoundingVolume(boundingBox);
        geometricError = DecimalUtils.cut(geometricError);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(geometricError);
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());
        return childNode;
    }

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        int minLevel = globalOptions.getMinLod();
        int maxLevel = globalOptions.getMaxLod();
        boolean refineAdd = globalOptions.isRefineAdd();

        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }
        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        String nodeCode = parentNode.getNodeCode();
        LevelOfDetail minLod = LevelOfDetail.getByLevel(minLevel);
        LevelOfDetail maxLod = LevelOfDetail.getByLevel(maxLevel);
        boolean hasContent = nodeCode.contains("C");
        if (!hasContent) {
            nodeCode = nodeCode + "C";
        }
        LevelOfDetail lod = getLodByNodeCode(minLod, maxLod, nodeCode);
        if (lod == LevelOfDetail.NONE) {
            return null;
        }
        nodeCode = nodeCode + index;
        log.info("[Tile][ContentNode][" + nodeCode + "][LOD{}][OBJECT{}]", lod.getLevel(), tileInfos.size());

        int lodError = refineAdd ? lod.getGeometricErrorBlock() : lod.getGeometricError();

        int lodErrorDouble = lodError;
        List<TileInfo> resultInfos;
        List<TileInfo> remainInfos;
        resultInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError >= lodErrorDouble;
        }).collect(Collectors.toList());
        remainInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError < lodErrorDouble;
        }).collect(Collectors.toList());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lodError + 0.1);
        childNode.setChildren(new ArrayList<>());

        childNode.setRefine(refineAdd ? Node.RefineType.ADD : Node.RefineType.REPLACE);
        if (!resultInfos.isEmpty()) {
            ContentInfo contentInfo = new ContentInfo();
            contentInfo.setName(nodeCode);
            contentInfo.setLod(lod);
            contentInfo.setBoundingBox(childBoundingBox);
            contentInfo.setNodeCode(nodeCode);
            contentInfo.setTileInfos(resultInfos);
            contentInfo.setRemainTileInfos(remainInfos);
            contentInfo.setTransformMatrix(transformMatrix);

            Content content = new Content();
            content.setUri("data/" + nodeCode + ".b3dm");
            content.setContentInfo(contentInfo);
            childNode.setContent(content);
        } else {
            log.debug("[Tile][ContentNode][{}] No Contents", nodeCode);
        }
        return childNode;
    }

    private LevelOfDetail getLodByNodeCode(LevelOfDetail minLod, LevelOfDetail maxLod, String nodeCode) {
        int minLevel = minLod.getLevel();
        int maxLevel = maxLod.getLevel();
        String[] splitCode = nodeCode.split("C");
        if (splitCode.length > 1) {
            String contentLevel = nodeCode.split("C")[1];
            int level = maxLevel - contentLevel.length();
            if (level < minLevel) {
                level = -1;
            }
            return LevelOfDetail.getByLevel(level);
        } else {
            return maxLod;
        }
    }
}
