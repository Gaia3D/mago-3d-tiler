package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaAttribute;
import com.gaia3d.basic.structure.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HalfEdgeScene {
    @Setter
    @Getter
    private Path originalPath;
    @Setter
    @Getter
    private GaiaBoundingBox gaiaBoundingBox;
    @Setter
    @Getter
    private GaiaAttribute attribute;

    @Setter
    @Getter
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    @Setter
    @Getter
    private List<GaiaMaterial> materials = new ArrayList<>();

    public void doTrianglesReduction()
    {
        for (HalfEdgeNode node : nodes)
        {
            node.doTrianglesReduction();
        }
    }

}
