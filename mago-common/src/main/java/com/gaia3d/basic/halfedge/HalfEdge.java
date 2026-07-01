package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.remesher.PlaneHEdgeIntersectionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class HalfEdge implements Serializable {
    private HalfEdge twin = null;
    private HalfEdge next = null;
    private HalfEdgeVertex startVertex = null;
    private HalfEdgeFace face = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;
    private int id = -1;
    private int classifyId = -1; // auxiliary variable

    public void setStartVertex(HalfEdgeVertex startVertex) {
        this.startVertex = startVertex;
        if (startVertex != null) {
            startVertex.setOutingHalfEdge(this);
        }
    }

    public void setFace(HalfEdgeFace face) {
        this.face = face;
        if (face != null) {
            face.setHalfEdge(this);
        }
    }

    public boolean setTwin(HalfEdge twin) {
        if (twin == null) {
            this.twin = null;
            return true;
        }

        if (this.isTwineableByPointers(twin)) {
            this.twin = twin;
            twin.twin = this;
            return true;
        }
        return false;
    }

    public void untwin() {
        if (twin != null) {
            twin.twin = null;
            twin = null;
        }
    }

    public boolean hasTwin() {
        if (this.twin == null) {
            return false;
        } else {
            if (this.twin.getStatus() == ObjectStatus.DELETED) {
                this.twin.setTwin(null);
                this.twin = null;
                return false;
            }
        }
        return true;
    }

    public HalfEdgeVertex getEndVertex() {
        if (next == null) {
            return null;
        }
        return next.getStartVertex();
    }

    public boolean isTwineableByPointers(HalfEdge twin) {
        HalfEdgeVertex thisStartVertex = this.getStartVertex();
        HalfEdgeVertex thisEndVertex = this.getEndVertex();
        HalfEdgeVertex twinStartVertex = twin.getStartVertex();
        HalfEdgeVertex twinEndVertex = twin.getEndVertex();

        return thisStartVertex == twinEndVertex && thisEndVertex == twinStartVertex;
    }

    public boolean isTwin(HalfEdge halfEdge) {
        if (halfEdge == null || halfEdge.twin == null) {
            return false;
        }

        if (this.twin == null) {
            return false;
        }

        return halfEdge.twin == this && this.twin == halfEdge;
    }

    public double getSquaredLength() {
        if (startVertex == null || next == null) {
            return -1;
        }
        HalfEdgeVertex endVertex = next.getStartVertex();
        if (endVertex == null) {
            return -1;
        }
        if (startVertex.getPosition() == null || endVertex.getPosition() == null) {
            return -1;
        }
        return startVertex.getPosition().distanceSquared(next.getStartVertex().getPosition());
    }

    public double getLength() {
        return Math.sqrt(getSquaredLength());
    }

    public List<HalfEdge> getLoop(
            List<HalfEdge> resultHalfEdgesLoop
    ) {
        if (resultHalfEdgesLoop == null) {
            resultHalfEdgesLoop =
                    new ArrayList<>();
        }

        resultHalfEdgesLoop.add(this);

        HalfEdge nextHalfEdge =
                this.next;

        int iterationCount = 0;
        final int maximumLoopSize = 1_000_000;

        while (nextHalfEdge != null
                && nextHalfEdge != this) {

            resultHalfEdgesLoop.add(
                    nextHalfEdge
            );

            nextHalfEdge =
                    nextHalfEdge.next;

            iterationCount++;

            if (iterationCount >= maximumLoopSize) {
                throw new IllegalStateException(
                        "Malformed half-edge loop: "
                                + "the loop did not return to its initial half-edge"
                );
            }
        }

        return resultHalfEdgesLoop;
    }

    public List<HalfEdge> getLoop_original(List<HalfEdge> resultHalfEdgesLoop) {
        if (resultHalfEdgesLoop == null) {
            resultHalfEdgesLoop = new ArrayList<>();
        }
        resultHalfEdgesLoop.add(this);
        HalfEdge nextHalfEdge = this.next;
        while (nextHalfEdge != null && nextHalfEdge != this) {
            resultHalfEdgesLoop.add(nextHalfEdge);
            nextHalfEdge = nextHalfEdge.next;
        }
        return resultHalfEdgesLoop;
    }

    public HalfEdge getPrev() {
        HalfEdge prev = this;
        while (prev.next != this) {
            prev = prev.next;
            if (prev == null) {
                return null;
            }
        }
        return prev;
    }

    public Vector3d getVector(Vector3d resultVector) {
        if (resultVector == null) {
            resultVector = new Vector3d();
        }
        if (startVertex == null || next == null) {
            return null;
        }
        HalfEdgeVertex endVertex = next.getStartVertex();
        if (endVertex == null) {
            return null;
        }
        return endVertex.getPosition().sub(startVertex.getPosition(), resultVector);
    }

    public boolean isDegeneratedByPointers() {
        HalfEdgeVertex startVertex = this.getStartVertex();
        HalfEdgeVertex endVertex = this.getEndVertex();

        return startVertex == endVertex;
    }

    public boolean isDegeneratedByPositions() {
        double length = this.getLength();
        return length < 0.0001;
    }

    public void breakRelations() {
        if (this.startVertex != null) {
            if (this.startVertex.getOutingHalfEdge() == this) {
                this.startVertex.setOutingHalfEdge(null);
            }
            this.startVertex = null;
        }

        if (this.face != null) {
            if (this.face.getHalfEdge() == this) {
                this.face.setHalfEdge(null);
            }
            this.face = null;
        }

        if (this.next != null) {
            this.next = null;
        }

        if (this.twin != null) {
            this.twin.twin = null;
            this.twin = null;
        }
    }

    public void setItselfAsOutingHalfEdgeToTheStartVertex() {
        if (this.startVertex != null) {
            this.startVertex.setOutingHalfEdge(this);
        }
    }

    public boolean isApplauseEdge() {
        if (this.twin == null) {
            return false;
        }

        HalfEdgeFace face1 = this.face;
        HalfEdgeFace face2 = this.twin.face;

        if (face1 == null || face2 == null) {
            return false;
        }

        return face1.isApplauseFace(face2);
    }

    public PlaneHEdgeIntersectionType getIntersectionByPlane(
            PlaneType planeType,
            Vector3d planePosition,
            HalfEdgeVertex resultIntersectionVertex,
            double error
    ) {
        if (planeType == PlaneType.XY) {
            return getIntersectionByPlaneXY(
                    planePosition,
                    resultIntersectionVertex,
                    error
            );
        }

        if (planeType == PlaneType.XZ) {
            return getIntersectionByPlaneXZ(
                    planePosition,
                    resultIntersectionVertex,
                    error
            );
        }

        if (planeType == PlaneType.YZ) {
            return getIntersectionByPlaneYZ(
                    planePosition,
                    resultIntersectionVertex,
                    error
            );
        }

        return PlaneHEdgeIntersectionType.NONE;
    }

    private PlaneHEdgeIntersectionType getIntersectionByPlaneXY(
            Vector3d planePosition,
            HalfEdgeVertex resultIntersectionVertex,
            double error
    ) {
        HalfEdgeVertex startVertex = this.startVertex;
        HalfEdgeVertex endVertex = this.getEndVertex();

        if (startVertex == null || endVertex == null) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        Vector3d startPosition = startVertex.getPosition();
        Vector3d endPosition = endVertex.getPosition();

        if (startPosition == null || endPosition == null) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        double startDistance =
                startPosition.z - planePosition.z;

        double endDistance =
                endPosition.z - planePosition.z;

        boolean startCoincident =
                Math.abs(startDistance) < error;

        boolean endCoincident =
                Math.abs(endDistance) < error;

        /*
         * Comprobar primero el caso coplanar.
         */
        if (startCoincident && endCoincident) {
            return PlaneHEdgeIntersectionType.COPLANAR_EDGE;
        }

        if (startCoincident) {
            return PlaneHEdgeIntersectionType.START_VERTEX;
        }

        if (endCoincident) {
            return PlaneHEdgeIntersectionType.END_VERTEX;
        }

        /*
         * Ambos vértices están estrictamente en el mismo lado.
         */
        if (startDistance * endDistance > 0.0) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        /*
         * Protección equivalente a la implementación antigua.
         */
        if (Math.abs(
                startPosition.z - endPosition.z
        ) < error) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        double t =
                (planePosition.z - startPosition.z)
                        / (endPosition.z - startPosition.z);

        if (t <= 0.0 || t >= 1.0) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        Vector3d intersectionPosition =
                new Vector3d(
                        startPosition.x
                                + t * (endPosition.x - startPosition.x),

                        startPosition.y
                                + t * (endPosition.y - startPosition.y),

                        planePosition.z
                );

        resultIntersectionVertex.setPosition(
                intersectionPosition
        );

        interpolateVertexAttributes(
                startVertex,
                endVertex,
                resultIntersectionVertex,
                t
        );

        return PlaneHEdgeIntersectionType.INNER_INTERSECTION;
    }

    private PlaneHEdgeIntersectionType getIntersectionByPlaneXZ(
            Vector3d planePosition,
            HalfEdgeVertex resultIntersectionVertex,
            double error
    ) {
        HalfEdgeVertex startVertex = this.startVertex;
        HalfEdgeVertex endVertex = this.getEndVertex();

        if (startVertex == null || endVertex == null) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        Vector3d startPosition = startVertex.getPosition();
        Vector3d endPosition = endVertex.getPosition();

        if (startPosition == null || endPosition == null) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        double startDistance =
                startPosition.y - planePosition.y;

        double endDistance =
                endPosition.y - planePosition.y;

        boolean startCoincident =
                Math.abs(startDistance) < error;

        boolean endCoincident =
                Math.abs(endDistance) < error;

        if (startCoincident && endCoincident) {
            return PlaneHEdgeIntersectionType.COPLANAR_EDGE;
        }

        if (startCoincident) {
            return PlaneHEdgeIntersectionType.START_VERTEX;
        }

        if (endCoincident) {
            return PlaneHEdgeIntersectionType.END_VERTEX;
        }

        if (startDistance * endDistance > 0.0) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        if (Math.abs(
                startPosition.y - endPosition.y
        ) < error) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        double t =
                (planePosition.y - startPosition.y)
                        / (endPosition.y - startPosition.y);

        if (t <= 0.0 || t >= 1.0) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        Vector3d intersectionPosition =
                new Vector3d(
                        startPosition.x
                                + t * (endPosition.x - startPosition.x),

                        planePosition.y,

                        startPosition.z
                                + t * (endPosition.z - startPosition.z)
                );

        resultIntersectionVertex.setPosition(
                intersectionPosition
        );

        interpolateVertexAttributes(
                startVertex,
                endVertex,
                resultIntersectionVertex,
                t
        );

        return PlaneHEdgeIntersectionType.INNER_INTERSECTION;
    }

    private PlaneHEdgeIntersectionType getIntersectionByPlaneYZ(
            Vector3d planePosition,
            HalfEdgeVertex resultIntersectionVertex,
            double error
    ) {
        HalfEdgeVertex startVertex = this.startVertex;
        HalfEdgeVertex endVertex = this.getEndVertex();

        if (startVertex == null || endVertex == null) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        Vector3d startPosition = startVertex.getPosition();
        Vector3d endPosition = endVertex.getPosition();

        if (startPosition == null || endPosition == null) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        double startDistance =
                startPosition.x - planePosition.x;

        double endDistance =
                endPosition.x - planePosition.x;

        boolean startCoincident =
                Math.abs(startDistance) < error;

        boolean endCoincident =
                Math.abs(endDistance) < error;

        if (startCoincident && endCoincident) {
            return PlaneHEdgeIntersectionType.COPLANAR_EDGE;
        }

        if (startCoincident) {
            return PlaneHEdgeIntersectionType.START_VERTEX;
        }

        if (endCoincident) {
            return PlaneHEdgeIntersectionType.END_VERTEX;
        }

        if (startDistance * endDistance > 0.0) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        if (Math.abs(
                startPosition.x - endPosition.x
        ) < error) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        double t =
                (planePosition.x - startPosition.x)
                        / (endPosition.x - startPosition.x);

        if (t <= 0.0 || t >= 1.0) {
            return PlaneHEdgeIntersectionType.NONE;
        }

        Vector3d intersectionPosition =
                new Vector3d(
                        planePosition.x,

                        startPosition.y
                                + t * (endPosition.y - startPosition.y),

                        startPosition.z
                                + t * (endPosition.z - startPosition.z)
                );

        resultIntersectionVertex.setPosition(
                intersectionPosition
        );

        interpolateVertexAttributes(
                startVertex,
                endVertex,
                resultIntersectionVertex,
                t
        );

        return PlaneHEdgeIntersectionType.INNER_INTERSECTION;
    }

    private static void interpolateVertexAttributes(
            HalfEdgeVertex startVertex,
            HalfEdgeVertex endVertex,
            HalfEdgeVertex resultVertex,
            double t
    ) {
        Vector3d startNormal = startVertex.getNormal();
        Vector3d endNormal = endVertex.getNormal();

        if (startNormal != null && endNormal != null) {
            Vector3d resultNormal =
                    new Vector3d(
                            startNormal.x
                                    + t * (endNormal.x - startNormal.x),

                            startNormal.y
                                    + t * (endNormal.y - startNormal.y),

                            startNormal.z
                                    + t * (endNormal.z - startNormal.z)
                    );

            resultVertex.setNormal(resultNormal);
        }

        Vector2d startTexCoord =
                startVertex.getTexcoords();

        Vector2d endTexCoord =
                endVertex.getTexcoords();

        if (startTexCoord != null && endTexCoord != null) {
            Vector2d resultTexCoord =
                    new Vector2d(
                            startTexCoord.x
                                    + t * (endTexCoord.x - startTexCoord.x),

                            startTexCoord.y
                                    + t * (endTexCoord.y - startTexCoord.y)
                    );

            resultVertex.setTexcoords(resultTexCoord);
        }

        byte[] startColor =
                startVertex.getColor();

        byte[] endColor =
                endVertex.getColor();

        if (startColor != null
                && endColor != null
                && startColor.length >= 4
                && endColor.length >= 4) {

            byte[] resultColor =
                    new byte[4];

            for (int i = 0; i < 4; i++) {
                resultColor[i] =
                        (byte) (
                                startColor[i]
                                        + t * (
                                        endColor[i]
                                                - startColor[i]
                                )
                        );
            }

            resultVertex.setColor(resultColor);
        }
    }

    public boolean intersectsPlane(PlaneType planeType, Vector3d planePosition, double error) {
        Vector3d startVertexPosition = startVertex.getPosition();
        Vector3d endVertexPosition = getEndVertex().getPosition();

        // 1rst, check if the startPoint or endPoint touches the plane
        if (planeType == PlaneType.XY) {
            if (Math.abs(startVertexPosition.z - planePosition.z) < error) {
                return false;
            } else if (Math.abs(endVertexPosition.z - planePosition.z) < error) {
                return false;
            }
        } else if (planeType == PlaneType.YZ) {
            if (Math.abs(startVertexPosition.x - planePosition.x) < error) {
                return false;
            } else if (Math.abs(endVertexPosition.x - planePosition.x) < error) {
                return false;
            }
        } else if (planeType == PlaneType.XZ) {
            if (Math.abs(startVertexPosition.y - planePosition.y) < error) {
                return false;
            } else if (Math.abs(endVertexPosition.y - planePosition.y) < error) {
                return false;
            }
        }

        if (planeType == PlaneType.XY) {
            // check if startZ and endZ are on the same side of the plane
            return !((startVertexPosition.z - planePosition.z) * (endVertexPosition.z - planePosition.z) > 0);
        } else if (planeType == PlaneType.YZ) {
            // check if startX and endX are on the same side of the plane
            return !((startVertexPosition.x - planePosition.x) * (endVertexPosition.x - planePosition.x) > 0);
        } else if (planeType == PlaneType.XZ) {
            // check if startY and endY are on the same side of the plane
            return !((startVertexPosition.y - planePosition.y) * (endVertexPosition.y - planePosition.y) > 0);
        }

        return true;
    }

}