package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GeometryUtils;
import org.apache.commons.io.FilenameUtils;
import org.joml.Vector2d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaTexCoordCorrector implements PreProcess {

    private boolean usedFlag = false;

    @Override
    public synchronized TileInfo run(TileInfo tileInfo) {
        boolean checkI3dm = tileInfo.isI3dm() && usedFlag;
        if (checkI3dm) {
            return tileInfo;
        }
        usedFlag = true;

        GaiaScene gaiaScene = tileInfo.getScene();
        Path originalFilePath = gaiaScene.getOriginalPath();
        String fileExtension = FilenameUtils.getExtension(String.valueOf(originalFilePath.getFileName()));
        FormatType formatType = FormatType.fromExtension(fileExtension);

        boolean invertTexCoordsYAxis = false;
        if (formatType == FormatType.MAX_3DS) {
            invertTexCoordsYAxis = true;
        } else if (formatType == FormatType.COLLADA) {
            invertTexCoordsYAxis = true;
        } else if (formatType == FormatType.GLTF || formatType == FormatType.GLB) {
            invertTexCoordsYAxis = true;
        }

        GaiaNode rootNode = gaiaScene.getNodes().get(0);

        List<GaiaMesh> allMeshes = new ArrayList<>();
        rootNode.extractMeshes(allMeshes);

        for (GaiaMesh mesh : allMeshes) {
            List<GaiaPrimitive> allPrimitives = mesh.getPrimitives();
            for (GaiaPrimitive primitive : allPrimitives) {
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
        boolean mustTranslateTexCoordsToPositiveQuadrant = false;

        if (tBoxWidth > 1.0 || tBoxHeight > 1.0) {
            mustTranslateTexCoordsToPositiveQuadrant = true;
        } else {
            if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0) {
                mustTranslateTexCoordsToPositiveQuadrant = true;
            }

            if (texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                mustTranslateTexCoordsToPositiveQuadrant = true;
            }
        }

        if (mustTranslateTexCoordsToPositiveQuadrant) {
            // must find welded triangles & translate the texCoords to the 1rst quadrant.
            int surfacesCount = primitive.getSurfaces().size();
            for (int i = 0; i < surfacesCount; i++) {
                GaiaSurface surface = primitive.getSurfaces().get(i);
                translateSurfaceTexCoordsToPositiveQuadrant(surface, primitive.getVertices());
            }
        }

    }

    private void translateSurfaceTexCoordsToPositiveQuadrant(GaiaSurface surface, List<GaiaVertex> vertices) {
        List<List<GaiaFace>> resultWeldedFaces = new ArrayList<>();
        surface.getWeldedFaces(resultWeldedFaces);

        int groupsCount = resultWeldedFaces.size();
        for (int i = 0; i < groupsCount; i++) {
            List<GaiaFace> groupFaces = resultWeldedFaces.get(i);

            GaiaRectangle texCoordRectangle = new GaiaRectangle();
            GeometryUtils.getTexCoordsBoundingRectangleOfFaces(groupFaces, vertices, texCoordRectangle);

            double boxWidth = texCoordRectangle.getWidth();
            double boxHeight = texCoordRectangle.getHeight();

            if (boxWidth > 1.0 || boxHeight > 1.0) {
                // in this case do nothing, bcos this is a repeat texture mode.***
                continue;
            }

            // check if texCoords must be translated.***
            double texCoordOriginX = texCoordRectangle.getMinX();
            double texCoordOriginY = texCoordRectangle.getMinY();
            double offsetX = 0.0;
            double offsetY = 0.0;
            boolean mustTranslate = false;
            if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0) {
                offsetX = Math.floor(texCoordOriginX);
                mustTranslate = true;
            }

            if (texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                offsetY = Math.floor(texCoordOriginY);
                mustTranslate = true;
            }

            if (mustTranslate) {
                Map<Integer, Integer> mapIndices = new HashMap<>();
                for (GaiaFace face : groupFaces) {
                    int[] indices = face.getIndices();
                    for (int index : indices) {
                        mapIndices.put(index, index);
                    }
                }

                // loop mapIndices.***
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
