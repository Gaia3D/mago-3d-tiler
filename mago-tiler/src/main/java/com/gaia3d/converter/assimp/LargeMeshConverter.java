package com.gaia3d.converter.assimp;

import com.gaia3d.basic.model.*;
import com.gaia3d.basic.splitter.GaiaSceneSplitter;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.GaiaSceneTempGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Deprecated
public class LargeMeshConverter implements Converter {
    private final Converter converter;
    private final int THRES_HOLD = 65535;

    @Override
    public List<GaiaScene> load(String path) {
        List<GaiaScene> scenes = converter.load(path);
        return separateScenes(scenes);
    }

    @Override
    public List<GaiaScene> load(File file) {
        List<GaiaScene> scenes = converter.load(file);

        List<GaiaScene> resultGaiaScenes = new ArrayList<>();
        GaiaSceneSplitter.splitScenes(scenes, resultGaiaScenes);

        return resultGaiaScenes; // new
    }

    @Override
    public List<GaiaScene> load(Path path) {
        List<GaiaScene> scenes = converter.load(path);
        return separateScenes(scenes);
    }

    @Override
    public List<GaiaSceneTempGroup> convertTemp(File input, File output) {
        return null;
    }

    private List<GaiaScene> separateScenes(List<GaiaScene> scenes) {
        int size = scenes.size();
        List<GaiaScene> removedScenes = new ArrayList<>();
        List<GaiaScene> separatedScenes = new ArrayList<>();
        for (GaiaScene scene : scenes) {
            List<GaiaScene> separatedMeshes = separateScene(scene);
            if (!separatedMeshes.isEmpty()) {
                removedScenes.add(scene);
                separatedScenes.addAll(separatedMeshes);
            }
        }
        scenes.removeAll(removedScenes);
        scenes.addAll(separatedScenes);
        log.info("[LargeMeshConverter] Before Scenes : {}, After Scenes : {}", size, scenes.size());
        return scenes;
    }

    private List<GaiaScene> separateScene(GaiaScene scene) {
        List<GaiaScene> separatedScenes = new ArrayList<>();
        List<GaiaNode> nodes = scene.getNodes();
        nodes.forEach(node -> {
            separateNode(separatedScenes, scene, node);
        });
        return separatedScenes;
    }

    private void separateNode(List<GaiaScene> newScenes, GaiaScene scene, GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        children.forEach(child -> {
            separateNode(newScenes, scene, child);
        });
        List<GaiaMesh> meshes = node.getMeshes();
        List<GaiaMesh> removedMeshes = new ArrayList<>();
        meshes.forEach(mesh -> {
            long triangleCount = mesh.getTriangleCount();
            long vertexCount = mesh.getPositionsCount();
            long indicesCount = mesh.getIndicesCount();

            if (vertexCount > THRES_HOLD) {
                int separatedCount = (int) (vertexCount / THRES_HOLD);

                // TODO implement logic to separate large mesh
                log.info("[LargeMeshConverter] Large Mesh Name : {}", node.getName());
                log.info(" - Mesh Triangle Count : {}", triangleCount);
                log.info(" - Mesh Vertex Count : {}", vertexCount);
                log.info(" - Mesh Indices Count : {}", indicesCount);
                log.info(" - Mesh Index : {}", separatedCount);

                List<GaiaScene> newScene = separateMesh(scene, node, mesh);
                newScenes.addAll(newScene);

                removedMeshes.add(mesh);
            }
        });
        meshes.removeAll(removedMeshes);
    }

    private List<GaiaScene> separateMesh(GaiaScene scene, GaiaNode node, GaiaMesh mesh) {
        List<GaiaPrimitive> newPrimitives = new ArrayList<>();
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        primitives.forEach(primitive -> {
            PrimitiveSeparator separator = new PrimitiveSeparator();
            List<GaiaPrimitive> separatedPrimitives = separator.separatePrimitives(primitive, THRES_HOLD);
            newPrimitives.addAll(separatedPrimitives);
        });

        List<GaiaScene> newScenes = new ArrayList<>();
        newPrimitives.forEach(primitive -> {
            GaiaScene newScene = createNewScene(scene, node, primitive);
            newScenes.add(newScene);
        });
        return newScenes;
    }

    private GaiaScene createNewScene(GaiaScene scene, GaiaNode node, GaiaPrimitive primitive) {
        GaiaScene newScene = new GaiaScene();
        List<GaiaNode> rootNode = new ArrayList<>();
        List<GaiaMaterial> materials = scene.getMaterials().stream().map(GaiaMaterial::clone).collect(Collectors.toList());

        newScene.setNodes(rootNode);
        newScene.setMaterials(materials);
        newScene.setOriginalPath(scene.getOriginalPath());

        GaiaNode newNode = new GaiaNode();
        List<GaiaMesh> newMeshes = new ArrayList<>();
        rootNode.add(newNode);
        newNode.setTransformMatrix(new Matrix4d(node.getTransformMatrix()));
        newNode.setMeshes(newMeshes);

        GaiaMesh newMesh = new GaiaMesh();
        List<GaiaPrimitive> newPrimitives = new ArrayList<>();
        newPrimitives.add(primitive);

        newMesh.setPrimitives(newPrimitives);
        newMeshes.add(newMesh);
        return newScene;
    }
}
