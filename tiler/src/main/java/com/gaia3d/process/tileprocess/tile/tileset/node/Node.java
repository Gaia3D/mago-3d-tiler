package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.util.DecimalUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;

import java.util.List;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class Node {
    @JsonIgnore
    private String nodeCode;
    @JsonIgnore
    private Node parent;
    @JsonIgnore
    private Matrix4d transformMatrix;
    @JsonIgnore
    private Matrix4d transformMatrixAux;
    @JsonIgnore
    private GaiaBoundingBox boundingBox;
    private BoundingVolume boundingVolume;
    private RefineType refine = RefineType.ADD;
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    private double geometricError = 0.0d;
    //@JsonIgnore
    private float[] transform;
    private List<Node> children;
    private Content content;

    @JsonIgnore
    private int depth;

    // TransformMatrix
    public void setTransformMatrix(Matrix4d transformMatrixAux, boolean useTransform) {
        this.transformMatrixAux = transformMatrixAux;
        if (useTransform) {
            if (parent == this) { // root
                this.transformMatrix = transformMatrixAux;
            } else if (parent.getTransformMatrixAux() != null) {
                Matrix4d resultTransformMatrix = new Matrix4d();
                Matrix4d parentTransformMatrix = parent.getTransformMatrixAux();
                Matrix4d parentTransformMatrixInv = new Matrix4d(parentTransformMatrix).invert();

                parentTransformMatrixInv.mul(transformMatrixAux, resultTransformMatrix);

                this.transformMatrix = resultTransformMatrix;
                this.transformMatrixAux = transformMatrixAux;
            } else {
                this.transformMatrix = transformMatrixAux;
                log.error("Error :: Wrong TransformMatrix");
            }

            this.transform = transformMatrix.get(new float[16]);

            for (int i = 0; i < transform.length; i++) {
                this.transform[i] = DecimalUtils.cut(this.transform[i]);
            }
        }
    }

    public void deleteNoContentNodes() {
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            Node childNode = children.get(i);
            if (childNode.getContent() == null) {
                children.remove(i);
                i--;
            } else {
                childNode.deleteNoContentNodes();
            }
        }
    }

    public enum RefineType {
        ADD,
        REPLACE,
    }

    public List<ContentInfo> findAllContentInfo(List<ContentInfo> contentInfoList) {
        if (content != null) {
            ContentInfo contentInfo = content.getContentInfo();
            if (contentInfo != null) {
                contentInfoList.add(contentInfo);
            }
        }
        if(children != null) {
            for (Node node : children) {
                node.findAllContentInfo(contentInfoList);
            }
        }
        return contentInfoList;
    }

    public int findMaxDepth()
    {
        int maxDepth = this.depth;
        if(this.children == null) {
            return maxDepth;
        }
        for (Node node : children) {
            int depth = node.findMaxDepth();
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        return maxDepth;
    }

    public void getNodesByDepth(int depth, List<Node> resultNodes)
    {
        if(this.depth == depth) {
            resultNodes.add(this);
            return;
        }
        if(this.children == null) {
            return;
        }
        for (Node node : children) {
            node.getNodesByDepth(depth, resultNodes);
        }
    }

    public Node getIntersectedNode(Vector2d cartographicRad, int depth) {
        if (this.depth == depth) {
            return this;
        }

        if (children == null) {
            return null;
        }

        for (Node childNode : children) {
            BoundingVolume childBoundingVolume = childNode.getBoundingVolume();

            double[] region = childBoundingVolume.getRegion();// minx, miny, maxx, maxy, minz, maxz

            // check if intersects centerLonRad and centerLatRad
            if (cartographicRad.x >= region[0] && cartographicRad.x <= region[2] && cartographicRad.y >= region[1] && cartographicRad.y <= region[3]) {
                return childNode.getIntersectedNode(cartographicRad, depth);
            }
        }

        return null;
    }
}
