package com.gaia3d.basic.model;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.model.structure.PrimitiveStructure;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.GLConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.*;

/**
 * A class that represents a primitive of a Gaia object.
 * It contains the vertices and surfaces.
 * The vertices are used for rendering.
 * The surfaces are used for calculating normals.
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaPrimitive extends PrimitiveStructure implements Serializable {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;

    public GaiaBoundingBox getBoundingBox(Matrix4d transform) {
        if (this.vertices == null || this.vertices.isEmpty()) {
            return null;
        }
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            Vector3d transformedPosition = new Vector3d(position);
            if (transform != null) {
                transform.transformPosition(position, transformedPosition);
            }
            boundingBox.addPoint(transformedPosition);
        }
        return boundingBox;
    }

    public GaiaRectangle getTexcoordBoundingRectangle(GaiaRectangle texcoordBoundingRectangle) {
        if (texcoordBoundingRectangle == null) {
            texcoordBoundingRectangle = new GaiaRectangle();
        }
        boolean is1rst = true;
        for (GaiaVertex vertex : vertices) {
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                if (is1rst) {
                    texcoordBoundingRectangle.setInit(textureCoordinate);
                    is1rst = false;
                } else {
                    texcoordBoundingRectangle.addPoint(textureCoordinate);
                }
            }
        }
        return texcoordBoundingRectangle;
    }

    public void calculateNormal() {
        for (GaiaSurface surface : surfaces) {
            surface.calculateNormal(this.vertices);
        }
    }

    public void calculateVertexNormals() {
        for (GaiaSurface surface : surfaces) {
            surface.calculateVertexNormals(this.vertices);
        }
    }

    public int[] getIndices() {
        int[] resultIndices = new int[0];
        for (GaiaSurface surface : surfaces) {
            resultIndices = ArrayUtils.addAll(resultIndices, surface.getIndices());
        }
        return resultIndices;
    }

    public void doNormalLengthUnitary() {
        for (GaiaVertex vertex : vertices) {
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normal.normalize();
            }
        }
    }

    public GaiaBufferDataSet toGaiaBufferSet(Matrix4d transformMatrixOrigin) {
        Matrix4d transformMatrix = new Matrix4d(transformMatrixOrigin);
        Matrix3d rotationMatrix = new Matrix3d();
        transformMatrix.get3x3(rotationMatrix);
        // normalize the rotation matrix
        rotationMatrix.normal();
        Matrix4d rotationMatrix4 = new Matrix4d(rotationMatrix);

        int[] indices = getIndices();

        GaiaRectangle texcoordBoundingRectangle = null;
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();

        // calculate texcoord BoundingRectangle by indices.
        for (int index : indices) {
            GaiaVertex vertex = vertices.get(index);
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                if (texcoordBoundingRectangle == null) {
                    texcoordBoundingRectangle = new GaiaRectangle();
                    texcoordBoundingRectangle.setInit(textureCoordinate);
                } else {
                    texcoordBoundingRectangle.addPoint(textureCoordinate);
                }
            }
        }

        int positionCount = 0;
        int normalCount = 0;
        int colorCount = 0;
        int batchIdCount = 0;
        int textureCoordinateCount = 0;
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position != null) {
                positionCount += 3;
            }
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normalCount += 3;
            }
            byte[] color = vertex.getColor();
            if (color != null) {
                colorCount += 4;
            }
            float batchId = vertex.getBatchId();
            if (batchId != 0) {
                batchIdCount += 1;
            }
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                textureCoordinateCount += 2;
            }
        }

        float[] positionList = new float[positionCount];
        float[] normalList = new float[normalCount];
        float[] batchIdList = new float[batchIdCount];
        float[] textureCoordinateList = new float[textureCoordinateCount];
        byte[] colorList = new byte[colorCount];

        int positionIndex = 0;
        int normalIndex = 0;
        int batchIdIndex = 0;
        int textureCoordinateIndex = 0;
        int colorIndex = 0;

        Random random = new Random();
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            transformMatrix.transformPosition(position);
            if (position != null) {
                positionList[positionIndex++] = (float) position.x;
                positionList[positionIndex++] = (float) position.y;
                positionList[positionIndex++] = (float) position.z;
            }
            boundingBox.addPoint(position);
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                rotationMatrix4.transformPosition(normal);

                Vector3d normalized = normal.normalize(new Vector3d());
                if (Double.isNaN(normalized.x()) || Double.isNaN(normalized.y()) || Double.isNaN(normalized.z())) {
                    log.error("[ERROR] Normal is NaN");
                    log.error(" - Normal : {}", normal);
                    log.error(" - Normalized : {}", normalized);
                    normalized = new Vector3d(0, 0, 1);
                }
                normalList[normalIndex++] = (float) normalized.x;
                normalList[normalIndex++] = (float) normalized.y;
                normalList[normalIndex++] = (float) normalized.z;
            }
            byte[] color = vertex.getColor();
            if (color != null) {
                colorList[colorIndex++] = color[0];
                colorList[colorIndex++] = color[1];
                colorList[colorIndex++] = color[2];
                colorList[colorIndex++] = color[3];
            }
            if (batchIdList.length > 0) {
                batchIdList[batchIdIndex++] = vertex.getBatchId();
            }
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                textureCoordinateList[textureCoordinateIndex++] = (float) textureCoordinate.x;
                textureCoordinateList[textureCoordinateIndex++] = (float) textureCoordinate.y;
            }
        }

        GaiaBufferDataSet gaiaBufferDataSet = new GaiaBufferDataSet();
        if (indices.length > 0) {
            GaiaBuffer indicesBuffer = new GaiaBuffer();
            indicesBuffer.setGlTarget(GLConstants.GL_ELEMENT_ARRAY_BUFFER);
            indicesBuffer.setGlType(GLConstants.GL_UNSIGNED_INT);
            indicesBuffer.setElementsCount(indices.length);
            indicesBuffer.setGlDimension((byte) 1);
            indicesBuffer.setInts(indices);
            gaiaBufferDataSet.getBuffers().put(AttributeType.INDICE, indicesBuffer);
        }
        if (normalList.length > 0) {
            GaiaBuffer normalBuffer = new GaiaBuffer();
            normalBuffer.setGlTarget(GLConstants.GL_ARRAY_BUFFER);
            normalBuffer.setGlType(GLConstants.GL_FLOAT);
            normalBuffer.setElementsCount(vertices.size());
            normalBuffer.setGlDimension((byte) 3);
            normalBuffer.setFloats(normalList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.NORMAL, normalBuffer);
        }
        if (colorList.length > 0) {
            GaiaBuffer colorBuffer = new GaiaBuffer();
            colorBuffer.setGlTarget(GLConstants.GL_ARRAY_BUFFER);
            colorBuffer.setGlType(GLConstants.GL_UNSIGNED_BYTE);
            colorBuffer.setElementsCount(vertices.size());
            colorBuffer.setGlDimension((byte) 4);
            colorBuffer.setBytes(colorList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.COLOR, colorBuffer);
        }
        if (batchIdList.length > 0) {
            GaiaBuffer batchIdBuffer = new GaiaBuffer();
            batchIdBuffer.setGlTarget(GLConstants.GL_ARRAY_BUFFER);
            batchIdBuffer.setGlType(GLConstants.GL_FLOAT);
            batchIdBuffer.setElementsCount(vertices.size());
            batchIdBuffer.setGlDimension((byte) 1);
            batchIdBuffer.setFloats(batchIdList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.BATCHID, batchIdBuffer);
        }
        if (positionList.length > 0) {
            GaiaBuffer positionBuffer = new GaiaBuffer();
            positionBuffer.setGlTarget(GLConstants.GL_ARRAY_BUFFER);
            positionBuffer.setGlType(GLConstants.GL_FLOAT);
            positionBuffer.setElementsCount(vertices.size());
            positionBuffer.setGlDimension((byte) 3);
            positionBuffer.setFloats(positionList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);
        }
        if (textureCoordinateList.length > 0) {
            GaiaBuffer textureCoordinateBuffer = new GaiaBuffer();
            textureCoordinateBuffer.setGlTarget(GLConstants.GL_ARRAY_BUFFER);
            textureCoordinateBuffer.setGlType(GLConstants.GL_FLOAT);
            textureCoordinateBuffer.setElementsCount(vertices.size());
            textureCoordinateBuffer.setGlDimension((byte) 2);
            textureCoordinateBuffer.setFloats(textureCoordinateList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.TEXCOORD, textureCoordinateBuffer);
        }
        gaiaBufferDataSet.setTexcoordBoundingRectangle(texcoordBoundingRectangle);
        gaiaBufferDataSet.setBoundingBox(boundingBox);

        GaiaBuffer buffer = gaiaBufferDataSet.getBuffers().get(AttributeType.POSITION);
        if (buffer == null) {
            log.error("[ERROR] No position buffer");
        }

        return gaiaBufferDataSet;
    }

    public void translate(Vector3d translation) {
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position != null) {
                position.add(translation);
            }
        }
    }

    public void clear() {
        if (this.vertices != null) {
            this.vertices.forEach(GaiaVertex::clear);
            this.vertices.clear();
        }
        if (this.surfaces != null) {
            this.surfaces.forEach(GaiaSurface::clear);
            this.surfaces.clear();
        }
    }

    public GaiaPrimitive clone() {
        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        gaiaPrimitive.setAccessorIndices(this.accessorIndices);
        gaiaPrimitive.setMaterialIndex(this.materialIndex);
        for (GaiaVertex vertex : this.vertices) {
            gaiaPrimitive.getVertices().add(vertex.clone());
        }
        for (GaiaSurface surface : this.surfaces) {
            gaiaPrimitive.getSurfaces().add(surface.clone());
        }
        return gaiaPrimitive;
    }

    public void deleteNormals() {
        for (GaiaVertex vertex : vertices) {
            vertex.setNormal(null);
        }
    }

    public boolean check() {
        boolean result = true;

        int verticesCount = this.vertices.size();
        for (GaiaSurface surface : this.surfaces) {
            int[] indices = surface.getIndices();
            for (int index : indices) {
                if (index >= verticesCount) {
                    log.error("[ERROR] Invalid index : {}", index);
                    result = false;
                }
            }
        }

        return result;
    }

    public void addPrimitive(GaiaPrimitive primitive) {
        int verticesCount = this.vertices.size();
        int primitiveVerticesCount = primitive.getVertices().size();
        this.vertices.addAll(primitive.getVertices());
        for (GaiaSurface surface : primitive.getSurfaces()) {
            List<GaiaFace> faces = surface.getFaces();
            for (GaiaFace face : faces) {
                int[] indices = face.getIndices();
                for (int i = 0; i < indices.length; i++) {
                    indices[i] += verticesCount;
                }

                GaiaFace newFace = new GaiaFace();
                newFace.setIndices(indices);
                this.surfaces.get(0).getFaces().add(newFace);
            }
        }
    }

    public Map<GaiaVertex, List<GaiaFace>> getMapVertexToFaces() {
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFace = new HashMap<>();
        for (GaiaSurface surface : this.surfaces) {
            for (GaiaFace face : surface.getFaces()) {
                int[] indices = face.getIndices();
                for (int index : indices) {
                    GaiaVertex vertex = this.vertices.get(index);
                    List<GaiaFace> faces = mapVertexToFace.computeIfAbsent(vertex, k -> new ArrayList<>());
                    faces.add(face);
                }
            }
        }
        return mapVertexToFace;
    }

    public List<GaiaFace> extractGaiaFaces(List<GaiaFace> resultFaces) {
        if (resultFaces == null) {
            resultFaces = new ArrayList<>();
        }

        for (GaiaSurface surface : this.surfaces) {
            resultFaces.addAll(surface.getFaces());
        }
        return resultFaces;
    }

    public void unWeldVertices() {
        List<GaiaVertex> newVertices = new ArrayList<>();
        List<GaiaFace> faces = this.extractGaiaFaces(null);
        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            int[] newIndices = new int[indices.length];
            for (int j = 0; j < indices.length; j++) {
                GaiaVertex vertex = this.vertices.get(indices[j]);
                GaiaVertex newVertex = vertex.clone();
                newVertices.add(newVertex);
                newIndices[j] = newVertices.size() - 1;
            }

            face.setIndices(newIndices);
        }

        if (this.vertices != null) {
            this.vertices.forEach(GaiaVertex::clear);
            this.vertices.clear();
        }
        this.vertices = newVertices;
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // Weld the vertices.
        GaiaBoundingBox boundingBox = this.getBoundingBox(null);
        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxSize = boundingBox.getMaxSize();
        boundingBox.set(minX, minY, minZ, minX + maxSize, minY + maxSize, minZ + maxSize); // cube bounding box

        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null, boundingBox);
        octreeVertices.addContents(this.vertices);
        octreeVertices.setMaxDepth(10);
        octreeVertices.setMinBoxSize(1.0); // 1m

        octreeVertices.makeTreeByMinVertexCount(50);

        List<GaiaOctree<GaiaVertex>> octreesWithContents = octreeVertices.extractOctreesWithContents();
        Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster = new HashMap<>();

        for (GaiaOctree<GaiaVertex> octree : octreesWithContents) {
            List<GaiaVertex> vertices = octree.getContents();
            getWeldableVertexMap(mapVertexToVertexMaster, vertices, error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }

        Map<GaiaVertex, GaiaVertex> mapVertexMasters = new HashMap<>();
        for (GaiaVertex vertexMaster : mapVertexToVertexMaster.values()) {
            mapVertexMasters.put(vertexMaster, vertexMaster);
        }

        List<GaiaVertex> newVerticesArray = new ArrayList<>(mapVertexMasters.values());

        Map<GaiaVertex, Integer> vertexIdxMap = new HashMap<>();
        int verticesCount = newVerticesArray.size();
        for (int i = 0; i < verticesCount; i++) {
            vertexIdxMap.put(newVerticesArray.get(i), i);
        }

        // Now, update the indices of the faces
        Map<GaiaFace, GaiaFace> mapDeleteFaces = new HashMap<>();
        for (GaiaSurface surface : this.surfaces) {
            int facesCount = surface.getFaces().size();
            for (int j = 0; j < facesCount; j++) {
                GaiaFace face = surface.getFaces().get(j);
                int[] indices = face.getIndices();
                for (int k = 0; k < indices.length; k++) {
                    GaiaVertex vertex = this.vertices.get(indices[k]);
                    GaiaVertex vertexMaster = mapVertexToVertexMaster.get(vertex);
                    int index = vertexIdxMap.get(vertexMaster);
                    indices[k] = index;
                }

                // check indices
                for (int k = 0; k < indices.length; k++) {
                    int index = indices[k];
                    for (int m = k + 1; m < indices.length; m++) {
                        if (index == indices[m]) {
                            // must remove the face
                            mapDeleteFaces.put(face, face);
                        }
                    }
                }
            }

            if (!mapDeleteFaces.isEmpty()) {
                List<GaiaFace> newFaces = new ArrayList<>();
                for (int j = 0; j < facesCount; j++) {
                    GaiaFace face = surface.getFaces().get(j);
                    if (!mapDeleteFaces.containsKey(face)) {
                        newFaces.add(face);
                    }
                }
                surface.setFaces(newFaces);
            }

        }

        // delete no used vertices
        for (GaiaVertex vertex : this.vertices) {
            if (!mapVertexMasters.containsKey(vertex)) {
                vertex.clear();
            }
        }
        this.vertices.clear();
        this.vertices = newVerticesArray;
    }

    private void getWeldableVertexMap(Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster, List<GaiaVertex> vertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        Map<GaiaVertex, GaiaVertex> visitedMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (visitedMap.containsKey(vertex)) {
                continue;
            }

            mapVertexToVertexMaster.put(vertex, vertex);

            for (int j = i + 1; j < verticesCount; j++) {
                GaiaVertex vertex2 = vertices.get(j);
                if (visitedMap.containsKey(vertex2)) {
                    continue;
                }
                if (vertex.isWeldable(vertex2, error, checkTexCoord, checkNormal, checkColor, checkBatchId)) {
                    mapVertexToVertexMaster.put(vertex2, vertex);

                    visitedMap.put(vertex, vertex);
                    visitedMap.put(vertex2, vertex2);
                }
            }
        }
    }

    public boolean deleteNoUsedVertices() {
        // Sometimes, there are no used vertices
        // The no used vertices must be deleted (vertex indices of the faces will be modified!)
        Map<GaiaVertex, Integer> vertexIdxMap = new HashMap<>();
        int surfacesCount = this.getSurfaces().size();
        for (int i = 0; i < surfacesCount; i++) {
            GaiaSurface surface = this.getSurfaces().get(i);
            List<GaiaFace> faces = surface.getFaces();
            for (GaiaFace face : faces) {
                int[] indices = face.getIndices();
                for (int index : indices) {
                    GaiaVertex vertex = this.getVertices().get(index);
                    vertexIdxMap.put(vertex, index);
                }
            }
        }

        int vertexCount = this.getVertices().size();
        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = this.getVertices().get(i);
            if (!vertexIdxMap.containsKey(vertex)) {
                vertex.clear();
            }
        }

        vertexCount = this.getVertices().size();
        int usedVertexCount = vertexIdxMap.size();
        if (vertexCount != usedVertexCount) {
            // Exists no used vertices
            List<GaiaVertex> usedVertices = new ArrayList<>();
            int idx = 0;
            Map<GaiaVertex, Integer> vertexIdxMap2 = new HashMap<>();
            for (GaiaVertex vertex : vertexIdxMap.keySet()) {
                usedVertices.add(vertex);
                vertexIdxMap2.put(vertex, idx);
                idx++;
            }

            // now, update the indices of the faces
            for (int i = 0; i < surfacesCount; i++) {
                GaiaSurface surface = this.getSurfaces().get(i);
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    int[] indices = face.getIndices();
                    for (int j = 0; j < indices.length; j++) {
                        GaiaVertex vertex = this.getVertices().get(indices[j]);
                        idx = vertexIdxMap2.get(vertex);
                        indices[j] = idx;
                    }
                }
            }

            // Finally, update the vertices
            this.getVertices().clear();
            this.setVertices(usedVertices);
        }

        return false;
    }

    public void deleteObjects() {
        if (this.vertices != null) {
            this.vertices.forEach(GaiaVertex::clear);
            this.vertices.clear();
        }
        if (this.surfaces != null) {
            this.surfaces.forEach(GaiaSurface::clear);
            this.surfaces.clear();
        }

        this.vertices = null;
        this.surfaces = null;
    }

    public void deleteDegeneratedFaces() {
        int facesDeletedCount = 0;
        for (GaiaSurface surface : this.surfaces) {
            facesDeletedCount = surface.deleteDegeneratedFaces(this.vertices);
        }

        if (facesDeletedCount > 0) {
            this.deleteNoUsedVertices();
        }
    }

    public void makeTriangleFaces() {
        for (GaiaSurface surface : this.surfaces) {
            surface.makeTriangleFaces();
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            Vector3d transformedPosition = new Vector3d();
            finalMatrix.transformPosition(position, transformedPosition);
            vertex.setPosition(transformedPosition);
        }
    }

    public void makeTriangularFaces() {
        for (GaiaSurface surface : surfaces) {
            surface.makeTriangularFaces(vertices);
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (GaiaSurface surface : surfaces) {
            facesCount += surface.getFaces().size();
        }
        return facesCount;
    }
}
