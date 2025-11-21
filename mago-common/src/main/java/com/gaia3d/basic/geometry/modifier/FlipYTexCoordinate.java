package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.*;
import org.joml.Vector2d;

import java.util.List;

public class FlipYTexCoordinate {

    public void flip(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            flip(node);
        }
    }

    private void flip(GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        for (GaiaNode child : children) {
            flip(child);
        }

        List<GaiaMesh> meshes = node.getMeshes();
        for (GaiaMesh mesh : meshes) {
            flip(mesh);
        }
    }

    private void flip(GaiaMesh mesh) {
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            for (GaiaVertex vertex : vertices) {
                flip(vertex);
            }
        }
    }

    private void flip(GaiaVertex vertex) {
        Vector2d texcoords = vertex.getTexcoords();
        texcoords.y = 1.0 - texcoords.y;
    }
}
