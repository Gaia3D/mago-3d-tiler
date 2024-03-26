package com.gaia3d.converter.geometry.tessellator;

import org.joml.Vector2d;

public class Segment2DTess
{
    public Point2DTess startPoint;
    public Point2DTess endPoint;

    public Segment2DTess(Point2DTess startPoint, Point2DTess endPoint)
    {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public void setPoint1(Point2DTess point2DTess) {
        this.startPoint = point2DTess;
    }

    public void setPoint2(Point2DTess point2DTess) {
        this.endPoint = point2DTess;
    }

    public Line2D getLine(Line2D resultLine)
    {
        if(resultLine == null)
        {
            resultLine = new Line2D(null, null);
        }

        resultLine.setBy2Points(this.startPoint.point, this.endPoint.point);

        return resultLine;
    }

    public double getLength()
    {
        return this.startPoint.distanceTo(this.endPoint);
    }

    public int intersectionWithPointByDistances(Point2DTess point, double error)
    {
        //****************************************************
        // 0 = no intersection,
        // 1 = intersection point is inside the segment,
        // 2 = intersection point is the start point,
        // 3 = intersection point is the end point.

        double distance1 = this.startPoint.distanceTo(point);
        double distance2 = this.endPoint.distanceTo(point);
        double distance = getLength();

        if(distance1 < error)
        {
            return 2;
        }
        else if(distance2 < error)
        {
            return 3;
        }
        else if(distance1 + distance2 - distance < error)
        {
            return 1;
        }

        return 0;
    }

    public int intersectionWithSegment(Segment2DTess segment, Point2DTess intersectionPoint, double error)
    {
        //*********************************************************************
        // 0 = no intersection,
        // 1 = intersection point is inside the both segments,
        // 2 = intersection point is the start point of this segment,
        // 3 = intersection point is the end point of this segment,
        // 4 = intersection point is the start point of the segment,
        // 5 = intersection point is the end point of the segment.
        //*********************************************************************

        Line2D line1 = new Line2D(null, null);
        Line2D line2 = new Line2D(null, null);

        this.getLine(line1);
        segment.getLine(line2);

        if(intersectionPoint.point == null)
        {
            intersectionPoint.point = new Vector2d();
        }

        if(line1.intersectionWithLine(line2, intersectionPoint.point))
        {
            int intersectionType1 = this.intersectionWithPointByDistances(intersectionPoint, error);
            int intersectionType2 = segment.intersectionWithPointByDistances(intersectionPoint, error);

            if(intersectionType1 == 1 && intersectionType2 == 1)
            {
                return 1;
            }
            else if(intersectionType1 == 2 && intersectionType2 == 1)
            {
                return 2;
            }
            else if(intersectionType1 == 3 && intersectionType2 == 1)
            {
                return 3;
            }
            else if(intersectionType1 == 1 && intersectionType2 == 2)
            {
                return 4;
            }
            else if(intersectionType1 == 1 && intersectionType2 == 3)
            {
                return 5;
            }
        }

        return 0;
    }
}
