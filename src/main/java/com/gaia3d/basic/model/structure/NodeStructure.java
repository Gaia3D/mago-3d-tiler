package com.gaia3d.basic.model.structure;

import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class NodeStructure {
    protected GaiaNode parent = null;
    protected List<GaiaMesh> meshes = new ArrayList<>();
    protected List<GaiaNode> children = new ArrayList<>();
}
