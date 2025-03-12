package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GeometryUtils;
import org.apache.commons.io.FilenameUtils;
import org.joml.Vector2d;

import java.nio.file.Path;
import java.util.*;

public class GaiaTexCoordCorrector implements PreProcess {

    private GaiaScene recentScene = null;

    @Override
    public synchronized TileInfo run(TileInfo tileInfo) {
        GaiaScene gaiaScene = tileInfo.getScene();

        if (recentScene == gaiaScene) {
            return tileInfo;
        }
        recentScene = gaiaScene;

        FormatType formatType = getFormatTypeFromScene(gaiaScene);
        boolean invertTexCoordsYAxis = isInvertTexCoordsYAxis(formatType);
        GaiaNode rootNode = gaiaScene.getNodes().get(0);

        List<GaiaMesh> allMeshes = new ArrayList<>();
        rootNode.extractMeshes(allMeshes);

        for (GaiaMesh mesh : allMeshes) {
            List<GaiaPrimitive> allPrimitives = mesh.getPrimitives();
            for (GaiaPrimitive primitive : allPrimitives) {
                int matIdx = primitive.getMaterialIndex();
                GaiaMaterial material = gaiaScene.getMaterials().get(matIdx);
                translatePrimitiveTexCoordsToPositiveQuadrant(primitive);
            }
        }

        if (invertTexCoordsYAxis) {
            for (GaiaMesh mesh : allMeshes) {
                List<GaiaPrimitive> allPrimitives = mesh.getPrimitives();
                for (GaiaPrimitive primitive : allPrimitives) {
                    invertTexCoordsYAxis(primitive);
                }
            }
        }

        return tileInfo;
    }

    private FormatType getFormatTypeFromScene(GaiaScene gaiaScene) {
        Path scenePath = gaiaScene.getOriginalPath();
        String extension = FilenameUtils.getExtension(scenePath.toString());
        return FormatType.fromExtension(extension);
    }

    private boolean isInvertTexCoordsYAxis(FormatType formatType) {
        // formatType == FormatType.FBX || (it seems that this is not necessary)
        return formatType == FormatType.MAX_3DS || formatType == FormatType.FBX || formatType == FormatType.COLLADA || formatType == FormatType.OBJ || formatType == FormatType.GLTF || formatType == FormatType.GLB;
    }

    private void invertTexCoordsYAxis(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        for (GaiaVertex vertex : vertices) {
            Vector2d texCoord = vertex.getTexcoords();
            texCoord.y = 1.0 - texCoord.y;
        }
    }

    private void translatePrimitiveTexCoordsToPositiveQuadrant(GaiaPrimitive primitive) {
        // 1rst, check the texCoords's bounding rectangle's size.
        GaiaRectangle texcoordBoundingRectangle = primitive.getTexcoordBoundingRectangle(null);

        double tBoxWidth = texcoordBoundingRectangle.getWidth();
        double tBoxHeight = texcoordBoundingRectangle.getHeight();
        double texCoordOriginX = texcoordBoundingRectangle.getMinX();
        double texCoordOriginY = texcoordBoundingRectangle.getMinY();

        if (tBoxWidth > 1.0 || tBoxHeight > 1.0) {
            // If the bRect's width or height is bigger than 1.0, is possible that the primitive uses repeat texture mode. BUT:
            // must find welded triangles & translate the texCoords to the 1rst quadrant.
            int surfacesCount = primitive.getSurfaces().size();
            for (int i = 0; i < surfacesCount; i++) {
                GaiaSurface surface = primitive.getSurfaces().get(i);
                translateSurfaceTexCoordsToPositiveQuadrant(surface, primitive.getVertices());
            }
        } else {
            if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0 || texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                double offsetX = 0.0;
                double offsetY = 0.0;
                if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0) {
                    offsetX = Math.round(texCoordOriginX);
                }

                if (texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                    offsetY = Math.round(texCoordOriginY);
                }

                List<GaiaVertex> vertices = primitive.getVertices();
                for (GaiaVertex vertex : vertices) {
                    Vector2d texCoord = vertex.getTexcoords();
                    texCoord.x = texCoord.x - offsetX;
                    texCoord.y = texCoord.y - offsetY;
                }
            }
        }
    }

    private void translateSurfaceTexCoordsToPositiveQuadrant(GaiaSurface surface, List<GaiaVertex> vertices) {
        List<List<GaiaFace>> resultWeldedFaces = new ArrayList<>();
        surface.getWeldedFaces(resultWeldedFaces);

        for (List<GaiaFace> groupFaces : resultWeldedFaces) {
            GaiaRectangle texCoordRectangle = new GaiaRectangle();
            GeometryUtils.getTexCoordsBoundingRectangleOfFaces(groupFaces, vertices, texCoordRectangle);

            double boxWidth = texCoordRectangle.getWidth();
            double boxHeight = texCoordRectangle.getHeight();

            if (boxWidth > 1.0 || boxHeight > 1.0) {
                // in this case do nothing, bcos this is a repeat texture mode
                break;
            }

            // check if texCoords must be translated
            double texCoordOriginX = texCoordRectangle.getMinX();
            double texCoordOriginY = texCoordRectangle.getMinY();
            double offsetX = 0.0;
            double offsetY = 0.0;
            boolean mustTranslate = false;
            double error = 1e-4;
            if (texCoordOriginX < 0.0 - error || texCoordOriginX > 1.0) {
                offsetX = Math.floor(texCoordOriginX);
                mustTranslate = true;

                if (offsetX + boxWidth > 1.0 + error) {
                    // cannot translate
                    break;
                }
            }

            if (texCoordOriginY < 0.0 - error || texCoordOriginY > 1.0) {
                offsetY = Math.floor(texCoordOriginY);
                mustTranslate = true;

                if (offsetY + boxHeight > 1.0 + error) {
                    // cannot translate
                    break;
                }
            }

            if (mustTranslate) {
                Map<Integer, Integer> mapIndices = new HashMap<>();
                for (GaiaFace face : groupFaces) {
                    int[] indices = face.getIndices();
                    for (int index : indices) {
                        mapIndices.put(index, index);
                    }
                }

                // loop mapIndices
                for (Integer index : mapIndices.keySet()) {
                    GaiaVertex vertex = vertices.get(index);
                    Vector2d texCoord = vertex.getTexcoords();

                    texCoord.x = texCoord.x - offsetX;
                    texCoord.y = texCoord.y - offsetY;
                }
            }
        }
    }
}
