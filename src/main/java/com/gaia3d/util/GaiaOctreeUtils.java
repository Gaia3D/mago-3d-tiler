package com.gaia3d.util;

import com.gaia3d.basic.geometry.octree.GaiaFaceData;
import com.gaia3d.basic.model.*;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class GaiaOctreeUtils {

    public static void getFaceDataListOfNode(GaiaScene sceneParent, GaiaNode node, List<GaiaFaceData> resultFaceDataList) {
        // 1rst, check meshes.
        if (node.getMeshes() != null) {
            for (int i = 0, length = node.getMeshes()
                    .size(); i < length; i++) {
                GaiaMesh mesh = node.getMeshes()
                        .get(i);
                if (mesh.getPrimitives() != null) {
                    for (int j = 0, primitivesLength = mesh.getPrimitives()
                            .size(); j < primitivesLength; j++) {
                        GaiaPrimitive primitive = mesh.getPrimitives()
                                .get(j);

//                        // Get the material.
//                        int matId = primitive.getMaterialIndex();
//                        GaiaMaterial material = sceneParent.getMaterials().get(matId);
//                        GaiaTexture diffuseTexture = null;
//                        List<GaiaTexture> diffuseTexturesArray = material.getTextures().get(TextureType.DIFFUSE);
//                        if (!diffuseTexturesArray.isEmpty()) {
//                            diffuseTexture = diffuseTexturesArray.get(0);
//                            diffuseTexture.loadImage();
//                        }

                        List<GaiaVertex> primitiveVertices = primitive.getVertices();
                        if (primitive.getSurfaces() != null) {
                            for (int k = 0, surfacesLength = primitive.getSurfaces()
                                    .size(); k < surfacesLength; k++) {
                                GaiaSurface surface = primitive.getSurfaces()
                                        .get(k);
                                if (surface.getFaces() != null) {
                                    for (int m = 0, facesLength = surface.getFaces()
                                            .size(); m < facesLength; m++) {
                                        GaiaFace face = surface.getFaces()
                                                .get(m);

                                        int indicesCount = face.getIndices().length;
                                        int[] indices = face.getIndices();
                                        int triangleCount = indicesCount / 3;
                                        for (int n = 0; n < triangleCount; n++) {
                                            int index0 = indices[n * 3];
                                            int index1 = indices[n * 3 + 1];
                                            int index2 = indices[n * 3 + 2];

                                            GaiaFaceData faceData = new GaiaFaceData();
                                            GaiaFace face0 = new GaiaFace();
                                            face0.setIndices(new int[]{index0, index1, index2});

                                            GaiaVertex vertex0 = primitiveVertices.get(index0);
                                            GaiaVertex vertex1 = primitiveVertices.get(index1);
                                            GaiaVertex vertex2 = primitiveVertices.get(index2);

                                            // check if the positions are valid.
                                            Vector3d position0 = vertex0.getPosition();
                                            Vector3d position1 = vertex1.getPosition();
                                            Vector3d position2 = vertex2.getPosition();
                                            if(Double.isNaN(position0.x) || Double.isNaN(position0.y) || Double.isNaN(position0.z) ||
                                               Double.isNaN(position1.x) || Double.isNaN(position1.y) || Double.isNaN(position1.z) ||
                                               Double.isNaN(position2.x) || Double.isNaN(position2.y) || Double.isNaN(position2.z)) {
                                                continue;
                                            }

                                            Vector2d texCoord0 = vertex0.getTexcoords();
                                            Vector2d texCoord1 = vertex1.getTexcoords();
                                            Vector2d texCoord2 = vertex2.getTexcoords();

//                                            Vector4d averageColor = material.getDiffuseColor();
//
//                                            if (texCoord0 != null && texCoord1 != null && texCoord2 != null) {
//
//                                                Vector2d texCoordCenter = new Vector2d();
//                                                texCoordCenter.add(texCoord0);
//                                                texCoordCenter.add(texCoord1);
//                                                texCoordCenter.add(texCoord2);
//                                                texCoordCenter.mul(1.0 / 3.0);
//
//                                                if (diffuseTexture != null) {
//                                                    //averageColor = GaiaTextureUtils.getColorOfTexture(diffuseTexture, texCoordCenter);
//                                                    averageColor = GaiaTextureUtils.getAverageColorOfTexture(diffuseTexture, texCoord0, texCoord1, texCoord2);
//                                                    if (averageColor == null) {
//                                                        averageColor = material.getDiffuseColor();
//                                                    } else {
//                                                        averageColor.x *= 2.0;
//                                                        if (averageColor.x > 1.0) averageColor.x = 1.0;
//
//                                                        averageColor.y *= 2.0;
//                                                        if (averageColor.y > 1.0) averageColor.y = 1.0;
//
//                                                        averageColor.z *= 2.0;
//                                                        if (averageColor.z > 1.0) averageColor.z = 1.0;
//                                                    }
//
//                                                }
//                                            }

                                            faceData.setSceneParent(sceneParent);
                                            faceData.setPrimitiveParent(primitive);
                                            faceData.setFace(face0);
                                            //faceData.setPrimaryColor(averageColor);
                                            resultFaceDataList.add(faceData);
                                        }
                                    }
                                }
                            }
                        }
//                        if (diffuseTexture != null) {
//                            diffuseTexture.deleteObjects();
//                        }
                    }
                }
            }
        }

        // now, check children.
        if (node.getChildren() != null) {
            for (int i = 0, length = node.getChildren()
                    .size(); i < length; i++) {
                GaiaNode child = node.getChildren()
                        .get(i);
                getFaceDataListOfNode(sceneParent, child, resultFaceDataList);
            }
        }
    }

    public static void getFaceDataListOfScene(GaiaScene gaiaScene, List<GaiaFaceData> resultFaceDataList) {
        for (GaiaNode node : gaiaScene.getNodes()) {
            getFaceDataListOfNode(gaiaScene, node, resultFaceDataList);
        }
    }
}
