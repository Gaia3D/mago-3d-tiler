package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.util.GeometryUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Slf4j
public class HalfEdgeFace implements Serializable {
    private HalfEdge halfEdge = null;
    private Vector3d normal = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;
    private String note = null;
    private int id = -1;
    private int halfEdgeId = -1;

    // auxiliary variables
    private int classifyId = -1; // use to classify the face for some purpose
    private PlaneType bestPlaneToProject; // use to classify the face for some purpose
    private CameraDirectionType cameraDirectionType; // use to classify the face for some purpose

    public void copyFrom(HalfEdgeFace face) {
        if (face == null) {
            return;
        }
        // do not copy pointers
        if (face.normal != null) {
            this.normal = new Vector3d(face.normal);
        }
        this.status = face.status;
        this.note = face.note;
        this.id = face.id;
        this.classifyId = face.classifyId;
        this.halfEdgeId = face.halfEdgeId;
        this.bestPlaneToProject = face.bestPlaneToProject;
        this.cameraDirectionType = face.cameraDirectionType;
    }

    public double calculateAspectRatioAsTriangle() {
        List<HalfEdge> halfEdgesLoop = this.getHalfEdgesLoop(null);
        if (halfEdgesLoop == null || halfEdgesLoop.size() < 3) {
            return 0.0;
        }

        HalfEdgeVertex a = halfEdgesLoop.get(0).getStartVertex();
        HalfEdgeVertex b = halfEdgesLoop.get(1).getStartVertex();
        HalfEdgeVertex c = halfEdgesLoop.get(2).getStartVertex();

        double aspectRatio = HalfEdgeUtils.calculateAspectRatioAsTriangle(a, b, c);
        return HalfEdgeUtils.calculateAspectRatioAsTriangle(a, b, c);
    }

    public double calculateArea()
    {
        List<HalfEdge> halfEdgesLoop = this.getHalfEdgesLoop(null);
        if (halfEdgesLoop == null || halfEdgesLoop.size() < 3) {
            return 0.0;
        }

        HalfEdgeVertex a = halfEdgesLoop.get(0).getStartVertex();
        HalfEdgeVertex b = halfEdgesLoop.get(1).getStartVertex();
        HalfEdgeVertex c = halfEdgesLoop.get(2).getStartVertex();

        return HalfEdgeUtils.calculateArea(a, b, c);
    }

    public PlaneType calculateBestPlaneToProject() {
        Vector3d normal = this.calculatePlaneNormal();
//        Vector3d zAxis = new Vector3d(0, 0, 1);
//        double dotProd = normal.dot(zAxis);
//        if (dotProd > 0.5) {
//            this.bestPlaneToProject = PlaneType.XY;
//            return this.bestPlaneToProject;
//        }
        this.bestPlaneToProject = GeometryUtils.getBestPlaneToProject(normal);
        return this.bestPlaneToProject;
    }

    public Vector3d calculatePlaneNormal() {
        List<HalfEdgeVertex> vertices = this.getVertices(null);
        if (vertices == null) {
            return null;
        }

        if (vertices.size() < 3) {
            return null;
        }

        if (this.normal == null) {
            this.normal = new Vector3d();
        }
        HalfEdgeVertex vertex0 = vertices.get(0);
        HalfEdgeVertex vertex1 = vertices.get(1);
        HalfEdgeVertex vertex2 = vertices.get(2);

        Vector3d v0 = new Vector3d(vertex1.getPosition());
        v0.sub(vertex0.getPosition());

        Vector3d v1 = new Vector3d(vertex2.getPosition());
        v1.sub(vertex0.getPosition());

        this.normal = v0.cross(v1, this.normal);
        this.normal.normalize();

        return this.normal;
    }

    public Vector3d getPlaneNormal() {
        if (this.normal == null) {
            this.normal = this.calculatePlaneNormal();
        }
        return this.normal;
    }

    public List<HalfEdge> getHalfEdgesLoop(List<HalfEdge> resultHalfEdgesLoop) {
        if (this.halfEdge == null) {
            return resultHalfEdgesLoop;
        }

        if (resultHalfEdgesLoop == null) {
            resultHalfEdgesLoop = new ArrayList<>();
        }

        return this.halfEdge.getLoop(resultHalfEdgesLoop);
    }

    public List<HalfEdgeVertex> getVertices(List<HalfEdgeVertex> resultVertices) {
        if (this.halfEdge == null) {
            return resultVertices;
        }

        if (resultVertices == null) {
            resultVertices = new ArrayList<>();
        }

        List<HalfEdge> halfEdgesLoop = this.halfEdge.getLoop(null);
        for (HalfEdge halfEdge : halfEdgesLoop) {
            resultVertices.add(halfEdge.getStartVertex());
        }

        return resultVertices;
    }

    public Vector3d getBarycenter(Vector3d resultBaricenter) {
        List<HalfEdgeVertex> vertices = this.getVertices(null);
        if (vertices == null) {
            return resultBaricenter;
        }

        if (resultBaricenter == null) {
            resultBaricenter = new Vector3d();
        }

        // init
        resultBaricenter.set(0, 0, 0);

        int verticesSize = vertices.size();
        for (HalfEdgeVertex vertex : vertices) {
            resultBaricenter.add(vertex.getPosition());
        }

        resultBaricenter.div(verticesSize);
        return resultBaricenter;
    }

    public void breakRelations() {
        if (this.halfEdge == null) {
            return;
        }

        this.normal = null;

        this.halfEdge = null;
    }

    public boolean isDegenerated() {
        // if area is 0, then is degenerated
        List<HalfEdge> halfEdgesLoop = this.getHalfEdgesLoop(null);
        if (halfEdgesLoop == null) {
            return true;
        }

        for (HalfEdge halfEdge : halfEdgesLoop) {
            if (halfEdge.isDegeneratedByPointers() || halfEdge.isDegeneratedByPositions()) {
                return true;
            }
        }

        return false;
    }

    public boolean isApplauseFace(HalfEdgeFace face) {
        // Note : 2 faces are applause faces if they have same vertices.
        if (face == null) {
            return false;
        }

        if (this.halfEdge == null || face.halfEdge == null) {
            return false;
        }

        List<HalfEdgeVertex> vertices = this.getVertices(null);
        Map<HalfEdgeVertex, HalfEdgeVertex> vertexMap = new HashMap<>();
        for (HalfEdgeVertex vertex : vertices) {
            vertexMap.put(vertex, vertex);
        }

        List<HalfEdgeVertex> faceVertices = face.getVertices(null);
        for (HalfEdgeVertex vertex : faceVertices) {
            if (vertexMap.get(vertex) == null) {
                return false;
            }
        }

        return true;
    }

    public void writeFile(ObjectOutputStream outputStream) {
        try {
            if (normal != null) {
                outputStream.writeBoolean(true);
                outputStream.writeObject(normal);
            } else {
                outputStream.writeBoolean(false);
            }

            outputStream.writeObject(status);

            halfEdgeId = -1;
            if (halfEdge != null) {
                halfEdgeId = halfEdge.getId();
            }
            outputStream.writeInt(halfEdgeId);
        } catch (Exception e) {
            log.error("Error Log : ", e);
        }
    }

    public void readFile(ObjectInputStream inputStream) {
        try {
            boolean hasNormal = inputStream.readBoolean();
            if (hasNormal) {
                normal = (Vector3d) inputStream.readObject();
            } else {
                normal = null;
            }

            status = (ObjectStatus) inputStream.readObject();
            halfEdgeId = inputStream.readInt();
        } catch (Exception e) {
            log.error("Error Log : ", e);
        }
    }

    public List<HalfEdgeFace> getAdjacentFaces(List<HalfEdgeFace> resultAdjacentFaces) {
        if (this.halfEdge == null) {
            return resultAdjacentFaces;
        }

        if (resultAdjacentFaces == null) {
            resultAdjacentFaces = new ArrayList<>();
        }

        List<HalfEdge> halfEdgesLoop = this.halfEdge.getLoop(null);
        for (HalfEdge halfEdge : halfEdgesLoop) {
            HalfEdge twin = halfEdge.getTwin();
            if (twin != null) {
                HalfEdgeFace adjacentFace = twin.getFace();
                if (adjacentFace != null) {
                    resultAdjacentFaces.add(adjacentFace);
                }
            }
        }

        return resultAdjacentFaces;
    }

    public boolean getWeldedFaces(List<HalfEdgeFace> resultWeldedFaces, Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces) {
        if (this.halfEdge == null) {
            return false;
        }

        mapVisitedFaces.put(this, this);
        resultWeldedFaces.add(this);

        List<HalfEdgeFace> adjacentFaces = this.getAdjacentFaces(null);
        if (adjacentFaces == null) {
            return false;
        }

        for (HalfEdgeFace adjacentFace : adjacentFaces) {
            if (adjacentFace != null) {
                // check if is visited
                if (mapVisitedFaces.get(adjacentFace) == null) {
                    resultWeldedFaces.add(adjacentFace);
                }
            }
        }

        return true;
    }

    public boolean getWeldedFacesRecursive(List<HalfEdgeFace> resultWeldedFaces, Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces) {
        if (this.halfEdge == null) {
            return false;
        }

//        if (mapVisitedFaces.get(this) != null)
//        {
//            return false;
//        }

        mapVisitedFaces.put(this, this);
        resultWeldedFaces.add(this);

        List<HalfEdgeFace> adjacentFaces = this.getAdjacentFaces(null);
        if (adjacentFaces == null) {
            return false;
        }

        for (HalfEdgeFace adjacentFace : adjacentFaces) {
            if (adjacentFace != null) {
                // check if is visited
                if (mapVisitedFaces.get(adjacentFace) == null) {
                    adjacentFace.getWeldedFacesRecursive(resultWeldedFaces, mapVisitedFaces);
                }
            }
        }

        return true;
    }

    public GaiaRectangle getTexCoordBoundingRectangle(GaiaRectangle resultRectangle, boolean invertY) {
        List<HalfEdgeVertex> vertices = this.getVertices(null);
        if (vertices == null) {
            return resultRectangle;
        }

        if (resultRectangle == null) {
            resultRectangle = new GaiaRectangle();
        }

        int verticesSize = vertices.size();
        for (int i = 0; i < verticesSize; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector2d texCoord = vertex.getTexcoords();
            double x = texCoord.x;
            double y = texCoord.y;

            if (invertY) {
                y = 1.0 - y;
            }
            if (i == 0) {
                resultRectangle.setMinX(x);
                resultRectangle.setMaxX(x);
                resultRectangle.setMinY(y);
                resultRectangle.setMaxY(y);
            } else {
                resultRectangle.addPoint(x, y);
            }
        }

        return resultRectangle;
    }

    public boolean intersectsPlane(PlaneType planeType, Vector3d planePosition, double error) {
        List<HalfEdge> halfEdgesLoop = this.getHalfEdgesLoop(null);
        if (halfEdgesLoop == null) {
            return false;
        }

        boolean intersects = false;
        for (HalfEdge halfEdge : halfEdgesLoop) {
            intersects = halfEdge.intersectsPlane(planeType, planePosition, error);
            if (intersects) {
                return true;
            }
        }

        return false;
    }

    public GaiaBoundingBox getBoundingBox() {
        List<HalfEdgeVertex> vertices = this.getVertices(null);
        if (vertices == null) {
            return null;
        }

        GaiaBoundingBox resultBBox = new GaiaBoundingBox();

        for (HalfEdgeVertex vertex : vertices) {
            resultBBox.addPoint(vertex.getPosition());
        }

        return resultBBox;
    }
}
