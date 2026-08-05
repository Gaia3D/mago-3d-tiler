package com.gaia3d.basic.pipe;

import com.gaia3d.basic.geometry.network.modeler.TopologicalEdge;
import com.gaia3d.basic.geometry.network.modeler.TopologicalNetwork;
import com.gaia3d.basic.geometry.network.modeler.TopologicalNode;
import com.gaia3d.basic.model.*;
import lombok.NoArgsConstructor;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class Modeler3D {
    public TopologicalNetwork getPipeNetworkFromPipeElbows(List<PipeElbow> pipeElbows) {
        TopologicalNetwork network = new TopologicalNetwork();

        // test value
        float pipeRadius = 0.5f;
        // for rectangular pipe.
        float[] pipeRectangularSize = new float[2];
        // 0 = unknown, 1 = circular, 2 = rectangular, 3 = oval, 4 = irregular, etc.
        PipeType pipeProfileType = PipeType.UNKNOWN;

        // 1rst create elbows.
        for (PipeElbow elbow : pipeElbows) {
            network.getNodes().add(elbow);
            pipeProfileType = elbow.getProfileType();
            pipeRadius = elbow.getPipeRadius();
            pipeRectangularSize = elbow.getPipeRectangularSize();
        }

        // 2nd create pipes.
        for (int i = 0; i < pipeElbows.size() - 1; i++) {
            TopologicalNode startNode = network.getNodes().get(i);
            TopologicalNode endNode = network.getNodes().get(i + 1);
            Pipe pipe = new Pipe(startNode, endNode);
            pipe.setPipeRadius(pipeRadius);
            pipe.setProfileType(pipeProfileType);
            pipe.setPipeRectangularSize(new float[]{pipeRectangularSize[0], pipeRectangularSize[1]});
            network.getEdges().add(pipe);
        }

        network.makeTopologicalEdgesListForTopologicalNodes();
        return network;
    }

    public GaiaNode makeGeometry(TopologicalNetwork network) {
        GaiaNode resultGaiaNode = new GaiaNode();

        // 1rst, calculate elbows
        for (int i = 0; i < network.getNodes().size(); i++) {
            TopologicalNode node = network.getNodes().get(i);
            if (node instanceof PipeElbow elbow) {
                // make geometry for this elbow.
                elbow.calculateElbowPositions();
            }
        }

        for (int i = 0; i < network.getNodes().size(); i++) {
            TopologicalNode node = network.getNodes().get(i);
            if (node instanceof PipeElbow elbow) {
                // make geometry for this elbow.
                GaiaMesh elbowMesh = elbow.makeGeometry();
                if (elbowMesh != null) {
                    resultGaiaNode.getMeshes().add(elbowMesh);
                }
            }
        }

        for (int i = 0; i < network.getEdges().size(); i++) {
            TopologicalEdge edge = network.getEdges().get(i);
            if (edge instanceof Pipe pipe) {
                if (i == 0 || i == network.getEdges().size() - 1) {
                    pipe.setBottomCap(true);
                    pipe.setTopCap(true);
                } else {
                    pipe.setBottomCap(false);
                    pipe.setTopCap(false);
                }
                // make geometry for this pipe.
                GaiaMesh pipeMesh = pipe.makeGeometry();
                resultGaiaNode.getMeshes().add(pipeMesh);
            }
        }

        return resultGaiaNode;
    }

    public GaiaSurface getLateralSurface(List<GaiaVertex> vertexListA, List<GaiaVertex> vertexListB, boolean isClosed, Map<GaiaVertex, Integer> mapVertexIndex, GaiaSurface lateralSurface, boolean isSmooth) {
        // Note : the vertexListA and the vertexListB must have the same vertex count.
        //--------------------------------------------------------------------------------------

        //        vertex_0B         vertex_1B         vertex_2B         vertex_3B            ...      vertex_nB
        //        +-----------------+-----------------+-----------------+-----------------+-----------------+
        //        |              /  |              /  |              /  |              /  |              /  |
        //        |           /     |           /     |           /     |           /     |           /     |
        //        |        /        |        /        |        /        |        /        |        /        |
        //        |     /           |     /           |     /           |     /           |     /           |
        //        |  /              |  /              |  /              |  /              |  /              |
        //        +-----------------+-----------------+-----------------+-----------------+-----------------+
        //        vertex_0A         vertex_1A         vertex_2A         vertex_3A            ...      vertex_nA

        if (lateralSurface == null) {
            lateralSurface = new GaiaSurface();
        }

        // make the lateral surface.
        int vertexCount = vertexListA.size();
        for (int i = 0; i < vertexCount; i++) {

            if (i == vertexCount - 1 && !isClosed) {
                break;
            }

            GaiaVertex vertexA = vertexListA.get(i);
            GaiaVertex vertexB = vertexListB.get(i);
            GaiaVertex vertexANext = vertexListA.get((i + 1) % vertexCount);
            GaiaVertex vertexBNext = vertexListB.get((i + 1) % vertexCount);

            // create 2 gaiaFaces
            // face1 : vertexA, vertexANext, vertexBNext.
            // face2 : vertexA, vertexBNext, vertexB.
            GaiaFace face1 = new GaiaFace();
            int[] indices = new int[3];
            indices[0] = mapVertexIndex.get(vertexA);
            indices[1] = mapVertexIndex.get(vertexANext);
            indices[2] = mapVertexIndex.get(vertexBNext);
            face1.setIndices(indices);

            GaiaFace face2 = new GaiaFace();
            indices = new int[3];
            indices[0] = mapVertexIndex.get(vertexA);
            indices[1] = mapVertexIndex.get(vertexBNext);
            indices[2] = mapVertexIndex.get(vertexB);
            face2.setIndices(indices);

            lateralSurface.getFaces().add(face1);
            lateralSurface.getFaces().add(face2);
        }
        return lateralSurface;
    }

    public GaiaPrimitive getPrimitiveFromMultipleProfiles(List<List<Vector3d>> profiles, boolean bottomCap, boolean topCap, boolean isClosed, boolean isSmooth) {
        GaiaPrimitive primitive = new GaiaPrimitive();

        // 1rst, create all transversal vertices
        List<List<GaiaVertex>> transversalVerticesList = new ArrayList<>();
        int profilesCount = profiles.size();
        for (List<Vector3d> vector3ds : profiles) {
            List<GaiaVertex> transversalVertices = new ArrayList<>();
            for (Vector3d vector3d : vector3ds) {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(vector3d);
                transversalVertices.add(vertex);
                primitive.getVertices().add(vertex);
            }

            transversalVerticesList.add(transversalVertices);
        }

        // make mapVertexIndex.
        Map<GaiaVertex, Integer> mapVertexIndex = new HashMap<>();
        for (int i = 0; i < primitive.getVertices().size(); i++) {
            GaiaVertex vertex = primitive.getVertices().get(i);
            mapVertexIndex.put(vertex, i);
        }

        // create lateral surface using 2 profiles for all pair of profiles.
        GaiaSurface lateralSurface = new GaiaSurface();
        for (int i = 0; i < profilesCount - 1; i++) {
            List<GaiaVertex> transversalVerticesA = transversalVerticesList.get(i);
            List<GaiaVertex> transversalVerticesB = transversalVerticesList.get((i + 1));
            getLateralSurface(transversalVerticesA, transversalVerticesB, isClosed, mapVertexIndex, lateralSurface, isSmooth);
        }

        primitive.getSurfaces().add(lateralSurface);

        if (bottomCap) {
            // must copy the 1rst profile vertices to the bottom cap vertices.
            List<GaiaVertex> bottomCapVertices = transversalVerticesList.get(0);
            List<GaiaVertex> bottomCapVerticesCopy = new ArrayList<>();
            GaiaSurface bottomCapSurface = new GaiaSurface();
            for (int i = bottomCapVertices.size() - 1; i >= 0; i--) // bottom vertices must be in reverse order.
            {
                GaiaVertex vertex = bottomCapVertices.get(i);
                GaiaVertex bottomCapVertex = new GaiaVertex();
                bottomCapVertex.setPosition(vertex.getPosition());
                primitive.getVertices().add(bottomCapVertex);
                mapVertexIndex.put(bottomCapVertex, primitive.getVertices().size() - 1);
                bottomCapVerticesCopy.add(bottomCapVertex);
            }

            int trianglesCount = bottomCapVerticesCopy.size() - 2;
            for (int i = 0; i < trianglesCount; i++) {
                GaiaFace face = new GaiaFace();
                int[] indices = new int[3];
                indices[0] = mapVertexIndex.get(bottomCapVerticesCopy.get(0));
                indices[1] = mapVertexIndex.get(bottomCapVerticesCopy.get(i + 1));
                indices[2] = mapVertexIndex.get(bottomCapVerticesCopy.get(i + 2));
                face.setIndices(indices);
                bottomCapSurface.getFaces().add(face);
            }

            primitive.getSurfaces().add(bottomCapSurface);
        }

        if (topCap) {
            // must copy the last profile vertices to the top cap vertices.
            List<GaiaVertex> topCapVertices = transversalVerticesList.get(profilesCount - 1);
            List<GaiaVertex> topCapVerticesCopy = new ArrayList<>();
            GaiaSurface topCapSurface = new GaiaSurface();
            for (GaiaVertex vertex : topCapVertices) {
                GaiaVertex topCapVertex = new GaiaVertex();
                topCapVertex.setPosition(vertex.getPosition());
                primitive.getVertices().add(topCapVertex);
                mapVertexIndex.put(topCapVertex, primitive.getVertices().size() - 1);
                topCapVerticesCopy.add(topCapVertex);
            }

            int trianglesCount = topCapVerticesCopy.size() - 2;
            for (int i = 0; i < trianglesCount; i++) {
                GaiaFace face = new GaiaFace();
                int[] indices = new int[3];
                indices[0] = mapVertexIndex.get(topCapVerticesCopy.get(0));
                indices[1] = mapVertexIndex.get(topCapVerticesCopy.get(i + 1));
                indices[2] = mapVertexIndex.get(topCapVerticesCopy.get(i + 2));
                face.setIndices(indices);
                topCapSurface.getFaces().add(face);
            }

            primitive.getSurfaces().add(topCapSurface);
        }

        return primitive;
    }

    private boolean getConcatenableGaiaPipeLines(GaiaPipeLineString pipeLine, List<GaiaPipeLineString> pipeLines, List<GaiaPipeLineString> resultPipeLinePrev, List<GaiaPipeLineString> resultPipeLineNext) {
        // check if there are pipeLines that can be concatenated with the position.
        boolean concatenated = false;
        Vector3d masterFirstPoint = pipeLine.getPositions().get(0);
        Vector3d masterLastPoint = pipeLine.getPositions().get(pipeLine.getPositions().size() - 1);

        double tolerance = 2.0;
        for (GaiaPipeLineString currPipeLine : pipeLines) {
            if (currPipeLine == pipeLine) {
                continue;
            }
            if (!currPipeLine.intersects(pipeLine, tolerance)) {
                continue;
            }
            if (!currPipeLine.isSameProfile(pipeLine)) {
                continue;
            }
            Vector3d firstPoint = currPipeLine.getPositions().get(0);
            Vector3d lastPoint = currPipeLine.getPositions().get(currPipeLine.getPositions().size() - 1);
            double minDistance = 0.5;
            if (firstPoint.distance(masterLastPoint) < minDistance) {
                // the first point of the pipeLine is the same as the position.
                concatenated = true;
                resultPipeLineNext.add(currPipeLine);
            } else if (lastPoint.distance(masterFirstPoint) < minDistance) {
                // the last point of the pipeLine is the same as the position.
                concatenated = true;
                resultPipeLinePrev.add(currPipeLine);
            }

            // break if the resultPipeLines.size is > 1.
            if (resultPipeLinePrev.size() > 1 && resultPipeLineNext.size() > 1) {
                break;
            }
        }

        return concatenated;
    }

    private boolean concatenatePipeLineWithPipeLines(GaiaPipeLineString pipeLine, List<GaiaPipeLineString> pipeLines) {
        boolean concatenated = false;
        GaiaPipeLineString pipeLinePrev = null;
        GaiaPipeLineString pipeLineNext = null;
        List<GaiaPipeLineString> concatenablesPrev = new ArrayList<>();
        List<GaiaPipeLineString> concatenablesNext = new ArrayList<>();

        // check 1rst point.
        if (getConcatenableGaiaPipeLines(pipeLine, pipeLines, concatenablesPrev, concatenablesNext)) {
            if (concatenablesPrev.size() == 1) {
                GaiaPipeLineString pipeLinePrevCandidate = concatenablesPrev.get(0);
                // check if the radius is the same.
                if (pipeLinePrevCandidate.isSameProfile(pipeLine)) {
                    pipeLinePrev = pipeLinePrevCandidate;
                    concatenated = true;
                }
            }

            if (concatenablesNext.size() == 1) {
                GaiaPipeLineString pipeLineNextCandidate = concatenablesNext.get(0);
                // check if the radius is the same.
                if (pipeLineNextCandidate.isSameProfile(pipeLine)) {
                    pipeLineNext = pipeLineNextCandidate;
                    concatenated = true;
                }
            }
        }

        if (pipeLinePrev != null) {
            // concatenate the pipeLine with the pipeLinePrev.
            // remove the last point of pipeLinePrev.
            List<Vector3d> positions = pipeLinePrev.getPositions();
            positions.remove(positions.size() - 1);
            pipeLine.pushFrontPoints(positions);

            // remove the pipeLine from the pipeLines.
            pipeLines.remove(pipeLinePrev);
        }

        if (pipeLineNext != null) {
            // concatenate the pipeLine with the pipeLineNext.
            // remove the first point of pipeLineNext.
            List<Vector3d> positions = pipeLineNext.getPositions();
            positions.remove(0);
            pipeLine.pushBackPoints(positions);

            // remove the pipeLineNext from the pipeLines.
            pipeLines.remove(pipeLineNext);
        }

        if (concatenated) {
            pipeLine.calculateBoundingBox();
        }

        return concatenated;
    }

    public GaiaPrimitive getExtrudedPrimitive(List<Vector3d> positions, Vector3d extrusionVector, boolean bottomCap, boolean topCap, boolean isClosed, boolean isLateralSurfaceSmooth) {
        GaiaPrimitive primitive = new GaiaPrimitive();

        // lateral surface.
        // make the bottom vertices.
        List<GaiaVertex> bottomVertices = new ArrayList<>();
        for (Vector3d position : positions) {
            GaiaVertex lateralVertex = new GaiaVertex();
            lateralVertex.setPosition(position);
            bottomVertices.add(lateralVertex);

            primitive.getVertices().add(lateralVertex);
        }

        // make the top vertices.
        List<GaiaVertex> topVertices = new ArrayList<>();
        for (Vector3d vector3d : positions) {
            Vector3d position = new Vector3d(vector3d);
            position.add(extrusionVector);
            GaiaVertex lateralVertex = new GaiaVertex();
            lateralVertex.setPosition(position);
            topVertices.add(lateralVertex);

            primitive.getVertices().add(lateralVertex);
        }

        // make mapVertexIndex.
        Map<GaiaVertex, Integer> mapVertexIndex = new HashMap<>();
        for (int i = 0; i < primitive.getVertices().size(); i++) {
            GaiaVertex vertex = primitive.getVertices().get(i);
            mapVertexIndex.put(vertex, i);
        }

        GaiaSurface lateralSurface = getLateralSurface(bottomVertices, topVertices, isClosed, mapVertexIndex, null, isLateralSurfaceSmooth);
        primitive.getSurfaces().add(lateralSurface);

        if (bottomCap) {
            // must copy the 1rst profile vertices to the bottom cap vertices.
            List<GaiaVertex> bottomCapVerticesCopy = new ArrayList<>();
            GaiaSurface bottomCapSurface = new GaiaSurface();
            for (int i = bottomVertices.size() - 1; i >= 0; i--) // bottom vertices must be in reverse order.
            {
                GaiaVertex vertex = bottomVertices.get(i);
                GaiaVertex bottomCapVertex = new GaiaVertex();
                bottomCapVertex.setPosition(vertex.getPosition());
                primitive.getVertices().add(bottomCapVertex);
                mapVertexIndex.put(bottomCapVertex, primitive.getVertices().size() - 1);
                bottomCapVerticesCopy.add(bottomCapVertex);
            }

            int trianglesCount = bottomCapVerticesCopy.size() - 2;
            for (int i = 0; i < trianglesCount; i++) {
                GaiaFace face = new GaiaFace();
                int[] indices = new int[3];
                indices[0] = mapVertexIndex.get(bottomCapVerticesCopy.get(0));
                indices[1] = mapVertexIndex.get(bottomCapVerticesCopy.get(i + 1));
                indices[2] = mapVertexIndex.get(bottomCapVerticesCopy.get(i + 2));
                face.setIndices(indices);
                bottomCapSurface.getFaces().add(face);
            }

            primitive.getSurfaces().add(bottomCapSurface);
        }

        if (topCap) {
            // must copy the last profile vertices to the top cap vertices.
            List<GaiaVertex> topCapVerticesCopy = new ArrayList<>();
            GaiaSurface topCapSurface = new GaiaSurface();
            for (GaiaVertex vertex : topVertices) {
                GaiaVertex topCapVertex = new GaiaVertex();
                topCapVertex.setPosition(vertex.getPosition());
                primitive.getVertices().add(topCapVertex);
                mapVertexIndex.put(topCapVertex, primitive.getVertices().size() - 1);
                topCapVerticesCopy.add(topCapVertex);
            }

            int trianglesCount = topCapVerticesCopy.size() - 2;
            for (int i = 0; i < trianglesCount; i++) {
                GaiaFace face = new GaiaFace();
                int[] indices = new int[3];
                indices[0] = mapVertexIndex.get(topCapVerticesCopy.get(0));
                indices[1] = mapVertexIndex.get(topCapVerticesCopy.get(i + 1));
                indices[2] = mapVertexIndex.get(topCapVerticesCopy.get(i + 2));
                face.setIndices(indices);
                topCapSurface.getFaces().add(face);
            }

            primitive.getSurfaces().add(topCapSurface);
        }

        return primitive;
    }

    /*public double vector2dCross(Vector2d a, Vector2d b) {
        return a.x * b.y - a.y * b.x;
    }*/

    public int getCircleInterpolationByRadius(double radius) {
        if (radius <= 0.4) {
            return 4;
        } else if (radius <= 0.8) {
            return 6;
        } else {
            return 8;
        }
    }

    public Matrix4d getMatrix4FromZDir(Vector3d zDir) {
        // Note : the zDir is the direction of the zAxis of the transformation matrix, and must be normalized.
        Matrix4d matrix = new Matrix4d();

        // if zDir is parallel to zAxis, then return the identity matrix.
        if (zDir.z == 1.0) {
            matrix.set(new double[]{1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0});
        } else if (zDir.z == -1.0) {
            // return rotated in xAxis 180 degrees.
            matrix.set(new double[]{1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0});
        } else {

            // calculate zRot & xRot.
            Vector2d projectedY = new Vector2d(zDir.x, zDir.y);

            // calculate heading. heading = ang between (0,1) and projectedY.
            Vector2d yAxis = new Vector2d(0.0, 1.0);
            double headingRad = yAxis.angle(projectedY);
            //double headingDeg = Math.toDegrees(headingRad);

            Matrix4d headingMat = new Matrix4d();
            headingMat.rotateZ(headingRad);

            // calculate pitch.
            Vector3d zAxis = new Vector3d(0.0, 0.0, 1.0);
            double pitchAngleRad = zAxis.angle(zDir);
            Vector3d cross = new Vector3d();
            cross.cross(zDir, zAxis);
            //double pitchAngleDeg = Math.toDegrees(pitchAngleRad);
            Matrix4d pitchMat = new Matrix4d();
            pitchMat.rotateX(-pitchAngleRad);

            matrix.identity();

            matrix.mul(headingMat);
            matrix.mul(pitchMat);
        }
        return matrix;
    }

}
