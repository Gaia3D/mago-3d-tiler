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
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
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
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class Batched3DModelTilerPhR extends DefaultTiler implements Tiler {
    public final GlobalOptions globalOptions = GlobalOptions.getInstance();

    //@Override
    public Tileset run_old(List<TileInfo> tileInfos) throws FileNotFoundException {
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

        //Old**************************************************************
        try {
            createNode(root, tileInfos, 0);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }
        //End Old.---------------------------------------------------------

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        return tileset;
    }

    @Override
    public Tileset run(List<TileInfo> tileInfos) throws FileNotFoundException {
        //**************************************************************
        // In photoRealistic, 1rst make a empty quadTree.
        // then use rectangleCakeCutter to fill the quadTree.
        //**************************************************************

        double geometricError = calcGeometricError(tileInfos);
        geometricError = DecimalUtils.cut(geometricError);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);

        // make globalBoundingBox as square.***
        double minLonDeg = globalBoundingBox.getMinX();
        double minLatDeg = globalBoundingBox.getMinY();
        double maxLonDeg = globalBoundingBox.getMaxX();
        double maxLatDeg = globalBoundingBox.getMaxY();

        GaiaBoundingBox cartographicBoundingBox = new GaiaBoundingBox(minLonDeg, minLatDeg, globalBoundingBox.getMinZ(), maxLonDeg, maxLatDeg, globalBoundingBox.getMaxZ(), false);

        double lonDegDiff = maxLonDeg - minLonDeg;
        double latDegDiff = maxLatDeg - minLatDeg;

        if(lonDegDiff > latDegDiff)
        {
            maxLatDeg = minLatDeg + lonDegDiff;
        }
        else if(latDegDiff > lonDegDiff)
        {
            maxLonDeg = minLonDeg + latDegDiff;
        }

        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        Node root = createRoot();
        root.setNodeCode("R");
        root.setDepth(0);
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(geometricError);

        double minLatLength = 300.0; // test value
        makeQuadTree(root, minLatLength);

        int lod = 0;
        List<TileInfo> tileInfosCopy = new ArrayList<>(tileInfos);
        int tileInfosCount = tileInfos.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            TileInfo tileInfoCopy = tileInfo.clone();

            // change the tempPath of the tileInfos by tempPathLod.***
            List<Path> tempPathLod = tileInfoCopy.getTempPathLod();
            Path pathLod = tempPathLod.get(lod);
            if(pathLod != null){
                tileInfoCopy.setTempPath(pathLod);
            }

            tileInfosCopy.add(tileInfoCopy);
        }

        try {
            cutRectangleCake(tileInfosCopy, lod, root);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        Map<Node, List<TileInfo>> nodeTileInfoMap = new HashMap<>();

        // distribute contents to node in the correspondent depth.***
        int maxDepth = root.findMaxDepth();
        int currDepth = maxDepth - lod;
        tileInfosCount = tileInfosCopy.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfosCopy.get(i);
            Matrix4d tileTransformMatrix = tileInfo.getTransformMatrix();
            GaiaBoundingBox tileBoundingBox = tileInfo.getBoundingBox();
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            Vector3d geoCoordCenter = cartographicBBox.getCenter();
            double centerLonRad = Math.toRadians(geoCoordCenter.x);
            double centerLatRad = Math.toRadians(geoCoordCenter.y);
            Vector2d tileInfoCenterGeoCoordRad = new Vector2d(centerLonRad, centerLatRad);

            Node childNode = root.getIntersectedNode(tileInfoCenterGeoCoordRad, currDepth);

            nodeTileInfoMap.computeIfAbsent(childNode, k -> new ArrayList<>()).add(tileInfo);
            List<TileInfo> tileInfosInNode = nodeTileInfoMap.get(childNode);
            tileInfosInNode.add(tileInfo);
        }

        scissorTextures(tileInfosCopy);
        makeContentsForNodes(nodeTileInfoMap, lod);
        // End lod 0.---------------------------------------------------------

        // make lod 1.---------------------------------------------------------
        lod = 1;
        tileInfosCopy.clear();
        tileInfosCount = tileInfos.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            TileInfo tileInfoCopy = tileInfo.clone();
            // change the tempPath of the tileInfos by tempPathLod.***
            List<Path> tempPathLod = tileInfoCopy.getTempPathLod();
            Path pathLod = tempPathLod.get(lod);
            if(pathLod != null){
                tileInfoCopy.setTempPath(pathLod);
            }
            tileInfosCopy.add(tileInfoCopy);
        }

        try {
            cutRectangleCake(tileInfosCopy, lod, root);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        nodeTileInfoMap.clear();

        // distribute contents to node in the correspondent depth.***
        currDepth = maxDepth - lod;
        tileInfosCount = tileInfosCopy.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfosCopy.get(i);
            Matrix4d tileTransformMatrix = tileInfo.getTransformMatrix();
            GaiaBoundingBox tileBoundingBox = tileInfo.getBoundingBox();
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            Vector3d geoCoordCenter = cartographicBBox.getCenter();
            double centerLonRad = Math.toRadians(geoCoordCenter.x);
            double centerLatRad = Math.toRadians(geoCoordCenter.y);
            Vector2d tileInfoCenterGeoCoordRad = new Vector2d(centerLonRad, centerLatRad);

            Node childNode = root.getIntersectedNode(tileInfoCenterGeoCoordRad, currDepth);

            nodeTileInfoMap.computeIfAbsent(childNode, k -> new ArrayList<>()).add(tileInfo);
            List<TileInfo> tileInfosInNode = nodeTileInfoMap.get(childNode);
            tileInfosInNode.add(tileInfo);
        }

        scissorTextures(tileInfosCopy);
        makeContentsForNodes(nodeTileInfoMap, lod);
        // End lod 1.---------------------------------------------------------

        // make lod 2.---------------------------------------------------------
        lod = 2;
        tileInfosCopy.clear();
        tileInfosCount = tileInfos.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            TileInfo tileInfoCopy = tileInfo.clone();
            // change the tempPath of the tileInfos by tempPathLod.***
            List<Path> tempPathLod = tileInfoCopy.getTempPathLod();
            Path pathLod = tempPathLod.get(lod);
            if(pathLod != null){
                tileInfoCopy.setTempPath(pathLod);
            }
            tileInfosCopy.add(tileInfoCopy);
        }

        try {
            cutRectangleCake(tileInfosCopy, lod, root);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        nodeTileInfoMap.clear();

        // distribute contents to node in the correspondent depth.***
        currDepth = maxDepth - lod;
        tileInfosCount = tileInfosCopy.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfosCopy.get(i);
            Matrix4d tileTransformMatrix = tileInfo.getTransformMatrix();
            GaiaBoundingBox tileBoundingBox = tileInfo.getBoundingBox();
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            Vector3d geoCoordCenter = cartographicBBox.getCenter();
            double centerLonRad = Math.toRadians(geoCoordCenter.x);
            double centerLatRad = Math.toRadians(geoCoordCenter.y);
            Vector2d tileInfoCenterGeoCoordRad = new Vector2d(centerLonRad, centerLatRad);

            Node childNode = root.getIntersectedNode(tileInfoCenterGeoCoordRad, currDepth);

            nodeTileInfoMap.computeIfAbsent(childNode, k -> new ArrayList<>()).add(tileInfo);
            List<TileInfo> tileInfosInNode = nodeTileInfoMap.get(childNode);
            tileInfosInNode.add(tileInfo);
        }

        // in lod2, we don't scissor textures.***
        // lod2 uses the rendered image.***
        makeContentsForNodes(nodeTileInfoMap, lod);
        // End lod 2.---------------------------------------------------------

        // now, delete nodes that have no contents.***
        root.deleteNoContentNodes();

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

    private void createUniqueTextureByRendering(TileInfo tileInfo, int lod)
    {
        Path path;
        if(tileInfo.getTempPathLod() != null) {
            List<Path> paths = tileInfo.getTempPathLod();
            path = paths.get(lod);
        }
        else {
            path = tileInfo.getTempPath();
        }

        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d geoCoordPosition = kmlInfo.getPosition();
        Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix);
        transformMatrixInv.invert();

        // load the file.***
        GaiaSet gaiaSet = null;
        try{
            gaiaSet = GaiaSet.readFile(path);
            if(gaiaSet == null)
                return;
        }
        catch (IOException e)
        {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

//        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
//        tilerExtensionModule.executePhotorealistic(scene, null);

    }

    private void scissorTextures(List<TileInfo> tileInfos)
    {
        int tileInfosCount = tileInfos.size();
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            Path path = tileInfo.getTempPath();

            // load the file.***
            try {
                GaiaSet gaiaSet = GaiaSet.readFile(path);
                GaiaScene scene = new GaiaScene(gaiaSet);
                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
                halfEdgeScene.scissorTextures();

                // once scene is scissored, must change the materials of the gaiaSet and overwrite the file.***
                GaiaScene scissorsScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
                gaiaSet = GaiaSet.fromGaiaScene(scissorsScene);

                // overwrite the file.***
                gaiaSet.writeFileInThePath(path);

                if (gaiaSet == null)
                    continue;
            }
            catch (IOException e)
            {
                log.error("Error : {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private void cutRectangleCake(List<TileInfo> tileInfos, int lod, Node rootNode) throws FileNotFoundException {
        int maxDepth = rootNode.findMaxDepth();
        int currDepth = maxDepth - lod;

        // the maxDepth corresponds to lod0.***
        List<Node> nodes = new ArrayList<>();
        rootNode.getNodesByDepth(maxDepth, nodes);
        BoundingVolume boundingVolume = rootNode.getBoundingVolume();
        double minLonDeg = Math.toDegrees(boundingVolume.getRegion()[0]);
        double minLatDeg = Math.toDegrees(boundingVolume.getRegion()[1]);
        double maxLonDeg = Math.toDegrees(boundingVolume.getRegion()[2]);
        double maxLatDeg = Math.toDegrees(boundingVolume.getRegion()[3]);

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

        int longitudesCount = lonDivisions.size();
        for(int i = 0; i < longitudesCount; i++)
        {
            double lonDeg = lonDivisions.get(i);
            cutRectangleCakeByLongitudeDeg(tileInfos, lod, lonDeg);
        }

        // 1rst cut by latitudes.***
        int latitudesCount = latDivisions.size();
        for(int i = 0; i < latitudesCount; i++)
        {
            double latDeg = latDivisions.get(i);
            cutRectangleCakeByLatitudeDeg(tileInfos, lod, latDeg);
        }

        int hola = 0;

    }

    private boolean cutHalfEdgeSceneByPlane(HalfEdgeScene halfEdgeScene, PlaneType planeType, Vector3d samplePointLC, TileInfo tileInfo, Path cutTempLodPath,
                                         List<TileInfo> cutTileInfos, double error)
    {
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d geoCoordPosition = kmlInfo.getPosition();
        Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix);
        transformMatrixInv.invert();

        if(halfEdgeScene.cutByPlane(planeType, samplePointLC, error))
        {
            // once scene is cut, then save the 2 scenes and delete the original.***
            halfEdgeScene.classifyFacesIdByPlane(planeType, samplePointLC);

            List<HalfEdgeScene> halfEdgeCutScenes = HalfEdgeUtils.getCopyHalfEdgeScenesByFaceClassifyId(halfEdgeScene, null);

            // create tileInfos for the cut scenes.***
            for(HalfEdgeScene halfEdgeCutScene : halfEdgeCutScenes)
            {
                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeCutScene);
                GaiaBoundingBox boundingBoxCutLC = gaiaSceneCut.getBoundingBox();

                // Calculate cartographicBoundingBox.***
                double minPosLCX = boundingBoxCutLC.getMinX();
                double minPosLCY = boundingBoxCutLC.getMinY();
                double minPosLCZ = boundingBoxCutLC.getMinZ();

                double maxPosLCX = boundingBoxCutLC.getMaxX();
                double maxPosLCY = boundingBoxCutLC.getMaxY();
                double maxPosLCZ = boundingBoxCutLC.getMaxZ();

                Vector3d leftDownBottomLC = new Vector3d(minPosLCX, minPosLCY, minPosLCZ);
                Vector3d rightDownBottomLC = new Vector3d(maxPosLCX, minPosLCY, minPosLCZ);
                Vector3d rightUpBottomLC = new Vector3d(maxPosLCX, maxPosLCY, minPosLCZ);

                Vector3d leftDownBottomWC = transformMatrix.transformPosition(leftDownBottomLC);
                Vector3d geoCoordLeftDownBottom = GlobeUtils.cartesianToGeographicWgs84(leftDownBottomWC);

                Vector3d rightDownBottomWC = transformMatrix.transformPosition(rightDownBottomLC);
                Vector3d geoCoordRightDownBottom = GlobeUtils.cartesianToGeographicWgs84(rightDownBottomWC);

                Vector3d rightUpBottomWC = transformMatrix.transformPosition(rightUpBottomLC);
                Vector3d geoCoordRightUpBottom = GlobeUtils.cartesianToGeographicWgs84(rightUpBottomWC);

                double minLonDegCut = geoCoordLeftDownBottom.x;
                double minLatDegCut = geoCoordLeftDownBottom.y;
                double maxLonDegCut = geoCoordRightDownBottom.x;
                double maxLatDegCut = geoCoordRightUpBottom.y;

                GaiaBoundingBox cartographicBoundingBox = new GaiaBoundingBox(minLonDegCut, minLatDegCut, boundingBoxCutLC.getMinZ(), maxLonDegCut, maxLatDegCut, boundingBoxCutLC.getMaxZ(), false);

                // create an originalPath for the cut scene.***
                Path cutScenePath = Paths.get("");
                gaiaSceneCut.setOriginalPath(cutScenePath);

                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);
                UUID identifier = UUID.randomUUID();
                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
                if(!gaiaSetCutFolderPath.toFile().exists())
                {
                    gaiaSetCutFolderPath.toFile().mkdirs();
                }

                Path tempPathLod = gaiaSetCut.writeFile(gaiaSetCutFolderPath);

                // create a new tileInfo for the cut scene.***
                TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(tileInfo.getOutputPath()).build();
                tileInfoCut.setTempPath(tempPathLod);
                Matrix4d transformMatrixCut = new Matrix4d(tileInfo.getTransformMatrix());
                tileInfoCut.setTransformMatrix(transformMatrixCut);
                tileInfoCut.setBoundingBox(boundingBoxCutLC);
                tileInfoCut.setCartographicBBox(cartographicBoundingBox);

                // make a kmlInfo for the cut scene.***
                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
                KmlInfo kmlInfoCut = KmlInfo.builder().position(geoCoordPosition).build();
                tileInfoCut.setKmlInfo(kmlInfoCut);
                cutTileInfos.add(tileInfoCut);
            }

            return true;

        }
        return false;
    }

    private void cutRectangleCakeByLongitudeDeg(List<TileInfo> tileInfos, int lod, double lonDeg) throws FileNotFoundException {
        log.info("lod : {}", lod);
        log.info(" #Cutting by longitude : {}", lonDeg);
        int tileInfosCount = tileInfos.size();
        PlaneType planeType = PlaneType.YZ;
        String outputPathString = globalOptions.getOutputPath();
        String cutTempPathString = outputPathString + File.separator + "cutTemp";
        Path cutTempPath = Paths.get(cutTempPathString);
        // create directory if not exists.***
        if(!cutTempPath.toFile().exists())
        {
            cutTempPath.toFile().mkdirs();
        }

        Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
        if(!cutTempLodPath.toFile().exists())
        {
            cutTempLodPath.toFile().mkdirs();
        }

        Map<TileInfo, TileInfo> deletedTileInfoMap = new HashMap<>();
        List<TileInfo> cutTileInfos = new ArrayList<>();
        double error = 1e-8;
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            if(deletedTileInfoMap.containsKey(tileInfo))
                continue;

            Path path;
            if(tileInfo.getTempPathLod() != null) {
                List<Path> paths = tileInfo.getTempPathLod();
                path = paths.get(lod);
            }
            else {
                path = tileInfo.getTempPath();
            }

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

            GaiaAttribute attribute = gaiaSet.getAttribute();

            GaiaScene scene = new GaiaScene(gaiaSet);
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            // create a point with lonDeg, geoCoordPosition.y, 0.0.***
            Vector3d samplePointGeoCoord = new Vector3d(lonDeg, geoCoordPosition.y, 0.0);
            Vector3d samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            Vector3d samplePointLC = new Vector3d();
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            if(this.cutHalfEdgeSceneByPlane(halfEdgeScene, planeType, samplePointLC, tileInfo, cutTempLodPath, cutTileInfos, error))
            {
                deletedTileInfoMap.put(tileInfo, tileInfo);
            }

            int hola = 0;
        }

        // remove from tileInfos the deleted tileInfos.***
        for(Map.Entry<TileInfo, TileInfo> entry : deletedTileInfoMap.entrySet())
        {
            // delete the temp folder of the tileInfo.***
            TileInfo tileInfo = entry.getKey();
            Path tempPath = tileInfo.getTempPath();
            Path tempPathFolder = tempPath.getParent();
            File tempPathFile = tempPathFolder.toFile();
            if(tempPathFile.exists())
            {
                tempPathFile.delete(); // no works. TODO: must delete the folder and all its contents.***
            }
            tileInfos.remove(entry.getKey());
        }

        // add the cutTileInfos to tileInfos.***
        tileInfos.addAll(cutTileInfos);

        int hola = 0;
    }

    private void cutRectangleCakeByLatitudeDeg(List<TileInfo> tileInfos, int lod, double latDeg) throws FileNotFoundException {
        log.info("lod : {}", lod);
        log.info(" #Cutting by latitude : {}", latDeg);
        int tileInfosCount = tileInfos.size();
        PlaneType planeType = PlaneType.XZ;
        String outputPathString = globalOptions.getOutputPath();
        String cutTempPathString = outputPathString + File.separator + "cutTemp";
        Path cutTempPath = Paths.get(cutTempPathString);
        // create directory if not exists.***
        if(!cutTempPath.toFile().exists())
        {
            cutTempPath.toFile().mkdirs();
        }

        Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
        if(!cutTempLodPath.toFile().exists())
        {
            cutTempLodPath.toFile().mkdirs();
        }

        Map<TileInfo, TileInfo> deletedTileInfoMap = new HashMap<>();
        List<TileInfo> cutTileInfos = new ArrayList<>();
        double error = 1e-8;
        for(int i = 0; i < tileInfosCount; i++)
        {
            TileInfo tileInfo = tileInfos.get(i);
            if(deletedTileInfoMap.containsKey(tileInfo))
                continue;
            Path path;
            if(tileInfo.getTempPathLod() != null) {
                List<Path> paths = tileInfo.getTempPathLod();
                path = paths.get(lod);
            }
            else {
                path = tileInfo.getTempPath();
            }

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

            GaiaAttribute attribute = gaiaSet.getAttribute();

            GaiaScene scene = new GaiaScene(gaiaSet);
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            // create a point with geoCoordPosition.x, latDeg, 0.0.***
            Vector3d samplePointGeoCoord = new Vector3d(geoCoordPosition.x, latDeg, 0.0);
            Vector3d samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            Vector3d samplePointLC = new Vector3d();
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            if(this.cutHalfEdgeSceneByPlane(halfEdgeScene, planeType, samplePointLC, tileInfo, cutTempLodPath, cutTileInfos, error))
            {
                deletedTileInfoMap.put(tileInfo, tileInfo);
            }


            int hola = 0;
        }

        // remove from tileInfos the deleted tileInfos.***
        for(Map.Entry<TileInfo, TileInfo> entry : deletedTileInfoMap.entrySet())
        {
            tileInfos.remove(entry.getKey());
        }

        // add the cutTileInfos to tileInfos.***
        tileInfos.addAll(cutTileInfos);
    }

    private void createQuadTreeChildrenForNode(Node node)
    {
        if (node == null)
            return;

        String parentNodeCode = node.getNodeCode();

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
        child0.setNodeCode(parentNodeCode + "0");

        // 1. right - down.***
        Node child1 = new Node();
        children.add(child1);
        child1.setParent(node);
        child1.setDepth(node.getDepth() + 1);
        child1.setGeometricError(childGeometricError);
        GaiaBoundingBox child1BoundingBox = new GaiaBoundingBox(midLonDeg, minLatDeg, minAltitude, maxLonDeg, midLatDeg, maxAltitude, false);
        child1.setBoundingVolume(new BoundingVolume(child1BoundingBox));
        child1.setNodeCode(parentNodeCode + "1");

        // 2. right - up.***
        Node child2 = new Node();
        children.add(child2);
        child2.setParent(node);
        child2.setDepth(node.getDepth() + 1);
        child2.setGeometricError(childGeometricError);
        GaiaBoundingBox child2BoundingBox = new GaiaBoundingBox(midLonDeg, midLatDeg, minAltitude, maxLonDeg, maxLatDeg, maxAltitude, false);
        child2.setBoundingVolume(new BoundingVolume(child2BoundingBox));
        child2.setNodeCode(parentNodeCode + "2");

        // 3. left - up.***
        Node child3 = new Node();
        children.add(child3);
        child3.setParent(node);
        child3.setDepth(node.getDepth() + 1);
        child3.setGeometricError(childGeometricError);
        GaiaBoundingBox child3BoundingBox = new GaiaBoundingBox(minLonDeg, midLatDeg, minAltitude, midLonDeg, maxLatDeg, maxAltitude, false);
        child3.setBoundingVolume(new BoundingVolume(child3BoundingBox));
        child3.setNodeCode(parentNodeCode + "3");

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

    private void makeContentsForNodes(Map<Node, List<TileInfo>> nodeTileInfoMap, int lod)
    {
        for(Map.Entry<Node, List<TileInfo>> entry : nodeTileInfoMap.entrySet())
        {
            Node childNode = entry.getKey();
            // Note : in each node, has NodeCode.***

            List<TileInfo> tileInfos = entry.getValue();

            GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
            Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
            if (globalOptions.isClassicTransformMatrix()) {
                rotateX90(transformMatrix);
            }
            BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);
            LevelOfDetail lodLevel = LevelOfDetail.getByLevel(lod);
            int lodError = lodLevel.getGeometricError();

            if(lod == 0)
            {
                lodError = 0;
            }
            else if(lod == 1)
            {
                lodError = 2;
            }
            else if(lod == 2)
            {
                lodError = 4;
            }

            childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
            childNode.setBoundingVolume(boundingVolume);
            childNode.setGeometricError(lodError + 0.1);

            childNode.setRefine(Node.RefineType.REPLACE);
            if (!tileInfos.isEmpty()) {
                ContentInfo contentInfo = new ContentInfo();
                String nodeCode = childNode.getNodeCode();
                contentInfo.setName(nodeCode);
                contentInfo.setLod(lodLevel);
                contentInfo.setBoundingBox(childBoundingBox);
                contentInfo.setNodeCode(nodeCode);
                contentInfo.setTileInfos(tileInfos);
                contentInfo.setTransformMatrix(transformMatrix);

                Content content = new Content();
                content.setUri("data/" + nodeCode + ".b3dm");
                content.setContentInfo(contentInfo);
                childNode.setContent(content);
            }
        }
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
        List<TileInfo> remainInfos; // small buildings, to add after as ADD.***
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
