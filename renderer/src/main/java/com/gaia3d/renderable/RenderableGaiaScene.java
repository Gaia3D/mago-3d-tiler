package com.gaia3d.renderable;

import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaScene;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class RenderableGaiaScene {
    private GaiaScene originalGaiaScene;
    private Path originalPath;
    List<RenderableNode> renderableNodess;
    private List<GaiaMaterial> materials = new ArrayList<>();
    public RenderableGaiaScene() {
        renderableNodess = new ArrayList<>();
        originalGaiaScene = null;
    }

    public void addRenderableNode(RenderableNode renderableNode) {
        renderableNodess.add(renderableNode);
    }

    public void extractRenderablePrimitives(List<RenderablePrimitive> resultRenderablePrimitives) {
        for (RenderableNode renderableNode : renderableNodess) {
            renderableNode.extractRenderablePrimitives(resultRenderablePrimitives);
        }
    }
}
