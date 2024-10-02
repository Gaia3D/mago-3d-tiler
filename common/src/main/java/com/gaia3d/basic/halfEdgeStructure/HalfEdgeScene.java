package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.structure.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class HalfEdgeScene {
    @Setter
    @Getter
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    @Setter
    @Getter
    private List<GaiaMaterial> materials = new ArrayList<>();

}
