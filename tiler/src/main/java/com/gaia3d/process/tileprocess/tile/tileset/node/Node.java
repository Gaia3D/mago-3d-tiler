package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.DecimalUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.opengis.geometry.BoundingBox;

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
        for (Node node : children) {
            node.findAllContentInfo(contentInfoList);
        }
        return contentInfoList;
    }

    public int getMaxDepth()
    {
        int maxDepth = this.depth;
        if(this.children == null) {
            return maxDepth;
        }
        for (Node node : children) {
            int depth = node.getMaxDepth();
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

    public void distributeContentsPhR(List<TileInfo> tileInfos, int depth) {
        if (this.depth == depth) {
            if (content != null) {
                //content.distributeContentsPhR(tileInfos);
                this.setRefine(Node.RefineType.REPLACE);

            }
            return;
        }

        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            TileInfo tileInfo = tileInfos.get(i);
            Matrix4d tileTransformMatrix = tileInfo.getTransformMatrix();
            GaiaBoundingBox tileBoundingBox = tileInfo.getBoundingBox();

            int hola = 0;
        }
        if (children == null) {
            return;
        }
        for (Node node : children) {
            node.distributeContentsPhR(tileInfos, depth);
        }
    }


}
