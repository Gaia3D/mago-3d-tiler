package com.gaia3d.util;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaFaceData;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeCoordinate;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import org.joml.Vector2d;
import org.joml.Vector4d;

import java.util.ArrayList;
import java.util.List;

public class GaiaOctreeUtils {

    public static void getFaceDataListOfNode(GaiaScene sceneParent, GaiaNode node, List<GaiaFaceData> resultFaceDataList) {
        // 1rst, check meshes.
        if (node.getMeshes() != null) {
            for (int i = 0, length = node.getMeshes().size(); i < length; i++) {
                GaiaMesh mesh = node.getMeshes().get(i);
                if (mesh.getPrimitives() != null) {
                    for (int j = 0, primitivesLength = mesh.getPrimitives().size(); j < primitivesLength; j++) {
                        GaiaPrimitive primitive = mesh.getPrimitives().get(j);

                        // Get the material.
                        int matId = primitive.getMaterialIndex();
                        GaiaMaterial material = sceneParent.getMaterials().get(matId);
                        GaiaTexture diffuseTexture = null;
                        List<GaiaTexture> diffuseTexturesArray = material.getTextures().get(TextureType.DIFFUSE);
                        if (!diffuseTexturesArray.isEmpty()) {
                            diffuseTexture = diffuseTexturesArray.get(0);
                            diffuseTexture.loadImage();
                        }

                        if (primitive.getSurfaces() != null) {
                            for (int k = 0, surfacesLength = primitive.getSurfaces().size(); k < surfacesLength; k++) {
                                GaiaSurface surface = primitive.getSurfaces().get(k);
                                if (surface.getFaces() != null) {
                                    for (int m = 0, facesLength = surface.getFaces().size(); m < facesLength; m++) {
                                        GaiaFace face = surface.getFaces().get(m);

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

                                            GaiaVertex vertex0 = primitive.getVertices().get(index0);
                                            GaiaVertex vertex1 = primitive.getVertices().get(index1);
                                            GaiaVertex vertex2 = primitive.getVertices().get(index2);

                                            Vector2d texCoord0 = vertex0.getTexcoords();
                                            Vector2d texCoord1 = vertex1.getTexcoords();
                                            Vector2d texCoord2 = vertex2.getTexcoords();

                                            Vector4d averageColor = material.getDiffuseColor();

                                            if (texCoord0 != null && texCoord1 != null && texCoord2 != null) {

                                                Vector2d texCoordCenter = new Vector2d();
                                                texCoordCenter.add(texCoord0);
                                                texCoordCenter.add(texCoord1);
                                                texCoordCenter.add(texCoord2);
                                                texCoordCenter.mul(1.0 / 3.0);

                                                if (diffuseTexture != null) {
                                                    //averageColor = GaiaTextureUtils.getColorOfTexture(diffuseTexture, texCoordCenter);
                                                    averageColor = GaiaTextureUtils.getAverageColorOfTexture(diffuseTexture, texCoord0, texCoord1, texCoord2);
                                                    if (averageColor == null) {
                                                        averageColor = material.getDiffuseColor();
                                                    } else {
                                                        averageColor.x *= 2.0;
                                                        if (averageColor.x > 1.0) averageColor.x = 1.0;

                                                        averageColor.y *= 2.0;
                                                        if (averageColor.y > 1.0) averageColor.y = 1.0;

                                                        averageColor.z *= 2.0;
                                                        if (averageColor.z > 1.0) averageColor.z = 1.0;
                                                    }

                                                }
                                            }

                                            faceData.setSceneParent(sceneParent);
                                            faceData.setPrimitiveParent(primitive);
                                            faceData.setFace(face0);
                                            faceData.setPrimaryColor(averageColor);
                                            resultFaceDataList.add(faceData);
                                        }
                                    }
                                }
                            }
                        }
                        if (diffuseTexture != null) {
                            diffuseTexture.deleteObjects();
                        }
                    }
                }
            }
        }

        // now, check children.
        if (node.getChildren() != null) {
            for (int i = 0, length = node.getChildren().size(); i < length; i++) {
                GaiaNode child = node.getChildren().get(i);
                getFaceDataListOfNode(sceneParent, child, resultFaceDataList);
            }
        }
    }

    public static void getFaceDataListOfScene(GaiaScene gaiaScene, List<GaiaFaceData> resultFaceDataList) {
        for (GaiaNode node : gaiaScene.getNodes()) {
            getFaceDataListOfNode(gaiaScene, node, resultFaceDataList);
        }
    }

    public static GaiaOctree getSceneOctree(GaiaScene gaiaScene, float octreeMinSize) {
        List<GaiaFaceData> faceDataList = new ArrayList<>();
        getFaceDataListOfScene(gaiaScene, faceDataList);

        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        GaiaOctree octree = new GaiaOctree(null);
        octree.setSize(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        octree.setAsCube();
        double size = octree.getMaxX() - octree.getMinX();
        int i = 0;
        while (size > octreeMinSize) {
            size /= 2.0;
            i++;
        }

        int maxDepth = i;

        octree.addFaceDataList(faceDataList);
        octree.setMaxDepth(maxDepth);

        //octree.recalculateSize();
        octree.makeTree(octreeMinSize);

        return octree;
    }

    public static int getOctreeIndex(GaiaOctreeCoordinate coordinate) {
        // children indices.
        // down                         up
        // +---------+---------+        +---------+---------+
        // |         |         |        |         |         |
        // |    3    |    2    |        |    7    |    6    |
        // |         |         |        |         |         |
        // +---------+---------+        +---------+---------+
        // |         |         |        |         |         |
        // |    0    |    1    |        |    4    |    5    |
        // |         |         |        |         |         |
        // +---------+---------+        +---------+---------+

        GaiaOctreeCoordinate parentCoord = coordinate.getParentCoord();
        if (parentCoord == null) {
            return 0;
        }

        int x = coordinate.getX();
        int y = coordinate.getY();
        int z = coordinate.getZ();

        int parentX = parentCoord.getX();
        int parentY = parentCoord.getY();
        int parentZ = parentCoord.getZ();

        int originX = parentX * 2;
        int originY = parentY * 2;
        int originZ = parentZ * 2;

        int difX = x - originX;
        int difY = y - originY;
        int difZ = z - originZ;

        int index = 0;
        if (difX > 0) {
            // 1, 2, 5, 6
            if (difY > 0) {
                // 2, 6
                if (difZ > 0) {
                    // 6
                    index = 6;
                } else {
                    // 2
                    index = 2;
                }
            } else {
                // 1, 5
                if (difZ > 0) {
                    // 5
                    index = 5;
                } else {
                    // 1
                    index = 1;
                }
            }
        } else {
            // 0, 3, 4, 7
            if (difY > 0) {
                // 3, 7
                if (difZ > 0) {
                    // 7
                    index = 7;
                } else {
                    // 3
                    index = 3;
                }
            } else {
                // 0, 4
                if (difZ > 0) {
                    // 4
                    index = 4;
                } else {
                    // 0
                    index = 0;
                }
            }
        }

        return index;
    }
}
