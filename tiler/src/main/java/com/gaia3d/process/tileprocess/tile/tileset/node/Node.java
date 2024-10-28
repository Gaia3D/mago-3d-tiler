package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

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
            if (!childNode.hasContentsInTree()) {
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

    public boolean hasContentsInTree()
    {
        if(this.content != null) {
            return true;
        }
        if(this.children == null || this.children.isEmpty()) {
            return false;
        }
        for (Node node : children) {
            if(node.hasContentsInTree()) {
                return true;
            }
        }
        return false;
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

    public GaiaBoundingBox calculateCartographicBoundingBox()
    {
        if(this.boundingVolume == null) {
            return null;
        }

        double[] region = this.boundingVolume.getRegion();
        double minLonDeg = Math.toDegrees(region[0]);
        double minLatDeg = Math.toDegrees(region[1]);
        double maxLonDeg = Math.toDegrees(region[2]);
        double maxLatDeg = Math.toDegrees(region[3]);
        double minAlt = region[4];
        double maxAlt = region[5];

        GaiaBoundingBox bbox = new GaiaBoundingBox(minLonDeg, minLatDeg, minAlt, maxLonDeg, maxLatDeg, maxAlt, false);

        return bbox;
    }

    public GaiaBoundingBox calculateLocalBoundingBox()
    {
        if(this.boundingVolume == null) {
            return null;
        }

        double[] region = this.boundingVolume.getRegion();
        double minLonDeg = Math.toDegrees(region[0]);
        double minLatDeg = Math.toDegrees(region[1]);
        double maxLonDeg = Math.toDegrees(region[2]);
        double maxLatDeg = Math.toDegrees(region[3]);
        double minAlt = region[4];
        double maxAlt = region[5];

        Vector3d centerCartographicRad = new Vector3d((minLonDeg + maxLonDeg) / 2.0, (minLatDeg + maxLatDeg) / 2.0, (minAlt + maxAlt) / 2.0);
        Vector3d certerCartesianWgs84 = GlobeUtils.geographicToCartesianWgs84(centerCartographicRad);
        Matrix4d tMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(certerCartesianWgs84);

        Vector3d leftDownBottom = new Vector3d(minLonDeg, minLatDeg, minAlt);
        Vector3d rightUpTop = new Vector3d(maxLonDeg, maxLatDeg, maxAlt);
        Vector3d rightDownBottom = new Vector3d(maxLonDeg, minLatDeg, minAlt);

        // transform the geoCoords to worldCoords.***
        Vector3d leftDownBottomWC = GlobeUtils.geographicToCartesianWgs84(leftDownBottom);
        Vector3d rightUpTopWC = GlobeUtils.geographicToCartesianWgs84(rightUpTop);
        Vector3d rightDownBottomWC = GlobeUtils.geographicToCartesianWgs84(rightDownBottom);

        // transform the worldCoords to localCoords.***
        Matrix4d transformMatrixInv = new Matrix4d(tMatrix).invert();
        Vector3d leftDownBottomLC = new Vector3d(leftDownBottomWC).mulPosition(transformMatrixInv);
        Vector3d rightUpTopLC = new Vector3d(rightUpTopWC).mulPosition(transformMatrixInv);
        Vector3d rightDownBottomLC = new Vector3d(rightDownBottomWC).mulPosition(transformMatrixInv);

        double minX = Math.min(leftDownBottomLC.x, Math.min(rightUpTopLC.x, rightDownBottomLC.x));
        double minY = Math.min(leftDownBottomLC.y, Math.min(rightUpTopLC.y, rightDownBottomLC.y));
        double minZ = Math.min(leftDownBottomLC.z, Math.min(rightUpTopLC.z, rightDownBottomLC.z));
        double maxX = Math.max(leftDownBottomLC.x, Math.max(rightUpTopLC.x, rightDownBottomLC.x));
        double maxY = Math.max(leftDownBottomLC.y, Math.max(rightUpTopLC.y, rightDownBottomLC.y));
        double maxZ = Math.max(leftDownBottomLC.z, Math.max(rightUpTopLC.z, rightDownBottomLC.z));

        GaiaBoundingBox bboxLC = new GaiaBoundingBox(minX, minY, minZ, maxX, maxY, maxZ, true);

        return bboxLC;
    }
}
