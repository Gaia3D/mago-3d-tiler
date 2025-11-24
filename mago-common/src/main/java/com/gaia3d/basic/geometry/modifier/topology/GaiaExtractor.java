package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.model.*;

import java.util.ArrayList;
import java.util.List;

public class GaiaExtractor {
    public List<GaiaMesh> extractMeshes(GaiaScene scene) {
        List<GaiaMesh> result = new ArrayList<>();
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            extractAllMeshes(result, node);
        }
        return result;
    }

    public List<GaiaMesh> extractMeshes(GaiaNode node) {
        return extractAllMeshes(null, node);
    }

    private List<GaiaMesh> extractAllMeshes(List<GaiaMesh> result, GaiaNode node) {
        if (result == null) {
            result = new ArrayList<>();
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null) {
            result.addAll(meshes);
        }

        for (GaiaNode childNode : node.getChildren()) {
            extractAllMeshes(result, childNode);
        }

        return result;
    }

    public List<GaiaNode> extractAllNodes(GaiaScene scene, boolean onlyLeafNodes) {
        List<GaiaNode> result = new ArrayList<>();
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            List<GaiaNode> childNodes = extractAllNodes(node, onlyLeafNodes);
            result.addAll(childNodes);
        }
        return result;
    }

    public List<GaiaNode> extractAllNodes(GaiaNode node, boolean onlyLeafNodes) {
        List<GaiaNode> result = new ArrayList<>();
        if (onlyLeafNodes && !node.getChildren().isEmpty()) {
            for (GaiaNode childNode : node.getChildren()) {
                List<GaiaNode> childNodes = extractAllNodes(childNode, onlyLeafNodes);
                result.addAll(childNodes);
            }
            return result;
        }

        for (GaiaNode childNode : node.getChildren()) {
            List<GaiaNode> childNodes = extractAllNodes(childNode, onlyLeafNodes);
            result.addAll(childNodes);
        }
        return result;
    }

    public List<GaiaPrimitive> extractAllPrimitives(GaiaScene scene) {
        List<GaiaPrimitive> result = new ArrayList<>();
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            List<GaiaPrimitive> primitives = extractAllPrimitives(node);
            result.addAll(primitives);
        }
        return result;
    }

    public List<GaiaPrimitive> extractAllPrimitives(GaiaNode node) {
        List<GaiaPrimitive> result = new ArrayList<>();
        List<GaiaMesh> meshes = extractAllMeshes(null, node);
        for (GaiaMesh mesh : meshes) {
            List<GaiaPrimitive> primitives = mesh.getPrimitives();
            if (primitives != null) {
                result.addAll(primitives);
            }
        }
        return result;
    }

    public List<GaiaSurface> extractAllSurfaces(GaiaScene scene) {
        List<GaiaSurface> result = new ArrayList<>();
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            List<GaiaSurface> surfaces = extractAllSurfaces(node);
            result.addAll(surfaces);
        }
        return result;
    }

    public List<GaiaSurface> extractAllSurfaces(GaiaNode node) {
        List<GaiaSurface> result = new ArrayList<>();
        List<GaiaPrimitive> primitives = extractAllPrimitives(node);
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            if (surfaces != null) {
                result.addAll(surfaces);
            }
        }
        return result;
    }

    public List<GaiaFace> extractAllFaces(GaiaScene scene) {
        List<GaiaFace> result = new ArrayList<>();
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            List<GaiaFace> faces = extractAllFaces(node);
            result.addAll(faces);
        }
        return result;
    }

    public List<GaiaFace> extractAllFaces(GaiaNode node) {
        List<GaiaFace> result = new ArrayList<>();
        List<GaiaSurface> surfaces = extractAllSurfaces(node);
        for (GaiaSurface surface : surfaces) {
            List<GaiaFace> faces = surface.getFaces();
            if (faces != null) {
                result.addAll(faces);
            }
        }
        return result;
    }
}
