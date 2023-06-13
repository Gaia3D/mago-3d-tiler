package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import geometry.basic.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

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
    private float geometricError = 0.0f;
    private float[] transform;
    private List<Node> children;
    private Content content;

    // TransfromMatrix
    public void setTransformMatrix(Matrix4d transformMatrixAux) {
        this.transformMatrixAux = transformMatrixAux;
        if (parent == this) { // root
            this.transformMatrix = transformMatrixAux;
            this.transformMatrixAux = transformMatrixAux;


        } else if (parent.getTransformMatrixAux() != null) {
            Matrix4d resultTransfromMatrix = new Matrix4d();
            Matrix4d parentTransformMatrix = parent.getTransformMatrixAux();
            Matrix4d parentTransformMatrixInv = new Matrix4d(parentTransformMatrix).invert();


            //log.info("{}", parentTransformMatrixInv);
            //log.info("{}", transformMatrixAux);
            parentTransformMatrixInv.mul(transformMatrixAux, resultTransfromMatrix);
            //log.info("{}", resultTransfromMatrix);

            this.transformMatrix = resultTransfromMatrix;
            this.transformMatrixAux = transformMatrixAux;
            //this.transformMatrix.set(3, 1, 0.0d);
        } else {
            // error
            this.transformMatrix = transformMatrixAux;
            this.transformMatrixAux = transformMatrixAux;
            log.error("error");
        }

        this.transform = transformMatrix.get(new float[16]);
        //log.info("{}", this.transformMatrix);
        //log.info("{}", this.transformMatrixAux);
        //log.info("{}", this.transformMatrix.get(3, 1));
    }

    public enum RefineType {
        ADD,
        REPLACE,
    }
}
