package com.gaia3d.basic.geometry.networkStructure.pipes;

import com.gaia3d.basic.geometry.networkStructure.modeler.Modeler3D;
import com.gaia3d.basic.geometry.networkStructure.modeler.TEdge;
import com.gaia3d.basic.geometry.networkStructure.modeler.TNode;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaPrimitive;
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
    int pipeProfileType = 0; // 0 = unknown, 1 = circular, 2 = rectangular, 3 = oval, 4 = irregular, etc.

    @Getter
    @Setter
    float elbowRadius = 0.0f;
    @Getter
    @Setter
    float pipeRadius = 0.0f;
    @Getter
    @Setter
    float[] pipeRectangularSize = new float[2]; // for rectangular pipe.

    double sweepAngRad = 0.0;
    Vector3d elbowAxis = new Vector3d(0.0, 0.0, 1.0);
    Vector3d elbowCenterPosition = new Vector3d(); // this is NOT the position of the TNode's position.***
    @Getter
    @Setter
    int elbowType = 1; // 0 = unknown, 1 = straight, 2 = toroidal, 3 = spherical.***

    // linkPositions.***
    Map<TEdge, Vector3d> mapEdgeLinkPositions;
    Map<TEdge, Vector3d> mapEdgeLinkNormals;

    int pipeRadiusInterpolationCount = 10; // 360 degrees / 10 = 36 degrees.***
    int elbowRadiusInterpolationCount = 10; // 360 degrees / 10 = 36 degrees.***

    boolean dirty = true;

//    public PipeElbow(Vector3d vector3d, float elbowRadius, float pipeRadius)
//    {
//        super(vector3d);
//        this.elbowRadius = elbowRadius;
//        this.pipeRadius = pipeRadius;
//        mapEdgeLinkPositions = new HashMap<TEdge, Vector3d>();
//        mapEdgeLinkNormals = new HashMap<TEdge, Vector3d>();
//    }

    public PipeElbow(Vector3d vector3d, int pipeProfileType, float elbowRadius)//, float pipeRadius, float[] pipeRectangularSize)
    {
        super(vector3d);
        this.pipeProfileType = pipeProfileType;
        this.elbowRadius = elbowRadius;
        this.pipeRadius = pipeRadius;
        this.pipeRectangularSize = pipeRectangularSize;
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

        // if the sweep angle is less than 0.1 deg, then this elbow has no mesh.
        double minSweepAngRad = Math.toRadians(0.1);
        double maxSweepAngRad = Math.toRadians(179.0);

        if(this.sweepAngRad < minSweepAngRad || this.sweepAngRad > maxSweepAngRad)
        {
            // if the sweep angle is less than 0.1 deg, then this elbow has no mesh.
            elbowCenterPosition = new Vector3d(posCenter);

            // now, calculate linkPositionsa.***
            Vector3d linkPosA = new Vector3d(posCenter);

            mapEdgeLinkPositions.put(edge1, linkPosA);
            mapEdgeLinkNormals.put(edge1, dirCA);

            Vector3d linkPosB = new Vector3d(posCenter);

            mapEdgeLinkPositions.put(edge2, linkPosB);
            mapEdgeLinkNormals.put(edge2, dirCB);

            return;
        }

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

        // check if the elbowAxis has nan components.
        if(Double.isNaN(elbowAxis.x) || Double.isNaN(elbowAxis.y) || Double.isNaN(elbowAxis.z))
        {
            int hola = 0;
        }

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

    private void getPipeProfilePoints(List<Vector3d> resultPoints)
    {
        if(this.getPipeProfileType() == 1)
        {
            // circle profile.***
            for(int i=0; i<pipeRadiusInterpolationCount; i++)
            {
                double angle = 2.0 * Math.PI * i / pipeRadiusInterpolationCount;
                double x = pipeRadius * Math.cos(angle);
                double y = pipeRadius * Math.sin(angle);
                double z = 0.0;

                // make the circle points.
                Vector3d circlePoint = new Vector3d(x, y, z);
                resultPoints.add(circlePoint);
            }
        }
        else if(this.getPipeProfileType() == 2)
        {
            // rectangular profile.***
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
        else
        {
            // unknown profile.***
            return;
        }
    }

    private GaiaMesh makeStraightElbowGeometry()
    {
        GaiaMesh gaiaMesh = new GaiaMesh();

        // make the circle points in local coordinates.
        List<Vector3d> circlePoints = new ArrayList<Vector3d>();
        this.getPipeProfilePoints(circlePoints);

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

        Matrix4d elbowAxisRotMat = new Matrix4d();
        this.elbowRadiusInterpolationCount = (int)(Math.toDegrees(this.sweepAngRad)/10.0);
        if(this.elbowRadiusInterpolationCount < 2)
        {
            this.elbowRadiusInterpolationCount = 2;
        }

        if(this.sweepAngRad < Math.toRadians(2.0))
        {
            int hola = 0;
        }
        double increSweepAngRad = this.sweepAngRad / this.elbowRadiusInterpolationCount;
        elbowAxisRotMat.rotate(-increSweepAngRad, elbowAxis);

        Vector3d thisNodePos = new Vector3d(this.getPosition());

        // calculate the 1rst transversal circle.***********************************************
        List<Vector3d> transversalCircle1 = new ArrayList<Vector3d>();
        Matrix4d tMat = modeler3D.getMatrix4FromZDir(dir1);
        Matrix4d translationMat = new Matrix4d();
        translationMat.translate(thisNodePos);
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
                transformedPoint.sub(thisNodePos);
                elbowAxisRotMat.transformPosition(transformedPoint, transformedPoint);
                transformedPoint.add(thisNodePos);
                if(Double.isNaN(transformedPoint.x) || Double.isNaN(transformedPoint.y) || Double.isNaN(transformedPoint.z))
                {
                    int hola = 0;
                }
                transversalCircle.add(transformedPoint);
            }
            transversalCircle1 = transversalCircle;

            // rotate currentDir & currentPos.
            elbowAxisRotMat.transformPosition(currentDir, currentDir);
            currentDir.normalize();
            currentPos.sub(thisNodePos);
            elbowAxisRotMat.transformPosition(currentPos, currentPos);
            currentPos.add(thisNodePos);

            transversalCircles.add(transversalCircle);
        }
        boolean bottomCap = true;
        boolean topCap  = true;
        boolean isClosed = true;
        boolean isLateralSurfaceSmooth = true;
        GaiaPrimitive primitive = modeler3D.getPrimitiveFromMultipleProfiles(transversalCircles, bottomCap, topCap, isClosed, isLateralSurfaceSmooth);
        primitive.calculateNormal();
        gaiaMesh.getPrimitives().add(primitive);

        return gaiaMesh;
    }

    public GaiaMesh makeGeometry() {
        if(dirty)
        {
            this.calculateElbowPositions();
        }

        if(this.getEdges().size() < 2)
        {
            // an elbow needs at least 2 edges.
            return null;
        }

        if(this.getElbowType() == 0)
        {
            // if the elbowType is unknown, then this elbow has no mesh.
            return null;
        }

        // check if the 2 pipes are isPhysicallyBuildable.
        Pipe pipe1 = (Pipe)this.getEdges().get(0);
        Pipe pipe2 = (Pipe)this.getEdges().get(1);

        if(!pipe1.isPhysicallyBuildable() || !pipe2.isPhysicallyBuildable())
        {
            // if the 2 pipes are not physically buildable, then this elbow has no mesh.
            // make a straight elbow.***
            // if the sweep angle is less than 0.1 deg, then this elbow has no mesh.
            TEdge edge1 = this.getEdge(0);
            TEdge edge2 = this.getEdge(1);

            TNode tNodeA = edge1.getTheAnotherNode(this);
            TNode tNodeB = edge2.getTheAnotherNode(this);

            Vector3d posA = new Vector3d(tNodeA.getPosition());
            Vector3d posB = new Vector3d(tNodeB.getPosition());
            Vector3d posCenter = new Vector3d(this.getPosition());

            // calculate the direction vector from posCenter to elbowCenter.
            Vector3d dirCA = new Vector3d(posA);
            dirCA.sub(posCenter);
            dirCA.normalize();

            Vector3d dirCB = new Vector3d(posB);
            dirCB.sub(posCenter);
            dirCB.normalize();

            elbowCenterPosition = new Vector3d(posCenter);

            // now, calculate linkPositionsa.***
            Vector3d linkPosA = new Vector3d(posCenter);

            mapEdgeLinkPositions.put(this.getEdge(0), linkPosA);
            mapEdgeLinkNormals.put(this.getEdge(0), dirCA);

            Vector3d linkPosB = new Vector3d(posCenter);

            mapEdgeLinkPositions.put(this.getEdge(1), linkPosB);
            mapEdgeLinkNormals.put(this.getEdge(1), dirCB);

            return this.makeStraightElbowGeometry();
        }

        GaiaMesh gaiaMesh = new GaiaMesh();

        double minSweepAngRad = Math.toRadians(0.1);
        double maxSweepAngRad = Math.toRadians(179.0);

        if(this.sweepAngRad < minSweepAngRad || this.sweepAngRad > maxSweepAngRad)
        {
            // if the sweep angle is less than 0.1 deg, then this elbow has no mesh.
            return null;
        }

        // make the circle points in local coordinates.
        List<Vector3d> circlePoints = new ArrayList<Vector3d>();
        this.getPipeProfilePoints(circlePoints);

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
        //       <------+---
        // linkNormal1  |     \
        //              |         \
        //              |            \
        //              |              \
        //              |               \
        //              |                |
        //              |                 |
        //  elbowCenter +-----------------+ linkPos2
        //                                |
        //                                |
        //                                |
        //                                V
        //                          linkNormal2

        Matrix4d elbowAxisRotMat = new Matrix4d();
        this.elbowRadiusInterpolationCount = (int)(Math.toDegrees(this.sweepAngRad)/10.0);
        if(this.elbowRadiusInterpolationCount < 2)
        {
            this.elbowRadiusInterpolationCount = 2;
        }

        if(this.sweepAngRad < Math.toRadians(2.0))
        {
            int hola = 0;
        }
        double increSweepAngRad = this.sweepAngRad / this.elbowRadiusInterpolationCount;
        elbowAxisRotMat.rotate(-increSweepAngRad, elbowAxis);

        // check if the elbowAxisRotMat has nan components.
//        for(int col = 0; col<4; col++)
//        {
//            for(int row = 0; row<4; row++)
//            {
//                if(Double.isNaN(elbowAxisRotMat.get(row, col)))
//                {
//                    int hola = 0;
//                }
//            }
//        }



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
                if(Double.isNaN(transformedPoint.x) || Double.isNaN(transformedPoint.y) || Double.isNaN(transformedPoint.z))
                {
                    int hola = 0;
                }
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
        boolean isLateralSurfaceSmooth = true;
        GaiaPrimitive primitive = modeler3D.getPrimitiveFromMultipleProfiles(transversalCircles, bottomCap, topCap, isClosed, isLateralSurfaceSmooth);
        primitive.calculateNormal();
        gaiaMesh.getPrimitives().add(primitive);

        return gaiaMesh;
    }

}
