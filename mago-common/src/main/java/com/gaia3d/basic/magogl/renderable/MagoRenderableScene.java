package com.gaia3d.basic.magogl.renderable;

import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MagoRenderableScene {

    private final List<MagoRenderableNode> renderableNodes =
            new ArrayList<>();

    private final List<GaiaMaterial> materials =
            new ArrayList<>();

    private GaiaScene originalGaiaScene;
    private Path originalPath;
}