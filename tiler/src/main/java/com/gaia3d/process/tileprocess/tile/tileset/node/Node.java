package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.util.DecimalUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.text.DecimalFormat;
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

    // TransfromMatrix
    public void setTransformMatrix(Matrix4d transformMatrixAux) {
        this.transformMatrixAux = transformMatrixAux;

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.isClassicTransformMatrix()) {
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
}
