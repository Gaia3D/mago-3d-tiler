package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NodeMerger {

    public void merge(GaiaScene scene) {
        List<GaiaNode> mergedNodes = new ArrayList<>();

        List<GaiaNode> rootNodes = scene.getNodes();
        for (GaiaNode node : rootNodes) {
            List<GaiaMesh> allMeshes = getAllMeshes(node);
            List<GaiaPrimitive> allPrimitives = getAllPrimitives(allMeshes);

            GaiaNode mergedNode = new GaiaNode();
            mergedNode.setName(node.getName() + "_merged");
            mergedNode.setTransformMatrix(node.getTransformMatrix());
            mergedNode.setMeshes(new ArrayList<>());
            if (!allPrimitives.isEmpty()) {
                GaiaPrimitive mergedPrimitive = mergePrimitives(allPrimitives);
                GaiaMesh mergedMesh = new GaiaMesh();
                mergedMesh.setPrimitives(List.of(mergedPrimitive));
                mergedNode.getMeshes().add(mergedMesh);
            }

            mergedNodes.add(mergedNode);

            log.info("Merging {} meshes in node {}", allMeshes.size(), node.getName());
        }

        scene.setNodes(mergedNodes);
    }

    private List<GaiaMesh> getAllMeshes(GaiaNode node) {
        return getAllMeshes(new ArrayList<>(), new Matrix4d().identity(), node);
    }

    private List<GaiaMesh> getAllMeshes(List<GaiaMesh> allMeshes, Matrix4d parentMatrix, GaiaNode node) {
        Matrix4d localMatrix = node.getTransformMatrix();
        Matrix4d worldMatrix = new Matrix4d(parentMatrix).mul(localMatrix);

        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                getAllMeshes(allMeshes, worldMatrix, child);
            }
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null && !meshes.isEmpty()) {
            allMeshes.addAll(meshes);
        }
        return allMeshes;
    }

    private List<GaiaPrimitive> getAllPrimitives(List<GaiaMesh> meshes) {
        List<GaiaPrimitive> allPrimitives = new ArrayList<>();
        for (GaiaMesh mesh : meshes) {
            List<GaiaPrimitive> primitives = mesh.getPrimitives();
            if (primitives != null && !primitives.isEmpty()) {
                allPrimitives.addAll(primitives);
            }
        }
        return allPrimitives;
    }

    private List<GaiaFace> getAllFaces(List<GaiaSurface> gaiaSurfaces) {
        List<GaiaFace> allFaces = new ArrayList<>();
        for (GaiaSurface surface : gaiaSurfaces) {
            List<GaiaFace> faces = surface.getFaces();
            if (faces != null && !faces.isEmpty()) {
                allFaces.addAll(faces);
            }
        }
        return allFaces;
    }

    private GaiaPrimitive mergePrimitives(List<GaiaPrimitive> primitives) {
        int materialIndex = primitives.get(0).getMaterialIndex();

        List<GaiaVertex> mergedVertices = new ArrayList<>();
        List<GaiaFace> mergedFaces = new ArrayList<>();
        //List<GaiaSurface> surfaces = new ArrayList<>();

        for (GaiaPrimitive primitive : primitives) {
            GaiaSurface mergedSurface = new GaiaSurface();
            //List<GaiaFace> mergedFaces = new ArrayList<>();

            List<GaiaVertex> vertices = primitive.getVertices();
            List<GaiaFace> faces = getAllFaces(primitive.getSurfaces());

            int vertexOffset = mergedVertices.size();
            mergedVertices.addAll(vertices);

            for (GaiaFace face : faces) {
                GaiaFace adjustedFace = new GaiaFace();
                adjustedFace.setId(face.getId());
                adjustedFace.setClassifyId(face.getClassifyId());

                int[] indices = face.getIndices();
                int[] adjustedIndices = new int[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    adjustedIndices[i] = vertexOffset + indices[i];
                }
                adjustedFace.setIndices(adjustedIndices);
                mergedFaces.add(adjustedFace);
            }

            mergedSurface.setFaces(mergedFaces);
            //surfaces.add(mergedSurface);
        }

        GaiaSurface mergedSurface = new GaiaSurface();
        mergedSurface.setFaces(mergedFaces);
        List<GaiaSurface> surfaces = List.of(mergedSurface);

        GaiaPrimitive mergedPrimitive = new GaiaPrimitive();
        mergedPrimitive.setVertices(mergedVertices);
        mergedPrimitive.setSurfaces(surfaces);
        mergedPrimitive.setMaterialIndex(materialIndex);

        return mergedPrimitive;
    }
}
