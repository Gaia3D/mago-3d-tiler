package com.gaia3d.engine.pipes;

import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.engine.modeler.Modeler3D;
import com.gaia3d.engine.modeler.TEdge;
import com.gaia3d.engine.modeler.TNode;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class Pipe extends TEdge {
    Vector3d startPosition;
    Vector3d endPosition;

    // set pipeRadius.
    @Setter
    float pipeRadius = 0.0f;

    int interpolationCount = 10; // 360 degrees / 10 = 36 degrees.***

    boolean dirty = true;

    // note : the startPosition & the endPosition are the link positions of the pipe, that are different from the startNode & the endNode.
    public Pipe(TNode startNode, TNode endNode) {

        super(startNode, endNode);
    }

    public GaiaMesh makeGeometry() {
        // make geometry for this pipe.
        TNode startNode = getStartNode();
        TNode endNode = getEndNode();

        Vector3d startLinkPos = new Vector3d();
        Vector3d endLinkPos = new Vector3d();

        // must know the types of the startNode and endNode.
        if (startNode instanceof PipeElbow) {
            PipeElbow startElbow = (PipeElbow)startNode;
            startLinkPos = startElbow.getLinkPosition(this);
        }

        if (endNode instanceof PipeElbow) {
            PipeElbow endElbow = (PipeElbow)endNode;
            endLinkPos = endElbow.getLinkPosition(this);
        }

        startPosition = startLinkPos;
        endPosition = endLinkPos;

        // calculate the circle positions on the startLinkPos and the endLinkPos.
        // make the circle points in local coordinates.
        List<Vector3d> circlePoints = new ArrayList<Vector3d>();
        for(int i=0; i<interpolationCount; i++) {
            double angle = 2.0 * Math.PI * i / interpolationCount;
            double x = pipeRadius * Math.cos(angle);
            double y = pipeRadius * Math.sin(angle);
            double z = 0.0;

            // make the circle points.
            Vector3d circlePoint = new Vector3d(x, y, z);
            circlePoints.add(circlePoint);
        }

        // calculate the transformation matrix.
        Modeler3D modeler3D = new Modeler3D();
        Vector3d dir = new Vector3d(endLinkPos).sub(startLinkPos);
        dir.normalize(); // this is the zAxis of the transformation matrix.

        Matrix4d tMat = modeler3D.getMatrix4FromZDir(dir);
        Matrix4d translationMat = new Matrix4d();
        translationMat.translate(startLinkPos);
        translationMat.mul(tMat, tMat);

        // transform the points of the circle.
        List<Vector3d> transformedCirclePoints = new ArrayList<Vector3d>();
        for(int i=0; i<circlePoints.size(); i++) {
            Vector3d circlePoint = circlePoints.get(i);
            Vector3d transformedPoint = new Vector3d();
            tMat.transformPosition(circlePoint, transformedPoint);
            transformedCirclePoints.add(transformedPoint);
        }


        Vector3d extrusionVector = new Vector3d(endLinkPos).sub(startLinkPos);
        GaiaPrimitive primitive = modeler3D.getExtrudedPrimitive(transformedCirclePoints, extrusionVector, true, true, true);
        primitive.calculateNormal();

        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);

        return mesh;
    }
}
