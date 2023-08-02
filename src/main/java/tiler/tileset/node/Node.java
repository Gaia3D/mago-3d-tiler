package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import geometry.basic.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import tiler.ContentInfo;

import java.util.List;

@Slf4j
@Getter
@Setter
public class Node {
    // TODO: 2023-06-26 builder 패턴으로 바꾸는 것 추천 
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
