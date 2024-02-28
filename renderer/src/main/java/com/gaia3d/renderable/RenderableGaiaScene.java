package com.gaia3d.renderable;

import java.util.ArrayList;
import java.util.List;

public class RenderableGaiaScene {
    List<RenderableNode> renderableNodess;
    public RenderableGaiaScene() {
        renderableNodess = new ArrayList<>();
    }

    public void addRenderableNode(RenderableNode renderableNode) {
        renderableNodess.add(renderableNode);
    }
}
