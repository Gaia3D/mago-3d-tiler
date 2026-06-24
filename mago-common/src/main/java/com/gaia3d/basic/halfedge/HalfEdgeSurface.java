package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.texture.atlas.TextureAtlasManager;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.*;

import static java.lang.Double.isNaN;

@SuppressWarnings("ALL")
@Slf4j
@Setter
@Getter
public class HalfEdgeSurface implements Serializable {
    // auxiliary variables
    Map<AttributeType, HalfEdgeRenderableBuffer> mapAttribTypeRenderableBuffer; // GL attributes
    private List<HalfEdge> halfEdges = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>();
    private List<HalfEdgeFace> faces = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;
    private boolean dirty = true;

    public void setTwins() {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertex.setId(i);
        }

        Map<Long, HalfEdge> map = new HashMap<>(halfEdges.size());

        for (HalfEdge he : halfEdges) {
            HalfEdgeVertex strVertex = he.getStartVertex();
            HalfEdgeVertex endVertex = he.getEndVertex();

            int a = strVertex.getId();
            int b = endVertex.getId();

            long key = (((long) a) << 32) | (b & 0xffffffffL);
            long twinKey = (((long) b) << 32) | (a & 0xffffffffL);

            HalfEdge twin = map.get(twinKey);

            if (twin != null && !twin.hasTwin() && he.isTwineableByPointers(twin)) {
                he.setTwin(twin);
                map.remove(twinKey);
            } else {
                map.put(key, he);
            }
        }

        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.hasTwin()) {
                hedge.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }
    }

    public void calculatePlaneNormals() {
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            face.calculatePlaneNormal();
        }
    }

    public void calculateNormals() {
        Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexAllFacesMap = this.getMapVertexAllFaces(null);
        Set<HalfEdgeVertex> vertexSet = vertexAllFacesMap.keySet();
        for (HalfEdgeVertex vertex : vertexSet) {
            List<HalfEdgeFace> faces = vertexAllFacesMap.get(vertex);
            if (faces == null || faces.isEmpty()) {
                continue;
            }
            Vector3d normal = vertex.getNormal();
            if (normal == null) {
                normal = new Vector3d();
            }
            normal.set(0, 0, 0);
            for (HalfEdgeFace face : faces) {
                Vector3d faceNormal = face.calculatePlaneNormal();
                if (isNaN(faceNormal.x) || isNaN(faceNormal.y) || isNaN(faceNormal.z)) {
                    faceNormal.set(0, 0, 1);
                }
                normal.add(faceNormal);
            }
            normal.normalize();
            if (isNaN(normal.x) || isNaN(normal.y) || isNaN(normal.z)) {
                normal.set(0, 0, 1);
            }
            vertex.setNormal(normal);
        }
    }

    public void deleteObjects() {
        // delete halfEdges
        for (HalfEdge halfEdge : halfEdges) {
            halfEdge.breakRelations();
        }
        halfEdges.clear();

        // delete faces
        for (HalfEdgeFace face : faces) {
            face.breakRelations();
        }
        faces.clear();

        // delete vertices
        for (HalfEdgeVertex vertex : vertices) {
            vertex.deleteObjects();
        }
        vertices.clear();
    }

    public Map<HalfEdge, Vector3d> getMapHalfEdgeToDirection(Map<HalfEdge, Vector3d> resultMapHalfEdgeToDirection) {
        if (resultMapHalfEdgeToDirection == null) {
            resultMapHalfEdgeToDirection = new HashMap<>();
        }
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            Vector3d direction = halfEdge.getVector(null);
            direction.normalize();
            resultMapHalfEdgeToDirection.put(halfEdge, direction);
        }
        return resultMapHalfEdgeToDirection;
    }

    public Map<Vector3d, List<HalfEdgeVertex>> getMapPositionToVertices(Map<Vector3d, List<HalfEdgeVertex>> resultMapPositionToVertices) {
        if (resultMapPositionToVertices == null) {
            resultMapPositionToVertices = new HashMap<>();
        }

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            List<HalfEdgeVertex> vertices = resultMapPositionToVertices.computeIfAbsent(position, k -> new ArrayList<>());
            vertices.add(vertex);
        }

        return resultMapPositionToVertices;
    }

    public Map<HalfEdgeFace, List<HalfEdge>> getMapFaceToHalfEdges(Map<HalfEdgeFace, List<HalfEdge>> resultMapFaceToHalfEdges) {
        if (resultMapFaceToHalfEdges == null) {
            resultMapFaceToHalfEdges = new HashMap<>();
        }

        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdgeFace face = halfEdge.getFace();
            List<HalfEdge> halfEdges = resultMapFaceToHalfEdges.computeIfAbsent(face, k -> new ArrayList<>());
            halfEdges.add(halfEdge);
        }

        return resultMapFaceToHalfEdges;
    }

    public List<HalfEdge> getHalfEdgesSortedByLength(List<HalfEdge> resultHalfEdgesSortedByLength) {
        if (resultHalfEdgesSortedByLength == null) {
            resultHalfEdgesSortedByLength = new ArrayList<>();
        }

        resultHalfEdgesSortedByLength.clear();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            resultHalfEdgesSortedByLength.add(halfEdge);
        }

        resultHalfEdgesSortedByLength.sort((o1, o2) -> {
            double length1 = o1.getSquaredLength();
            double length2 = o2.getSquaredLength();
            if (length1 < length2) {
                return -1;
            } else if (length1 > length2) {
                return 1;
            }
            return 0;
        });

        return resultHalfEdgesSortedByLength;
    }

    public int deleteDegeneratedFaces(Map<HalfEdgeFace, List<HalfEdge>> mapFaceToHalfEdges) {
        int facesCount = faces.size();
        int deletedCount = 0;
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.isDegenerated()) {
                face.setStatus(ObjectStatus.DELETED);
                List<HalfEdge> halfEdges = null;
                if (mapFaceToHalfEdges != null) {
                    halfEdges = mapFaceToHalfEdges.get(face);
                } else {
                    halfEdges = face.getHalfEdgesLoop(halfEdges);
                }
                if (halfEdges == null) {
                    continue;
                }
                for (HalfEdge halfEdge : halfEdges) {
                    halfEdge.setStatus(ObjectStatus.DELETED);
                }

                deletedCount++;
            }
        }

        return deletedCount;
    }

    public int deleteDegeneratedFaces() {
        int facesCount = faces.size();
        int deletedCount = 0;
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.isDegenerated()) {
                face.setStatus(ObjectStatus.DELETED);
                deletedCount++;
            }
        }

        return deletedCount;
    }

    public int deleteNoUsedVertices() {
        // Sometimes, there are no used vertices
        // The no used vertices must be deleted (vertex indices of the faces will be modified!)
        Map<HalfEdgeVertex, HalfEdgeVertex> mapUsedVertices = new HashMap<>();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            mapUsedVertices.put(startVertex, startVertex);
        }

        int deletedCount = 0;
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (mapUsedVertices.get(vertex) == null) {
                vertex.setStatus(ObjectStatus.DELETED);
                deletedCount++;
            }
        }

        return deletedCount;
    }

    public void removeDeletedObjects() {
        // delete objects that status is DELETED
        // delete halfEdges that status is DELETED
        int halfEdgesCount = this.halfEdges.size();
        List<HalfEdge> copyHalfEdges = new ArrayList<>(this.halfEdges);
        this.halfEdges.clear();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = copyHalfEdges.get(i);
            if (halfEdge.getStatus() != ObjectStatus.DELETED) {
                this.halfEdges.add(halfEdge);
            } else {
                halfEdge.breakRelations();
            }
        }
        copyHalfEdges.clear();

        // delete vertices that status is DELETED
        int verticesCount = this.vertices.size();
        List<HalfEdgeVertex> copyVertices = new ArrayList<>(this.vertices);
        this.vertices.clear();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = copyVertices.get(i);
            if (vertex.getStatus() != ObjectStatus.DELETED) {
                this.vertices.add(vertex);
            } else {
                vertex.deleteObjects();
            }
        }
        copyVertices.clear();

        // delete faces that status is DELETED
        int facesCount = this.faces.size();
        List<HalfEdgeFace> copyFaces = new ArrayList<>(this.faces);
        this.faces.clear();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = copyFaces.get(i);
            if (face.getStatus() != ObjectStatus.DELETED) {
                this.faces.add(face);
            } else {
                face.breakRelations();
            }
        }
        copyFaces.clear();
    }

    public void setObjectIdsInList() {
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            halfEdge.setId(i);
        }

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertex.setId(i);
        }

        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            face.setId(i);
        }
    }

    public void setItselfAsOutingHalfEdgeToTheStartVertex() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            halfEdge.setItselfAsOutingHalfEdgeToTheStartVertex();
        }

    }

    public Map<HalfEdgeVertex, List<HalfEdgeFace>> getMapVertexAllFaces(Map<HalfEdgeVertex, List<HalfEdgeFace>> resultVertexAllFacesMap) {
        if (resultVertexAllFacesMap == null) {
            resultVertexAllFacesMap = new HashMap<>();
        }

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeFace face = halfEdge.getFace();
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdgeFace> faces = resultVertexAllFacesMap.computeIfAbsent(startVertex, k -> new ArrayList<>());
            faces.add(face);
        }

        return resultVertexAllFacesMap;
    }

    public Map<HalfEdgeVertex, List<HalfEdge>> getMapVertexAllOutingEdges(Map<HalfEdgeVertex, List<HalfEdge>> resultVertexAllOutingEdgesMap) {
        if (resultVertexAllOutingEdgesMap == null) {
            resultVertexAllOutingEdgesMap = new HashMap<>();
        }

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> edges = resultVertexAllOutingEdgesMap.computeIfAbsent(startVertex, k -> new ArrayList<>());
            edges.add(halfEdge);
        }

        return resultVertexAllOutingEdgesMap;
    }

    private HalfEdgeVertex getVertexWithClassifyId(List<HalfEdgeVertex> listVertices, int classifyId) {
        int listVerticesCount = listVertices.size();
        for (int i = 0; i < listVerticesCount; i++) {
            HalfEdgeVertex vertex = listVertices.get(i);
            if (vertex.getClassifyId() == classifyId) {
                return vertex;
            }
        }

        return null;
    }

    public boolean collapseFace(HalfEdgeFace face) {
        List<HalfEdge> halfEdgesLoop = face.getHalfEdgesLoop(null);
        int halfEdgesLoopCount = halfEdgesLoop.size();
        for (int i = 0; i < halfEdgesLoopCount; i++) {
            HalfEdge halfEdge = halfEdgesLoop.get(i);
            halfEdge.setStatus(ObjectStatus.DELETED);
        }

        face.setStatus(ObjectStatus.DELETED);

        return true;
    }

    private double getButterFlyDotProdForHalfEdge(HalfEdge halfEdge) {
        HalfEdge twin = halfEdge.getTwin();
        if (twin == null) {
            return 0.0;
        }

        HalfEdgeFace faceA = halfEdge.getFace();
        HalfEdgeFace faceB = twin.getFace();
        if (faceA == null || faceB == null) {
            return 0.0;
        }

        Vector3d normalA = faceA.getNormal();
        if (normalA == null) {
            faceA.calculatePlaneNormal();
            normalA = faceA.getNormal();
        }

        Vector3d normalB = faceB.getNormal();
        if (normalB == null) {
            faceB.calculatePlaneNormal();
            normalB = faceB.getNormal();
        }

        if (normalA == null || normalB == null) {
            return 0.0;
        }

        double dotProd = normalA.dot(normalB);
        return dotProd;
    }

    public void setTwinsBetweenHalfEdges(List<HalfEdge> halfEdges) {
        // This function sets the twins between the halfEdges
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED || halfEdge.hasTwin()) {
                continue;
            }

            for (int j = i + 1; j < halfEdgesCount; j++) {
                HalfEdge halfEdge2 = halfEdges.get(j);
                if (halfEdge2.getStatus() == ObjectStatus.DELETED || halfEdge2.hasTwin()) {
                    continue;
                }

                if (halfEdge.setTwin(halfEdge2)) {
                    break;
                }
            }
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            if (position != null) {
                Vector3d transformedPosition = new Vector3d();
                finalMatrix.transformPosition(position, transformedPosition);
                vertex.setPosition(transformedPosition);
            }
        }
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if (vertices == null || vertices.isEmpty()) {
            return resultBBox;
        }

        GaiaBoundingBox myBBox = new GaiaBoundingBox();
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            Vector3d position = vertex.getPosition();
            if (position != null) {
                myBBox.addPoint(position);
            }
        }

        if (resultBBox == null) {
            resultBBox = myBBox;
        } else {
            resultBBox.addBoundingBox(myBBox);
        }

        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        if (planeType == PlaneType.XY) {
            cutByPlaneXY(planePosition, error);
        } else if (planeType == PlaneType.XZ) {
            cutByPlaneXZ(planePosition, error);
        } else if (planeType == PlaneType.YZ) {
            cutByPlaneYZ(planePosition, error);
        }

        removeDeletedObjects();
    }

    private void cutByPlaneXY(Vector3d planePosition, double error) {
        // find halfEdges that are cut by the plane
        int hedgesCutCount = 0;
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex intersectionVertex = new HalfEdgeVertex();
            if (hedge.getIntersectionByPlane(PlaneType.XY, planePosition, intersectionVertex, error)) {
                splitHalfEdge(hedge, intersectionVertex);
                hedgesCount = halfEdges.size();
                hedgesCutCount++;
            }

        }
        log.info("[Tile][Photogrammetry][cut][cutByPlaneXY] hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount);
    }

    private void cutByPlaneXZ(Vector3d planePosition, double error) {
        // find halfEdges that are cut by the plane
        int hedgesCount = halfEdges.size();
        int hedgesCutCount = 0;
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex intersectionVertex = new HalfEdgeVertex();
            if (hedge.getIntersectionByPlane(PlaneType.XZ, planePosition, intersectionVertex, error)) {
                splitHalfEdge(hedge, intersectionVertex);
                hedgesCount = halfEdges.size();
                hedgesCutCount++;
            }
        }
        log.info("[Tile][Photogrammetry][cut][cutByPlaneXZ] hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount);
    }

    private void cutByPlaneYZ(Vector3d planePosition, double error) {
        // find halfEdges that are cut by the plane
        int hedgesCount = halfEdges.size();
        int hedgesCutCount = 0;
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex intersectionVertex = new HalfEdgeVertex();
            if (hedge.getIntersectionByPlane(PlaneType.YZ, planePosition, intersectionVertex, error)) {
                splitHalfEdge(hedge, intersectionVertex);
                hedgesCount = halfEdges.size();
                hedgesCutCount++;
            }
        }
        log.info("[Tile][Photogrammetry][cut][cutByPlaneYZ] hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount);
    }

    public boolean checkHalfEdgesFaces() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (hedge.getFace() == null) {
                log.error("[ERROR] HalfEdgeSurface.checkHalfEdgesFaces() : hedge.getFace() == null.");
                return false;
            }
        }

        return true;
    }

    public boolean checkTwins() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdge twin = hedge.getTwin();

            if (twin != null && twin.getStatus() != ObjectStatus.DELETED && twin.getTwin() != hedge) {
                log.error("[ERROR] HalfEdgeSurface.checkTwins() : twin.getTwin() != hedge.");
                return false;
            }
        }

        return true;
    }

    private void splitHalfEdge(HalfEdge halfEdge, HalfEdgeVertex intersectionVertex) {
        // When split a halfEdge, must split the face too
        // If exist twin, must split the twin and twin's face too
        HalfEdge twin = halfEdge.getTwin();

        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();

        if (twin != null && twin.getStatus() != ObjectStatus.DELETED) {

            //intersectionVertex.setNote("intersectionVertex");

            //intersectionVertex.getPosition().add(0.0, 0.0, 10.0); // test
            this.getVertices().add(intersectionVertex);

            if (twin != null && twin.getStatus() != ObjectStatus.DELETED) {
                HalfEdgeFace twinsFace = twin.getFace();
                if (twinsFace == null) {

                }

            }

            // must split the twin too
            HalfEdgeFace faceA = halfEdge.getFace();
            HalfEdgeFace faceB = twin.getFace();

            if (faceA.getStatus() == ObjectStatus.DELETED || faceB.getStatus() == ObjectStatus.DELETED) {

            }

            faceA.setStatus(ObjectStatus.DELETED);
            //faceA.setNote("faceA_deleted");
            faceB.setStatus(ObjectStatus.DELETED);
            //faceB.setNote("faceB_deleted");

            List<HalfEdge> halfEdgesLoopA = new ArrayList<>();
            halfEdgesLoopA = halfEdge.getLoop(halfEdgesLoopA);

            List<HalfEdge> halfEdgesLoopB = new ArrayList<>();
            halfEdgesLoopB = twin.getLoop(halfEdgesLoopB);

            int hedgesACount = halfEdgesLoopA.size();
            int hedgesBCount = halfEdgesLoopB.size();

            //                                               oppositeVertexA
            //                                                    / \
            //                                                 /       \
            //                                              /             \
            //                                           /                   \
            //                 exteriorHEdgeA2        /                         \   exteriorHEdgeA1
            //                                     /                               \
            //                                  /             faceA                   \
            //                               /                                           \
            //                            /                                                 \
            //                         /                    halfEdge--->                       \
            //             startV   *-------------------------------------------------------------*  endV
            //                         \                    <---twin                           /
            //                            \                                                 /
            //                               \                                           /
            //                                  \            faceB                    /
            //                                     \                               /
            //                  exteriorHEdgeB1       \                         /   exteriorHEdgeB2
            //                                           \                   /
            //                                              \             /
            //                                                 \       /
            //                                                    \ /
            //                                               oppositeVertexB

            // Final situation
            //                                               oppositeVertexA
            //                                                    /|\
            //                                                 /   |   \
            //                                              /      |      \
            //                                           /         |         \
            //                     exteriorHEdgeA2    /            |            \   exteriorHEdgeA1
            //                                     /               |               \
            //                                  /          faceA   |    faceC         \
            //                               /                     |                     \
            //                            /                        |                        \
            //                         /        halfEdge--->       |     newHalfEdgeC1--->     \
            //             startV   *------------------------------*------------------------------*  endV  (in the center there are intersectionVertex)
            //                         \      <--->twin            |   <--->newHalfEdgeD1      /
            //                            \                        |                        /
            //                               \                     |                     /
            //                                  \          faceB   |    faceD         /
            //                                     \               |               /
            //                                        \            |            /
            //                      exteriorHEdgeB1      \         |         /   exteriorHEdgeB2
            //                                              \      |      /
            //                                                 \   |   /
            //                                                    \|/
            //                                               oppositeVertexB

            // Find oppositeVertexA and oppositeVertexB
            HalfEdgeVertex oppositeVertexA = halfEdge.getPrev().getStartVertex();
            HalfEdgeVertex oppositeVertexB = twin.getPrev().getStartVertex();

            HalfEdge exteriorHEdgeA1 = halfEdge.getNext().getTwin();
            HalfEdge exteriorHEdgeA2 = halfEdge.getPrev().getTwin();
            HalfEdge exteriorHEdgeB1 = twin.getNext().getTwin();
            // test
            if (exteriorHEdgeB1 != null) {
                HalfEdgeVertex extB1StartVertex = exteriorHEdgeB1.getStartVertex();
                HalfEdgeVertex extB1EndVertex = exteriorHEdgeB1.getEndVertex();
                if (extB1StartVertex != oppositeVertexB || extB1EndVertex != startVertex) {

                }
                // end test.---
            }
            HalfEdge exteriorHEdgeB2 = twin.getPrev().getTwin();

            // Face A*****************************
            // In this face use the halfEdge
            HalfEdgeFace newFaceA = new HalfEdgeFace();
            //newFaceA.setNote("newFaceA");
            HalfEdge newHalfEdgeA1 = new HalfEdge();
            HalfEdge newHalfEdgeA2 = new HalfEdge();
            HalfEdge newHalfEdgeA3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeA1);
            this.getHalfEdges().add(newHalfEdgeA2);
            this.getHalfEdges().add(newHalfEdgeA3);
            this.getFaces().add(newFaceA);

            newHalfEdgeA1.setNext(newHalfEdgeA2);
            newHalfEdgeA2.setNext(newHalfEdgeA3);
            newHalfEdgeA3.setNext(newHalfEdgeA1);

            newHalfEdgeA1.setFace(newFaceA);
            newHalfEdgeA2.setFace(newFaceA);
            newHalfEdgeA3.setFace(newFaceA);

            newHalfEdgeA1.setStartVertex(startVertex);
            newHalfEdgeA2.setStartVertex(intersectionVertex);
            newHalfEdgeA3.setStartVertex(oppositeVertexA);

            newFaceA.setHalfEdge(newHalfEdgeA1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeA2);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeA3);

            // Face B*****************************
            // In this face use the twin
            HalfEdgeFace newFaceB = new HalfEdgeFace();
            //newFaceB.setNote("newFaceB");
            HalfEdge newHalfEdgeB1 = new HalfEdge();
            HalfEdge newHalfEdgeB2 = new HalfEdge();
            HalfEdge newHalfEdgeB3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeB1);
            this.getHalfEdges().add(newHalfEdgeB2);
            this.getHalfEdges().add(newHalfEdgeB3);
            this.getFaces().add(newFaceB);

            newHalfEdgeB1.setNext(newHalfEdgeB2);
            newHalfEdgeB2.setNext(newHalfEdgeB3);
            newHalfEdgeB3.setNext(newHalfEdgeB1);

            newHalfEdgeB1.setFace(newFaceB);
            newHalfEdgeB2.setFace(newFaceB);
            newHalfEdgeB3.setFace(newFaceB);

            newHalfEdgeB1.setStartVertex(intersectionVertex);
            newHalfEdgeB2.setStartVertex(startVertex);
            newHalfEdgeB3.setStartVertex(oppositeVertexB);

            newFaceB.setHalfEdge(newHalfEdgeB1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeB1);
            oppositeVertexB.setOutingHalfEdge(newHalfEdgeB3);

            // Face C*****************************
            // In this face use the newHalfEdgeC
            HalfEdgeFace newFaceC = new HalfEdgeFace();
            //newFaceC.setNote("newFaceC");
            HalfEdge newHalfEdgeC1 = new HalfEdge();
            HalfEdge newHalfEdgeC2 = new HalfEdge();
            HalfEdge newHalfEdgeC3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeC1);
            this.getHalfEdges().add(newHalfEdgeC2);
            this.getHalfEdges().add(newHalfEdgeC3);
            this.getFaces().add(newFaceC);

            newHalfEdgeC1.setNext(newHalfEdgeC2);
            newHalfEdgeC2.setNext(newHalfEdgeC3);
            newHalfEdgeC3.setNext(newHalfEdgeC1);

            newHalfEdgeC1.setFace(newFaceC);
            newHalfEdgeC2.setFace(newFaceC);
            newHalfEdgeC3.setFace(newFaceC);

            newHalfEdgeC1.setStartVertex(intersectionVertex);
            newHalfEdgeC2.setStartVertex(endVertex);
            newHalfEdgeC3.setStartVertex(oppositeVertexA);

            newFaceC.setHalfEdge(newHalfEdgeC1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeC1);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeC3);

            // Face D*****************************
            // In this face use the newHalfEdgeD
            HalfEdgeFace newFaceD = new HalfEdgeFace();
            //newFaceD.setNote("newFaceD");
            HalfEdge newHalfEdgeD1 = new HalfEdge();
            HalfEdge newHalfEdgeD2 = new HalfEdge();
            HalfEdge newHalfEdgeD3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeD1);
            this.getHalfEdges().add(newHalfEdgeD2);
            this.getHalfEdges().add(newHalfEdgeD3);
            this.getFaces().add(newFaceD);

            newHalfEdgeD1.setNext(newHalfEdgeD2);
            newHalfEdgeD2.setNext(newHalfEdgeD3);
            newHalfEdgeD3.setNext(newHalfEdgeD1);

            newHalfEdgeD1.setFace(newFaceD);
            newHalfEdgeD2.setFace(newFaceD);
            newHalfEdgeD3.setFace(newFaceD);

            newHalfEdgeD1.setStartVertex(endVertex);
            newHalfEdgeD2.setStartVertex(intersectionVertex);
            newHalfEdgeD3.setStartVertex(oppositeVertexB);

            newFaceD.setHalfEdge(newHalfEdgeD1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeD2);
            oppositeVertexB.setOutingHalfEdge(newHalfEdgeD3);

            // Now set twins
            if (!newHalfEdgeA1.setTwin(newHalfEdgeB1)) {

            }

            if (!newHalfEdgeA2.setTwin(newHalfEdgeC3)) {

            }

            if (exteriorHEdgeA2 != null) {
                HalfEdge currTwinOfExteriorA2 = exteriorHEdgeA2.getTwin();
                if (!newHalfEdgeA3.setTwin(exteriorHEdgeA2)) {

                }

                if (currTwinOfExteriorA2 != null) {
                    currTwinOfExteriorA2.setTwin(null);
                }
            }

            if (exteriorHEdgeB1 != null) {
                HalfEdge currTwinOfExteriorB1 = exteriorHEdgeB1.getTwin();
                if (!newHalfEdgeB2.setTwin(exteriorHEdgeB1)) {

                }

                if (currTwinOfExteriorB1 != null) {
                    currTwinOfExteriorB1.setTwin(null);
                }
            }

            if (!newHalfEdgeB3.setTwin(newHalfEdgeD2)) {

            }

            if (!newHalfEdgeC1.setTwin(newHalfEdgeD1)) {

            }

            if (exteriorHEdgeA1 != null) {
                HalfEdge currTwinOfExteriorA1 = exteriorHEdgeA1.getTwin();
                if (!newHalfEdgeC2.setTwin(exteriorHEdgeA1)) {

                }

                if (currTwinOfExteriorA1 != null) {
                    currTwinOfExteriorA1.setTwin(null);
                }
            }

            if (exteriorHEdgeB2 != null) {
                HalfEdge currTwinOfExteriorB2 = exteriorHEdgeB2.getTwin();
                if (!newHalfEdgeD3.setTwin(exteriorHEdgeB2)) {

                }

                if (currTwinOfExteriorB2 != null) {
                    currTwinOfExteriorB2.setTwin(null);
                }
            }

            // finally break the relations of the halfEdgesLoopA
            for (int i = 0; i < hedgesACount; i++) {
                HalfEdge hedgeA = halfEdgesLoopA.get(i);
                //if (hedgeA != halfEdge)
                {
                    hedgeA.setStatus(ObjectStatus.DELETED);
                    hedgeA.breakRelations();

                }
            }

            // finally break the relations of the halfEdgesLoopB
            for (int i = 0; i < hedgesBCount; i++) {
                HalfEdge hedgeB = halfEdgesLoopB.get(i);
                //if (hedgeB != twin)
                {
                    hedgeB.setStatus(ObjectStatus.DELETED);
                    hedgeB.breakRelations();
                }
            }

        } else {
            //intersectionVertex.setNote("intersectionVertex");
            //intersectionVertex.getPosition().add(0.0, 0.0, 10.0); // test
            this.getVertices().add(intersectionVertex);

            HalfEdgeFace faceA = halfEdge.getFace();

            faceA.setStatus(ObjectStatus.DELETED);

            List<HalfEdge> halfEdgesLoopA = new ArrayList<>();
            halfEdgesLoopA = halfEdge.getLoop(halfEdgesLoopA);

            int hedgesACount = halfEdgesLoopA.size();

            // Initial situation
            //                                               oppositeVertexA
            //                                                    / \
            //                                                 /       \
            //                                              /             \
            //                                           /                   \
            //                 exteriorHEdgeA2        /                         \   exteriorHEdgeA1
            //                                     /                               \
            //                                  /             faceA                   \
            //                               /                                           \
            //                            /                                                 \
            //                         /                    halfEdge--->                       \
            //             startV   *-------------------------------------------------------------*  endV

            // Final situation
            //                                               oppositeVertexA
            //                                                    /|\
            //                                                 /   |   \
            //                                              /      |      \
            //                                           /         |         \
            //                     exteriorHEdgeA2    /            |            \   exteriorHEdgeA1
            //                                     /               |               \
            //                                  /          faceA   |    faceC         \
            //                               /                     |                     \
            //                            /                        |                        \
            //                         /        halfEdge--->       |     newHalfEdgeC--->      \
            //             startV   *------------------------------*------------------------------*  endV  (in the center there are intersectionVertex)

            // Find oppositeVertexA and oppositeVertexB
            HalfEdgeVertex oppositeVertexA = halfEdge.getPrev().getStartVertex();

            HalfEdge exteriorHEdgeA1 = halfEdge.getNext().getTwin();
            HalfEdge exteriorHEdgeA2 = halfEdge.getPrev().getTwin();

            // Face A*****************************
            // In this face use the halfEdge
            HalfEdgeFace newFaceA = new HalfEdgeFace();
            HalfEdge newHalfEdgeA1 = new HalfEdge();
            HalfEdge newHalfEdgeA2 = new HalfEdge();
            HalfEdge newHalfEdgeA3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeA1);
            this.getHalfEdges().add(newHalfEdgeA2);
            this.getHalfEdges().add(newHalfEdgeA3);
            this.getFaces().add(newFaceA);

            newHalfEdgeA1.setNext(newHalfEdgeA2);
            newHalfEdgeA2.setNext(newHalfEdgeA3);
            newHalfEdgeA3.setNext(newHalfEdgeA1);

            newHalfEdgeA1.setFace(newFaceA);
            newHalfEdgeA2.setFace(newFaceA);
            newHalfEdgeA3.setFace(newFaceA);

            newHalfEdgeA1.setStartVertex(startVertex); // is redundant
            newHalfEdgeA2.setStartVertex(intersectionVertex);
            newHalfEdgeA3.setStartVertex(oppositeVertexA);

            newFaceA.setHalfEdge(newHalfEdgeA1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeA2);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeA3);

            // Face C*****************************
            // In this face use the newHalfEdgeC
            HalfEdgeFace newFaceC = new HalfEdgeFace();
            HalfEdge newHalfEdgeC1 = new HalfEdge();
            HalfEdge newHalfEdgeC2 = new HalfEdge();
            HalfEdge newHalfEdgeC3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeC1);
            this.getHalfEdges().add(newHalfEdgeC2);
            this.getHalfEdges().add(newHalfEdgeC3);
            this.getFaces().add(newFaceC);

            newHalfEdgeC1.setNext(newHalfEdgeC2);
            newHalfEdgeC2.setNext(newHalfEdgeC3);
            newHalfEdgeC3.setNext(newHalfEdgeC1);

            newHalfEdgeC1.setFace(newFaceC);
            newHalfEdgeC2.setFace(newFaceC);
            newHalfEdgeC3.setFace(newFaceC);

            newHalfEdgeC1.setStartVertex(intersectionVertex);
            newHalfEdgeC2.setStartVertex(endVertex);
            newHalfEdgeC3.setStartVertex(oppositeVertexA);

            newFaceC.setHalfEdge(newHalfEdgeC1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeC1);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeC3);

            // Now set twins
            if (!newHalfEdgeA2.setTwin(newHalfEdgeC3)) {

            }
            if (exteriorHEdgeA2 != null) {
                HalfEdge currTwinOfExteriorA2 = exteriorHEdgeA2.getTwin();
                if (!newHalfEdgeA3.setTwin(exteriorHEdgeA2)) {

                }

                if (currTwinOfExteriorA2 != null) {
                    currTwinOfExteriorA2.setTwin(null);
                }
            }

            if (exteriorHEdgeA1 != null) {
                HalfEdge currTwinOfExteriorA1 = exteriorHEdgeA1.getTwin();
                if (!newHalfEdgeC2.setTwin(exteriorHEdgeA1)) {

                }

                if (currTwinOfExteriorA1 != null) {
                    currTwinOfExteriorA1.setTwin(null);
                }
            }

            // finally break the relations of the halfEdgesLoopA
            for (int i = 0; i < hedgesACount; i++) {
                HalfEdge hedgeA = halfEdgesLoopA.get(i);
                //if (hedgeA != halfEdge) {
                hedgeA.setStatus(ObjectStatus.DELETED);
                hedgeA.breakRelations();
            }
        }
    }


    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition) {
        int facesCount = faces.size();
        Vector3d barycenter = new Vector3d();
        if (planeType == PlaneType.XY) {
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                barycenter = face.getBarycenter(barycenter);
                double z = barycenter.z;
                if (z > planePosition.z) {
                    face.setClassifyId(2);
                } else if (z < planePosition.z) {
                    face.setClassifyId(1);
                } else {
                    face.setClassifyId(1);
                }
            }
        } else if (planeType == PlaneType.XZ) {
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                barycenter = face.getBarycenter(barycenter);
                double y = barycenter.y;
                if (y > planePosition.y) {
                    face.setClassifyId(2);
                } else if (y < planePosition.y) {
                    face.setClassifyId(1);
                } else {
                    face.setClassifyId(1);
                }
            }
        } else if (planeType == PlaneType.YZ) {
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                barycenter = face.getBarycenter(barycenter);
                double x = barycenter.x;
                if (x > planePosition.x) {
                    face.setClassifyId(2);
                } else if (x < planePosition.x) {
                    face.setClassifyId(1);
                } else {
                    face.setClassifyId(1);
                }
            }
        }
    }

    public void deleteFacesWithClassifyId(int classifyId) {
        // must delete the faces, halfEdges, vertices
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face == null) {
                log.error("[ERROR] HalfEdgeSurface.deleteFacesWithClassifyId() : face == null.");
                continue;
            }
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (face.getClassifyId() == classifyId) {
                face.setStatus(ObjectStatus.DELETED);
                HalfEdge halfEdge = face.getHalfEdge();
                List<HalfEdge> halfEdgesLoop = new ArrayList<>();
                halfEdgesLoop = halfEdge.getLoop(halfEdgesLoop);
                int hedgesLoopCount = halfEdgesLoop.size();
                for (int j = 0; j < hedgesLoopCount; j++) {
                    HalfEdge halfEdgeLoop = halfEdgesLoop.get(j);
                    halfEdgeLoop.setStatus(ObjectStatus.DELETED);
                }
            }
        }

        removeDeletedObjects();

        // check no used vertices
        List<HalfEdgeVertex> noUsedVertices = new ArrayList<>();
        if (existNoUsedVertices(noUsedVertices)) {
            int noUsedVerticesCount = noUsedVertices.size();
            for (int i = 0; i < noUsedVerticesCount; i++) {
                HalfEdgeVertex vertex = noUsedVertices.get(i);
                vertex.setStatus(ObjectStatus.DELETED);
            }

            removeDeletedObjects();
        }

        setObjectIdsInList();
    }

    public void calculateVertices() {
        Map<HalfEdgeVertex, HalfEdgeVertex> vertices = new HashMap<>();
        for (HalfEdgeFace face : faces) {
            HalfEdge halfEdge = face.getHalfEdge();
            List<HalfEdge> halfEdgesLoop = new ArrayList<>();
            halfEdgesLoop = halfEdge.getLoop(halfEdgesLoop);
            int hedgesLoopCount = halfEdgesLoop.size();
            for (int j = 0; j < hedgesLoopCount; j++) {
                HalfEdge halfEdgeLoop = halfEdgesLoop.get(j);
                HalfEdgeVertex vertex = halfEdgeLoop.getStartVertex();
                vertices.put(vertex, vertex);
            }
        }

        this.vertices = new ArrayList<>(vertices.keySet());
    }

    public void deleteFacesWithNoClassifyId(int classifyId) {
        // must delete the faces, halfEdges, vertices
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face == null) {
                log.error("[ERROR] HalfEdgeSurface.deleteFacesWithClassifyId() : face == null.");
                continue;
            }
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (face.getClassifyId() != classifyId) {
                face.setStatus(ObjectStatus.DELETED);
                HalfEdge halfEdge = face.getHalfEdge();
                List<HalfEdge> halfEdgesLoop = new ArrayList<>();
                halfEdgesLoop = halfEdge.getLoop(halfEdgesLoop);
                int hedgesLoopCount = halfEdgesLoop.size();
                for (int j = 0; j < hedgesLoopCount; j++) {
                    HalfEdge halfEdgeLoop = halfEdgesLoop.get(j);
                    halfEdgeLoop.setStatus(ObjectStatus.DELETED);
                }
            }
        }

        removeDeletedObjects();

        // check no used vertices
        List<HalfEdgeVertex> noUsedVertices = new ArrayList<>();
        if (existNoUsedVertices(noUsedVertices)) {
            int noUsedVerticesCount = noUsedVertices.size();
            for (int i = 0; i < noUsedVerticesCount; i++) {
                HalfEdgeVertex vertex = noUsedVertices.get(i);
                vertex.setStatus(ObjectStatus.DELETED);
            }

            removeDeletedObjects();
        }

        setObjectIdsInList();
    }

//    private void postReadFile() {
//        // set the twins & others
//        int hedgesCount = halfEdges.size();
//        for (int i = 0; i < hedgesCount; i++) {
//            HalfEdge hedge = halfEdges.get(i);
//            if (hedge.getStatus() == ObjectStatus.DELETED) {
//                continue;
//            }
//
//            // set the twin
//            int twinIndex = hedge.getTwinId();
//            if (twinIndex >= 0) {
//                HalfEdge twin = halfEdges.get(twinIndex);
//                hedge.setTwin(twin);
//            }
//
//            // set the next
//            int nextIndex = hedge.getNextId();
//            if (nextIndex >= 0) {
//                HalfEdge next = halfEdges.get(nextIndex);
//                hedge.setNext(next);
//            }
//
//            // set the startVertex
//            int startVertexIndex = hedge.getStartVertexId();
//            if (startVertexIndex >= 0) {
//                HalfEdgeVertex startVertex = vertices.get(startVertexIndex);
//                hedge.setStartVertex(startVertex);
//            }
//
//            // set the face
//            int faceIndex = hedge.getFaceId();
//            if (faceIndex >= 0) {
//                HalfEdgeFace face = faces.get(faceIndex);
//                hedge.setFace(face);
//            }
//        }
//
//        // set the faces
//        int facesCount = faces.size();
//        for (int i = 0; i < facesCount; i++) {
//            HalfEdgeFace face = faces.get(i);
//            if (face.getStatus() == ObjectStatus.DELETED) {
//                continue;
//            }
//
//            int halfEdgeIndex = face.getHalfEdgeId();
//            if (halfEdgeIndex >= 0) {
//                HalfEdge halfEdge = halfEdges.get(halfEdgeIndex);
//                face.setHalfEdge(halfEdge);
//            }
//        }
//
//        // set the startVertex
//        int verticesCount = vertices.size();
//        for (int i = 0; i < verticesCount; i++) {
//            HalfEdgeVertex vertex = vertices.get(i);
//            if (vertex.getStatus() == ObjectStatus.DELETED) {
//                continue;
//            }
//
//            int outingHalfEdgeIndex = vertex.getOutingHalfEdgeId();
//            if (outingHalfEdgeIndex >= 0) {
//                HalfEdge outingHalfEdge = halfEdges.get(outingHalfEdgeIndex);
//                vertex.setOutingHalfEdge(outingHalfEdge);
//            }
//        }
//    }

//    public void writeFile(ObjectOutputStream outputStream) {
//        /*
//        private List<HalfEdge> halfEdges = new ArrayList<>();
//        private List<HalfEdgeVertex> vertices = new ArrayList<>();
//        private List<HalfEdgeFace> faces = new ArrayList<>();
//        private GaiaBoundingBox boundingBox = null;
//         */
//
//        this.setObjectIdsInList();
//
//        try {
//            // vertices
//            outputStream.writeInt(vertices.size());
//            for (HalfEdgeVertex vertex : vertices) {
//                vertex.writeFile(outputStream);
//            }
//
//            // faces
//            outputStream.writeInt(faces.size());
//            for (HalfEdgeFace face : faces) {
//                face.writeFile(outputStream);
//            }
//
//            outputStream.writeObject(boundingBox);
//
//            outputStream.writeInt(halfEdges.size());
//            int counter = 0;
//            for (HalfEdge halfEdge : halfEdges) {
//                halfEdge.writeFile(outputStream);
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void readFile(ObjectInputStream inputStream) {
//        try {
//            // vertices
//            int verticesCount = inputStream.readInt();
//            for (int i = 0; i < verticesCount; i++) {
//                HalfEdgeVertex vertex = new HalfEdgeVertex();
//                vertex.readFile(inputStream);
//                vertices.add(vertex);
//            }
//            // faces
//            int facesCount = inputStream.readInt();
//            for (int i = 0; i < facesCount; i++) {
//                HalfEdgeFace face = new HalfEdgeFace();
//                face.readFile(inputStream);
//                faces.add(face);
//            }
//            boundingBox = (GaiaBoundingBox) inputStream.readObject();
//
//            int halfEdgesCount = inputStream.readInt();
//            for (int i = 0; i < halfEdgesCount; i++) {
//                HalfEdge halfEdge = new HalfEdge();
//                halfEdge.readFile(inputStream);
//                halfEdges.add(halfEdge);
//            }
//
//        } catch (IOException | ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//
//        postReadFile();
//    }

    public boolean existNoUsedVertices(List<HalfEdgeVertex> noUsedVertices) {
        // check if there are no used vertices
        Map<HalfEdgeVertex, HalfEdgeVertex> vertexMap = new HashMap<>();
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex startVertex = hedge.getStartVertex();
            vertexMap.put(startVertex, startVertex);
        }

        noUsedVertices.clear();

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (!vertexMap.containsKey(vertex)) {
                noUsedVertices.add(vertex);
            }
        }

        return !noUsedVertices.isEmpty();
    }

    public HalfEdgeSurface cloneByClassifyId(int classifyId) {
        /*
         * Después de esta llamada, asumimos:
         *
         * vertex.getId() == posición en vertices
         * face.getId() == posición en faces
         * halfEdge.getId() == posición en halfEdges
         */
        this.setObjectIdsInList();

        final int totalFacesCount = this.faces.size();
        final int totalVerticesCount = this.vertices.size();
        final int totalHalfEdgesCount = this.halfEdges.size();

        /*
         * 1. Seleccionar las faces que pertenecen al classifyId.
         */
        List<HalfEdgeFace> selectedFaces = new ArrayList<>();

        for (int i = 0; i < totalFacesCount; i++) {
            HalfEdgeFace face = this.faces.get(i);

            if (face == null
                    || face.getStatus() == ObjectStatus.DELETED
                    || face.getClassifyId() != classifyId) {
                continue;
            }

            selectedFaces.add(face);
        }

        if (selectedFaces.isEmpty()) {
            return null;
        }

        /*
         * Extraer únicamente los half-edges pertenecientes
         * a las faces seleccionadas.
         */
        List<HalfEdge> selectedHalfEdges =
                HalfEdgeUtils.getHalfEdgesOfFaces(
                        selectedFaces,
                        null
                );

        if (selectedHalfEdges == null
                || selectedHalfEdges.isEmpty()) {
            return null;
        }

        HalfEdgeSurface clonedSurface = new HalfEdgeSurface();

        /*
         * Arrays de correspondencia directa:
         *
         * originalId -> objeto clonado
         */
        HalfEdgeVertex[] clonedVertexById =
                new HalfEdgeVertex[totalVerticesCount];

        HalfEdgeFace[] clonedFaceById =
                new HalfEdgeFace[totalFacesCount];

        HalfEdge[] clonedHalfEdgeById =
                new HalfEdge[totalHalfEdgesCount];

        /*
         * 2. Clonar faces.
         */
        int selectedFacesCount = selectedFaces.size();

        for (int i = 0; i < selectedFacesCount; i++) {
            HalfEdgeFace originalFace = selectedFaces.get(i);
            int faceId = originalFace.getId();

            HalfEdgeFace clonedFace = new HalfEdgeFace();
            clonedFace.copyFrom(originalFace);

            clonedFaceById[faceId] = clonedFace;
            clonedSurface.faces.add(clonedFace);
        }

        /*
         * 3. Crear half-edges y clonar vértices bajo demanda.
         *
         * Un vértice compartido por varias faces se clona solo una vez.
         */
        int selectedHalfEdgesCount = selectedHalfEdges.size();

        for (int i = 0; i < selectedHalfEdgesCount; i++) {
            HalfEdge originalHalfEdge = selectedHalfEdges.get(i);

            if (originalHalfEdge == null
                    || originalHalfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            int halfEdgeId = originalHalfEdge.getId();

            HalfEdgeVertex originalStartVertex =
                    originalHalfEdge.getStartVertex();

            if (originalStartVertex == null
                    || originalStartVertex.getStatus()
                    == ObjectStatus.DELETED) {
                continue;
            }

            int vertexId = originalStartVertex.getId();

            HalfEdgeVertex clonedStartVertex =
                    clonedVertexById[vertexId];

            if (clonedStartVertex == null) {
                clonedStartVertex = new HalfEdgeVertex();
                clonedStartVertex.copyFrom(originalStartVertex);

                clonedVertexById[vertexId] = clonedStartVertex;
                clonedSurface.vertices.add(clonedStartVertex);
            }

            HalfEdge clonedHalfEdge = new HalfEdge();
            clonedHalfEdge.setStartVertex(clonedStartVertex);

            clonedHalfEdgeById[halfEdgeId] = clonedHalfEdge;
            clonedSurface.halfEdges.add(clonedHalfEdge);

            /*
             * Preservar preferentemente el outingHalfEdge original.
             *
             * Si el outing original no pertenece al subconjunto,
             * se conserva provisionalmente el primero encontrado.
             */
            HalfEdge originalOuting =
                    originalStartVertex.getOutingHalfEdge();

            if (clonedStartVertex.getOutingHalfEdge() == null
                    || originalOuting == originalHalfEdge) {

                clonedStartVertex.setOutingHalfEdge(
                        clonedHalfEdge
                );
            }
        }

        /*
         * 4. Conectar next y face.
         */
        for (int i = 0; i < selectedHalfEdgesCount; i++) {
            HalfEdge originalHalfEdge = selectedHalfEdges.get(i);

            if (originalHalfEdge == null) {
                continue;
            }

            int halfEdgeId = originalHalfEdge.getId();

            HalfEdge clonedHalfEdge =
                    clonedHalfEdgeById[halfEdgeId];

            if (clonedHalfEdge == null) {
                continue;
            }

            /*
             * Next.
             *
             * El next de un half-edge perteneciente a una face
             * seleccionada debería pertenecer a la misma face.
             */
            HalfEdge originalNext = originalHalfEdge.getNext();

            if (originalNext != null) {
                int nextId = originalNext.getId();

                if (nextId >= 0
                        && nextId < clonedHalfEdgeById.length) {

                    clonedHalfEdge.setNext(
                            clonedHalfEdgeById[nextId]
                    );
                }
            }

            /*
             * Face.
             */
            HalfEdgeFace originalFace =
                    originalHalfEdge.getFace();

            if (originalFace != null) {
                int faceId = originalFace.getId();

                HalfEdgeFace clonedFace =
                        clonedFaceById[faceId];

                clonedHalfEdge.setFace(clonedFace);
            }
        }

        /*
         * 5. Preservar el half-edge principal de cada face.
         *
         * En el método anterior se sobrescribía mediante
         * cloneFace.setHalfEdge() por cada arista, por lo que
         * terminaba quedándose con la última.
         */
        for (int i = 0; i < selectedFacesCount; i++) {
            HalfEdgeFace originalFace = selectedFaces.get(i);
            HalfEdgeFace clonedFace =
                    clonedFaceById[originalFace.getId()];

            HalfEdge originalFaceHalfEdge =
                    originalFace.getHalfEdge();

            if (originalFaceHalfEdge == null) {
                continue;
            }

            int halfEdgeId = originalFaceHalfEdge.getId();

            if (halfEdgeId >= 0
                    && halfEdgeId < clonedHalfEdgeById.length) {

                clonedFace.setHalfEdge(
                        clonedHalfEdgeById[halfEdgeId]
                );
            }
        }

        /*
         * 6. Conectar twins.
         */
        for (int i = 0; i < selectedHalfEdgesCount; i++) {
            HalfEdge originalHalfEdge = selectedHalfEdges.get(i);

            if (originalHalfEdge == null) {
                continue;
            }

            HalfEdge clonedHalfEdge =
                    clonedHalfEdgeById[originalHalfEdge.getId()];

            if (clonedHalfEdge == null) {
                continue;
            }

            HalfEdge originalTwin =
                    originalHalfEdge.getTwin();

            if (originalTwin == null) {
                continue;
            }

            int twinId = originalTwin.getId();

            HalfEdge clonedTwin = null;

            if (twinId >= 0
                    && twinId < clonedHalfEdgeById.length) {

                clonedTwin = clonedHalfEdgeById[twinId];
            }

            if (clonedTwin == null) {
                /*
                 * El twin existe en la superficie original,
                 * pero pertenece a una face no seleccionada.
                 *
                 * Por tanto, esta arista se convierte en frontera.
                 */
                clonedHalfEdge.setClassifyId(10);
            } else {
                clonedHalfEdge.setTwin(clonedTwin);
            }
        }

        return clonedSurface;
    }

    public HalfEdgeSurface cloneByClassifyId_original(int classifyId) {
        List<HalfEdgeFace> faces = new ArrayList<>();
        int facesCount = this.faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = this.faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (face.getClassifyId() == classifyId) {
                faces.add(face);
            }
        }

        if (faces.isEmpty()) {
            return null;
        }

        HalfEdgeSurface cloneSurface = new HalfEdgeSurface();

        this.setObjectIdsInList();

        // 1rst, copy vertices
        Map<HalfEdgeVertex, HalfEdgeVertex> mapOriginalToCloneVertex = new HashMap<>();
        List<HalfEdgeVertex> faceVertexList = new ArrayList<>();
        HalfEdgeUtils.getVerticesOfFaces(faces, faceVertexList);
        int verticesCount = faceVertexList.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = faceVertexList.get(i);
            HalfEdgeVertex cloneVertex = new HalfEdgeVertex();
            cloneVertex.copyFrom(vertex);
            cloneSurface.vertices.add(cloneVertex);

            mapOriginalToCloneVertex.put(vertex, cloneVertex);
        }

        // copy faces
        Map<HalfEdgeFace, HalfEdgeFace> mapOriginalToCloneFace = new HashMap<>();
        facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            HalfEdgeFace cloneFace = new HalfEdgeFace();
            cloneFace.copyFrom(face);
            cloneSurface.faces.add(cloneFace);

            mapOriginalToCloneFace.put(face, cloneFace);
        }

        // copy halfEdges
        Map<HalfEdge, HalfEdge> mapOriginalToCloneHalfEdge = new HashMap<>();
        List<HalfEdge> halfEdgesOfFaces = HalfEdgeUtils.getHalfEdgesOfFaces(faces, null);
        int halfEdgesCount = halfEdgesOfFaces.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgesOfFaces.get(i);

            // startVertex
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex cloneStartVertex = mapOriginalToCloneVertex.get(startVertex);
            HalfEdge cloneHalfEdge = new HalfEdge();
            cloneHalfEdge.setStartVertex(cloneStartVertex);
            cloneStartVertex.setOutingHalfEdge(cloneHalfEdge);

            mapOriginalToCloneHalfEdge.put(halfEdge, cloneHalfEdge);
        }

        if (mapOriginalToCloneHalfEdge.isEmpty()) {
            return null;
        }

        halfEdgesCount = halfEdgesOfFaces.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgesOfFaces.get(i);
            HalfEdge cloneHalfEdge = mapOriginalToCloneHalfEdge.get(halfEdge);

            // next
            HalfEdge next = halfEdge.getNext();
            HalfEdge cloneNext = mapOriginalToCloneHalfEdge.get(next);
            cloneHalfEdge.setNext(cloneNext);

            // face
            HalfEdgeFace face = halfEdge.getFace();
            HalfEdgeFace cloneFace = mapOriginalToCloneFace.get(face);
            cloneHalfEdge.setFace(cloneFace);
            cloneFace.setHalfEdge(cloneHalfEdge);

            cloneSurface.halfEdges.add(cloneHalfEdge);
        }

        halfEdgesCount = halfEdgesOfFaces.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgesOfFaces.get(i);
            HalfEdge cloneHalfEdge = mapOriginalToCloneHalfEdge.get(halfEdge);

            // twin
            HalfEdge twin = halfEdge.getTwin();
            if (twin == null) {
                continue;
            }
            HalfEdge cloneTwin = mapOriginalToCloneHalfEdge.get(twin);
            if (cloneTwin == null) {
                // the twin is not in the cloneSurface, so cloneHalfEdge is frontier
                cloneHalfEdge.setClassifyId(10);
            }
            cloneHalfEdge.setTwin(cloneTwin);
        }

        cloneSurface.setTwins();

        return cloneSurface;
    }

    public HalfEdgeSurface clone() {
        HalfEdgeSurface cloneSurface = new HalfEdgeSurface();

        this.setObjectIdsInList();

        // 1rst, copy vertices
        Map<HalfEdgeVertex, HalfEdgeVertex> mapOriginalToCloneVertex = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            HalfEdgeVertex cloneVertex = new HalfEdgeVertex();
            cloneVertex.copyFrom(vertex);
            cloneSurface.vertices.add(cloneVertex);

            mapOriginalToCloneVertex.put(vertex, cloneVertex);
        }

        // copy faces
        Map<HalfEdgeFace, HalfEdgeFace> mapOriginalToCloneFace = new HashMap<>();
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            HalfEdgeFace cloneFace = new HalfEdgeFace();
            cloneFace.copyFrom(face);
            cloneSurface.faces.add(cloneFace);

            mapOriginalToCloneFace.put(face, cloneFace);
        }

        // copy halfEdges
        Map<HalfEdge, HalfEdge> mapOriginalToCloneHalfEdge = new HashMap<>();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge cloneHalfEdge = new HalfEdge();
            mapOriginalToCloneHalfEdge.put(halfEdge, cloneHalfEdge);
        }

        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge cloneHalfEdge = mapOriginalToCloneHalfEdge.get(halfEdge);

            // startVertex
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex cloneStartVertex = mapOriginalToCloneVertex.get(startVertex);
            cloneHalfEdge.setStartVertex(cloneStartVertex);
            cloneStartVertex.setOutingHalfEdge(cloneHalfEdge);

            // next
            HalfEdge next = halfEdge.getNext();
            HalfEdge cloneNext = mapOriginalToCloneHalfEdge.get(next);
            cloneHalfEdge.setNext(cloneNext);

            // twin
            HalfEdge twin = halfEdge.getTwin();
            HalfEdge cloneTwin = mapOriginalToCloneHalfEdge.get(twin);
            cloneHalfEdge.setTwin(cloneTwin);

            // face
            HalfEdgeFace face = halfEdge.getFace();
            HalfEdgeFace cloneFace = mapOriginalToCloneFace.get(face);
            cloneHalfEdge.setFace(cloneFace);
            cloneFace.setHalfEdge(cloneHalfEdge);

            cloneSurface.halfEdges.add(cloneHalfEdge);

            mapOriginalToCloneHalfEdge.put(halfEdge, cloneHalfEdge);
        }

        return cloneSurface;
    }

    public void scissorTextures(GaiaMaterial material) {
        // Provisionally scissor only the "DiffuseTexture"
        if (material == null) {
            return;
        }

        Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
        List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
        if (diffuseTextures == null || diffuseTextures.isEmpty()) {
            return;
        }

        // load the image
        boolean existPngTextures = false;
        GaiaTexture texture = diffuseTextures.get(0);
        if (texture.getPath().endsWith(".png") || texture.getPath().endsWith(".PNG")) {
            existPngTextures = true;
        }

        if (texture.getBufferedImage() == null) {
            // here loads the image
            return;
        }
        int texWidth = texture.getWidth();
        int texHeight = texture.getHeight();

        // must find welded face-groups (faces group that are not connected with other faces)
        List<List<HalfEdgeFace>> weldedFacesGroups_ = new ArrayList<>();
        WeldedFacesFinder.getWeldedFacesGroups(this, weldedFacesGroups_);
        List<List<HalfEdgeFace>> mergedWeldedFacesGroups = new ArrayList<>();
        mergeWeldedFacesGroupsByTexCoords(weldedFacesGroups_, mergedWeldedFacesGroups);

        //*************************************************************************************************
        // Before do scissoring and atlasing, check:
        // If the sum of GaiaTextureScissorData-rectangle is aprox 1.0, then do not scissor.
        if (!checkIfNecessaryScissorTextures(mergedWeldedFacesGroups)) {
            log.debug("NO NEED Scissor textures");
            return;
        }
        // End checking------------------------------------------------------------------------------------

        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMap = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMap = new HashMap<>();

        // Calculate textures scissor dates.****************************************************************************
        BufferedImage srcImage = texture.getBufferedImage();
        GaiaTexture textureAtlas = new GaiaTexture();
        List<GaiaTextureScissorData> textureScissorDatas = textureAtlasManager.calculateTextureScissorDates(mergedWeldedFacesGroups,
                texWidth,
                texHeight,
                existPngTextures,
                srcImage,
                textureAtlas,
                false);

        // write the textureAtlas into a file
        String imageParentPath = texture.getParentPath();
        String texturePath = texture.getPath();
        File textureFile = new File(texturePath);
        String textureRawName = textureFile.getName();
        int lastDotIndex = textureRawName.lastIndexOf(".");
        String[] textureRawNameParts = textureRawName.split("\\.");
        String textureImageExtension = textureRawNameParts[textureRawNameParts.length - 1];

        // TODO : test
        textureImageExtension = "png";
        String textureAtlasName = textureRawNameParts[0] + "_atlas_image" + "." + textureImageExtension;
        String textureAtlasPath = imageParentPath + File.separator + textureAtlasName;

        // change the diffuseTexture path
        texture.clear(); // free memory the original texture
        textureAtlas.setPath(textureAtlasName);
        diffuseTextures.set(0, textureAtlas); // set the textureAtlas
    }

    private GaiaRectangle getTexCoordBoundingRectangle(List<HalfEdgeFace> faces, boolean invertTexCoordY, GaiaRectangle resultTexCoordBRect) {
        if (resultTexCoordBRect == null) {
            resultTexCoordBRect = new GaiaRectangle();
        }
        boolean texCoordBBoxStarted = false;
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        int facesCount = faces.size();
        GaiaRectangle faceTexCoordBRect = new GaiaRectangle();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            memSaveVertices.clear();
            faceTexCoordBRect = face.getTexCoordBoundingRectangle(faceTexCoordBRect, invertTexCoordY, memSaveVertices);

            if (!texCoordBBoxStarted) {
                resultTexCoordBRect.copyFrom(faceTexCoordBRect);
                texCoordBBoxStarted = true;
            } else {
                resultTexCoordBRect.addBoundingRectangle(faceTexCoordBRect);
            }
        }

        return resultTexCoordBRect;
    }

    private boolean checkIfNecessaryScissorTextures(List<List<HalfEdgeFace>> mergedWeldedFacesGroups) {
        // If the sum of GaiaTextureScissorData-rectangle is aprox 1.0, then do not scissor.
        int weldedFacesGroupsCount = mergedWeldedFacesGroups.size();
        boolean invertTexCoordY = false;// original
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        double totalTextureUsedArea = 0.0;
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
            List<HalfEdgeFace> weldedFacesGroup = mergedWeldedFacesGroups.get(i);
            int weldedFacesCount = weldedFacesGroup.size();
            if (weldedFacesCount == 0) {
                continue;
            }
            groupTexCoordBRect = getTexCoordBoundingRectangle(weldedFacesGroup, invertTexCoordY, groupTexCoordBRect);

            double width = groupTexCoordBRect.getWidth();
            double height = groupTexCoordBRect.getHeight();

            totalTextureUsedArea += width * height;
        }

        if (totalTextureUsedArea > 0.85) {
            return false;
        }
        return true;
    }

    public void scissorTexturesByMotherScene(GaiaMaterial material, GaiaMaterial motherMaterial) {
        // Provisionally scissor only the "DiffuseTexture"
        if (material == null) {
            return;
        }

        Map<TextureType, List<GaiaTexture>> texturesMother = motherMaterial.getTextures();
        List<GaiaTexture> diffuseTexturesMother = texturesMother.get(TextureType.DIFFUSE);
        if (diffuseTexturesMother == null || diffuseTexturesMother.isEmpty()) {
            return;
        }

        // load the image
        boolean existPngTextures = false;
        GaiaTexture textureMother = diffuseTexturesMother.get(0);
        if (textureMother.getPath().endsWith(".png") || textureMother.getPath().endsWith(".PNG")) {
            existPngTextures = true;
        }

        if (textureMother.getBufferedImage() == null) {
            // here loads the image
            return;
        }
        int texWidth = textureMother.getWidth();
        int texHeight = textureMother.getHeight();

        // must find welded face-groups (faces group that are not connected with other faces)
        List<List<HalfEdgeFace>> weldedFacesGroups_ = new ArrayList<>();
        WeldedFacesFinder.getWeldedFacesGroups(this, weldedFacesGroups_);

        List<List<HalfEdgeFace>> mergedWeldedFacesGroups = new ArrayList<>();
        mergeWeldedFacesGroupsByTexCoords(weldedFacesGroups_, mergedWeldedFacesGroups);

        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        int weldedFacesGroupsCount = mergedWeldedFacesGroups.size();
        boolean invertTexCoordY = false;// original

        //*************************************************************************************************
        // Before do scissoring and atlasing, check:
        // If the sum of GaiaTextureScissorData-rectangle is aprox 1.0, then do not scissor.
        if (!checkIfNecessaryScissorTextures(mergedWeldedFacesGroups)) {
            // if exist motherMaterial, the copy the texture.
            log.debug("NO NEED Scissor textures by Mother material");
            if (motherMaterial != null) {
                GaiaMaterial motherMaterialCopy2 = motherMaterial.clone();
                material.setTextures(motherMaterialCopy2.getTextures());
            }
            return;
        }
        // End checking------------------------------------------------------------------------------------

        // now, for each faceGroup, create a scissorData
        // there are 2 types of scissorData :
        // 1- more width than height.
        // 2- more height than width.
        List<GaiaTextureScissorData> textureScissorDatasWidth = new ArrayList<>();
        List<GaiaTextureScissorData> textureScissorDatasHeight = new ArrayList<>();

        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMap = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMap = new HashMap<>();

        // do texture atlas process
        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        if (mergedWeldedFacesGroups.size() == 0) {
            log.warn("[WARN] HalfEdgeSurface.scissorTexturesByMotherScene() : mergedWeldedFacesGroups.size() == 0.");
            return;
        }

        // Calculate textures scissor dates.****************************************************************************
        BufferedImage srcImage = textureMother.getBufferedImage();
        GaiaTexture textureAtlas = new GaiaTexture();
        List<GaiaTextureScissorData> textureScissorDatas = textureAtlasManager.calculateTextureScissorDates(mergedWeldedFacesGroups,
                texWidth,
                texHeight,
                existPngTextures,
                srcImage,
                textureAtlas,
                false);

        // write the textureAtlas into a file
        String texturePath = textureMother.getPath();
        File textureFile = new File(texturePath);
        String textureRawName = textureFile.getName();
        String[] textureRawNameParts = textureRawName.split("\\.");
        String textureImageExtension = textureRawNameParts[textureRawNameParts.length - 1];

        // TODO : test
        textureImageExtension = "png";
        String textureAtlasName = textureRawNameParts[0] + "_atlas_image" + "." + textureImageExtension;

        // change the diffuseTexture path
        textureAtlas.setPath(textureAtlasName);

        Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
        List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
        diffuseTextures.set(0, textureAtlas); // set the textureAtlas
    }


    private int mergeScissorDates(List<GaiaTextureScissorData> list) {
        int originalSize = list.size();

        // Resultado final
        List<GaiaTextureScissorData> result = new ArrayList<>(originalSize);

        // Opcional pero MUY recomendable: ordenar por X (mejora muchísimo el rendimiento)
        list.sort(Comparator.comparingDouble(d -> d.getTexCoordBoundary().getMinX()));

        for (int i = 0; i < originalSize; i++) {
            //log.debug("merge scissorDates " + i + " / " + originalSize);
            GaiaTextureScissorData current = list.get(i);

            if (current.getFaces().isEmpty()) {continue;}

            boolean merged = false;

            // Intentar mergear con los ya procesados
            for (int j = 0; j < result.size(); j++) {
                GaiaTextureScissorData existing = result.get(j);

                // 🔥 FILTRO ESPACIAL (clave para rendimiento)
                if (!existing.getTexCoordBoundary().intersects(current.getTexCoordBoundary(), 1e-6)) {
                    continue;
                }

                // 🔥 merge real (dinámico, como tu versión original)
                if (existing.mergeIfMergeable(current)) {
                    merged = true;
                    break;
                }
            }

            // Si no se ha podido mergear, lo añadimos como nuevo grupo
            if (!merged) {
                result.add(current);
            }
        }

        // Reemplazar lista original
        list.clear();
        list.addAll(result);

        return originalSize - result.size();
    }

    private int mergeScissorDates_original(List<GaiaTextureScissorData> scissorDataList) {
        boolean finished = false;
        int iterations = 0;
        while (!finished && iterations < 20) {
            int scissorDatesCount = scissorDataList.size();
            boolean merged = false;
            for (int i = 0; i < scissorDatesCount; i++) {
                GaiaTextureScissorData textureScissorData = scissorDataList.get(i);
                if (textureScissorData.getFaces().isEmpty()) {
                    continue;
                }

                for (int j = i; j < scissorDatesCount; j++) {
                    if (i == j) {
                        continue;
                    }
                    GaiaTextureScissorData textureScissorData2 = scissorDataList.get(j);
                    if (textureScissorData2.getFaces().isEmpty()) {
                        continue;
                    }
                    if (textureScissorData.mergeIfMergeable(textureScissorData2)) {
                        // remove textureScissorData2
                        scissorDataList.remove(j);
                        scissorDatesCount = scissorDataList.size();
                        j--;
                        merged = true;
                    }
                }
            }

            if (!merged) {
                finished = true;
            }

            iterations++;
        }

        // now, delete the scissorData that are merged
        int mergedCount = 0;
        List<GaiaTextureScissorData> newScissorDataList = new ArrayList<>();
        for (int i = 0; i < scissorDataList.size(); i++) {
            GaiaTextureScissorData textureScissorData = scissorDataList.get(i);
            if (textureScissorData.getFaces().isEmpty()) {
                //scissorDataList.remove(i);
                //i--;
                mergedCount++;
            } else {
                newScissorDataList.add(textureScissorData);
            }
        }

        scissorDataList.clear();
        scissorDataList.addAll(newScissorDataList);

        return mergedCount;
    }

    public List<List<HalfEdgeFace>> mergeWeldedFacesGroupsByTexCoords(List<List<HalfEdgeFace>> weldedFacesGroups, List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        if (resultWeldedFacesGroups == null) {
            resultWeldedFacesGroups = new ArrayList<>();
        }

        // now, join the groups that are connected by vertex
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        List<GaiaTextureScissorData> textureScissorDatas = new ArrayList<>();
        boolean invertTexCoordY = false;
        int weldedFacesGroupsCount = weldedFacesGroups.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(i);
            GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
            int weldedFacesCount = weldedFacesGroup.size();
            for (int j = 0; j < weldedFacesCount; j++) {
                GaiaRectangle texCoordBRect = new GaiaRectangle();
                HalfEdgeFace face = weldedFacesGroup.get(j);
                memSaveVertices.clear();
                texCoordBRect = face.getTexCoordBoundingRectangle(texCoordBRect, invertTexCoordY, memSaveVertices);

                if (j == 0) {
                    groupTexCoordBRect.copyFrom(texCoordBRect);
                } else {
                    groupTexCoordBRect.addBoundingRectangle(texCoordBRect);
                }
            }

            // create a new GaiaTextureScissorData
            double width = groupTexCoordBRect.getWidthInt();
            double height = groupTexCoordBRect.getHeightInt();

            if (width == 0 || height == 0) {
                continue;
            }

            GaiaTextureScissorData textureScissorData = new GaiaTextureScissorData();
            textureScissorData.setTexCoordBoundary(groupTexCoordBRect);
            textureScissorData.setFaces(weldedFacesGroup); // set the faces
            textureScissorDatas.add(textureScissorData);
        }

        int scissorDatesCountPre = textureScissorDatas.size();
        this.mergeScissorDates(textureScissorDatas);
        log.debug("getWeldedFacesGroups : scissorDates mergedCount = " + (scissorDatesCountPre - textureScissorDatas.size()));

        resultWeldedFacesGroups.clear();
        weldedFacesGroupsCount = textureScissorDatas.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            GaiaTextureScissorData textureScissorData = textureScissorDatas.get(i);
            List<HalfEdgeFace> weldedFacesGroup = textureScissorData.getFaces();
            resultWeldedFacesGroups.add(weldedFacesGroup);
        }

        return resultWeldedFacesGroups;
    }


    public int getTrianglesCount() {
        int hedgesCount = halfEdges.size();
        int trianglesCount = hedgesCount / 3; // provisionally
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            Vector2d texCoord = new Vector2d();
            double relPosX = (position.x - box.getMinX()) / box.getSizeX();
            double relPosY = (position.y - box.getMinY()) / box.getSizeY();

            texCoord.set(relPosX, 1.0 - relPosY);
            vertex.setTexcoords(texCoord);
        }
    }

    public void changeOutingHEdgesOfVertexIfHEdgeIsDeleted() {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdge outgoingHEdge = vertex.getOutingHalfEdge();
            if (outgoingHEdge.getStatus() == ObjectStatus.DELETED) {
                vertex.changeOutingHalfEdge();
            }
        }
    }

    private GaiaSurface getGaiaSurface(List<GaiaVertex> resultGaiaVertices) {
        // 1rst, make maps
        GaiaSurface provisionalSurface = new GaiaSurface();
        Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex = new HashMap<>();
        Map<GaiaVertex, Integer> mapGaiaVertexToIndex = new HashMap<>();

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            GaiaVertex provisionalVertex = vertex.toGaiaVertex();
            resultGaiaVertices.add(provisionalVertex);
            mapHalfEdgeVertexToGaiaVertex.put(vertex, provisionalVertex);
            mapGaiaVertexToIndex.put(provisionalVertex, i);
        }

        // now, make the provisional faces
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace halfEdgeFace = faces.get(i);
            if (halfEdgeFace == null) {
                continue;
            }

            if (halfEdgeFace.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (halfEdgeFace.isDegenerated()) {
                continue;
            }

            GaiaFace gaiaFace = new GaiaFace();

            List<HalfEdgeVertex> halfEdgeVertices = halfEdgeFace.getVertices(null);
            int faceVerticesCount = halfEdgeVertices.size();
            int[] indices = new int[faceVerticesCount];
            int indicesCount = 0;
            for (int j = 0; j < faceVerticesCount; j++) {
                HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(j);
                GaiaVertex gaiaVertex = mapHalfEdgeVertexToGaiaVertex.get(halfEdgeVertex);
                if (gaiaVertex == null) {
                    continue;
                }
                indices[j] = mapGaiaVertexToIndex.get(gaiaVertex);
                indicesCount++;
            }

            if (indicesCount > 2) {
                gaiaFace.setIndices(indices);
                provisionalSurface.getFaces().add(gaiaFace);
            } else {
                gaiaFace = null;
            }
        }

        return provisionalSurface;
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // make a provisional GaiaSurface
        List<GaiaVertex> gaiaVertices = new ArrayList<>();
        GaiaSurface gaiaSurface = this.getGaiaSurface(gaiaVertices);

        HalfEdgeUtils.weldVerticesGaiaSurface(gaiaSurface, gaiaVertices, error, checkTexCoord, checkNormal, checkColor, checkBatchId);

        // Now, delete the halfEdge objects
        this.deleteObjects();

        // now, make halfEdgeSurface from the provisionalSurface
        Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex = new HashMap<>();

        List<HalfEdge> memSaveHalfEdges = new ArrayList<>();

        // faces
        List<GaiaFace> memSaveGaiaFaces = new ArrayList<>();
        List<GaiaFace> gaiaFaces = gaiaSurface.getFaces();
        int facesCount = gaiaFaces.size();
        for (int i = 0; i < facesCount; i++) {
            GaiaFace gaiaFace = gaiaFaces.get(i);
            if (gaiaFace == null) {
                log.error("[ERROR] gaiaFace == null");
                continue;
            }
            memSaveGaiaFaces.clear();
            memSaveGaiaFaces = HalfEdgeUtils.getGaiaTriangleFacesFromGaiaFace(gaiaFace, memSaveGaiaFaces);
            int triangleFacesCount = memSaveGaiaFaces.size();
            for (int j = 0; j < triangleFacesCount; j++) {
                GaiaFace gaiaTriangleFace = memSaveGaiaFaces.get(j);
                if (gaiaTriangleFace == null) {
                    continue;
                }
                memSaveHalfEdges.clear();
                HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaTriangleFace, gaiaVertices, this, mapGaiaVertexToHalfEdgeVertex, memSaveHalfEdges);
                this.getFaces().add(halfEdgeFace);
            }
        }

        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>(mapGaiaVertexToHalfEdgeVertex.values());
        this.getVertices().addAll(halfEdgeVertices);

        // set twins
        this.setTwins();
        //this.checkSandClockFaces();

        // finally delete gaiaSurface
        gaiaSurface.clear();

        // delete gaiaVertices 20260212.***
        for (GaiaVertex gaiaVertex : gaiaVertices) {
            gaiaVertex.clear();
        }
        gaiaVertices.clear();
    }

    public void translate(Vector3d translation) {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            position.add(translation);
        }
    }

    public int[] getIndices() {
        Map<HalfEdgeVertex, Integer> vertexIndexMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertexIndexMap.put(vertex, i);
        }

        int facesCount = faces.size();
        int[] indices = new int[facesCount * 3];
        int index = 0;
        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            faceVertices.clear();
            faceVertices = face.getVertices(faceVertices);
            for (int j = 0; j < 3; j++) {
                HalfEdgeVertex vertex = faceVertices.get(j);
                int vertexIndex = vertexIndexMap.get(vertex);
                indices[index++] = vertexIndex;
            }
        }

        return indices;
    }

    public boolean getDirty() {
        return dirty;
    }

    public void joinSurface(HalfEdgeSurface newSurface) {
        this.vertices.addAll(newSurface.getVertices());
        this.faces.addAll(newSurface.getFaces());
        this.halfEdges.addAll(newSurface.getHalfEdges());
        this.dirty = true;
    }

    public Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> getMapClassifyIdToCameraDirectionTypeToFaces(Map<Integer, Map<CameraDirectionType,
            List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndObliqueCamDirType) {
        if (mapFaceGroupByClassifyIdAndObliqueCamDirType == null) {
            mapFaceGroupByClassifyIdAndObliqueCamDirType = new HashMap<>();
        }

        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            CameraDirectionType bestObliqueCameraDirectionType = face.getCameraDirectionType();
            int ClassifyId = face.getClassifyId();

            Map<CameraDirectionType, List<HalfEdgeFace>> mapFaceGroupByPlaneType = mapFaceGroupByClassifyIdAndObliqueCamDirType.computeIfAbsent(ClassifyId, k -> new HashMap<>());
            List<HalfEdgeFace> faceGroup = mapFaceGroupByPlaneType.computeIfAbsent(bestObliqueCameraDirectionType, k -> new ArrayList<>());
            faceGroup.add(face);
        }

        return mapFaceGroupByClassifyIdAndObliqueCamDirType;
    }

    public void splitFacesByBestObliqueCameraDirectionToProject() {
        // make faceGroups by classifyId & bestObliqueCameraDirectionType
        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndObliqueCamDirType = this.getMapClassifyIdToCameraDirectionTypeToFaces(null);

        // for each faceGroups make a surface
        HalfEdgeSurface newSurfaceMaster = new HalfEdgeSurface();
        boolean checkClassifyId = true;
        boolean checkBestPlaneToProject = true;
        for (Map<CameraDirectionType, List<HalfEdgeFace>> mapFaceGroupByPlaneType : mapFaceGroupByClassifyIdAndObliqueCamDirType.values()) {
            for (List<HalfEdgeFace> faceGroup : mapFaceGroupByPlaneType.values()) {
                HalfEdgeSurface newSurface = HalfEdgeCutter.createHalfEdgeSurfaceByFacesCopy(faceGroup, checkClassifyId, checkBestPlaneToProject);
                // for each faceGroup, find welded faceGroups
                List<List<HalfEdgeFace>> resultWeldedFacesGroups = WeldedFacesFinder.getWeldedFacesGroups(newSurface, null);
                for (List<HalfEdgeFace> weldedFaceGroup : resultWeldedFacesGroups) {
                    HalfEdgeSurface newSurface2 = HalfEdgeCutter.createHalfEdgeSurfaceByFacesCopy(weldedFaceGroup, checkClassifyId, checkBestPlaneToProject);
                    newSurfaceMaster.joinSurface(newSurface2);
                }

                newSurface.getFaces().clear();
            }
        }

        this.deleteObjects();
        this.joinSurface(newSurfaceMaster);
        this.setObjectIdsInList();
    }

    public void updateFacesList() {
        // remake faces list by halfEdges
        Map<HalfEdgeFace, HalfEdgeFace> mapFace = new HashMap<>();
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeFace face = halfEdge.getFace();
            if (face == null || face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            mapFace.put(face, face);
        }

        this.faces.clear();
        this.faces.addAll(mapFace.values());
    }

    public void updateVerticesList() {
        // remake vertices list by faces
        Map<HalfEdgeVertex, HalfEdgeVertex> mapVertex = new HashMap<>();
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex vertex = halfEdge.getStartVertex();
            mapVertex.put(vertex, vertex);
        }

//        int facesCount = faces.size();
//        for (int i = 0; i < facesCount; i++) {
//            HalfEdgeFace face = faces.get(i);
//            if (face == null || face.getStatus() == ObjectStatus.DELETED) {
//                continue;
//            }
//            List<HalfEdgeVertex> faceVertices = face.getVertices(null);
//            int faceVerticesCount = faceVertices.size();
//            for (int j = 0; j < faceVerticesCount; j++) {
//                HalfEdgeVertex vertex = faceVertices.get(j);
//                mapVertex.put(vertex, vertex);
//            }
//        }
//
//        int verticesCount = vertices.size();
//        for (int i = 0; i < verticesCount; i++) {
//            HalfEdgeVertex vertex = vertices.get(i);
//            if (vertex.getStatus() == ObjectStatus.DELETED) {
//                continue;
//            }
//            mapVertex.put(vertex, vertex);
//        }

        this.vertices.clear();
        this.vertices.addAll(mapVertex.values());
    }

    public void getWestEastSouthNorthVertices(GaiaBoundingBox bbox, List<HalfEdgeVertex> westVertices,
                                              List<HalfEdgeVertex> eastVertices,
                                              List<HalfEdgeVertex> southVertices,
                                              List<HalfEdgeVertex> northVertices, double error) {

        double west = bbox.getMinX();
        double east = bbox.getMaxX();
        double south = bbox.getMinY();
        double north = bbox.getMaxY();

        this.updateVerticesList();
        int verticesCount = vertices.size();

        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();

            if (Math.abs(position.x - west) < error) {
                westVertices.add(vertex);
            } else if (Math.abs(position.x - east) < error) {
                eastVertices.add(vertex);
            }
            if (Math.abs(position.y - south) < error) {
                southVertices.add(vertex);
            } else if (Math.abs(position.y - north) < error) {
                northVertices.add(vertex);
            }
        }
    }

    public double calculateArea() {
        double area = 0.0;
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            area += face.calculateArea();
        }
        return area;
    }

    public GaiaRectangle getTexCoordinateBoundingRectangle(GaiaRectangle resultRectangle) {
        if (resultRectangle == null) {
            resultRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        }
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector2d texCoord = vertex.getTexcoords();
            if (i == 0) {
                resultRectangle.setMinX(texCoord.x);
                resultRectangle.setMinY(texCoord.y);
                resultRectangle.setMaxX(texCoord.x);
                resultRectangle.setMaxY(texCoord.y);
            } else {
                resultRectangle.addPoint(texCoord);
            }
        }
        return resultRectangle;
    }


    public int getFacesCount() {
        return faces.size();
    }

    public void getIntersectedFacesByPlane(PlaneType planeType, Vector3d planePosition, List<HalfEdgeFace> resultFaces, double error) {
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (face.intersectsPlane(planeType, planePosition, error)) {
                resultFaces.add(face);
            }
        }
    }

    public boolean TEST_checkTexCoords() {
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (!face.TEST_checkTexCoords()) {
                return false;
            }
        }

        return true;
    }
}