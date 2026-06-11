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
@Deprecated
public class GaiaOctreeMeteorologyBox extends GaiaOctree<GeometryContent> {
    private int limitDepth = 5;
    private double limitSize = 1.0; // Minimum size of the bounding box to stop subdividing
    private boolean contentsCanBeInMultipleChildren = false;

    public GaiaOctreeMeteorologyBox(GaiaOctree<GeometryContent> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);

        if (parent != null) {
            GaiaOctreeMeteorologyBox parentFaces = (GaiaOctreeMeteorologyBox) parent;
            this.limitDepth = parentFaces.getLimitDepth();
            this.contentsCanBeInMultipleChildren = parentFaces.contentsCanBeInMultipleChildren;
        }
    }

    @Override
    public GaiaOctreeMeteorologyBox createChild(GaiaBoundingBox childBoundingBox) {
        return new GaiaOctreeMeteorologyBox(this, childBoundingBox);
    }

    public void distributeContentsByCenterPoint() {
        List<GeometryContent> contents = this.getContents();
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
        List<GaiaOctree<GeometryContent>> children = this.getChildren();
        for (GeometryContent geometryContent : contents) {
            Vector3d centerPoint = geometryContent.getCenterPoint();
            if (centerPoint.x < midX) {
                // 0, 3, 4, 7
                if (centerPoint.y < midY) {
                    // 0, 4
                    if (centerPoint.z < midZ) {
                        children.get(0).addContent(geometryContent);
                    } else {
                        children.get(4).addContent(geometryContent);
                    }
                } else {
                    // 3, 7
                    if (centerPoint.z < midZ) {
                        children.get(3).addContent(geometryContent);
                    } else {
                        children.get(7).addContent(geometryContent);
                    }
                }
            } else {
                // 1, 2, 5, 6
                if (centerPoint.y < midY) {
                    // 1, 5
                    if (centerPoint.z < midZ) {
                        children.get(1).addContent(geometryContent);
                    } else {
                        children.get(5).addContent(geometryContent);
                    }
                } else {
                    // 2, 6
                    if (centerPoint.z < midZ) {
                        children.get(2).addContent(geometryContent);
                    } else {
                        children.get(6).addContent(geometryContent);
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
        List<GeometryContent> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        List<GaiaOctree<GeometryContent>> children = this.getChildren();
        for (GeometryContent geometryContent : contents) {
            if (geometryContent instanceof GaiaFaceContent faceContent) {
                GaiaTriangle triangle = faceContent.getTriangle();
                for (int i = 0; i < children.size(); i++) {
                    GaiaOctreeMeteorologyBox child = (GaiaOctreeMeteorologyBox) children.get(i);
                    if (child == null) {
                        log.error("[ERROR][distributeContentsByIntersection] : Child octree is null at index " + i);
                        continue;
                    }
                    if (child.intersects(triangle)) {
                        child.addContent(geometryContent);
                        if (!this.contentsCanBeInMultipleChildren) {
                            break;
                        }
                    }
                }
            } else if (geometryContent instanceof GaiaVertexContent vertexContent) {
                // TODO: Handle vertex content if needed
                throw new RuntimeException("Not implemented yet");
            } else {
                log.warn("Unknown GeometryContent type: {}", geometryContent.getClass().getName());
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

        List<GeometryContent> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        createChildren();
        distributeContentsByIntersection();

        List<GaiaOctree<GeometryContent>> children = this.getChildren();
        for (GaiaOctree<GeometryContent> child : children) {
            GaiaOctreeMeteorologyBox childFaces = (GaiaOctreeMeteorologyBox) child;
            childFaces.makeTree();
        }
    }
}
