package com.gaia3d.basic.model.structure;

import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class PrimitiveStructure {
    protected List<GaiaVertex> vertices = new ArrayList<>();
    protected List<GaiaSurface> surfaces = new ArrayList<>();
}
