package com.gaia3d.renderer.engine.graph;

import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.RenderableTexturesUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

@Getter
@Setter
@Slf4j
public class HalfEdgeRenderer {
    private boolean renderWireFrame = false;
    private int colorMode = 2; // 0 = oneColor, 1 = vertexColor, 2 = textureColor

    public void renderHalfEdgeScenes(List<HalfEdgeScene> halfEdgeScenes, ShaderProgram shaderProgram) {
        for (HalfEdgeScene halfEdgeScene : halfEdgeScenes) {
            renderHalfEdgeScene(halfEdgeScene, shaderProgram);
        }
    }

    public void renderHalfEdgeScene(HalfEdgeScene halfEdgeScene, ShaderProgram shaderProgram) {
        List<HalfEdgeNode> halfEdgeNodes = halfEdgeScene.getNodes();
        for (HalfEdgeNode halfEdgeNode : halfEdgeNodes) {
            renderHalfEdgeNode(halfEdgeScene, halfEdgeNode, shaderProgram);
        }
    }

    public void renderHalfEdgeNode(HalfEdgeScene halfEdgeScene, HalfEdgeNode halfEdgeNode, ShaderProgram shaderProgram) {
        List<HalfEdgeMesh> halfEdgeMeshes = halfEdgeNode.getMeshes();
        for (HalfEdgeMesh halfEdgeMesh : halfEdgeMeshes) {
            renderHalfEdgeMesh(halfEdgeScene, halfEdgeMesh, shaderProgram);
        }

        // check if there are children nodes
        List<HalfEdgeNode> childrenNodes = halfEdgeNode.getChildren();
        for (HalfEdgeNode childrenNode : childrenNodes) {
            renderHalfEdgeNode(halfEdgeScene, childrenNode, shaderProgram);
        }
    }

    public void renderHalfEdgeMesh(HalfEdgeScene halfEdgeScene, HalfEdgeMesh halfEdgeMesh, ShaderProgram shaderProgram) {
        List<HalfEdgePrimitive> halfEdgePrimitives = halfEdgeMesh.getPrimitives();
        for (HalfEdgePrimitive halfEdgePrimitive : halfEdgePrimitives) {
            renderHalfEdgePrimitive(halfEdgeScene, halfEdgePrimitive, shaderProgram);
        }
    }

    public void renderHalfEdgePrimitive(HalfEdgeScene halfEdgeScene, HalfEdgePrimitive halfEdgePrimitive, ShaderProgram shaderProgram) {
        // Check material textures
        boolean textureBinded = false;
        int materialId = halfEdgePrimitive.getMaterialIndex();
        if (materialId >= 0) {
            GaiaMaterial material = halfEdgeScene.getMaterials().get(materialId);

            if (material == null) {
                material = new GaiaMaterial();
                material.setDiffuseColor(new Vector4d(0.2, 0.95, 0.2, 1.0));
            }

            // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
            UniformsMap uniformsMap = shaderProgram.getUniformsMap();
            uniformsMap.setUniform1i("uColorMode", 0);
            Map<TextureType, List<GaiaTexture>> mapTextures = material.getTextures();
            if (mapTextures.containsKey(TextureType.DIFFUSE)) {
                List<GaiaTexture> diffuseTextures = mapTextures.get(TextureType.DIFFUSE);
                if (!diffuseTextures.isEmpty()) {
                    GaiaTexture diffuseTexture = diffuseTextures.get(0);
                    if (diffuseTexture.getTextureId() < 0) {
                        int minFilter = GL_LINEAR; // GL_LINEAR, GL_NEAREST
                        int magFilter = GL_LINEAR;
                        int wrapS = GL_REPEAT; // GL_CLAMP_TO_EDGE
                        int wrapT = GL_REPEAT;
                        BufferedImage bufferedImage = diffuseTexture.getBufferedImage();
                        int textureId = RenderableTexturesUtils.createGlTextureFromBufferedImage(bufferedImage, minFilter, magFilter, wrapS, wrapT, true);

                        diffuseTexture.setTextureId(textureId);
                        diffuseTexture.setBufferedImage(null);
                    }

                    // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
                    uniformsMap.setUniform1i("uColorMode", 2);
                    GL20.glEnable(GL20.GL_TEXTURE_2D);
                    GL20.glActiveTexture(GL20.GL_TEXTURE0);
                    GL20.glBindTexture(GL20.GL_TEXTURE_2D, diffuseTexture.getTextureId());
                    textureBinded = true;
                }
            }
            // End check material textures.---------------------------------------------------------------------------------------
        }

        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            renderHalfEdgeSurface(halfEdgeSurface, shaderProgram, textureBinded);
        }
    }

    public void renderHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface, ShaderProgram shaderProgram, boolean textureBinded) {
        Map<AttributeType, HalfEdgeRenderableBuffer> mapAttribTypeRenderableBuffer = halfEdgeSurface.getMapAttribTypeRenderableBuffer();
        if (mapAttribTypeRenderableBuffer == null || mapAttribTypeRenderableBuffer.isEmpty() || halfEdgeSurface.getDirty()) {
            makeHalfEdgeSurfaceAttributes(halfEdgeSurface);
        }

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL20.glDisableVertexAttribArray(3);

        mapAttribTypeRenderableBuffer = halfEdgeSurface.getMapAttribTypeRenderableBuffer();
        for (Map.Entry<AttributeType, HalfEdgeRenderableBuffer> entry : mapAttribTypeRenderableBuffer.entrySet()) {
            AttributeType attributeType = entry.getKey();
            HalfEdgeRenderableBuffer renderableBuffer = entry.getValue();
            bindBuffer(renderableBuffer, shaderProgram);
        }

        UniformsMap uniformsMap = shaderProgram.getUniformsMap();


        if (this.colorMode == 0) {
            uniformsMap.setUniform1i("uColorMode", 0);
            uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.9f, 0.3f, 0.6f, 1.0f));
        } else if (this.colorMode == 1) {
            uniformsMap.setUniform1i("uColorMode", 1);
        } else if (this.colorMode == 2) {
            uniformsMap.setUniform1i("uColorMode", 2);
        }

        if (!textureBinded) {
            HalfEdgeRenderableBuffer renderableBufferColor = mapAttribTypeRenderableBuffer.get(AttributeType.COLOR);
            if (renderableBufferColor == null) {
                uniformsMap.setUniform1i("uColorMode", 0);
                uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.9f, 0.3f, 0.6f, 1.0f));
            } else {
                uniformsMap.setUniform1i("uColorMode", 1);
            }
        }

        HalfEdgeRenderableBuffer renderableBuffer = mapAttribTypeRenderableBuffer.get(AttributeType.INDICE);
        if (renderableBuffer == null) {
            // use glDrawArrays.
            GL20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
            GL20.glPolygonOffset(1.0f, 1.0f);
//            uniformsMap.setUniform1i("uColorMode", 0);
//            uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
            GL20.glDrawArrays(GL_LINE_STRIP, 0, 16);
            return;
        }
        int elemsCount = renderableBuffer.getElementsCount();
        int type = renderableBuffer.getGlType();

        GL20.glPointSize(10.0f);
        GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, type, 0);

        // render wireframe
        if (renderWireFrame) {
            GL20.glLineWidth(1.0f);

            uniformsMap.setUniform1i("uColorMode", 0);
            uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
            GL20.glPolygonMode(GL20.GL_FRONT_AND_BACK, GL20.GL_LINE);
            // do offset
            GL20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
            GL20.glPolygonOffset(1.0f, 1.0f);
            GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, type, 0);
        }

//            // render the 1rst point of the primitive
//            GL20.glPointSize(10.0f);
//            GL20.glDrawArrays(GL20.GL_POINTS, 0, 1);


        // return polygonMode to fill
        GL20.glPolygonMode(GL20.GL_FRONT_AND_BACK, GL20.GL_FILL);
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0); // unbind texture
    }

    private void makeHalfEdgeSurfaceAttributes(HalfEdgeSurface halfEdgeSurface) {
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
        vertexAllOutingEdgesMap = halfEdgeSurface.getMapVertexAllOutingEdges(vertexAllOutingEdgesMap);

        //Map<Integer, Color> classifyIdColorMap = new HashMap<>();
        Map<Integer, Map<PlaneType, Color>> classifyIdPlaneTypeColorMap = new HashMap<>();

        List<HalfEdgeVertex> vertices = halfEdgeSurface.getVertices();
        int verticesCount = vertices.size();
        float[] positions = new float[verticesCount * 3];
        float[] texCoords = new float[verticesCount * 2];
        byte[] colors = new byte[verticesCount * 4];
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            positions[i * 3] = (float) position.x;
            positions[i * 3 + 1] = (float) position.y;
            positions[i * 3 + 2] = (float) position.z;
            Vector2d texCoord = vertex.getTexcoords();
            texCoords[i * 2] = (float) texCoord.x;
            texCoords[i * 2 + 1] = (float) texCoord.y;

            // color.
            PositionType positionType = PositionType.INTERIOR;
            List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(vertex);
            int outingEdgesCount = outingEdges.size();
            for (int j = 0; j < outingEdgesCount; j++) {
                HalfEdge outingEdge = outingEdges.get(j);
                if (!outingEdge.hasTwin()) {
                    positionType = PositionType.BOUNDARY_EDGE;
                    break;
                }
            }

//            if (positionType == PositionType.INTERIOR)
//            {
//                colors[i * 4] = (byte) (0.9 * 255.0);
//                colors[i * 4 + 1] = (byte) (0.3 * 255.0);
//                colors[i * 4 + 2] = (byte) (0.6 * 255.0);
//                colors[i * 4 + 3] = (byte) 255;
//            }
//            else if (positionType == PositionType.BOUNDARY_EDGE)
//            {
//                colors[i * 4] = (byte) (0.3 * 255.0);
//                colors[i * 4 + 1] = (byte) (0.6 * 255.0);
//                colors[i * 4 + 2] = (byte) (0.9 * 255.0);
//                colors[i * 4 + 3] = (byte) 255;
//            }
            HalfEdge outingEdge = vertex.getOutingHalfEdge();
            int classifyId = outingEdge.getFace().getClassifyId();
            PlaneType planeType = outingEdge.getFace().getBestPlaneToProject();
            Map<PlaneType, Color> planeTypeColorMap = classifyIdPlaneTypeColorMap.computeIfAbsent(classifyId, k -> new HashMap<>());
            Color color = planeTypeColorMap.computeIfAbsent(planeType, k -> new Color(new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)));
            colors[i * 4] = (byte) color.getRed();
            colors[i * 4 + 1] = (byte) color.getGreen();
            colors[i * 4 + 2] = (byte) color.getBlue();
            colors[i * 4 + 3] = (byte) 255;

        }

        // 0.9f, 0.3f, 0.6f

        // make vertex buffer
        Map<AttributeType, HalfEdgeRenderableBuffer> mapAttribTypeRenderableBuffer = halfEdgeSurface.getMapAttribTypeRenderableBuffer();
        if (mapAttribTypeRenderableBuffer == null) {
            mapAttribTypeRenderableBuffer = new HashMap<>();
        } else {
            // delete all buffers
            for (HalfEdgeRenderableBuffer renderableBuffer : mapAttribTypeRenderableBuffer.values()) {
                int vboId = renderableBuffer.getVboId();
                if (vboId != -1) {
                    GL20.glDeleteBuffers(vboId);
                    renderableBuffer.setVboId(-1);
                }
            }
            mapAttribTypeRenderableBuffer.clear();
        }

        // make position buffer
        AttributeType attributeType = AttributeType.POSITION;
        int elementsCount = verticesCount;
        byte glDimension = 3;
        int glType = 5126; // GL_FLOAT
        int glTarget = 34962; // GL_ARRAY_BUFFER
        HalfEdgeRenderableBuffer positionBuffer = new HalfEdgeRenderableBuffer(attributeType, elementsCount, glDimension, glType, glTarget);

        // Positions VBO
        int[] vboId = new int[1];
        GL20.glGenBuffers(vboId);

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, positions, GL20.GL_STATIC_DRAW);
        positionBuffer.setVboId(vboId[0]);

        mapAttribTypeRenderableBuffer.put(attributeType, positionBuffer);

        // make texcoord buffer
        attributeType = AttributeType.TEXCOORD;
        elementsCount = verticesCount;
        glDimension = 2;
        glType = 5126; // GL_FLOAT
        glTarget = 34962; // GL_ARRAY_BUFFER
        HalfEdgeRenderableBuffer texCoordBuffer = new HalfEdgeRenderableBuffer(attributeType, elementsCount, glDimension, glType, glTarget);

        // TexCoords VBO
        vboId = new int[1];
        GL20.glGenBuffers(vboId);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, texCoords, GL20.GL_STATIC_DRAW);
        texCoordBuffer.setVboId(vboId[0]);

        mapAttribTypeRenderableBuffer.put(attributeType, texCoordBuffer);

        // make indice buffer
        int[] indices = halfEdgeSurface.getIndices();
        int indicesCount = indices.length;
        HalfEdgeRenderableBuffer indiceBuffer = new HalfEdgeRenderableBuffer(AttributeType.INDICE, indicesCount, (byte) 1, GL20.GL_UNSIGNED_INT, GL20.GL_ELEMENT_ARRAY_BUFFER);

        // Indices VBO
        vboId = new int[1];
        GL20.glGenBuffers(vboId);
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId[0]);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);
        indiceBuffer.setVboId(vboId[0]);

        mapAttribTypeRenderableBuffer.put(AttributeType.INDICE, indiceBuffer);

        // make color buffer
        attributeType = AttributeType.COLOR;
        elementsCount = verticesCount;
        glDimension = 4;
        glType = 5121; // GL_UNSIGNED_BYTE
        glTarget = 34962; // GL_ARRAY_BUFFER
        HalfEdgeRenderableBuffer colorBuffer = new HalfEdgeRenderableBuffer(attributeType, elementsCount, glDimension, glType, glTarget);

        // Colors VBO
        ByteBuffer colorByteBuffer = ByteBuffer.allocateDirect(colors.length);
        colorByteBuffer.put(colors);
        colorByteBuffer.flip(); // Cambiar a modo lectura

        vboId = new int[1];
        GL20.glGenBuffers(vboId);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, colorByteBuffer, GL20.GL_STATIC_DRAW);
        colorBuffer.setVboId(vboId[0]);

        mapAttribTypeRenderableBuffer.put(attributeType, colorBuffer);

        halfEdgeSurface.setMapAttribTypeRenderableBuffer(mapAttribTypeRenderableBuffer);
        halfEdgeSurface.setDirty(false);
    }

    private void bindBuffer(HalfEdgeRenderableBuffer renderableBuffer, ShaderProgram shaderProgram) {

        AttributeType attributeType = renderableBuffer.getAttributeType();
        int vboId = renderableBuffer.getVboId();
        int elementsCount = renderableBuffer.getElementsCount();
        byte glDimension = renderableBuffer.getGlDimension();
        int glType = renderableBuffer.getGlType();

        if (attributeType == AttributeType.POSITION) {
            int location = shaderProgram.enableAttribLocation("aPosition");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        } else if (attributeType == AttributeType.NORMAL) {
            int location = shaderProgram.enableAttribLocation("aNormal");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        } else if (attributeType == AttributeType.COLOR) {
            int location = shaderProgram.enableAttribLocation("aColor");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, true, 0, 0);
            }
        } else if (attributeType == AttributeType.TEXCOORD) {
            int location = shaderProgram.enableAttribLocation("aTexCoord");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        } else if (attributeType == AttributeType.INDICE) {
            GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId);
        }

    }
}
