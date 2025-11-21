package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class Colorizer {

    private int faceIndex = 0;
    private final ColorizeType colorizeType;

    public void colorize(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            colorize(node);
        }
    }

    private void colorize(GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                colorize(child);
            }
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null || meshes.isEmpty()) {
            return;
        }

        byte[] color = null;

        for (GaiaMesh mesh : meshes) {
            if (colorizeType == ColorizeType.MESH) {
                color = new byte[4];
                color[0] = (byte) (Math.random() * 255);
                color[1] = (byte) (Math.random() * 255);
                color[2] = (byte) (Math.random() * 255);
                color[3] = (byte) 255; // Alpha channel
            }

            List<GaiaPrimitive> primitives = mesh.getPrimitives();
            for (GaiaPrimitive primitive : primitives) {
                if (colorizeType == ColorizeType.PRIMITIVE) {
                    color = new byte[4];
                    color[0] = (byte) (Math.random() * 255);
                    color[1] = (byte) (Math.random() * 255);
                    color[2] = (byte) (Math.random() * 255);
                    color[3] = (byte) 255; // Alpha channel
                }

                List<GaiaVertex> vertices = primitive.getVertices();
                List<GaiaSurface> surfaces = primitive.getSurfaces();
                for (GaiaSurface surface : surfaces) {
                    if (colorizeType == ColorizeType.SURFACE) {
                        color = new byte[4];
                        color[0] = (byte) (Math.random() * 255);
                        color[1] = (byte) (Math.random() * 255);
                        color[2] = (byte) (Math.random() * 255);
                        color[3] = (byte) 255; // Alpha channel
                    }

                    List<GaiaFace> faces = surface.getFaces();
                    for (GaiaFace face : faces) {
                        if (colorizeType == ColorizeType.FACE) {
                            color = new byte[4];
                            color[0] = (byte) (Math.random() * 255);
                            color[1] = (byte) (Math.random() * 255);
                            color[2] = (byte) (Math.random() * 255);
                            color[3] = (byte) 255; // Alpha channel
                        }

                        int[] indices = face.getIndices();
                        for (int index : indices) {
                            if (colorizeType == ColorizeType.VERTEX) {
                                color = new byte[4];
                                color[0] = (byte) (Math.random() * 255);
                                color[1] = (byte) (Math.random() * 255);
                                color[2] = (byte) (Math.random() * 255);
                                color[3] = (byte) 255; // Alpha channel
                            }

                            if (index < 0 || index >= vertices.size()) {
                                log.warn("Index {} is out of bounds for vertices size {}", index, vertices.size());
                                continue;
                            }
                            GaiaVertex vertex = vertices.get(index);
                            vertex.setColor(color);
                        }
                    }
                }
            }
        }
    }

    public void generateFaceColorAndFaceId(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            generateFaceColorAndFaceIdByVertexColorByNode(node);
        }
    }

    private void generateFaceColorAndFaceIdByVertexColor(GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                generateFaceColorAndFaceIdByVertexColor(child);
            }
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null || meshes.isEmpty()) {
            return;
        }

        int faceId = 0;
        for (GaiaMesh mesh : meshes) {
            List<GaiaPrimitive> primitives = mesh.getPrimitives();
            for (GaiaPrimitive primitive : primitives) {
                List<GaiaVertex> vertices = primitive.getVertices();
                List<GaiaSurface> surfaces = primitive.getSurfaces();
                for (GaiaSurface surface : surfaces) {
                    List<GaiaFace> faces = surface.getFaces();

                    GaiaFace previousFace = null;
                    for (GaiaFace face : faces) {
                        int[] indices = face.getIndices();
                        byte[] vertexColor = null;
                        if (indices == null || indices.length == 0) {
                            vertexColor = new byte[]{(byte) (Math.random() * 255), (byte) (Math.random() * 255), (byte) (Math.random() * 255), (byte) 255};
                        } else {
                            GaiaVertex vertex = vertices.get(indices[0]);
                            vertexColor = vertex.getColor();
                        }

                        // setBatchId for the face
                        for (int index : indices) {
                            if (index < 0 || index >= vertices.size()) {
                                log.warn("Index {} is out of bounds for vertices size {}", index, vertices.size());
                                continue;
                            }
                            GaiaVertex vertex = vertices.get(index);
                            //vertex.setBatchId(faceId);
                        }

                        int id;
                        if (previousFace == null) {
                            id = faceId++;
                        } else {
                            int[] previousIndices = previousFace.getIndices();
                            boolean isSameFirstIndex = previousIndices[0] == indices[0];
                            boolean isSamePrevLastIndexAndSecondIndex = previousIndices[previousIndices.length - 1] == indices[1];
                            if (isSameFirstIndex && isSamePrevLastIndexAndSecondIndex) {
                                id = previousFace.getId();
                            } else {
                                id = faceId++;
                            }
                        }

                        byte[] faceColor = new byte[4];
                        faceColor[0] = vertexColor[0];
                        faceColor[1] = vertexColor[1];
                        faceColor[2] = vertexColor[2];
                        faceColor[3] = vertexColor[3];
                        face.setId(id);

                        previousFace = face;
                    }
                }
            }
        }
    }

    private void generateFaceColorAndFaceIdByVertexColorByNode(GaiaNode node) {
        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                generateFaceColorAndFaceIdByVertexColorByNode(child);
            }
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null || meshes.isEmpty()) {
            return;
        }

        for (GaiaMesh mesh : meshes) {
            List<GaiaPrimitive> primitives = mesh.getPrimitives();
            for (GaiaPrimitive primitive : primitives) {
                List<GaiaVertex> vertices = primitive.getVertices();
                List<GaiaSurface> surfaces = primitive.getSurfaces();
                for (GaiaSurface surface : surfaces) {
                    List<GaiaFace> faces = surface.getFaces();

                    GaiaFace previousFace = null;
                    for (GaiaFace face : faces) {
                        int[] indices = face.getIndices();
                        byte[] vertexColor = null;
                        if (indices == null || indices.length == 0) {
                            vertexColor = new byte[]{(byte) (Math.random() * 255), (byte) (Math.random() * 255), (byte) (Math.random() * 255), (byte) 255};
                        } else {
                            GaiaVertex vertex = vertices.get(indices[0]);
                            vertexColor = vertex.getColor();
                        }

                        // setBatchId for the face
                        for (int index : indices) {
                            if (index < 0 || index >= vertices.size()) {
                                log.warn("Index {} is out of bounds for vertices size {}", index, vertices.size());
                                continue;
                            }
                            GaiaVertex vertex = vertices.get(index);
                        }

                        int id;
                        if (previousFace == null) {
                            id = faceIndex++;
                        } else {
                            int[] previousIndices = previousFace.getIndices();
                            boolean isSameFirstIndex = previousIndices[0] == indices[0];
                            boolean isSamePrevLastIndexAndSecondIndex = previousIndices[previousIndices.length - 1] == indices[1];
                            if (isSameFirstIndex && isSamePrevLastIndexAndSecondIndex) {
                                id = previousFace.getId();
                            } else {
                                id = faceIndex++;
                            }
                        }

                        face.setId(id);

                        previousFace = face;
                    }
                }
            }
        }
    }

    public enum ColorizeType {
        FACE, SURFACE, NODE, VERTEX, PRIMITIVE, MESH
    }
}
