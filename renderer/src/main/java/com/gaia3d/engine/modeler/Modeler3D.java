package com.gaia3d.engine.modeler;

import com.gaia3d.basic.structure.*;
import com.gaia3d.engine.pipes.Pipe;
import com.gaia3d.engine.pipes.PipeElbow;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Modeler3D
{
    public Modeler3D() {

    }

    public TNetwork TEST_getPipeTNetworkFromPointsArray(List<Vector3d> points) {
        TNetwork network = new TNetwork();

        float pipeRadius = 0.5f; // test value.***

        // 1rst create elbows.
        float elbowRadius = pipeRadius * 1.5f; // test value.***
        for (int i = 0; i < points.size(); i++) {
            PipeElbow elbow = new PipeElbow(points.get(i), elbowRadius, pipeRadius);
            elbow.setElbowRadius(elbowRadius);
            elbow.setPipeRadius(pipeRadius);
            network.getNodes().add(elbow);
        }

        // 2nd create pipes.
        for (int i = 0; i < points.size() - 1; i++) {
            TNode startNode = network.getNodes().get(i);
            TNode endNode = network.getNodes().get(i + 1);
            Pipe pipe = new Pipe(startNode, endNode);
            pipe.setPipeRadius(pipeRadius);
            network.getEdges().add(pipe);
        }

        network.makeTEdgesListForTNodes();
        return network;
    }

    public TNetwork TEST_getPipeNetworkFromPipeElbows(List<PipeElbow> pipeElbows) {
        TNetwork network = new TNetwork();

        float pipeRadius = 0.5f; // test value.***

        // 1rst create elbows.
        for (int i = 0; i < pipeElbows.size(); i++) {
            PipeElbow elbow = pipeElbows.get(i);
            network.getNodes().add(elbow);
        }

        // 2nd create pipes.
        for (int i = 0; i < pipeElbows.size() - 1; i++) {
            TNode startNode = network.getNodes().get(i);
            TNode endNode = network.getNodes().get(i + 1);
            Pipe pipe = new Pipe(startNode, endNode);
            pipe.setPipeRadius(pipeRadius);
            network.getEdges().add(pipe);
        }

        network.makeTEdgesListForTNodes();
        return network;
    }

    public GaiaNode makeGeometry(TNetwork network) {
        GaiaNode gaiaNode = new GaiaNode();

        for (int i = 0; i < network.getNodes().size(); i++) {
            TNode node = network.getNodes().get(i);
            if (node instanceof PipeElbow) {
                PipeElbow elbow = (PipeElbow)node;
                // make geometry for this elbow.
                GaiaMesh elbowMesh = elbow.makeGeometry();
                if(elbowMesh != null)
                {
                    gaiaNode.getMeshes().add(elbowMesh);
                }
            }
        }

        for (int i = 0; i < network.getEdges().size(); i++) {
            TEdge edge = network.getEdges().get(i);
            if (edge instanceof Pipe) {
                Pipe pipe = (Pipe)edge;
                // make geometry for this pipe.
                GaiaMesh pipeMesh = pipe.makeGeometry();
                gaiaNode.getMeshes().add(pipeMesh);
                int hola = 0;
            }
        }

        return gaiaNode;
    }

    public GaiaSurface getLateralSurface(List<GaiaVertex> vertexListA, List<GaiaVertex> vertexListB, boolean isClosed, Map<GaiaVertex, Integer> mapVertexIndex, GaiaSurface lateralSurface) {
        //**************************************************************************************
        // Note : the vertexListA and the vertexListB must have the same vertex count.
        //--------------------------------------------------------------------------------------

        //        vertex_0B          vertex_1B          vertex_2B          vertex_3B            ...      vertex_nB
        //        +-----------------+-----------------+-----------------+-----------------+-----------------+
        //        |              /  |              /  |              /  |              /  |              /  |
        //        |           /     |           /     |           /     |           /     |           /     |
        //        |        /        |        /        |        /        |        /        |        /        |
        //        |     /           |     /           |     /           |     /           |     /           |
        //        |  /              |  /              |  /              |  /              |  /              |
        //        +-----------------+-----------------+-----------------+-----------------+-----------------+
        //        vertex_0A          vertex_1A          vertex_2A          vertex_3A            ...      vertex_nA

        if(lateralSurface == null)
        {
            lateralSurface = new GaiaSurface();
        }

        // make the lateral surface.
        int vertexCount = vertexListA.size();
        for(int i=0; i<vertexCount; i++) {

            if(i == vertexCount - 1 && !isClosed) {
                break;
            }

            GaiaVertex vertexA = vertexListA.get(i);
            GaiaVertex vertexB = vertexListB.get(i);
            GaiaVertex vertexANext = vertexListA.get((i+1)%vertexCount);
            GaiaVertex vertexBNext = vertexListB.get((i+1)%vertexCount);

            // create 2 gaiaFaces.***
            // face1 : vertexA, vertexANext, vertexBNext.
            // face2 : vertexA, vertexBNext, vertexB.
            GaiaFace face1 = new GaiaFace();
            int indices[] = new int[3];
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

    public GaiaPrimitive getPrimitiveFromMultipleProfiles(List<List<Vector3d>> profiles, boolean bottomCap, boolean topCap, boolean isClosed)
    {
        GaiaPrimitive primitive = new GaiaPrimitive();

        // 1rst, create all transversal vertices.***
        List<List<GaiaVertex>> transversalVerticesList = new ArrayList<List<GaiaVertex>>();
        int profilesCount = profiles.size();
        for(int i=0; i<profilesCount; i++)
        {
            List<GaiaVertex> transversalVertices = new ArrayList<GaiaVertex>();
            List<Vector3d> profile = profiles.get(i);
            int pointsCount = profile.size();
            for(int j=0; j<pointsCount; j++)
            {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(profile.get(j));
                transversalVertices.add(vertex);
                primitive.getVertices().add(vertex);
            }

            transversalVerticesList.add(transversalVertices);
        }

        // make mapVertexIndex.
        Map<GaiaVertex, Integer> mapVertexIndex = new HashMap<GaiaVertex, Integer>();
        for(int i=0; i<primitive.getVertices().size(); i++) {
            GaiaVertex vertex = primitive.getVertices().get(i);
            mapVertexIndex.put(vertex, i);
        }

        // create lateral surface using 2 profiles for all pair of profiles.
        GaiaSurface lateralSurface = new GaiaSurface();
        for(int i=0; i<profilesCount-1; i++)
        {
            List<GaiaVertex> transversalVerticesA = transversalVerticesList.get(i);
            List<GaiaVertex> transversalVerticesB = transversalVerticesList.get((i+1));
            getLateralSurface(transversalVerticesA, transversalVerticesB, isClosed, mapVertexIndex, lateralSurface);
        }

        primitive.getSurfaces().add(lateralSurface);

        return primitive;
    }

    public GaiaPrimitive getExtrudedPrimitive(List<Vector3d> positions, Vector3d extrusionVector, boolean bottomCap, boolean topCap, boolean isClosed) {
        GaiaPrimitive primitive = new GaiaPrimitive();

        // lateral surface.
        // make the bottom vertices.
        List<GaiaVertex> bottomVertices = new ArrayList<GaiaVertex>();
        for(int i=0; i<positions.size(); i++) {
            Vector3d position = positions.get(i);
            GaiaVertex lateralVertex = new GaiaVertex();
            lateralVertex.setPosition(position);
            bottomVertices.add(lateralVertex);

            primitive.getVertices().add(lateralVertex);
        }

        // make the top vertices.
        List<GaiaVertex> topVertices = new ArrayList<GaiaVertex>();
        for(int i=0; i<positions.size(); i++) {
            Vector3d position = new Vector3d(positions.get(i));
            position.add(extrusionVector);
            GaiaVertex lateralVertex = new GaiaVertex();
            lateralVertex.setPosition(position);
            topVertices.add(lateralVertex);

            primitive.getVertices().add(lateralVertex);
        }

        // make mapVertexIndex.
        Map<GaiaVertex, Integer> mapVertexIndex = new HashMap<GaiaVertex, Integer>();
        for(int i=0; i<primitive.getVertices().size(); i++) {
            GaiaVertex vertex = primitive.getVertices().get(i);
            mapVertexIndex.put(vertex, i);
        }

        GaiaSurface lateralSurface = getLateralSurface(bottomVertices, topVertices, isClosed, mapVertexIndex, null);
        primitive.getSurfaces().add(lateralSurface);

        if(bottomCap)
        {
//            // make the bottom cap.
//            GaiaSurface bottomCapSurface = new GaiaSurface();
//            primitive.getSurfaces().add(bottomCapSurface);
//
//            // make the bottom cap vertices.
//            for(int i=0; i<positions.size(); i++) {
//                Vector3d position = positions.get(i);
//                GaiaVertex bottomCapVertex = new GaiaVertex();
//                bottomCapVertex.setPosition(position);
//
//            }
        }

        return primitive;
    }

    public Matrix4d getMatrix4FromZDir(Vector3d zDir)
    {
        // Note : the zDir is the direction of the zAxis of the transformation matrix, and must be normalized.
        Matrix4d matrix = new Matrix4d();

        // if zDir is parallel to zAxis, then return the identity matrix.
        if(zDir.z == 1.0)
        {
            matrix.set(new double[] {
                    1.0, 0.0, 0.0, 0.0,
                    0.0, 1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
            });
        }
        else if(zDir.z == -1.0)
        {
            // return rotated in xAxis 180 degrees.
            matrix.set(new double[] {
                    1.0, 0.0, 0.0, 0.0,
                    0.0, -1.0, 0.0, 0.0,
                    0.0, 0.0, -1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
            });
        }
        else
        {
            // calculate the rotation angle.
            double rotationAngle = Math.acos(zDir.z);
            Vector3d rotationAxis = new Vector3d(-zDir.y, zDir.x, 0.0);
            rotationAxis.normalize();

            matrix.rotation(rotationAngle, rotationAxis);
        }

        return matrix;
    }

}
