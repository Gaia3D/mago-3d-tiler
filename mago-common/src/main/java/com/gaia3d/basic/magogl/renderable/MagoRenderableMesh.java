package com.gaia3d.basic.magogl.renderable;

import com.gaia3d.basic.model.GaiaMesh;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MagoRenderableMesh {
    private final List<MagoRenderablePrimitive> renderablePrimitives =
            new ArrayList<>();

    public void deleteObjects(){
        for(MagoRenderablePrimitive primitive : this.renderablePrimitives){
            primitive.deleteObjects();
        }
    }
}