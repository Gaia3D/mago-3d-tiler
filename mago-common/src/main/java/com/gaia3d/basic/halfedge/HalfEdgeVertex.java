package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.model.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HalfEdgeVertex implements Serializable {
    private Vector2d texcoords;
    private Vector3d position;
    private Vector3d normal;
    private byte[] color;
    private float batchId;
    private HalfEdge outingHalfEdge = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;
    private PositionType positionType = null;
    private int id = -1;
    private int classifyId = -1; // auxiliary variable
    private float roughness = 0.0f; // auxiliary variable

    public HalfEdgeVertex(GaiaVertex vertex) {
        copyFromGaiaVertex(vertex);
    }

    public void deleteObjects() {
        this.texcoords = null;
        this.position = null;
        this.normal = null;
        this.color = null;
        this.batchId = 0;
        this.outingHalfEdge = null;
        this.positionType = null;
        this.status = ObjectStatus.DELETED;
    }

    public void copyFrom(HalfEdgeVertex vertex) {
        if (vertex.texcoords != null) {
            this.texcoords = new Vector2d(vertex.texcoords);
        }

        if (vertex.position != null) {
            this.position = new Vector3d(vertex.position);
        }

        if (vertex.normal != null) {
            this.normal = new Vector3d(vertex.normal);
        }

        if (vertex.color != null) {
            this.color = vertex.color.clone();
        }

        this.batchId = vertex.batchId;
        // no copy pointer
        this.status = vertex.status;
        this.positionType = vertex.positionType;
        this.id = vertex.id;
        //this.outingHalfEdgeId = vertex.outingHalfEdgeId;
        this.classifyId = vertex.classifyId;
    }

    public void copyFromGaiaVertex(GaiaVertex vertex) {
        Vector3d position = vertex.getPosition();
        Vector3d normal = vertex.getNormal();
        Vector2d texcoords = vertex.getTexcoords();
        byte[] color = vertex.getColor();
        float batchId = vertex.getBatchId();

        this.position = new Vector3d(position);

        if (normal != null) {
            this.normal = new Vector3d(normal);
        }

        if (texcoords != null) {
            this.texcoords = new Vector2d(texcoords);
        }

        if (color != null) {
            this.color = color.clone();
        }

        this.batchId = batchId;
    }

    public GaiaVertex toGaiaVertex() {
        GaiaVertex vertex = new GaiaVertex();
        vertex.setPosition(new Vector3d(position));

        if (normal != null) {
            vertex.setNormal(new Vector3d(normal));
        }

        if (texcoords != null) {
            vertex.setTexcoords(new Vector2d(texcoords));
        }

        if (color != null) {
            vertex.setColor(color.clone());
        }

        vertex.setBatchId(batchId);

        return vertex;
    }

    public Vector3d calculateNormal() {
        List<HalfEdgeFace> faces = this.getFaces(null);
        Vector3d normal = new Vector3d();
        for (HalfEdgeFace face : faces) {
            Vector3d faceNormal = face.calculatePlaneNormal();
            normal.add(faceNormal);
        }

        normal.normalize();
        return normal;
    }

    public List<HalfEdge> getIncomingHalfEdges(List<HalfEdge> resultHalfEdges) {
        if (this.outingHalfEdge == null) {
            return resultHalfEdges;
        }

        if (this.outingHalfEdge.getStatus() == ObjectStatus.DELETED) {
            log.info("HalfEdgeVertex.getIncomingHalfEdges() : outingHalfEdge is deleted!.");
        }

        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }

        List<HalfEdge> outingEdges = this.getOutingHalfEdges(null);
        for (HalfEdge edge : outingEdges) {
            HalfEdge prevEdge = edge.getPrev();
            if (prevEdge != null) {
                resultHalfEdges.add(prevEdge);
            }
        }

        return resultHalfEdges;
    }

    private static final int MAX_OUTGOING_HALF_EDGES = 50;

    private static boolean containsIdentity(
            List<HalfEdge> halfEdges,
            int startIndex,
            HalfEdge target
    ) {
        for (int i = startIndex; i < halfEdges.size(); i++) {
            if (halfEdges.get(i) == target) {
                return true;
            }
        }

        return false;
    }

    public List<HalfEdge> getOutingHalfEdges_new(
            List<HalfEdge> resultHalfEdges
    ) {
        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>(8);
        }

        HalfEdge startEdge = this.outingHalfEdge;

        if (startEdge == null) {
            return resultHalfEdges;
        }

        if (startEdge.getStatus() == ObjectStatus.DELETED) {
            log.warn(
                    "HalfEdgeVertex.getOutingHalfEdges(): "
                            + "outingHalfEdge is deleted."
            );
            return resultHalfEdges;
        }

        /*
         * La lista recibida puede contener resultados anteriores.
         * Solo buscamos ciclos entre las aristas añadidas por esta llamada.
         */
        final int resultStartIndex = resultHalfEdges.size();

        resultHalfEdges.add(startEdge);

        /*
         * Primera dirección:
         *
         * outgoingEdge
         *     -> twin
         *     -> next outgoing edge alrededor del vértice
         */
        HalfEdge currentEdge = startEdge;
        boolean boundaryFound = false;

        while (true) {
            HalfEdge twin = currentEdge.getTwin();

            if (twin == null) {
                boundaryFound = true;
                break;
            }

            HalfEdge nextOutgoingEdge = twin.getNext();

            if (nextOutgoingEdge == null) {
                log.warn(
                        "HalfEdgeVertex.getOutingHalfEdges(): "
                                + "twin.getNext() is null."
                );
                boundaryFound = true;
                break;
            }

            /*
             * Hemos dado una vuelta completa en un vértice interior.
             */
            if (nextOutgoingEdge == startEdge) {
                break;
            }

            /*
             * Protección ante topología corrupta o ciclos inesperados.
             */
            if (containsIdentity(
                    resultHalfEdges,
                    resultStartIndex,
                    nextOutgoingEdge
            )) {
                log.warn(
                        "HalfEdgeVertex.getOutingHalfEdges(): "
                                + "unexpected half-edge cycle detected."
                );
                break;
            }

            if (nextOutgoingEdge.getStatus() == ObjectStatus.DELETED) {
//                log.warn(
//                        "HalfEdgeVertex.getOutingHalfEdges(): "
//                                + "encountered a deleted half-edge."
//                );
                break;
            }

            resultHalfEdges.add(nextOutgoingEdge);

            if (resultHalfEdges.size() - resultStartIndex
                    >= MAX_OUTGOING_HALF_EDGES) {

                log.error(
                        "HalfEdgeVertex.getOutingHalfEdges(): "
                                + "outgoing half-edge count reached {}.",
                        MAX_OUTGOING_HALF_EDGES
                );
                return resultHalfEdges;
            }

            currentEdge = nextOutgoingEdge;
        }

        /*
         * En un vértice interior, la primera búsqueda ya recorrió
         * todas las aristas.
         */
        if (!boundaryFound) {
            return resultHalfEdges;
        }

        /*
         * Segunda dirección para un vértice de borde:
         *
         * start outgoing
         *     -> previous incoming
         *     -> twin outgoing
         */
        HalfEdge incomingEdge = startEdge.getPrev();

        if (incomingEdge == null) {
            return resultHalfEdges;
        }

        HalfEdge reverseOutgoingEdge = incomingEdge.getTwin();

        while (reverseOutgoingEdge != null) {
            if (reverseOutgoingEdge == startEdge) {
                break;
            }

            if (containsIdentity(
                    resultHalfEdges,
                    resultStartIndex,
                    reverseOutgoingEdge
            )) {
                break;
            }

            if (reverseOutgoingEdge.getStatus() == ObjectStatus.DELETED) {
                log.warn(
                        "HalfEdgeVertex.getOutingHalfEdges(): "
                                + "encountered a deleted reverse half-edge."
                );
                break;
            }

            resultHalfEdges.add(reverseOutgoingEdge);

            if (resultHalfEdges.size() - resultStartIndex
                    >= MAX_OUTGOING_HALF_EDGES) {

                log.error(
                        "HalfEdgeVertex.getOutingHalfEdges(): "
                                + "outgoing half-edge count reached {}.",
                        MAX_OUTGOING_HALF_EDGES
                );
                break;
            }

            HalfEdge previousEdge = reverseOutgoingEdge.getPrev();

            if (previousEdge == null) {
                break;
            }

            reverseOutgoingEdge = previousEdge.getTwin();
        }

        return resultHalfEdges;
    }

    public List<HalfEdge> getOutingHalfEdges(List<HalfEdge> resultHalfEdges) {
        if (this.outingHalfEdge == null) {
            return resultHalfEdges;
        }

        if (this.outingHalfEdge.getStatus() == ObjectStatus.DELETED) {
            log.info("HalfEdgeVertex.getOutingHalfEdges() : outingHalfEdge is deleted!.");
        }

        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }

        Set<HalfEdge> visited = new HashSet<>();

        boolean isInterior = true; // init as true
        HalfEdge currentEdge = this.outingHalfEdge;
        resultHalfEdges.add(currentEdge);
        HalfEdge currTwin = currentEdge.getTwin();
        visited.add(currentEdge);
        if (currTwin == null) {
            isInterior = false;
        } else {
            HalfEdge nextOutingEdge = currTwin.getNext();
            while (nextOutingEdge != this.outingHalfEdge) {
                if (visited.contains(nextOutingEdge)) {
                    break;
                }
                visited.add(nextOutingEdge);
                resultHalfEdges.add(nextOutingEdge);
                if (resultHalfEdges.size() > 50) {
                    log.error("Error: HalfEdgeVertex.getOutingHalfEdges() : outingHalfEdges.size() > 50");
                    return resultHalfEdges;
                }
                currTwin = nextOutingEdge.getTwin();
                if (currTwin == null) {
                    isInterior = false;
                    break;
                }
                nextOutingEdge = currTwin.getNext();
            }
        }

        if (!isInterior) {
            // search from incomingEdge
            HalfEdge incomingEdge = this.outingHalfEdge.getPrev();
            HalfEdge outingEdge = incomingEdge.getTwin();
            if (outingEdge == null) {
                return resultHalfEdges;
            }

            resultHalfEdges.add(outingEdge);

            HalfEdge prevEdge = outingEdge.getPrev();
            HalfEdge prevTwin = prevEdge.getTwin();
            while (prevTwin != null && prevTwin != outingEdge) {
                if (visited.contains(prevTwin)) {
                    break;
                }
                visited.add(prevTwin);
                resultHalfEdges.add(prevTwin);
                prevEdge = prevTwin.getPrev();
                if (prevEdge == null) {
                    break;
                }
                prevTwin = prevEdge.getTwin();
            }
        }

        return resultHalfEdges;
    }

    public boolean changeOutingHalfEdge() {
        if (this.outingHalfEdge.getStatus() != ObjectStatus.DELETED) {
            return true;
        }

        List<HalfEdge> outingEdges = this.getOutingHalfEdges(null);
        if (outingEdges == null) {
            return false;
        }

        for (HalfEdge edge : outingEdges) {
            if (edge.getStatus() != ObjectStatus.DELETED) {
                this.outingHalfEdge = edge;
                return true;
            }
        }

        return true;
    }

    public PositionType getPositionType() {
        if (this.positionType == null) {
            if (this.outingHalfEdge != null) {
                List<HalfEdge> outingEdges = this.getOutingHalfEdges(null);
                for (HalfEdge edge : outingEdges) {
                    if (edge.getTwin() == null) {
                        this.positionType = PositionType.BOUNDARY_EDGE;
                        break;
                    }
                }

                if (this.positionType == null) {
                    this.positionType = PositionType.INTERIOR;
                }
            }
        }
        return positionType;
    }

    public List<HalfEdgeFace> getFaces(List<HalfEdgeFace> resultFaces) {
        if (this.outingHalfEdge == null) {
            return resultFaces;
        }

        if (this.outingHalfEdge.getStatus() == ObjectStatus.DELETED) {
            log.info("HalfEdgeVertex.getFaces() : outingHalfEdge is deleted!.");
        }

        if (resultFaces == null) {
            resultFaces = new ArrayList<>();
        }

        List<HalfEdge> outingEdges = this.getOutingHalfEdges(null);
        for (HalfEdge edge : outingEdges) {
            HalfEdgeFace face = edge.getFace();
            if (face != null) {
                resultFaces.add(face);
            }
        }

        return resultFaces;
    }

    public boolean isWeldable(HalfEdgeVertex vertex2, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // 1rst, check position
        double distance = position.distance(vertex2.position);
        if (distance > error) {
            return false;
        }

        // 2nd, check texCoord
        if (checkTexCoord && texcoords != null && vertex2.texcoords != null) {
            double texCoordDist = texcoords.distance(vertex2.texcoords);
            if (texCoordDist > error) {
                return false;
            }
        }

        // 3rd, check normal
        if (checkNormal && normal != null && vertex2.normal != null) {
            if (normal.distance(vertex2.normal) > error) {
                return false;
            }
        }

        // 4th, check color
        if (checkColor && color != null && vertex2.color != null) {
            for (int i = 0; i < color.length; i++) {
                if (Math.abs(color[i] - vertex2.color[i]) > error) {
                    return false;
                }
            }
        }

        // 5th, check batchId
        return !checkBatchId || !(Math.abs(batchId - vertex2.batchId) > error);
    }


}