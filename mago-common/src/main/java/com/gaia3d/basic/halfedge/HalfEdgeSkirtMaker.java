package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
@Setter
public class HalfEdgeSkirtMaker {

    private static List<HalfEdgeVertex> getHalfEdgeVerticesOfString(List<HalfEdge> halfEdgeString) {
        //*******************************************************************************************
        //   vertex0             vertex1                               vertex2
        //     *-------------------*---------------------------------------*
        //            hedge0                  hedge1
        //
        //  in this sample, there are 2 halfEdges, and 3 vertices.
        //*******************************************************************************************
        List<HalfEdgeVertex> halfEdgeVertices = new java.util.ArrayList<>();

        int halfEdgesCount = halfEdgeString.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgeString.get(i);
            HalfEdgeVertex vertex = halfEdge.getStartVertex();
            halfEdgeVertices.add(vertex);

            if (i == halfEdgesCount - 1) {
                // add the last vertex
                HalfEdgeVertex lastVertex = halfEdge.getEndVertex();
                halfEdgeVertices.add(lastVertex);
            }
        }

        return halfEdgeVertices;
    }

    private static Vector3d getVerticalSkirtDirection(List<HalfEdge> halfEdgeString, int idx) {
        //*******************************************************************************************
        //   vertex0             vertex1                               vertex2
        //     *-------------------*---------------------------------------*
        //            hedge0                  hedge1
        //
        //  in this sample, there are 2 halfEdges, and 3 vertices.
        //*******************************************************************************************
        // idx range: 0 ~ halfEdgeString.size()
        Vector3d skirtDirection = null;
        int halfEdgesCount = halfEdgeString.size();

        if (idx == 0) {
            HalfEdge halfEdge = halfEdgeString.get(idx);
            HalfEdgeFace face0 = halfEdge.getFace();
            Vector3d face0Normal = face0.calculatePlaneNormal();
            skirtDirection = new Vector3d(face0Normal);
            skirtDirection.negate();
        } else if (idx == halfEdgesCount) {
            HalfEdge halfEdge = halfEdgeString.get(idx - 1);
            HalfEdgeFace face0 = halfEdge.getFace();
            Vector3d face0Normal = face0.calculatePlaneNormal();
            skirtDirection = new Vector3d(face0Normal);
            skirtDirection.negate();
        } else {
            HalfEdge halfEdge0 = halfEdgeString.get(idx - 1);
            HalfEdgeFace face0 = halfEdge0.getFace();
            Vector3d face0Normal = face0.calculatePlaneNormal();

            HalfEdge halfEdge1 = halfEdgeString.get(idx);
            HalfEdgeFace face1 = halfEdge1.getFace();
            Vector3d face1Normal = face1.calculatePlaneNormal();

            Vector3d averagedNormal = new Vector3d(
                    (face0Normal.x + face1Normal.x) / 2.0,
                    (face0Normal.y + face1Normal.y) / 2.0,
                    (face0Normal.z + face1Normal.z) / 2.0
            );

            if (averagedNormal.length() < 1e-8) {
                // in this case, the face0 and face1 are in "applause" form, so,
                // don't make a skirt here.
                return null;
            }

            skirtDirection = new Vector3d(face0Normal);
            skirtDirection.negate();
            skirtDirection.normalize();
        }

        return skirtDirection;
    }

    private static boolean concatenateStrings(List<HalfEdge> string1, List<HalfEdge> string2, List<HalfEdge> resultString) {
        // try to concatenate string2 to string1
        HalfEdge firstHalfEdgeInString1 = string1.get(0);
        HalfEdgeVertex firstStartVertexInString1 = firstHalfEdgeInString1.getStartVertex();

        HalfEdge lastHalfEdgeInString2 = string2.get(string2.size() - 1);
        HalfEdgeVertex lastEndVertexInString2 = lastHalfEdgeInString2.getEndVertex();
        if (firstStartVertexInString1 == lastEndVertexInString2) {
            // can concatenate
            // add string2 to the front of string1
            resultString.addAll(string2);
            resultString.addAll(string1);
            return true;
        }

        // try to concatenate string1 to string2
        HalfEdge lastHalfEdgeInString1 = string1.get(string1.size() - 1);
        HalfEdgeVertex lastEndVertexInString1 = lastHalfEdgeInString1.getEndVertex();

        HalfEdge firstHalfEdgeInString2 = string2.get(0);
        HalfEdgeVertex firstStartVertexInString2 = firstHalfEdgeInString2.getStartVertex();
        if (lastEndVertexInString1 == firstStartVertexInString2) {
            // can concatenate
            // add string1 to the end of string2
            resultString.addAll(string1);
            resultString.addAll(string2);
            return true;
        }
        return false;
    }

    private static List<List<HalfEdge>> getHalfEdgeStringsForMeshSkirt(List<HalfEdge> halfEdges) {
        List<List<HalfEdge>> resultHalfEdgeStrings = new ArrayList<>();
        Set<HalfEdge> visitedHalfEdges = new HashSet<>();

        for (HalfEdge halfEdge : halfEdges) {
            if (visitedHalfEdges.contains(halfEdge)) {
                continue;
            }

            List<HalfEdge> halfEdgeString = new ArrayList<>();
            HalfEdge currentHalfEdge = halfEdge;

            // Here, we don't find halsEdge-next.
            // If a halfEdge-startVertex is same as the previous halfEdge-endVertex, we consider it as next halfEdge in the string.
            double error = 1e-8;
            int currStringsCount = resultHalfEdgeStrings.size();
            boolean addedToString = false;
            for (int i = 0; i < currStringsCount; i++) {
                List<HalfEdge> aString = resultHalfEdgeStrings.get(i);
                HalfEdge firstHalfEdgeInString = aString.get(0);
                HalfEdge lastHalfEdgeInString = aString.get(aString.size() - 1);

                HalfEdgeVertex currentStartVertex = currentHalfEdge.getStartVertex();
                HalfEdgeVertex currentEndVertex = currentHalfEdge.getEndVertex(); // next halfEdge's startVertex is current halfEdge's endVertex

                HalfEdgeVertex firstStartVertex = firstHalfEdgeInString.getStartVertex();
                HalfEdgeVertex lastEndVertex = lastHalfEdgeInString.getEndVertex();

                if (currentEndVertex == firstStartVertex) {
                    // add to the front of the string
                    aString.add(0, currentHalfEdge);
                    visitedHalfEdges.add(currentHalfEdge);
                    addedToString = true;
                    break;
                } else if (currentStartVertex == lastEndVertex) {
                    // add to the end of the string
                    aString.add(currentHalfEdge);
                    visitedHalfEdges.add(currentHalfEdge);
                    addedToString = true;
                    break;
                }
            }

            if (!addedToString) {
                // create a new string
                List<HalfEdge> newHalfEdgeString = new ArrayList<>();
                newHalfEdgeString.add(currentHalfEdge);
                visitedHalfEdges.add(currentHalfEdge);
                resultHalfEdgeStrings.add(newHalfEdgeString);
            }
        }

        // once finished, try to connect the strings if possible
        int stringsCount = resultHalfEdgeStrings.size();
        for (int i = 0; i < stringsCount; i++) {
            List<HalfEdge> string1 = resultHalfEdgeStrings.get(i);
            for (int j = i + 1; j < stringsCount; j++) {
                List<HalfEdge> string2 = resultHalfEdgeStrings.get(j);
                List<HalfEdge> concatenatedString = new ArrayList<>();
                boolean canConcatenate = HalfEdgeSkirtMaker.concatenateStrings(string1, string2, concatenatedString);
                if (canConcatenate) {
                    // replace string1 with concatenatedString
                    resultHalfEdgeStrings.set(i, concatenatedString);
                    // remove string2
                    resultHalfEdgeStrings.remove(j);
                    stringsCount = resultHalfEdgeStrings.size();
                    // restart from the beginning
                    i = -1; // because i++ in the outer loop
                    break;
                }
            }
        }

        return resultHalfEdgeStrings;
    }

    private static void makeVerticalSkirtForHalfEdgeString(List<HalfEdge> halfEdgeString, double skirtHeight, HalfEdgeSurface surfaceMaster, int classifyId) {
        int halfEdgesCount = halfEdgeString.size();
        List<HalfEdgeVertex> vertexOfString = HalfEdgeSkirtMaker.getHalfEdgeVerticesOfString(halfEdgeString);
        int vertexCountOfString = vertexOfString.size();

        // 1rst, create verticesA and verticesB
        List<HalfEdgeVertex> verticesA = new ArrayList<>();
        List<HalfEdgeVertex> verticesB = new ArrayList<>();

        for (int j = 0; j < vertexCountOfString; j++) {
            HalfEdgeVertex vertex = vertexOfString.get(j);

            // get skirtDirection0
            Vector3d direction = HalfEdgeSkirtMaker.getVerticalSkirtDirection(halfEdgeString, j);
            if (direction == null) {
                // in this case, the face0 and face1 are in "applause" form, so,
                // don't make a skirt here.
                continue;
            }

            if (direction.z > 0.0) { // provisionally
                continue;
            }


            if (classifyId == 20 || classifyId == 21) { // south or north border
                direction.y = 0.0;
            } else if (classifyId == 22 || classifyId == 23) { // west or east border
                direction.x = 0.0;
            }

            direction.normalize();

            if (direction.length() < 1e-8) {
                log.error("Making skirt: zero length skirt direction.");
                continue;
            }

            if (Double.isNaN(direction.x) || Double.isNaN(direction.y) || Double.isNaN(direction.z)) {
                log.error("Making skirt: NaN in skirt direction.");
                continue;
            }

            direction.mul(skirtHeight);

            // copy startVertex and endVertex
            HalfEdgeVertex vertexCopy = new HalfEdgeVertex();
            vertexCopy.copyFrom(vertex);

            verticesA.add(vertexCopy);

            // create skirtVertexes
            Vector3d position = vertex.getPosition();
            Vector3d skirtPosition = new Vector3d(position.x, position.y, position.z);
            skirtPosition.add(direction); // skirtDirection0

            HalfEdgeVertex skirtVertex = new HalfEdgeVertex();
            skirtVertex.copyFrom(vertex);
            skirtVertex.setPosition(skirtPosition);

            if (Double.isNaN(skirtPosition.x) || Double.isNaN(skirtPosition.y) || Double.isNaN(skirtPosition.z)) {
                log.error("Making skirt: NaN position in endSkirtVertex.");
            }

            verticesB.add(skirtVertex);
        }

        // now create the skirt faces between verticesA and verticesB
        //
        // v0A--------------------------------v1A
        //  | \ \            he3               |
        //  |   \ \                            |
        //  |     \ \          faceB           |
        //  |       \ \                        |
        //  |         \ \                  he5 |
        //  |           \ \                    |
        //  |             \ \ he4              |
        //  |               \ \                |
        //  |             he1 \ \              |
        //  | he2               \ \            |
        //  |         faceA       \ \          |
        //  |                       \ \        |
        //  |                         \ \      |
        //  |                           \ \    |
        //  |          he0                \ \  |
        // v0B--------------------------------v1B

        int verticesCount = verticesA.size();

        if (verticesCount < 2) {
            return;
        }

        if (verticesCount != verticesB.size()) {
            log.error("Error making skirt: verticesA size != verticesB size");
            return;
        }

        surfaceMaster.getVertices().addAll(verticesA);
        surfaceMaster.getVertices().addAll(verticesB);

        for (int j = 0; j < verticesCount - 1; j++) {
            HalfEdgeVertex v0A = verticesA.get(j);
            HalfEdgeVertex v1A = verticesA.get(j + 1);
            HalfEdgeVertex v1B = verticesB.get(j + 1);
            HalfEdgeVertex v0B = verticesB.get(j);
            HalfEdgeFace newFaceA = new HalfEdgeFace();
            HalfEdgeFace newFaceB = new HalfEdgeFace();

            // create the 6 halfEdges
            HalfEdge he0 = new HalfEdge();
            HalfEdge he1 = new HalfEdge();
            HalfEdge he2 = new HalfEdge();
            HalfEdge he3 = new HalfEdge();
            HalfEdge he4 = new HalfEdge();
            HalfEdge he5 = new HalfEdge();

            // set startVertex, endVertex, face
            he0.setStartVertex(v0B);
            he0.setFace(newFaceA);
            he0.setNext(he1);
            he1.setStartVertex(v1B);
            he1.setFace(newFaceA);
            he1.setNext(he2);
            he2.setStartVertex(v0A);
            he2.setFace(newFaceA);
            he2.setNext(he0);
            newFaceA.setHalfEdge(he0); // any halfEdge

            he3.setStartVertex(v1A);
            he3.setFace(newFaceB);
            he3.setNext(he4);
            he4.setStartVertex(v0A);
            he4.setFace(newFaceB);
            he4.setNext(he5);
            he5.setStartVertex(v1B);
            he5.setFace(newFaceB);
            he5.setNext(he3);
            newFaceB.setHalfEdge(he3); // any halfEdge

            // set twins between he1 and he4
            if (!he1.setTwin(he4)) {
                log.error("Error setting twin between he1 and he4");
            }
            if (!he4.setTwin(he1)) {
                log.error("Error setting twin between he4 and he1");
            }

            // finally add the new faces to the surface
            surfaceMaster.getHalfEdges().add(he0);
            surfaceMaster.getHalfEdges().add(he1);
            surfaceMaster.getHalfEdges().add(he2);
            surfaceMaster.getHalfEdges().add(he3);
            surfaceMaster.getHalfEdges().add(he4);
            surfaceMaster.getHalfEdges().add(he5);
            surfaceMaster.getFaces().add(newFaceA);
            surfaceMaster.getFaces().add(newFaceB);
        }
    }

    public static void makeVerticalSkirtByClassifyId(HalfEdgeScene scene, double skirtHeight) {
        //**************************************************************************************************************
        // the halfEdges of the scene with classifyId 20,21,22,23 are considered as border halfEdges for skirt making.
        // 20 = south border
        // 21 = north border
        // 22 = west border
        // 23 = east border
        //**************************************************************************************************************
        GaiaBoundingBox bbox = scene.getBoundingBox();
        if (bbox == null) {
            log.info("Making skirt : Error: bbox is null");
            return;
        }

        List<HalfEdgeSurface> surfaces = new ArrayList<>();
        scene.extractSurfaces(surfaces); // surfaces size must be 1
        HalfEdgeSurface surfaceMaster = surfaces.getFirst();

        List<HalfEdge> borderHalfEdgesSouth = new ArrayList<>();
        List<HalfEdge> borderHalfEdgesNorth = new ArrayList<>();
        List<HalfEdge> borderHalfEdgesWest = new ArrayList<>();
        List<HalfEdge> borderHalfEdgesEast = new ArrayList<>();
        for (HalfEdgeSurface surface : surfaces) {
            List<HalfEdge> halfEdges = surface.getHalfEdges();
            if (halfEdges == null || halfEdges.isEmpty()) {
                continue;
            }
            for (HalfEdge halfEdge : halfEdges) {
                if (halfEdge.getClassifyId() == 20) {
                    borderHalfEdgesSouth.add(halfEdge);
                } else if (halfEdge.getClassifyId() == 21) {
                    borderHalfEdgesNorth.add(halfEdge);
                } else if (halfEdge.getClassifyId() == 22) {
                    borderHalfEdgesWest.add(halfEdge);
                } else if (halfEdge.getClassifyId() == 23) {
                    borderHalfEdgesEast.add(halfEdge);
                }
            }
        }

        // south border
        if (!borderHalfEdgesSouth.isEmpty()) {
            // now make connected halfEdges strings with borderHalfEdges
            List<List<HalfEdge>> halfEdgeStrings = HalfEdgeSkirtMaker.getHalfEdgeStringsForMeshSkirt(borderHalfEdgesSouth);
            int stringsCount = halfEdgeStrings.size();
            for (List<HalfEdge> halfEdgeString : halfEdgeStrings) {
                HalfEdgeSkirtMaker.makeVerticalSkirtForHalfEdgeString(halfEdgeString, skirtHeight, surfaceMaster, 20);
            }
        }

        // north border
        if (!borderHalfEdgesNorth.isEmpty()) {
            // now make connected halfEdges strings with borderHalfEdges
            List<List<HalfEdge>> halfEdgeStrings = HalfEdgeSkirtMaker.getHalfEdgeStringsForMeshSkirt(borderHalfEdgesNorth);
            int stringsCount = halfEdgeStrings.size();
            for (List<HalfEdge> halfEdgeString : halfEdgeStrings) {
                HalfEdgeSkirtMaker.makeVerticalSkirtForHalfEdgeString(halfEdgeString, skirtHeight, surfaceMaster, 21);
            }
        }

        // west border
        if (!borderHalfEdgesWest.isEmpty()) {
            // now make connected halfEdges strings with borderHalfEdges
            List<List<HalfEdge>> halfEdgeStrings = HalfEdgeSkirtMaker.getHalfEdgeStringsForMeshSkirt(borderHalfEdgesWest);
            int stringsCount = halfEdgeStrings.size();
            for (List<HalfEdge> halfEdgeString : halfEdgeStrings) {
                HalfEdgeSkirtMaker.makeVerticalSkirtForHalfEdgeString(halfEdgeString, skirtHeight, surfaceMaster, 22);
            }
        }

        // east border
        if (!borderHalfEdgesEast.isEmpty()) {
            // now make connected halfEdges strings with borderHalfEdges
            List<List<HalfEdge>> halfEdgeStrings = HalfEdgeSkirtMaker.getHalfEdgeStringsForMeshSkirt(borderHalfEdgesEast);
            int stringsCount = halfEdgeStrings.size();
            for (List<HalfEdge> halfEdgeString : halfEdgeStrings) {
                HalfEdgeSkirtMaker.makeVerticalSkirtForHalfEdgeString(halfEdgeString, skirtHeight, surfaceMaster, 23);
            }
        }
    }
}
