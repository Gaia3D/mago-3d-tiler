package com.gaia3d.renderable;

import com.gaia3d.basic.structure.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class RenderableGaiaScene {
    private Path originalPath;
    List<RenderableNode> renderableNodess;
    private List<GaiaMaterial> materials = new ArrayList<>();
    public RenderableGaiaScene() {
        renderableNodess = new ArrayList<>();
    }

    public void addRenderableNode(RenderableNode renderableNode) {
        renderableNodess.add(renderableNode);
    }
}
