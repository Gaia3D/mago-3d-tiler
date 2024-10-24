package com.gaia3d.renderer.renderable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RenderableNode {
    private String name = "";
    private GaiaNode originalGaiaNode = null;
    private List<RenderableMesh> renderableMeshes = new ArrayList<>();
    private List<RenderableNode> children = new ArrayList<>();

    private Matrix4d transformMatrix = new Matrix4d();
    private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
    private GaiaBoundingBox gaiaBoundingBox = null;

    public RenderableNode() {
        renderableMeshes = new ArrayList<>();
        children = new ArrayList<>();
    }

    public void addRenderableMesh(RenderableMesh renderableMesh) {
        renderableMeshes.add(renderableMesh);
    }

    public void addChild(RenderableNode child) {
        children.add(child);
    }

    public void extractRenderablePrimitives(List<RenderablePrimitive> resultRenderablePrimitives) {
        for (RenderableMesh renderableMesh : renderableMeshes) {
            renderableMesh.extractRenderablePrimitives(resultRenderablePrimitives);
        }

        for (RenderableNode child : children) {
            child.extractRenderablePrimitives(resultRenderablePrimitives);
        }
    }
}
