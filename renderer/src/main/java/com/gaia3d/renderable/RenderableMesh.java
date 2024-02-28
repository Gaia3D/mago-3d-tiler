package com.gaia3d.renderable;

import com.gaia3d.basic.structure.GaiaPrimitive;

import java.util.ArrayList;
import java.util.List;

public class RenderableMesh {
    private List<RenderablePrimitive> renderablePrimitives = new ArrayList<>();

    public void RenderableMesh() {
        renderablePrimitives = new ArrayList<>();
    }

    public void addRenderablePrimitive(RenderablePrimitive renderablePrimitive) {
        renderablePrimitives.add(renderablePrimitive);
    }
}
