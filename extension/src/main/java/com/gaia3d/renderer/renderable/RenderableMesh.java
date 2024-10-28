package com.gaia3d.renderer.renderable;

import com.gaia3d.basic.model.GaiaMesh;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RenderableMesh {
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


    public void deleteGLBuffers() {
        for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
            renderablePrimitive.deleteGLBuffers();
        }

        // remove all elements from map.
        renderablePrimitives.clear();
    }
}
