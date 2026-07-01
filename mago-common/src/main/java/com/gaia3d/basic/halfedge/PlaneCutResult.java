package com.gaia3d.basic.halfedge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaneCutResult {

    private final List<PlaneCutPoint> cuttingPoints =
            new ArrayList<>();

    private final List<PlaneCutPoint> tangentPoints =
            new ArrayList<>();

    private final List<PlaneCutPoint> coplanarPoints =
            new ArrayList<>();

    private int halfEdgesCutCount;

    public PlaneCutResult() {
    }

    public PlaneCutResult(
            List<PlaneCutPoint> cuttingPoints,
            List<PlaneCutPoint> tangentPoints,
            List<PlaneCutPoint> coplanarPoints,
            int halfEdgesCutCount
    ) {
        addPoints(
                this.cuttingPoints,
                cuttingPoints
        );

        addPoints(
                this.tangentPoints,
                tangentPoints
        );

        addPoints(
                this.coplanarPoints,
                coplanarPoints
        );

        this.halfEdgesCutCount =
                Math.max(halfEdgesCutCount, 0);
    }

    public List<PlaneCutPoint> getCuttingPoints() {
        return Collections.unmodifiableList(
                cuttingPoints
        );
    }

    public List<PlaneCutPoint> getTangentPoints() {
        return Collections.unmodifiableList(
                tangentPoints
        );
    }

    public List<PlaneCutPoint> getCoplanarPoints() {
        return Collections.unmodifiableList(
                coplanarPoints
        );
    }

    public int getHalfEdgesCutCount() {
        return halfEdgesCutCount;
    }

    public void addCuttingPoint(
            PlaneCutPoint point
    ) {
        if (point != null) {
            cuttingPoints.add(point.copy());
        }
    }

    public void addTangentPoint(
            PlaneCutPoint point
    ) {
        if (point != null) {
            tangentPoints.add(point.copy());
        }
    }

    public void addCoplanarPoint(
            PlaneCutPoint point
    ) {
        if (point != null) {
            coplanarPoints.add(point.copy());
        }
    }

    public void incrementHalfEdgesCutCount() {
        halfEdgesCutCount++;
    }

    public void addHalfEdgesCutCount(
            int count
    ) {
        if (count > 0) {
            halfEdgesCutCount += count;
        }
    }

    public void add(
            PlaneCutResult other
    ) {
        if (other == null) {
            return;
        }

        addPoints(
                cuttingPoints,
                other.cuttingPoints
        );

        addPoints(
                tangentPoints,
                other.tangentPoints
        );

        addPoints(
                coplanarPoints,
                other.coplanarPoints
        );

        halfEdgesCutCount +=
                other.halfEdgesCutCount;
    }

    public boolean hasCut() {
        return halfEdgesCutCount > 0;
    }

    public boolean isEmpty() {
        return cuttingPoints.isEmpty()
                && tangentPoints.isEmpty()
                && coplanarPoints.isEmpty();
    }

    private static void addPoints(
            List<PlaneCutPoint> destination,
            List<PlaneCutPoint> source
    ) {
        if (source == null || source.isEmpty()) {
            return;
        }

        for (PlaneCutPoint point : source) {
            if (point != null) {
                destination.add(point.copy());
            }
        }
    }
}