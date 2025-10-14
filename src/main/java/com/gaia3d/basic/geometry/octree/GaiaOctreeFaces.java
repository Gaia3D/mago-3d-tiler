package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@Setter
@Getter
public class GaiaOctreeFaces extends GaiaOctree<GaiaFaceData> {
    private int limitDepth = 5;
    private double limitSize = 1.0; // Minimum size of the bounding box to stop subdividing
    private boolean contentsCanBeInMultipleChildren = false;

    public GaiaOctreeFaces(GaiaOctree<GaiaFaceData> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);

        if (parent != null) {
            GaiaOctreeFaces parentFaces = (GaiaOctreeFaces) parent;
            this.limitDepth = parentFaces.getLimitDepth();
            this.contentsCanBeInMultipleChildren = parentFaces.contentsCanBeInMultipleChildren;
        }
    }

    @Override
    public GaiaOctreeFaces createChild(GaiaBoundingBox childBoundingBox) {
        return new GaiaOctreeFaces(this, childBoundingBox);
    }

    public void distributeContentsByCenterPoint() {
        List<GaiaFaceData> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }
        GaiaBoundingBox bbox = this.getBoundingBox();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        int debugCounter = 0;
        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaFaceData faceData : contents) {
            Vector3d centerPoint = faceData.getCenterPoint();
            if (centerPoint.x < midX) {
                // 0, 3, 4, 7
                if (centerPoint.y < midY) {
                    // 0, 4
                    if (centerPoint.z < midZ) {
                        children.get(0).addContent(faceData);
                    } else {
                        children.get(4).addContent(faceData);
                    }
                } else {
                    // 3, 7
                    if (centerPoint.z < midZ) {
                        children.get(3).addContent(faceData);
                    } else {
                        children.get(7).addContent(faceData);
                    }
                }
            } else {
                // 1, 2, 5, 6
                if (centerPoint.y < midY) {
                    // 1, 5
                    if (centerPoint.z < midZ) {
                        children.get(1).addContent(faceData);
                    } else {
                        children.get(5).addContent(faceData);
                    }
                } else {
                    // 2, 6
                    if (centerPoint.z < midZ) {
                        children.get(2).addContent(faceData);
                    } else {
                        children.get(6).addContent(faceData);
                    }
                }
            }

            debugCounter++;
        }

        // once the contents are distributed, clear the list
        contents.clear();
    }

    public boolean intersects(GaiaTriangle triangle) {
        GaiaBoundingBox bbox = this.getBoundingBox();
        return bbox.intersectsTriangle(triangle);
    }

    public void distributeContentsByIntersection() {
        List<GaiaFaceData> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaFaceData faceData : contents) {
            GaiaTriangle triangle = faceData.getTriangle();
            for (int i = 0; i < children.size(); i++) {
                GaiaOctreeFaces child = (GaiaOctreeFaces) children.get(i);
                if (child == null) {
                    log.error("[ERROR][distributeContentsByIntersection] : Child octree is null at index " + i);
                    continue;
                }
                if (child.intersects(triangle)) {
                    child.addContent(faceData);
                    if (!this.contentsCanBeInMultipleChildren) {
                        break;
                    }
                }
            }
        }

        // once the contents are distributed, clear the list
        contents.clear();
    }

    public void makeTree() {
        GaiaBoundingBox bbox = this.getBoundingBox();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();
        if ((maxX - minX) < limitSize || (maxY - minY) < limitSize || (maxZ - minZ) < limitSize) {
            return;
        }

        if (this.getDepth() >= limitDepth) {
            return;
        }

        List<GaiaFaceData> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        createChildren();
        distributeContentsByIntersection();

        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaOctree<GaiaFaceData> child : children) {
            GaiaOctreeFaces childFaces = (GaiaOctreeFaces) child;
            childFaces.makeTree();
        }
    }
}
