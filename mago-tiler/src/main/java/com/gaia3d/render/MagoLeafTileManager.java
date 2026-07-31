package com.gaia3d.render;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.geometry.modifier.topology.GaiaSceneCleaner;
import com.gaia3d.basic.geometry.modifier.topology.GaiaTriangulator;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.geometry.modifier.transform.GaiaBaker;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.texture.atlas.TextureAtlasManager;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageResizer;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MagoLeafTileManager {

    public void integralLeafScene(List<SceneInfo> sceneInfos, List<GaiaScene> resultGaiaScenes, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, String outputPathString, String nodeName, int lod) {
        try {
            Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
            nodeMatrixInv.invert();
            double weldError = 1e-6; // 1e-6 is a good value for remeshing
            int scenesCount = sceneInfos.size();

            GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder().error(weldError).checkTexCoord(false).checkNormal(false).checkColor(false).checkBatchId(false).build();

            List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
            for (int i = 0; i < scenesCount; i++) {
                // load and render, one by one
                SceneInfo sceneInfo = sceneInfos.get(i);
                String scenePath = sceneInfo.getScenePath();
                Matrix4d sceneTMat = sceneInfo.getTransformMatrix();

                // must find the local position of the scene rel to node
                Vector3d scenePosWC = new Vector3d(sceneTMat.m30(), sceneTMat.m31(), sceneTMat.m32());
                Vector3d scenePosLC = nodeMatrixInv.transformPosition(scenePosWC, new Vector3d());

                // calculate the local sceneTMat
                Matrix4d sceneTMatLC = new Matrix4d();
                sceneTMatLC.identity();
                sceneTMatLC.m30(scenePosLC.x);
                sceneTMatLC.m31(scenePosLC.y);
                sceneTMatLC.m32(scenePosLC.z);

                // load the set file
                GaiaSet gaiaSet = null;
                GaiaScene gaiaScene = null;

                Path path = Paths.get(scenePath);
                try {
                    gaiaSet = GaiaSet.readFile(path);
                    gaiaScene = new GaiaScene(gaiaSet);
                } catch (Exception e) {
                    log.error("[ERROR] reading the file: ", e);
                }

                if (gaiaScene == null) {
                    // throw error
                    throw new RuntimeException("[ERROR] integralReMeshByObliqueCamera : GaiaScene is null");
                }

                GaiaTriangulator triangulator = new GaiaTriangulator();
                triangulator.apply(gaiaScene);

                GaiaNode gaiaNode = gaiaScene.getNodes().getFirst();
                gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));

                GaiaBaker baker = new GaiaBaker();
                baker.apply(gaiaScene);

                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                halfEdgeScenes.add(halfEdgeScene);

                gaiaSet.clear();
                gaiaScene.clear();
            }

            atlasTextureForIntegralLeafScenes(halfEdgeScenes, resultGaiaScenes, outputPathString, nodeName);

            // delete halfEdgeScenes.
            for (HalfEdgeScene scene : halfEdgeScenes) {
                scene.deleteObjects();
            }
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }
    }

    private void atlasTextureForIntegralLeafScenes(List<HalfEdgeScene> halfEdgeScenes, List<GaiaScene> resultGaiaScenes, String outputPathString, String nodeName) {
        if (halfEdgeScenes == null || halfEdgeScenes.isEmpty()) {
            log.info("atlasTextureForIntegralLeafScenes: halfEdgeScenes is null or empty.");
            return;
        }

        List<GaiaTextureScissorDataFull> scissorDataFullList = new ArrayList<>();
        int classificationId = -1;
        int scissorExpandPixels = 2;
        int scenesCount = halfEdgeScenes.size();
        List<HalfEdgePrimitive> primitives = new ArrayList<>();
        GaiaAttribute gaiaAttribute = null;
        for (int i = 0; i < scenesCount; i++) {
            HalfEdgeScene scene = halfEdgeScenes.get(i);
            if (gaiaAttribute == null) {
                // Take the 1rst gaiaAttribute.
                gaiaAttribute = scene.getAttribute().getCopy();
            }
            primitives.clear();
            primitives = scene.extractPrimitives(primitives);
            int primitivesCount = primitives.size();
            for (int j = 0; j < primitivesCount; j++) {
                HalfEdgePrimitive primitive = primitives.get(j);
                int materialId = primitive.getMaterialIndex();
                GaiaMaterial material = scene.getMaterials().get(materialId);
                Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
                List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
                if (diffuseTextures == null || diffuseTextures.isEmpty()) {
                    continue;
                }
                GaiaTexture diffuseTexture = diffuseTextures.getFirst();
                BufferedImage bufferedImage = diffuseTexture.getBufferedImage();
                if (bufferedImage == null) {
                    continue;
                }
                classificationId += 1; // a classificationId for primitive.

                List<HalfEdgeSurface> surfaces = primitive.getSurfaces();
                int surfacesCount = surfaces.size();
                for (int k = 0; k < surfacesCount; k++) {
                    HalfEdgeSurface surface = surfaces.get(k);
                    List<List<HalfEdgeFace>> weldedFacesGroups = new ArrayList<>();
                    WeldedFacesFinder.getWeldedFacesGroups(surface, weldedFacesGroups);

                    int weldedFacesGroupsCount = weldedFacesGroups.size();
                    for (int l = 0; l < weldedFacesGroupsCount; l++) {
                        List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(l);
                        GaiaTextureScissorDataFull scissorDataFull = new GaiaTextureScissorDataFull();
                        scissorDataFull.setClassifyId(classificationId);
                        scissorDataFull.setFaces(weldedFacesGroup);
                        scissorDataFull.takeScissoredImageFromMotherImage(bufferedImage);
                        scissorDataFull.expandScissorImage(scissorExpandPixels);

                        if (scissorDataFull.getCurrentBoundary() == null) {
                            log.error("atlasTextureForIntegralLeafScenes: scissorDataFull.getCurrentBoundary() is null for classificationId: " + classificationId);
                            continue;
                        }

                        scissorDataFullList.add(scissorDataFull);
                    }
                }

                // delete BufferedImage.
                bufferedImage.flush();
                bufferedImage = null;
            }
        }

        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        textureAtlasManager.doAtlasTextureProcessByScissorDatesFull(scissorDataFullList);
        int bufferImageType = BufferedImage.TYPE_INT_ARGB;
        GaiaTexture atlasTexture = textureAtlasManager.makeAtlasTextureScissorDataFull(scissorDataFullList, bufferImageType);

        if (atlasTexture == null) {
            log.info("makeAtlasTexture() : atlasTexture is null.");
            return;
        }

        int atlasWidth = atlasTexture.getWidth();
        int atlasHeight = atlasTexture.getHeight();
        for (GaiaTextureScissorDataFull scissorDataFull : scissorDataFullList) {
            scissorDataFull.recalculateTexCoordsForAtlas(atlasWidth, atlasHeight);
        }

        List<HalfEdgeFace> allFaces = new ArrayList<>();
        for (GaiaTextureScissorDataFull scissorDataFull : scissorDataFullList) {
            List<HalfEdgeFace> faces = scissorDataFull.getFaces();
            allFaces.addAll(faces);
        }

        Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace = new HashMap<>();
        GaiaScene resultGaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeFaces(allFaces, mapGaiaFaceToHalfEdgeFace);
        resultGaiaScene.setAttribute(gaiaAttribute);

        // make the material with the atlasTexture.
        GaiaMaterial material = new GaiaMaterial();
        material.setName("atlasTextureMaterial");
        Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
        List<GaiaTexture> atlasTextures = new ArrayList<>();
        atlasTextures.add(atlasTexture);
        textures.put(TextureType.DIFFUSE, atlasTextures);
        material.setTextures(textures);
        material.setId(0);

        resultGaiaScene.getMaterials().clear();
        resultGaiaScene.getMaterials().add(material);

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> gaiaPrimitives = extractor.extractAllPrimitives(resultGaiaScene);
        int primitivesCount = gaiaPrimitives.size();
        for (int i = 0; i < primitivesCount; i++) {
            GaiaPrimitive primitive = gaiaPrimitives.get(i);
            primitive.setMaterialIndex(0);
        }

        // save atlas texture data**********************************************************************************
        String netTempPathString = outputPathString + File.separator + "temp" + File.separator + "reMeshTemp";
        Path netTempPath = Paths.get(netTempPathString);
        // create dirs if not exists
        File netTempFile = netTempPath.toFile();
        if (!netTempFile.exists() && netTempFile.mkdirs()) {
            log.debug("info : netTemp folder created.");
        }

        String netSetFolderPathString = netTempPathString + File.separator + nodeName;
        Path netSetFolderPath = Paths.get(netSetFolderPathString);
        // create dirs if not exists
        File netSetFile = netSetFolderPath.toFile();
        if (!netSetFile.exists() && netTempFile.mkdirs()) {
            log.debug("info : netSet folder created.");
        }
        String netSetImagesFolderPathString = netSetFolderPathString + File.separator + "images";
        Path netSetImagesFolderPath = Paths.get(netSetImagesFolderPathString);
        // create dirs if not exists
        File netSetImagesFolder = netSetImagesFolderPath.toFile();
        if (!netSetImagesFolder.exists() && netSetImagesFolder.mkdirs()) {
            log.debug("info : netSetImages folder created.");
        }

        String fileName = nodeName + "_Atlas";
        String extension = ".png";

        atlasTexture.setPath(fileName + extension);
        atlasTexture.setParentPath(netSetImagesFolderPath.toString());

        textures = material.getTextures();
        atlasTextures = textures.get(TextureType.DIFFUSE);
        GaiaTexture atlasScissoredTexture = atlasTextures.getFirst();
        atlasScissoredTexture.setParentPath(netSetImagesFolderPath.toString());

        // resize the atlas texture if necessary.
        int lod = 0;
        BufferedImage atlasBufferedImage = atlasScissoredTexture.getBufferedImage();
        if (atlasBufferedImage.getWidth() > 1024 || atlasBufferedImage.getHeight() > 1024) {
            BufferedImage resized = ImageResizer.resizeMultiStepSmart(atlasBufferedImage, lod);
            atlasBufferedImage.flush();
            atlasScissoredTexture.setBufferedImage(resized);
        }

        // save the atlas image to disk
        try {
            String imagePath = atlasScissoredTexture.getFullPath();
            File imageFile = new File(imagePath);
            ImageIO.write(atlasScissoredTexture.getBufferedImage(), "png", imageFile);
        } catch (IOException e) {
            log.debug("Error writing image: {}", e);
        }

        resultGaiaScenes.add(resultGaiaScene);

        // delete scissorDataFullList.
        for (GaiaTextureScissorDataFull scissorDataFull : scissorDataFullList) {
            scissorDataFull.clear();
        }
    }
}
