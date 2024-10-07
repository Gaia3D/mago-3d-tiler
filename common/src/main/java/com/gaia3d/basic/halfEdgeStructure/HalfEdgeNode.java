package com.gaia3d.basic.halfEdgeStructure;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class HalfEdgeNode {
    @Setter
    @Getter
    private HalfEdgeNode parent = null;
    @Setter
    @Getter
    private List<HalfEdgeMesh> meshes = new ArrayList<>();
    @Setter
    @Getter
    private List<HalfEdgeNode> children = new ArrayList<>();

    public void doTrianglesReduction()
    {
        for (HalfEdgeMesh mesh : meshes)
        {
            mesh.doTrianglesReduction();
        }
        for (HalfEdgeNode child : children)
        {
            child.doTrianglesReduction();
        }
    }
}
