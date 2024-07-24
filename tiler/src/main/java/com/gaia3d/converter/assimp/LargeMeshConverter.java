package com.gaia3d.converter.assimp;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.basic.structure.GaiaSceneSplitter;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

// decorator pattern
@Slf4j
@RequiredArgsConstructor
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

        // as a test, calculate the geoCoord of the center of the scene.***
        GaiaScene originalScene = scenes.get(0);
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaBoundingBox originalBBox = originalScene.getBoundingBox();
        CoordinateReferenceSystem source = globalOptions.getCrs();
        Vector3d center = originalBBox.getCenter();
        Vector3d originalCenterGeoCoord = new Vector3d();
        if (source != null) {
            ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, originalBBox.getMinZ());
            ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
            Vector3d geoCoord = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);
            originalCenterGeoCoord.set(geoCoord);
            int hola = 0;
        }
        // End test.------------------------------------------------------------

        // Test new scene splitter.*****************************************
        GaiaSceneSplitter splitter = new GaiaSceneSplitter();
        List<GaiaScene> resultGaiaScenes = new ArrayList<>();
        splitter.splitScenes(scenes, resultGaiaScenes);
        // End test new scene splitter.-------------------------------------

        // as a test, calculate the geoCoord of the center of the scenes.***
        GaiaBoundingBox totalBoundingBox = new GaiaBoundingBox();

        int splittedScenesCount = resultGaiaScenes.size();
        for (int i = 0; i < splittedScenesCount; i++) {
            GaiaScene scene = resultGaiaScenes.get(i);
            GaiaBoundingBox sceneBoundingBox = scene.getBoundingBox();
            totalBoundingBox.addBoundingBox(sceneBoundingBox);
        }

        Vector3d splittedCenter = totalBoundingBox.getCenter();

        Vector3d splittedCenterGeoCoord = new Vector3d();
        if (source != null) {
            ProjCoordinate centerSourceSplitted = new ProjCoordinate(splittedCenter.x, splittedCenter.y, totalBoundingBox.getMinZ());
            ProjCoordinate centerWgs84Splitted = GlobeUtils.transform(source, centerSourceSplitted);
            Vector3d geoCoord = new Vector3d(centerWgs84Splitted.x, centerWgs84Splitted.y, 0.0d);
            splittedCenterGeoCoord.set(geoCoord);
            int hola = 0;
        }
        // End test.------------------------------------------------------------

        return resultGaiaScenes; // new.***
        //return separateScenes(scenes); // original.***
    }

    @Override
    public List<GaiaScene> load(Path path) {
        List<GaiaScene> scenes = converter.load(path);
        return separateScenes(scenes);
    }

    private List<GaiaScene> separateScenes(List<GaiaScene> scenes) {
        int size = scenes.size();
        List<GaiaScene> removedScenes = new ArrayList<>();
        List <GaiaScene> separatedScenes = new ArrayList<>();
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
                int spareatedCount = (int) (vertexCount / THRES_HOLD);

                // TODO implement logic to separate large mesh
                log.info("[LargeMeshConverter] Large Mesh Name : {}", node.getName());
                log.info(" - Mesh Triangle Count : {}", triangleCount);
                log.info(" - Mesh Vertex Count : {}", vertexCount);
                log.info(" - Mesh Indices Count : {}", indicesCount);
                log.info(" - Mesh Index : {}", spareatedCount);

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
        List<GaiaMaterial> materials = scene.getMaterials()
                .stream().map(GaiaMaterial::clone)
                .collect(Collectors.toList());

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
