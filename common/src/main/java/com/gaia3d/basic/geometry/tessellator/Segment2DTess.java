package com.gaia3d.basic.geometry.tessellator;

import org.joml.Vector2d;

public class Segment2DTess {
    public Point2DTess startPoint;
    public Point2DTess endPoint;

    public Segment2DTess(Point2DTess startPoint, Point2DTess endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public void setPoint1(Point2DTess point2DTess) {
        this.startPoint = point2DTess;
    }

    public void setPoint2(Point2DTess point2DTess) {
        this.endPoint = point2DTess;
    }

    public Line2D getLine(Line2D resultLine) {
        if (resultLine == null) {
            resultLine = new Line2D(null, null);
        }

        resultLine.setBy2Points(this.startPoint.point, this.endPoint.point);

        return resultLine;
    }

    public double getLengthSquared()
    {
        return this.startPoint.squareDistanceTo(this.endPoint);
    }

    public double getLength()
    {
        return this.startPoint.distanceTo(this.endPoint);
    }

    public int intersectionWithPointByDistances(Point2DTess point, double error) {
        //****************************************************
        // 0 = no intersection,
        // 1 = point is inside the segment,
        // 2 = point is the start point,
        // 3 = point is the end point.
        //****************************************************

        double distance1 = this.startPoint.distanceTo(point);
        double distance2 = this.endPoint.distanceTo(point);
        double distance = getLength();

        if (distance1 < error) {
            return 2;
        } else if (distance2 < error) {
            return 3;
        } else if (distance1 + distance2 - distance < error) {
            return 1;
        }

        return 0;
    }

    public int intersectionWithSegment(Segment2DTess segment, Point2DTess intersectionPoint, double error) {
        //*********************************************************************
        // 0 = no intersection,
        // 1 = intersection point is inside the both segments,
        // 2 = intersection point is the start point of this segment,
        // 3 = intersection point is the end point of this segment,
        // 4 = intersection point is the start point of the segment,
        // 5 = intersection point is the end point of the segment.
        // 6 = lines are collinear with intersection.
        //*********************************************************************

        Line2D line1 = new Line2D(null, null);
        Line2D line2 = new Line2D(null, null);

        this.getLine(line1);
        segment.getLine(line2);

        if (intersectionPoint.point == null) {
            intersectionPoint.point = new Vector2d();
        }

        if (line1.intersectionWithLine(line2, intersectionPoint.point, error)) {
            int intersectionType1 = this.intersectionWithPointByDistances(intersectionPoint, error);
            int intersectionType2 = segment.intersectionWithPointByDistances(intersectionPoint, error);

            if (intersectionType1 == 1 && intersectionType2 == 1) {
                return 1;
            } else if (intersectionType1 == 2 && intersectionType2 == 1) {
                return 2;
            } else if (intersectionType1 == 3 && intersectionType2 == 1) {
                return 3;
            } else if (intersectionType1 == 1 && intersectionType2 == 2) {
                return 4;
            } else if (intersectionType1 == 1 && intersectionType2 == 3) {
                return 5;
            }
        }
        else
        {
            // lines are paralel.***
            // check if any point of the segment is inside the this segment.***
            //****************************************************
            // 0 = no intersection,
            // 1 = point is inside the segment,
            // 2 = point is the start point,
            // 3 = point is the end point.
            //****************************************************
            if(this.intersectionWithPointByDistances(segment.startPoint, error) == 1)
            {
                return 6;
            }
            else if(this.intersectionWithPointByDistances(segment.endPoint, error) == 1)
            {
                return 6;
            }
            else if(segment.intersectionWithPointByDistances(this.startPoint, error) == 1)
            {
                return 6;
            }
            else if(segment.intersectionWithPointByDistances(this.endPoint, error) == 1)
            {
                return 6;
            }

            // check total coincidence.***
            if(this.startPoint.point.equals(segment.startPoint.point) && this.endPoint.point.equals(segment.endPoint.point))
            {
                return 6;
            }
            else if(this.startPoint.point.equals(segment.endPoint.point) && this.endPoint.point.equals(segment.startPoint.point))
            {
                return 6;
            }

            // check if are collinear.***
//            Vector2d p1 = this.startPoint.point;
//            double error2 = 1.0e-7;
//            if(line2.pointBelongsToLine(p1, error2))
//            {
//                // are colineals.***
//                // check if any point of this segment is inside the segment.***
//                //****************************************************
//                // 0 = no intersection,
//                // 1 = point is inside the segment,
//                // 2 = point is the start point,
//                // 3 = point is the end point.
//                //****************************************************
//                int intersectionType1 = segment.intersectionWithPointByDistances(this.startPoint, error);
//                if(intersectionType1 == 1)
//                {
//                    return 6;
//                }
//                int intersectionType2 = segment.intersectionWithPointByDistances(this.endPoint, error);
//                if(intersectionType2 == 1)
//                {
//                    return 6;
//                }
//
//            }
        }

        return 0;
    }
}
