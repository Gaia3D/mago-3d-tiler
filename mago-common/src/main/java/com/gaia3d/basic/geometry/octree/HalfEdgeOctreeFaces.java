package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdge;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import com.gaia3d.basic.halfedge.HalfEdgeVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter
public class HalfEdgeOctreeFaces extends GaiaOctree<HalfEdgeFace> {
    private int limitDepth = 5;

    public HalfEdgeOctreeFaces(HalfEdgeOctreeFaces parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);
        if (parent != null) {
            this.limitDepth = parent.limitDepth;
        }
    }

    @Override
    protected HalfEdgeOctreeFaces createChild(GaiaBoundingBox boundingBox) {
        return new HalfEdgeOctreeFaces(this, boundingBox);
    }

    public void distributeFacesToTargetDepth(int targetDepth) {
        List<HalfEdgeFace> faces = this.getContents();
        if (faces.isEmpty()) {return;}

        if (this.getDepth() >= targetDepth) {return;}

        if (this.getChildren() == null || this.getChildren().isEmpty()) {
            this.createChildren();
        }

        GaiaBoundingBox boundingBox = this.getBoundingBox();
        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        List<GaiaOctree<HalfEdgeFace>> children = this.getChildren();

        GaiaOctree<HalfEdgeFace> child0 = children.get(0);
        GaiaOctree<HalfEdgeFace> child1 = children.get(1);
        GaiaOctree<HalfEdgeFace> child2 = children.get(2);
        GaiaOctree<HalfEdgeFace> child3 = children.get(3);
        GaiaOctree<HalfEdgeFace> child4 = children.get(4);
        GaiaOctree<HalfEdgeFace> child5 = children.get(5);
        GaiaOctree<HalfEdgeFace> child6 = children.get(6);
        GaiaOctree<HalfEdgeFace> child7 = children.get(7);

        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        List<HalfEdge> memSaveHalfEdges = new ArrayList<>();
        Vector3d center = new Vector3d();
        for (HalfEdgeFace face : faces) {
            center = face.getBarycenter(center, memSaveVertices, memSaveHalfEdges);
            if (center.x < midX) {
                if (center.y < midY) {
                    if (center.z < midZ) {
                        child0.addContent(face);
                    } else {
                        child4.addContent(face);
                    }
                } else {
                    if (center.z < midZ) {
                        child3.addContent(face);
                    } else {
                        child7.addContent(face);
                    }
                }
            } else {
                if (center.y < midY) {
                    if (center.z < midZ) {
                        child1.addContent(face);
                    } else {
                        child5.addContent(face);
                    }
                } else {
                    if (center.z < midZ) {
                        child2.addContent(face);
                    } else {
                        child6.addContent(face);
                    }
                }
            }
        }

        // clear the faces list
        faces.clear();

        if (this.getCoordinate().getDepth() < targetDepth) {
            for (GaiaOctree<HalfEdgeFace> child : children) {
                HalfEdgeOctreeFaces childOctree = (HalfEdgeOctreeFaces) child;
                childOctree.distributeFacesToTargetDepth(targetDepth);
            }
        }
    }
}
