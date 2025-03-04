package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.GaiaTextureScissorData;

import java.util.ArrayList;
import java.util.List;

public class GillotinePacker {
    private final int width;
    private final int height;
    private final List<GaiaRectangle> freeRectangles = new ArrayList<>();
    private final List<GaiaTextureScissorData> placedRectangles = new ArrayList<>();
    private final GaiaRectangle currentBoundary;
    private double candidateArea = 0.0;

    public GillotinePacker(int width, int height) {
        this.width = width;
        this.height = height;
        freeRectangles.add(new GaiaRectangle(0, 0, width, height));
        currentBoundary = new GaiaRectangle(0, 0, 0, 0);
    }

    public boolean insert(GaiaTextureScissorData rect) {

        int bestIndex = -1;
        GaiaRectangle bestRect = null;

        GaiaRectangle rectBoundary = rect.getOriginBoundary();
        double rectWidth = rectBoundary.getWidthInt();
        double rectHeight = rectBoundary.getHeightInt();

        double currentBoundaryArea = currentBoundary.getArea();

        double currentCandidateArea = Double.MAX_VALUE;
        double bestAreaFit = Double.MAX_VALUE;

        GaiaRectangle candidateFreeRect = null;
        if (freeRectangles.size() == 1) {
            GaiaRectangle firstRect = rect.getOriginBoundary();
            currentBoundary.addBoundingRectangle(firstRect);
            bestRect = freeRectangles.get(0);
            bestIndex = 0;
        } else {
            for (int i = 0; i < freeRectangles.size(); i++) {
                GaiaRectangle freeRect = freeRectangles.get(i);
                if (rectWidth < freeRect.getWidth() && rectHeight < freeRect.getHeight()) {
                    if (bestRect == null) {
                        bestIndex = i;
                        bestRect = freeRect;
                    }
                    GaiaRectangle candidateTotalBoundary = new GaiaRectangle(currentBoundary);
                    GaiaRectangle placedRect = new GaiaRectangle(freeRect.getMinX(), freeRect.getMinY(), freeRect.getMinX() + rectWidth, freeRect.getMinY() + rectHeight);
                    candidateTotalBoundary.addBoundingRectangle(placedRect);
                    double currTotalArea = candidateTotalBoundary.getArea();
                    //double areaFit = freeRect.getArea() - rect.getOriginBoundary().getArea();

                    if (Math.abs(currTotalArea - currentBoundaryArea) < 1.0) {
                        bestIndex = i;
                        bestRect = freeRect;
                        break;
                    } else {
//                        if(areaFit < bestAreaFit) {
//                            bestAreaFit = areaFit;
//                            bestIndex = i;
//                            bestRect = freeRect;
//                        }
                        if (currTotalArea < currentCandidateArea) {
                            currentCandidateArea = currTotalArea;
                            bestIndex = i;
                            bestRect = freeRect;
                        }
                    }
                }
            }
        }

        if (bestRect == null && candidateFreeRect != null) {
            bestRect = candidateFreeRect;
        }

        if (bestRect == null) {
            // in this case, create a new free rect.***
            GaiaRectangle newFreeRect = createNewFreeRectangle(rect.getOriginBoundary());
            freeRectangles.add(newFreeRect);
            bestRect = newFreeRect;
            //return false;
        }


        // set position to rect.***
        rect.setBatchedBoundary(new GaiaRectangle(bestRect.getMinX(), bestRect.getMinY(), bestRect.getMinX() + rectWidth, bestRect.getMinY() + rectHeight));
        placedRectangles.add(rect);

        currentBoundary.addBoundingRectangle(rect.getBatchedBoundary());
        candidateArea = currentBoundary.getArea();

        // split the free rect.***
        splitFreeRects(bestRect, rect.getBatchedBoundary());

        return true;
    }

    private GaiaRectangle createNewFreeRectangle(GaiaRectangle rectangleToPut) {
        // 1rst, calculate the current boundary.***
        GaiaRectangle currBoundaryPlacedRect = new GaiaRectangle(0, 0, 0, 0);
        for (GaiaTextureScissorData rect : placedRectangles) {
            currBoundaryPlacedRect.addBoundingRectangle(rect.getBatchedBoundary());
        }

        double width = currBoundaryPlacedRect.getWidth();
        double height = currBoundaryPlacedRect.getHeight();

        double rectToPutWidth = rectangleToPut.getWidth();
        double rectToPutHeight = rectangleToPut.getHeight();
        GaiaRectangle newFreeRect = null;
        if (width < height) {
            newFreeRect = new GaiaRectangle(width, 0, width + rectToPutWidth, height);
        } else {
            newFreeRect = new GaiaRectangle(0, height, width, height + rectToPutHeight);
        }
        return newFreeRect;
    }

    private void splitFreeRects(GaiaRectangle freeRect, GaiaRectangle placedRect) {
        freeRectangles.remove(freeRect);

        int rigthWidth = (int) (freeRect.getWidth() - placedRect.getWidth());
        int bottomHeight = (int) (freeRect.getHeight() - placedRect.getHeight());

        if (rigthWidth < bottomHeight) {
            // split the free rect horizontally.***
            double x = freeRect.getMinX() + placedRect.getWidth();
            double y = freeRect.getMinY();
            GaiaRectangle freeRect1 = new GaiaRectangle(x, y, x + rigthWidth, y + placedRect.getHeight());
            x = freeRect.getMinX();
            y = freeRect.getMinY() + placedRect.getHeight();
            GaiaRectangle freeRect2 = new GaiaRectangle(x, y, x + freeRect.getWidth(), y + bottomHeight);
            freeRectangles.add(freeRect1);
            freeRectangles.add(freeRect2);
        } else {
            // split the free rect vertically.***
            double x = freeRect.getMinX();
            double y = freeRect.getMinY() + placedRect.getHeight();
            GaiaRectangle freeRect1 = new GaiaRectangle(x, y, x + placedRect.getWidth(), y + bottomHeight);
            x = freeRect.getMinX() + placedRect.getWidth();
            y = freeRect.getMinY();
            GaiaRectangle freeRect2 = new GaiaRectangle(x, y, x + rigthWidth, y + freeRect.getHeight());
            freeRectangles.add(freeRect1);
            freeRectangles.add(freeRect2);
        }

    }
}
