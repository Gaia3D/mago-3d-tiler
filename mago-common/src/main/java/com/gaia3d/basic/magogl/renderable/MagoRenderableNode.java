package com.gaia3d.basic.magogl.renderable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import lombok.Getter;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class MagoRenderableNode {

    private String name = "";
    private GaiaNode originalGaiaNode;

    private MagoRenderableNode parent;

    private final List<MagoRenderableMesh> renderableMeshes =
            new ArrayList<>();

    private final List<MagoRenderableNode> children =
            new ArrayList<>();

    /**
     * Local transformation relative to the parent node.
     */
    private final Matrix4d transformMatrix =
            new Matrix4d();

    /**
     * Accumulated transformation relative to the scene root.
     */
    private final Matrix4d preMultipliedTransformMatrix =
            new Matrix4d();

    private GaiaBoundingBox gaiaBoundingBox;

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public void setOriginalGaiaNode(GaiaNode originalGaiaNode) {
        this.originalGaiaNode = originalGaiaNode;
    }

    public void setGaiaBoundingBox(GaiaBoundingBox gaiaBoundingBox) {
        this.gaiaBoundingBox = gaiaBoundingBox;
    }

    public void setTransformMatrix(Matrix4d matrix) {
        Objects.requireNonNull(
                matrix,
                "transform matrix must not be null"
        );

        transformMatrix.set(matrix);
    }

    public void setPreMultipliedTransformMatrix(Matrix4d matrix) {
        Objects.requireNonNull(
                matrix,
                "pre-multiplied transform matrix must not be null"
        );

        preMultipliedTransformMatrix.set(matrix);
    }

    public void addChild(MagoRenderableNode child) {
        Objects.requireNonNull(child, "child must not be null");

        if (child == this) {
            throw new IllegalArgumentException(
                    "A node cannot be its own child."
            );
        }

        if (child.parent != null && child.parent != this) {
            throw new IllegalStateException(
                    "The child already belongs to another parent."
            );
        }

        if (children.contains(child)) {
            return;
        }

        child.parent = this;
        children.add(child);
    }

    public boolean removeChild(MagoRenderableNode child) {
        if (child == null) {
            return false;
        }

        boolean removed = children.remove(child);

        if (removed && child.parent == this) {
            child.parent = null;
        }

        return removed;
    }

    public void addRenderableMesh(MagoRenderableMesh mesh) {
        Objects.requireNonNull(mesh, "mesh must not be null");

        if (!renderableMeshes.contains(mesh)) {
            renderableMeshes.add(mesh);
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean hasMeshes() {
        return !renderableMeshes.isEmpty();
    }

    public void setParent(MagoRenderableNode parent) {
        this.parent = parent;
    }

    public void deleteObjects(){
        // 1rst, check if exist mesh.
        if(hasMeshes()){
            // delete meshes.
            for(MagoRenderableMesh mesh : renderableMeshes){
                mesh.deleteObjects();
            }
        }

        if(hasChildren()){
            for(MagoRenderableNode child : children){
                child.deleteObjects();
            }
        }
    }
}