package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SceneCleaner {

    public void clean(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            removeDegenerateTriangles(node);
            removeUnusedVertices(node);

            if (isEmptyNode(node)) {
                log.debug("Removing empty node: {}", node.getName());
            }
        }
    }

    private void removeDegenerateTriangles(GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        for (GaiaNode child : children) {
            removeDegenerateTriangles(child);
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null) {return;}
        for (GaiaMesh mesh : meshes) {
            removeDegenerateTriangles(mesh);
        }
    }

    private void removeDegenerateTriangles(GaiaMesh mesh) {
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null) {return;}
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            if (vertices == null || surfaces == null) {continue;}
            for (GaiaSurface surface : surfaces) {
                List<GaiaFace> faces = surface.getFaces();
                if (faces == null) {continue;}

                List<GaiaFace> newFaces = new ArrayList<>();
                for (GaiaFace face : faces) {
                    List<Integer> newIndices = new ArrayList<>();

                    int[] indices = face.getIndices();
                    for (int i = 0; i < indices.length; i += 3) {
                        GaiaVertex v1 = vertices.get(indices[i]);
                        GaiaVertex v2 = vertices.get(indices[i + 1]);
                        GaiaVertex v3 = vertices.get(indices[i + 2]);
                        Vector3d p1 = v1.getPosition();
                        Vector3d p2 = v2.getPosition();
                        Vector3d p3 = v3.getPosition();

                        boolean isEqual = p1.equals(p2) || p2.equals(p3) || p3.equals(p1);
                        if (isEqual) {
                            log.debug("[DEBUG] Degenerate triangle found due to identical vertices: {}, {}, {}", p1, p2, p3);
                            continue;
                        }

                        boolean isCollinear = (p2.x - p1.x) * (p3.y - p1.y) == (p3.x - p1.x) * (p2.y - p1.y) &&
                                (p2.y - p1.y) * (p3.z - p1.z) == (p3.y - p1.y) * (p2.z - p1.z) &&
                                (p2.z - p1.z) * (p3.x - p1.x) == (p3.z - p1.z) * (p2.x - p1.x);
                        if (isCollinear) {
                            log.debug("[DEBUG] Degenerate triangle found due to collinear vertices: {}, {}, {}", p1, p2, p3);
                            continue;
                        }

                        newIndices.add(indices[i]);
                        newIndices.add(indices[i + 1]);
                        newIndices.add(indices[i + 2]);
                    }

                    // Add the new face if it has at least 3 indices
                    if (newIndices.size() >= 3) {
                        int[] indicesArray = newIndices.stream().mapToInt(Integer::intValue).toArray();
                        GaiaFace newFace = new GaiaFace();
                        newFace.setId(face.getId());
                        newFace.setClassifyId(face.getClassifyId());
                        newFace.setIndices(indicesArray);
                        newFaces.add(newFace);
                    }
                }

                surface.setFaces(newFaces);
            }
        }
    }


    private void removeUnusedVertices(GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        for (GaiaNode child : children) {
            removeUnusedVertices(child);
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null) {return;}
        for (GaiaMesh mesh : meshes) {
            removeUnusedVertices(mesh);
        }
    }

    private void removeUnusedVertices(GaiaMesh mesh) {
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null) {return;}
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            if (vertices == null || surfaces == null) {continue;}

            boolean[] used = new boolean[vertices.size()];
            for (GaiaSurface surface : surfaces) {
                List<GaiaFace> faces = surface.getFaces();
                if (faces == null) {continue;}
                for (GaiaFace face : faces) {
                    int[] indices = face.getIndices();
                    for (int index : indices) {
                        used[index] = true;
                    }
                }
            }

            List<GaiaVertex> newVertices = new ArrayList<>();
            int[] indexMap = new int[vertices.size()];
            int newIndex = 0;
            for (int i = 0; i < vertices.size(); i++) {
                if (used[i]) {
                    newVertices.add(vertices.get(i));
                    indexMap[i] = newIndex++;
                } else {
                    indexMap[i] = -1; // Mark unused vertex
                }
            }
            primitive.setVertices(newVertices);

            for (GaiaSurface surface : surfaces) {
                List<GaiaFace> faces = surface.getFaces();
                if (faces == null) {continue;}
                for (GaiaFace face : faces) {
                    int[] indices = face.getIndices();
                    for (int i = 0; i < indices.length; i++) {
                        indices[i] = indexMap[indices[i]];
                    }
                    face.setIndices(indices);
                }
            }
        }
    }

    private boolean isEmptyNode(GaiaNode node) {
        boolean isEmpty = false;

        boolean hasMeshes = false;
        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null) {
            for (GaiaMesh mesh : meshes) {
                removeEmptyMeshes(mesh);
            }
            boolean allMeshesEmpty = meshes.stream().allMatch(mesh -> mesh.getPrimitives() == null || mesh.getPrimitives().isEmpty());
            if (allMeshesEmpty) {
                node.getMeshes().clear();
            } else {
                hasMeshes = true;
            }
        }

        boolean hasChildren = false;
        List<GaiaNode> children = node.getChildren();
        if (children != null) {
            for (GaiaNode child : children) {
                if (!isEmptyNode(child)) {
                    hasChildren = true;
                }
            }
        }

        if (!hasMeshes && !hasChildren) {
            isEmpty = true;
        }
        return isEmpty;
    }

    private void removeEmptyMeshes(GaiaMesh mesh) {
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null) {return;}
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            if (surfaces != null) {
                surfaces.removeIf(surface -> surface.getFaces() == null || surface.getFaces().isEmpty());
            }
        }
        primitives.removeIf(primitive -> primitive.getSurfaces() == null || primitive.getSurfaces().isEmpty());
    }
}
