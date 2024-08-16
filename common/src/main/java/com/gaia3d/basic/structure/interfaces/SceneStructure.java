package com.gaia3d.basic.structure.interfaces;

import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaNode;
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
