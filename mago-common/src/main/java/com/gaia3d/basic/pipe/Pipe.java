package com.gaia3d.basic.pipe;

import com.gaia3d.basic.geometry.network.modeler.TopologicalEdge;
import com.gaia3d.basic.geometry.network.modeler.TopologicalNode;
import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaPrimitive;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
@Setter
@Getter
public class Pipe extends TopologicalEdge {
    private PipeType profileType = PipeType.UNKNOWN;
    // set pipeRadius.
    private float pipeRadius = 0.0f;
    private float[] pipeRectangularSize = new float[2]; // for rectangular pipe.
    private int interpolationCount = 10; // 360 degrees / 10 = 36 degrees
    private boolean dirty = true;
    private boolean bottomCap = false;
    private boolean topCap = false;

    // note : the startPosition & the endPosition are the link positions of the pipe, that are different from the startNode & the endNode.
    public Pipe(TopologicalNode startNode, TopologicalNode endNode) {
        super(startNode, endNode);
    }

    public Vector3d getStartLinkPosition() {
        TopologicalNode startNode = getStartNode();
        Vector3d startLinkPos = null;
        if (startNode instanceof PipeElbow startElbow) {
            startLinkPos = startElbow.getLinkPosition(this);
        }
        return startLinkPos;
    }

    public Vector3d getEndLinkPosition() {
        TopologicalNode endNode = getEndNode();
        Vector3d endLinkPos = null;
        if (endNode instanceof PipeElbow endElbow) {
            endLinkPos = endElbow.getLinkPosition(this);
        }
        return endLinkPos;
    }

    public Vector3d getDirection() {
        Vector3d startNodePos = this.getStartNode().getPosition();
        Vector3d endNodePos = this.getEndNode().getPosition();
        Vector3d direction = new Vector3d(endNodePos).sub(startNodePos);
        direction.normalize();
        return direction;
    }

    public boolean isPhysicallyBuildable() {
        // startNodePos +----------------+-------------------+-----------------+ endNodePos
        //                               ^                   ^
        //                               |                   |
        //                               |                   |
        //                         startLinkPos           endLinkPos

        // the startLinkPos must be closer to the startNodePos than the endNodePos.
        // the endLinkPos must be closer to the endNodePos than the startNodePos.
        // say it in other words, the vector(startLinkPos, endLikPos) must be equal to the vector(startNodePos, endNodePos).
        Vector3d startLinkPos = this.getStartLinkPosition();
        Vector3d endLinkPos = this.getEndLinkPosition();

        Vector3d pipeDir = new Vector3d(endLinkPos).sub(startLinkPos);
        pipeDir.normalize();

        Vector3d edgeDir = this.getDirection();

        return !(pipeDir.dot(edgeDir) < 0.0);
    }


    private void getPipeProfilePoints(List<Vector3d> resultPoints) {
        if (profileType == PipeType.CIRCULAR) {
            Modeler3D modeler3D = new Modeler3D();
            interpolationCount = modeler3D.getCircleInterpolationByRadius(pipeRadius);

            // circle profile
            for (int i = 0; i < interpolationCount; i++) {

                double angle = 2.0 * Math.PI * i / interpolationCount;
                double x = pipeRadius * Math.cos(angle);
                double y = pipeRadius * Math.sin(angle);
                double z = 0.0;

                // make the circle points.
                Vector3d circlePoint = new Vector3d(x, y, z);
                resultPoints.add(circlePoint);
            }
        } else if (profileType == PipeType.RECTANGULAR) {
            // rectangular profile
            double halfWidth = pipeRectangularSize[0] / 2.0;
            double halfHeight = pipeRectangularSize[1] / 2.0;

            Vector3d point1 = new Vector3d(halfWidth, halfHeight, 0.0);
            Vector3d point2 = new Vector3d(-halfWidth, halfHeight, 0.0);
            Vector3d point3 = new Vector3d(-halfWidth, -halfHeight, 0.0);
            Vector3d point4 = new Vector3d(halfWidth, -halfHeight, 0.0);

            resultPoints.add(point1);
            resultPoints.add(point2);
            resultPoints.add(point3);
            resultPoints.add(point4);
        }
    }

    public GaiaMesh makeGeometry() {
        // make geometry for this pipe.
        Vector3d startLinkPos = this.getStartLinkPosition();
        Vector3d endLinkPos = this.getEndLinkPosition();

        // calculate the circle positions on the startLinkPos and the endLinkPos.
        // make the circle points in local coordinates.
        List<Vector3d> circlePoints = new ArrayList<>();
        this.getPipeProfilePoints(circlePoints);

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
        for (Vector3d circlePoint : circlePoints) {
            Vector3d transformedPoint = new Vector3d();
            tMat.transformPosition(circlePoint, transformedPoint);
            transformedCirclePoints.add(transformedPoint);
        }

        Vector3d extrusionVector = new Vector3d(endLinkPos).sub(startLinkPos);
        boolean isLateralSurfaceSmooth = true;
        GaiaPrimitive primitive = modeler3D.getExtrudedPrimitive(transformedCirclePoints, extrusionVector, this.bottomCap, this.topCap, true, isLateralSurfaceSmooth);
        primitive.calculateNormal();

        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);

        return mesh;
    }
}
