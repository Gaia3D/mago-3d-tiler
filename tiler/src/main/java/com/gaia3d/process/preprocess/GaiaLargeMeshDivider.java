package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Deprecated
public class GaiaLargeMeshDivider {
    public List<GaiaScene> run(List<GaiaScene> gaiaScenes) {
        return gaiaScenes;
    }

    private void divideScene(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            divideNode(node);
        }
    }

    private void divideNode(GaiaNode node) {
        List<GaiaMesh> dividedMeshes = new ArrayList<>();
        List<GaiaMesh> meshes = node.getMeshes();
        meshes.forEach((mesh) -> {
            int[] indices = mesh.getIndices();
            if (indices.length >= 65535) {
                log.info("Divide mesh : {}", indices.length);
                dividedMeshes.addAll(divideMesh(mesh));
            } else {
                dividedMeshes.add(mesh);
            }
        });
        node.setMeshes(dividedMeshes);
        List<GaiaNode> children = node.getChildren();
        for (GaiaNode child : children) {
            divideNode(child);
        }
    }

    private List<GaiaMesh> divideMesh(GaiaMesh mesh) {
        List<GaiaMesh> dividedMeshes = new ArrayList<>();

        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        primitives.forEach((primitive) -> {
            int[] indices = primitive.getIndices();
            if (indices.length >= 65535) {
                log.info("Divide Primitive : {}", indices.length);
                List<GaiaPrimitive> dividedPrimitives = dividePrimitive(primitive);
                for (GaiaPrimitive dividedPrimitive : dividedPrimitives) {
                    GaiaMesh dividedMesh = new GaiaMesh();
                    dividedMesh.setPrimitives(List.of(dividedPrimitive));
                    dividedMeshes.add(dividedMesh);
                }
            } else {
                GaiaMesh dividedMesh = new GaiaMesh();
                dividedMesh.setPrimitives(primitives);
                dividedMeshes.add(dividedMesh);
            }
        });
        return dividedMeshes;
    }

    private List<GaiaPrimitive> dividePrimitive(GaiaPrimitive primitive) {
        List<GaiaPrimitive> dividedPrimitives = new ArrayList<>();

        List<GaiaSurface> surfaces = primitive.getSurfaces();
        surfaces.forEach((surface) -> {
            int[] indices = surface.getIndices();
            if (indices.length >= 65535) {
                log.info("Divide Surface : {}", indices.length);
                dividedPrimitives.addAll(divideSurface(primitive, surface));
            } else {
                GaiaPrimitive dividedPrimitive = new GaiaPrimitive();
                dividedPrimitive.setMaterialIndex(primitive.getMaterialIndex());
                dividedPrimitive.getSurfaces().add(surface);
                dividedPrimitives.add(dividedPrimitive);
            }
        });

        return dividedPrimitives;
    }

    private List<GaiaPrimitive> divideSurface(GaiaPrimitive primitive, GaiaSurface surface) {
        List<GaiaPrimitive> dividedPrimitives = new ArrayList<>();
        List<GaiaFace> faces = surface.getFaces();
        List<GaiaVertex> vertices = primitive.getVertices();

        GaiaSurface dividedSurface = null;
        //List<GaiaVertex> dividedVertices = null;

        int minIndex = Integer.MAX_VALUE;
        int maxIndex = 0;
        for (GaiaFace face : faces) {
            if (dividedSurface == null) {
                dividedSurface = new GaiaSurface();
                //dividedVertices = new ArrayList<>();
                GaiaPrimitive dividedPrimitive = new GaiaPrimitive();
                dividedPrimitive.getSurfaces().add(dividedSurface);
                dividedPrimitive.setMaterialIndex(primitive.getMaterialIndex());
                dividedPrimitive.setVertices(primitive.getVertices());

                dividedPrimitives.add(dividedPrimitive);
            } else if (dividedSurface.getIndices().length >= 65000) {
                dividedSurface = new GaiaSurface();
                //dividedVertices = new ArrayList<>();

                GaiaPrimitive dividedPrimitive = new GaiaPrimitive();
                dividedPrimitive.getSurfaces().add(dividedSurface);
                dividedPrimitive.setMaterialIndex(primitive.getMaterialIndex());
                dividedPrimitive.setVertices(primitive.getVertices());

                // only test
                dividedPrimitives.add(dividedPrimitive);
            }

            List<GaiaFace> dividedSurfaceFaces = dividedSurface.getFaces();
            dividedSurfaceFaces.add(face.clone());

            /*int[] indices = face.getIndices();
            for (int index : indices) {
                dividedVertices.add(vertices.get(index));
            }*/
        }

        for (GaiaPrimitive dividedPrimitive : dividedPrimitives) {
            rearrangeVerticesWithIndices(dividedPrimitive);
        }
        return dividedPrimitives;
    }

    /*private void rearrangeVerticesWithIndices(GaiaPrimitive primitive) {
        GaiaSurface surface = primitive.getSurfaces().get(0); // only have one surface
        List<GaiaFace> faces = surface.getFaces();
        List<GaiaVertex> vertices = primitive.getVertices();

        int minIndex = Integer.MAX_VALUE;
        int maxIndex = 0;
        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            for (int index : indices) {
                if (index < minIndex) {
                    minIndex = index;
                }
                if (index > maxIndex) {
                    maxIndex = index;
                }
            }
        }
        log.info("minIndex : {}, maxIndex : {}, offset : {}", minIndex, maxIndex, maxIndex - minIndex);

        //List<GaiaVertex> subtractedVertices = vertices.subList(minIndex, maxIndex + 1);
        List<GaiaVertex> subtractedVertices = new ArrayList<>();
        for (int i = minIndex; i <= maxIndex; i++) {
            subtractedVertices.add(vertices.get(i).clone());
        }

        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            for (int i = 0; i < indices.length; i++) {
                indices[i] -= minIndex;
            }
        }

        primitive.setVertices(subtractedVertices);
    }*/

    private void rearrangeVerticesWithIndices(GaiaPrimitive primitive) {
        GaiaSurface surface = primitive.getSurfaces().get(0); // only have one surface
        List<GaiaFace> faces = surface.getFaces();
        List<GaiaVertex> vertices = primitive.getVertices();

        Map<Integer, GaiaVertex> vertexHashMap = new HashMap<>();
        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            for (int index : indices) {
                GaiaVertex vertex = vertices.get(index);
                vertexHashMap.put(index, vertex.clone());
            }
        }
        List<GaiaVertex> subtractedVertices = new ArrayList<>(vertexHashMap.values());


        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            for (int i = 0; i < indices.length; i++) {
                indices[i] = subtractedVertices.indexOf(vertexHashMap.get(indices[i]));
            }
        }

        primitive.setVertices(subtractedVertices);
    }
}
