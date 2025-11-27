package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.*;

@Slf4j
public class SeparateNodeByFace {

    public void combineNodesByFace(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            combineNodesByFace(node);
        }
        scene.updateBoundingBox();
    }

    private void combineNodesByFace(GaiaNode node) {
        for (GaiaNode child : node.getChildren()) {
            combineNodesByFace(child);
        }

        List<GaiaMesh> meshes = node.getMeshes();
        for (GaiaMesh mesh : meshes) {
            List<GaiaPrimitive> primitives = mesh.getPrimitives();
            for (GaiaPrimitive primitive : primitives) {
                List<GaiaSurface> surfaces = primitive.getSurfaces();
                for (GaiaSurface surface : surfaces) {
                    List<GaiaFace> faces = surface.getFaces();
                    if (faces.isEmpty()) {
                        log.debug("[DEBUG] No faces found in surface of primitive in node {}", node.getName());
                        continue;
                    }
                    if (faces.size() == 1) {
                        log.debug("[DEBUG] Node {} has only one face, no need to combine.", node.getName());
                        continue;
                    }

                    // sort faces by id
                    List<GaiaFace> sortedFaces = faces.stream()
                            .sorted(Comparator.comparingInt(GaiaFace::getId))
                            .toList();

                    // same faceId combine
                    List<GaiaFace> combinedFaces = new ArrayList<>();

                    // single triangle faces
                    for (int i = 0; i < sortedFaces.size(); i++) {
                        GaiaFace face = sortedFaces.get(i);
                        GaiaFace nextFace = (i < sortedFaces.size() - 1) ? sortedFaces.get(i + 1) : null;
                        GaiaFace previousFace = (i > 0) ? sortedFaces.get(i - 1) : null;

                        if (i == 0) {
                            if (face.getId() != nextFace.getId()) {
                                combinedFaces.add(face);
                            }
                            continue;
                        } else if (i == sortedFaces.size() - 1) {
                            if (face.getId() != previousFace.getId()) {
                                combinedFaces.add(face);
                            }
                            continue;
                        }

                        if (face.getId() != previousFace.getId() && face.getId() != nextFace.getId()) {
                            combinedFaces.add(face);
                        }
                    }

                    // double triangle faces
                    GaiaFace previousFace = null;
                    for (GaiaFace face : sortedFaces) {
                        if (previousFace != null) {
                            boolean sameFaceId = previousFace.getId() == face.getId();
                            // dont check indices
                            if (sameFaceId) {
                                // combine faces
                                int[] previousIndices = previousFace.getIndices();
                                int[] currentIndices = face.getIndices();
                                int[] combinedIndices = new int[previousIndices.length + currentIndices.length];
                                System.arraycopy(previousIndices, 0, combinedIndices, 0, previousIndices.length);
                                System.arraycopy(currentIndices, 0, combinedIndices, previousIndices.length, currentIndices.length);

                                GaiaFace combinedFace = new GaiaFace();
                                combinedFace.setId(previousFace.getId());
                                combinedFace.setIndices(combinedIndices);
                                combinedFaces.add(combinedFace);
                            }
                        }
                        previousFace = face;
                    }
                    surface.setFaces(combinedFaces);
                }
            }
        }
    }

    public void separateNodesBySurface(GaiaScene scene) {
        List<GaiaNode> result = new ArrayList<>();
        for (GaiaNode node : scene.getNodes()) {
            List<GaiaNode> separatedNode = separateNodeBySurface(null, node);

            GaiaNode clonedNode = new GaiaNode();
            clonedNode.setName(node.getName());
            clonedNode.setChildren(separatedNode);
            clonedNode.setTransformMatrix(new Matrix4d(node.getTransformMatrix()));

            result.add(clonedNode);
        }
        scene.setNodes(result);
        scene.updateBoundingBox();
    }

    private List<GaiaNode> separateNodeBySurface(List<GaiaNode> separatedNodes, GaiaNode node) {
        if (separatedNodes == null) {
            separatedNodes = new ArrayList<>();
        }

        for (GaiaNode child : node.getChildren()) {
            separateNodeBySurface(separatedNodes, child);
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null || meshes.isEmpty()) {
            return separatedNodes;
        }
        GaiaMesh firstMesh = meshes.get(0);

        List<GaiaPrimitive> primitives = firstMesh.getPrimitives();
        if (primitives.isEmpty()) {
            log.warn("Node {} has no primitives to separate by face.", node.getName());
            return separatedNodes;
        }
        GaiaPrimitive firstPrimitive = primitives.get(0);
        int materialIndex = firstPrimitive.getMaterialIndex();

        List<GaiaSurface> surfaces = firstPrimitive.getSurfaces();
        if (surfaces.isEmpty()) {
            log.warn("Node {} has no surfaces to separate by face.", node.getName());
            return separatedNodes;
        }

        int surfaceCount = 0;
        for (GaiaSurface surface : surfaces) {
            GaiaNode childNode = new GaiaNode();
            String nodeName = "" + surfaceCount++;
            childNode.setName(nodeName);
            childNode.setTransformMatrix(new Matrix4d(node.getTransformMatrix()));

            GaiaMesh newMesh = new GaiaMesh();
            childNode.getMeshes()
                    .add(newMesh);

            GaiaPrimitive newPrimitive = new GaiaPrimitive();
            newPrimitive.setMaterialIndex(materialIndex);
            newPrimitive.setVertices(firstPrimitive.getVertices());
            newPrimitive.getSurfaces()
                    .add(surface);
            newMesh.getPrimitives()
                    .add(newPrimitive);

            removeUnusedVertices(newPrimitive);
            separatedNodes.add(childNode);
        }

        return separatedNodes;
    }

    public void removeUnusedVertices(GaiaPrimitive primitive) {
        List<GaiaVertex> originalVertices = primitive.getVertices();
        List<GaiaFace> faces = primitive.getSurfaces()
                .stream()
                .flatMap(surface -> surface.getFaces()
                        .stream())
                .toList();

        // 1. 사용된 인덱스만 수집
        Set<Integer> usedIndexSet = new HashSet<>();
        for (GaiaFace face : faces) {
            //log.info(Arrays.toString(face.getFaceColor()) + " - " + face.getIndices().length);
            for (int index : face.getIndices()) {
                usedIndexSet.add(index);
            }
        }

        // 2. 기존 index -> 새로운 index 매핑 생성
        Map<Integer, Integer> oldToNewIndexMap = new HashMap<>();
        List<GaiaVertex> newVertices = new ArrayList<>();
        int newIndex = 0;
        for (int oldIndex : usedIndexSet.stream()
                .sorted()
                .toList()) {
            oldToNewIndexMap.put(oldIndex, newIndex++);
            newVertices.add(originalVertices.get(oldIndex)
                    .clone());
        }

        // 3. faces의 indices를 새 인덱스로 재정렬
        for (GaiaFace face : faces) {
            int[] oldIndices = face.getIndices();
            int[] remappedIndices = new int[oldIndices.length];
            for (int i = 0; i < oldIndices.length; i++) {
                remappedIndices[i] = oldToNewIndexMap.get(oldIndices[i]);
            }
            face.setIndices(remappedIndices);
        }

        // 4. primitive에 새로운 vertex 리스트 설정
        primitive.setVertices(newVertices);

        // 5 . primitive의 surfaces를 새로 설정
        List<GaiaSurface> newSurfaces = new ArrayList<>();
        GaiaSurface newSurface = new GaiaSurface();
        newSurface.setFaces(faces);
        newSurfaces.add(newSurface);
        primitive.setSurfaces(newSurfaces);

    }
}
