package com.gaia3d.basic.magogl.renderable;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MagoRenderableScene {

    private final List<MagoRenderableNode> renderableNodes =
            new ArrayList<>();

    public void deleteObjects(){
        if(!renderableNodes.isEmpty()){
            for(MagoRenderableNode node : renderableNodes){
                node.deleteObjects();
            }
        }
    }

    public boolean isEmpty() {
        return renderableNodes.isEmpty();
    }
}
