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

    private Path originalPath;

    public void deleteObjects(){
        if(!renderableNodes.isEmpty()){
            for(MagoRenderableNode node : renderableNodes){
                node.deleteObjects();
            }
        }
    }
}