package com.gaia3d.renderable;

import com.gaia3d.basic.structure.GaiaPrimitive;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class RenderableMesh {
    private List<RenderablePrimitive> renderablePrimitives = new ArrayList<>();

    public void RenderableMesh() {
        renderablePrimitives = new ArrayList<>();
    }

    public void addRenderablePrimitive(RenderablePrimitive renderablePrimitive) {
        renderablePrimitives.add(renderablePrimitive);
    }
}
