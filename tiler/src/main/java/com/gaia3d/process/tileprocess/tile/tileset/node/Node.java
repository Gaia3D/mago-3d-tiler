package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.text.DecimalFormat;
import java.util.List;

@Slf4j
@Getter
@Setter
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
    private double geometricError = 0.0d;
    private float[] transform;
    private List<Node> children;
    private Content content;

    // TransfromMatrix
    public void setTransformMatrix(Matrix4d transformMatrixAux) {
        this.transformMatrixAux = transformMatrixAux;
        if (parent == this) { // root
            this.transformMatrix = transformMatrixAux;
        } else if (parent.getTransformMatrixAux() != null) {
            Matrix4d resultTransfromMatrix = new Matrix4d();
            Matrix4d parentTransformMatrix = parent.getTransformMatrixAux();
            Matrix4d parentTransformMatrixInv = new Matrix4d(parentTransformMatrix).invert();

            parentTransformMatrixInv.mul(transformMatrixAux, resultTransfromMatrix);

            this.transformMatrix = resultTransfromMatrix;
            this.transformMatrixAux = transformMatrixAux;
        } else {
            this.transformMatrix = transformMatrixAux;
            log.error("Error :: Wrong TransformMatrix");
        }

        this.transform = transformMatrix.get(new float[16]);

        for (int i = 0; i < transform.length; i++) {
            float value = this.transform[i];
            DecimalFormat df = new DecimalFormat("0.00000000");
            String result = df.format(value);
            value = Float.parseFloat(result);
            this.transform[i] = value;
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
}
