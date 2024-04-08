package com.gaia3d.renderable;

import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaPrimitive;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class RenderableMesh  extends RenderableObject{
    private GaiaMesh originalGaiaMesh = null;
    private List<RenderablePrimitive> renderablePrimitives = new ArrayList<>();

    public void RenderableMesh() {
        renderablePrimitives = new ArrayList<>();
    }

    public void addRenderablePrimitive(RenderablePrimitive renderablePrimitive) {
        renderablePrimitives.add(renderablePrimitive);
    }

    public void extractRenderablePrimitives(List<RenderablePrimitive> resultRenderablePrimitives) {
        for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
            resultRenderablePrimitives.add(renderablePrimitive);
        }
    }
}
