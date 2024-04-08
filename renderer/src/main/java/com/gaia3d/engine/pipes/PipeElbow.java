package com.gaia3d.engine.pipes;

import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.engine.modeler.Modeler3D;
import com.gaia3d.engine.modeler.TEdge;
import com.gaia3d.engine.modeler.TNode;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipeElbow extends TNode {
    @Getter
    @Setter
    float elbowRadius = 0.0f;
    @Getter
    @Setter
    float pipeRadius = 0.0f;

    double sweepAngRad = 0.0;
    Vector3d elbowAxis = new Vector3d(0.0, 0.0, 1.0);
    Vector3d elbowCenterPosition = new Vector3d(); // this is NOT the position of the TNode's position.***

    // linkPositions.***
    Map<TEdge, Vector3d> mapEdgeLinkPositions;
    Map<TEdge, Vector3d> mapEdgeLinkNormals;

    int pipeRadiusInterpolationCount = 10; // 360 degrees / 10 = 36 degrees.***
    int elbowRadiusInterpolationCount = 10; // 360 degrees / 10 = 36 degrees.***

    boolean dirty = true;

    public PipeElbow(Vector3d vector3d, float elbowRadius, float pipeRadius) {
        super(vector3d);
        this.elbowRadius = elbowRadius;
        this.pipeRadius = pipeRadius;
        mapEdgeLinkPositions = new HashMap<TEdge, Vector3d>();
        mapEdgeLinkNormals = new HashMap<TEdge, Vector3d>();
    }

    private void calculateElbowPositionsIfExistOnlyOneTEdge()
    {
        TEdge edge1 = this.getEdge(0);
        TEdge edge2 = null;

        TNode tNodeA = edge1.getTheAnotherNode(this);
        TNode tNodeB = null;

        Vector3d posA = new Vector3d(tNodeA.getPosition());
        Vector3d posB = null;
        Vector3d posCenter = new Vector3d(this.getPosition());

        //                                             posCenter
        //                                                 +
        //                                               / | \
        //                                            /    |    \
        //                             linkPosA    +       |       +   linkPosB
        //                                      /          |          \
        //                                   /             |             \
        //                                /                +                \
        //                             /             elbowCenter               \
        //                          +                                             +
        //                        posA                                           posB

        // calculate the direction vector from posCenter to elbowCenter.
        Vector3d dirCA = new Vector3d(posA);
        dirCA.sub(posCenter);
        dirCA.normalize();
        mapEdgeLinkNormals.put(edge1, dirCA);

        this.sweepAngRad = 0.0;

        // calculate the elbowCenter position.
        elbowCenterPosition = new Vector3d(posCenter);

        // now, calculate linkPositionsa.***
        Vector3d linkPosA = new Vector3d(posCenter);

        mapEdgeLinkPositions.put(edge1, linkPosA);
        mapEdgeLinkNormals.put(edge1, dirCA);

        dirty = false;
    }

    public void calculateElbowPositions()
    {
        int tNodesCount = this.getEdges().size();

        // if the tNodesCount is different from 2, then return.
        if (tNodesCount == 0)
        {
            return;
        }

        if(tNodesCount == 1)
        {
            // if this TNode has only 1 edge, then return the position of the TNode.
            return;
        }

        TEdge edge1 = this.getEdge(0);
        TEdge edge2 = this.getEdge(1);

        TNode tNodeA = edge1.getTheAnotherNode(this);
        TNode tNodeB = edge2.getTheAnotherNode(this);

        Vector3d posA = new Vector3d(tNodeA.getPosition());
        Vector3d posB = new Vector3d(tNodeB.getPosition());
        Vector3d posCenter = new Vector3d(this.getPosition());

        //                                             posCenter
        //                                                 +
        //                                               / | \
        //                                            /    |    \
        //                             linkPosA    +       |       +   linkPosB
        //                                      /          |          \
        //                                   /             |             \
        //                                /                +                \
        //                             /             elbowCenter               \
        //                          +                                             +
        //                        posA                                           posB

        // calculate the direction vector from posCenter to elbowCenter.
        Vector3d dirCA = new Vector3d(posA);
        dirCA.sub(posCenter);
        dirCA.normalize();
        mapEdgeLinkNormals.put(edge1, dirCA);

        Vector3d dirCB = new Vector3d(posB);
        dirCB.sub(posCenter);
        dirCB.normalize();
        mapEdgeLinkNormals.put(edge2, dirCB);

        Vector3d dirCenter = new Vector3d(dirCA);
        dirCenter.add(dirCB);
        dirCenter.normalize();

        // calculate the angle between dirCA & dirCB.
        double angleRad = dirCA.angle(dirCB);

        // so, the angle between dirCA & dirCenter is angleRad/2.
        double semiAngRad = angleRad / 2.0;

        // calculate the sweep angle.
        double semiSweepAngRad = Math.toRadians(90) - semiAngRad;
        this.sweepAngRad = semiSweepAngRad * 2.0;

        // calculate the distance from posCenter to elbowCenter.
        // dist * sin(semiAngRad) = elbowRadius.
        double dist = elbowRadius / Math.sin(semiAngRad);

        // calculate the elbowCenter position.
        elbowCenterPosition = new Vector3d(dirCenter);
        elbowCenterPosition.mul(dist);
        elbowCenterPosition.add(posCenter);

        // now, calculate linkPositionsa.***
        double distCtoLink = Math.cos(semiAngRad) * dist;
        Vector3d linkPosA = new Vector3d(dirCA);
        linkPosA.mul(distCtoLink);
        linkPosA.add(posCenter);

        Vector3d linkPosB = new Vector3d(dirCB);
        linkPosB.mul(distCtoLink);
        linkPosB.add(posCenter);

        // calculate elbowAxis.
        Vector3d dirPosCenterToLinkA = new Vector3d(linkPosA);
        dirPosCenterToLinkA.sub(posCenter);
        dirPosCenterToLinkA.normalize();

        Vector3d dirPosCenterToLinkB = new Vector3d(linkPosB);
        dirPosCenterToLinkB.sub(posCenter);
        dirPosCenterToLinkB.normalize();

        // the elbowAxis is the cross product of dirPosCenterToLinkA & dirPosCenterToLinkB.
        elbowAxis = new Vector3d(dirPosCenterToLinkA);
        elbowAxis.cross(dirPosCenterToLinkB);
        elbowAxis.normalize();

        mapEdgeLinkPositions.put(edge1, linkPosA);
        mapEdgeLinkPositions.put(edge2, linkPosB);

        mapEdgeLinkNormals.put(edge1, dirCA);
        mapEdgeLinkNormals.put(edge2, dirCB);

        dirty = false;
    }

    public Vector3d getLinkPosition(Pipe pipe)
    {
        // if this TNode has only 1 edge, then return the position of the TNode.
        if (this.getEdges().size() == 1)
        {
            return this.getPosition();
        }

        // 1rst, calculate the elbow center position.
        if(dirty)
        {
            this.calculateElbowPositions();
        }

        Vector3d linkPosition = mapEdgeLinkPositions.get(pipe);

        return linkPosition;
    }

    public GaiaMesh makeGeometry() {
        GaiaMesh gaiaMesh = new GaiaMesh();

        if(dirty)
        {
            this.calculateElbowPositions();
        }

        if(this.getEdges().size() < 2)
        {
            // an elbow needs at least 2 edges.
            return null;
        }

        // make the circle points in local coordinates.
        List<Vector3d> circlePoints = new ArrayList<Vector3d>();
        for(int i=0; i<pipeRadiusInterpolationCount; i++) {
            double angle = 2.0 * Math.PI * i / pipeRadiusInterpolationCount;
            double x = pipeRadius * Math.cos(angle);
            double y = pipeRadius * Math.sin(angle);
            double z = 0.0;

            // make the circle points.
            Vector3d circlePoint = new Vector3d(x, y, z);
            circlePoints.add(circlePoint);
        }

        // take the 1rst edge.
        TEdge edge1 = this.getEdges().get(0);
        Vector3d dir1 = mapEdgeLinkNormals.get(edge1);
        // in the start link normal, must negate the direction.
        dir1.negate();
        Vector3d linkPos1 = mapEdgeLinkPositions.get(edge1);

        // take the 2nd edge.
        TEdge edge2 = this.getEdges().get(1);
        Vector3d dir2 = mapEdgeLinkNormals.get(edge2);
        Vector3d linkPos2 = mapEdgeLinkPositions.get(edge2);

        // starting from linkPos1, calculate the circle points.
        Modeler3D modeler3D = new Modeler3D();

        //          linkPos1
        //       <------+
        // linkNormal1  |   \
        //              |       \
        //              |           \
        //              |              \
        //              |               \
        //              |                |
        //              |                |
        //  elbowCenter +----------------+ linkPos2
        //                               |
        //                               |
        //                               |
        //                               V
        //                          linkNormal2

        Matrix4d elbowAxisRotMat = new Matrix4d();
        this.elbowRadiusInterpolationCount = (int)(Math.toDegrees(this.sweepAngRad)/10.0);
        if(this.elbowRadiusInterpolationCount < 2)
        {
            this.elbowRadiusInterpolationCount = 2;
        }
        double increSweepAngRad = this.sweepAngRad / this.elbowRadiusInterpolationCount;
        elbowAxisRotMat.rotate(-increSweepAngRad, elbowAxis);

        // calculate the 1rst transversal circle.***********************************************
        List<Vector3d> transversalCircle1 = new ArrayList<Vector3d>();
        Matrix4d tMat = modeler3D.getMatrix4FromZDir(dir1);
        Matrix4d translationMat = new Matrix4d();
        translationMat.translate(linkPos1);
        translationMat.mul(tMat, tMat);

        // transform the points of the circle.
        for (int j = 0; j < circlePoints.size(); j++) {
            Vector3d circlePoint = circlePoints.get(j);
            Vector3d transformedPoint = new Vector3d();
            tMat.transformPosition(circlePoint, transformedPoint);
            transversalCircle1.add(transformedPoint);
        }

        // end calculating the 1rst transversal circle.-----------------------------------------

        List<List<Vector3d>> transversalCircles = new ArrayList<List<Vector3d>>();
        transversalCircles.add(transversalCircle1); // add the 1rst transversal circle.

        Vector3d currentDir = new Vector3d(dir1);
        Vector3d currentPos = new Vector3d(linkPos1);
        for(int i=0; i<this.elbowRadiusInterpolationCount; i++)
        {
            List<Vector3d> transversalCircle = new ArrayList<Vector3d>();
            for (int j = 0; j < circlePoints.size(); j++) {
                Vector3d circlePoint = transversalCircle1.get(j);
                Vector3d transformedPoint = new Vector3d(circlePoint);
                transformedPoint.sub(elbowCenterPosition);
                elbowAxisRotMat.transformPosition(transformedPoint, transformedPoint);
                transformedPoint.add(elbowCenterPosition);
                transversalCircle.add(transformedPoint);
            }
            transversalCircle1 = transversalCircle;

            // rotate currentDir & currentPos.
            elbowAxisRotMat.transformPosition(currentDir, currentDir);
            currentDir.normalize();
            currentPos.sub(elbowCenterPosition);
            elbowAxisRotMat.transformPosition(currentPos, currentPos);
            currentPos.add(elbowCenterPosition);

            transversalCircles.add(transversalCircle);
        }
        boolean bottomCap = true;
        boolean topCap  = true;
        boolean isClosed = true;
        GaiaPrimitive primitive = modeler3D.getPrimitiveFromMultipleProfiles(transversalCircles, bottomCap, topCap, isClosed);
        primitive.calculateNormal();
        gaiaMesh.getPrimitives().add(primitive);

        return gaiaMesh;
    }

}
