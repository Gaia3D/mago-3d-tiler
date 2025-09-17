package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.util.GaiaOctreeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter

public class GaiaOctreeFaces extends GaiaOctree<GaiaFaceData> {
    private int limitDepth = 5;
    private boolean contentsCanBeInMultipleChildren = false;

    public GaiaOctreeFaces(GaiaOctree<GaiaFaceData> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);

        if (parent != null) {
            GaiaOctreeFaces parentFaces = (GaiaOctreeFaces) parent;
            this.limitDepth = parentFaces.getLimitDepth();
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

    private boolean intersects(GaiaFaceData faceData) {
        GaiaBoundingBox bbox = this.getBoundingBox();
        if (bbox == null) {
            log.error("[ERROR][intersects] : Bounding box is null.");
            return false;
        }

        GaiaBoundingBox faceBoundingBox = faceData.getBoundingBox();
        if (faceBoundingBox == null) {
            log.error("[ERROR][intersects] : Face bounding box is null.");
            return false;
        }

        // Check if the bounding boxes intersect
        if (!bbox.intersects(faceBoundingBox)) {
            return false;
        }

        GaiaPlane facePlane = faceData.getPlane();
        if (!bbox.intersectsPlane(facePlane)) {
            // If the bounding box does not intersect the face plane, return false
            return false;
        }


        return true;
    }

    public void distributeContentsByIntersection() {
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

    public void makeTree(double minBoxSize) {
        GaiaBoundingBox bbox = this.getBoundingBox();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();
        if ((maxX - minX) < minBoxSize || (maxY - minY) < minBoxSize || (maxZ - minZ) < minBoxSize) {
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
        distributeContentsByCenterPoint();

        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaOctree<GaiaFaceData> child : children) {
            GaiaOctreeFaces childFaces = (GaiaOctreeFaces) child;
            childFaces.makeTree(minBoxSize);
        }
    }
}
