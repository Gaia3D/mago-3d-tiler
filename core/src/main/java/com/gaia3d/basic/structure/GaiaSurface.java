package com.gaia3d.basic.structure;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.util.GeometryUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Vector2d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that represents a face of a Gaia object.
 * It contains the indices and the face normal.
 * The face normal is calculated by the indices and the vertices.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Face_normal">Face normal</a>
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSurface implements Serializable {
    private ArrayList<GaiaFace> faces = new ArrayList<>();

    public void calculateNormal(List<GaiaVertex> vertices) {
        for (GaiaFace face : faces) {
            face.calculateFaceNormal(vertices);
        }
    }

    public int[] getIndices() {
        int index = 0;
        int indicesCount = getIndicesCount();
        int[] resultIndices = new int[indicesCount];
        for (GaiaFace face : faces) {
            for (int indices : face.getIndices()) {
                resultIndices[index++] = indices;
            }
        }
        return resultIndices;
    }

    public int getIndicesCount() {
        int count = 0;
        for (GaiaFace face : faces) {
            count += face.getIndices().length;
        }
        return count;
    }

    public void clear() {
        faces.forEach(GaiaFace::clear);
        faces.clear();
    }

    private boolean getFacesWeldedWithFace(GaiaFace face, List<GaiaFace> resultFaces) {
        boolean newFaceAdded = false;
        for (GaiaFace f : faces) {
            if (f == face) {
                continue;
            }
            if (f.hasCoincidentIndices(face)) {
                resultFaces.add(f);
                newFaceAdded = true;
            }
        }
        return newFaceAdded;
    }

    public boolean getFacesWeldedWithFaces(List<GaiaFace> faces, List<GaiaFace> resultFaces) {
        boolean newFaceAdded = false;
        List<GaiaFace> resultFacesTemp = new ArrayList<>();
        List<GaiaFace> masterFaces = new ArrayList<>();
        masterFaces.addAll(faces);
        Map<GaiaFace, GaiaFace> mapVisitedFaces = new HashMap<>();
        int masterFacesCount = faces.size();
        int i=0;
        boolean finished = false;
        while(!finished)
        {
            GaiaFace masterFace = masterFaces.get(i);
            mapVisitedFaces.put(masterFace, masterFace);

            resultFacesTemp.clear();
            if(this.getFacesWeldedWithFace(masterFace, resultFacesTemp))
            {
                int addedFacesCount = resultFacesTemp.size();
                for(int j=0; j<addedFacesCount; j++)
                {
                    GaiaFace addedFace = resultFacesTemp.get(j);
                    if(!mapVisitedFaces.containsKey(addedFace))
                    {
                        resultFaces.add(addedFace);
                        mapVisitedFaces.put(addedFace, addedFace);
                        newFaceAdded = true;

                        // reset the loop.***
                        i = 0;
                        masterFaces.add(addedFace);
                        masterFacesCount = masterFaces.size();
                    }
                }
            }

            i++;
            if(i>=masterFacesCount)
            {
                finished = true;
            }
        }

        return newFaceAdded;
    }

    public void translateTexCoordsToPositiveQuadrant(List<GaiaVertex> vertices)
    {
        // this function is used to translate the texture coordinates to the positive quadrant.***
        List<GaiaFace> weldedFaces = new ArrayList<>();
        List<GaiaFace> masterFaces = new ArrayList<>();
        Map<GaiaFace, GaiaFace> mapVisitedFaces = new HashMap<>();
        List<GaiaVertex> verticesAux = new ArrayList<>();
        int facesSize = faces.size();
        for(int i=0; i<facesSize; i++)
        {
            GaiaFace masterFace = faces.get(i);
            if(mapVisitedFaces.containsKey(masterFace))
            {
                continue;
            }
            mapVisitedFaces.put(masterFace, masterFace);

            masterFaces.clear();
            masterFaces.add(masterFace);

            weldedFaces.clear();
            if(this.getFacesWeldedWithFaces(masterFaces, weldedFaces))
            {
                masterFaces.addAll(weldedFaces);
                int weldedFacesCount = weldedFaces.size();
                for(int j=0; j<weldedFacesCount; j++)
                {
                    GaiaFace weldedFace = weldedFaces.get(j);
                    mapVisitedFaces.put(weldedFace, weldedFace);
                }
            }

            // extract all vertices from the masterFaces.***
            Map<Integer, Integer> mapIndices = new HashMap<>();
            for(GaiaFace face : masterFaces)
            {
                int[] indices = face.getIndices();
                for(int index : indices)
                {
                    mapIndices.put(index, index);
                }
            }

            verticesAux.clear();
            // loop mapIndices.***
            for(Integer index : mapIndices.keySet())
            {
                GaiaVertex vertex = vertices.get(index);
                verticesAux.add(vertex);
            }


            // calculate the texCoordBounds of the welded faces.***
            GaiaRectangle texCoordRectangle = new GaiaRectangle();
            GeometryUtils.getTexCoordsBoundingRectangle(verticesAux, texCoordRectangle);

            // check if texCoords must be translated.***
            double texCoordOriginX = texCoordRectangle.getMinX();
            double texCoordOriginY = texCoordRectangle.getMinY();
            double offsetX = 0.0;
            double offsetY = 0.0;
            boolean mustTranslate = false;
            if(texCoordOriginX < 0.0 || texCoordOriginX > 1.0)
            {
                offsetX = Math.floor(texCoordOriginX);
                mustTranslate = true;
            }

            if(texCoordOriginY < 0.0 || texCoordOriginY > 1.0)
            {
                offsetY = Math.floor(texCoordOriginY);
                mustTranslate = true;
            }

            if(mustTranslate)
            {
                Vector2d translateVector = new Vector2d(-offsetX, -offsetY);
                texCoordRectangle.translate(translateVector);
            }

            if(mustTranslate)
            {
                for (GaiaVertex vertex : verticesAux) {
                    Vector2d texCoord = vertex.getTexcoords();
                    if (mustTranslate) {
                        texCoord.x = texCoord.x - offsetX;
                        texCoord.y = texCoord.y - offsetY;
                    }
                }
            }
        }
    }
}
