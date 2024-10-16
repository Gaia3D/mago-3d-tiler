package com.gaia3d.basic.model.structure;

import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class SceneStructure {
    protected List<GaiaNode> nodes = new ArrayList<>();
    protected List<GaiaMaterial> materials = new ArrayList<>();
}
