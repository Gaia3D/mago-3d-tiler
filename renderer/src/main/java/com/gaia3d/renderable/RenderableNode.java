package com.gaia3d.renderable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaNode;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class RenderableNode {
    private String name = "";
    private GaiaNode parent = null;
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
}
