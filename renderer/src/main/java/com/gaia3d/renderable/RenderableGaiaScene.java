package com.gaia3d.renderable;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class RenderableGaiaScene {
    List<RenderableNode> renderableNodess;
    public RenderableGaiaScene() {
        renderableNodess = new ArrayList<>();
    }

    public void addRenderableNode(RenderableNode renderableNode) {
        renderableNodess.add(renderableNode);
    }
}
