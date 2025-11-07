package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

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

        for (HalfEdgeFace face : faces) {
            Vector3d center = face.getBarycenter(null);
            if (center.x < midX) {
                if (center.y < midY) {
                    if (center.z < midZ) {
                        children.get(0).addContent(face);
                    } else {
                        children.get(4).addContent(face);
                    }
                } else {
                    if (center.z < midZ) {
                        children.get(3).addContent(face);
                    } else {
                        children.get(7).addContent(face);
                    }
                }
            } else {
                if (center.y < midY) {
                    if (center.z < midZ) {
                        children.get(1).addContent(face);
                    } else {
                        children.get(5).addContent(face);
                    }
                } else {
                    if (center.z < midZ) {
                        children.get(2).addContent(face);
                    } else {
                        children.get(6).addContent(face);
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
