package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HalfEdgeScene {
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();

    public void doTrianglesReduction() {
        for (HalfEdgeNode node : nodes) {
            node.doTrianglesReduction();
        }
    }

    public void deleteObjects() {
        for (HalfEdgeNode node : nodes) {
            node.deleteObjects();
        }
        nodes.clear();
        materials.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeNode node : nodes) {
            node.checkSandClockFaces();
        }
    }

    public void spendTransformationMatrix() {
        for (HalfEdgeNode node : nodes) {
            node.spendTransformationMatrix();
        }
    }

}
