package com.gaia3d.engine;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.renderable.*;
import org.joml.Matrix4d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class InternDataConverter {

    public static RenderableGaiaScene getRenderableGaiaScene(GaiaScene gaiaScene) {
        RenderableGaiaScene renderableGaiaScene = new RenderableGaiaScene();

        List<GaiaNode> nodes = gaiaScene.getNodes();
        for (GaiaNode node : nodes) {
            RenderableNode renderableNode = getRenderableNode(node);
            renderableGaiaScene.addRenderableNode(renderableNode);
        }

        return renderableGaiaScene;
    }

    public static RenderableNode getRenderableNode(GaiaNode gaiaNode) {
        RenderableNode renderableNode = new RenderableNode();

        String name = gaiaNode.getName();
        Matrix4d transformMatrix = gaiaNode.getTransformMatrix();
        Matrix4d preMultipliedTransformMatrix = gaiaNode.getPreMultipliedTransformMatrix();
        GaiaBoundingBox gaiaBoundingBox = gaiaNode.getGaiaBoundingBox();

        renderableNode.setName(name);
        renderableNode.setTransformMatrix(transformMatrix);
        renderableNode.setPreMultipliedTransformMatrix(preMultipliedTransformMatrix);
        renderableNode.setGaiaBoundingBox(gaiaBoundingBox);

        List<GaiaMesh> meshes = gaiaNode.getMeshes();
        int meshesCount = meshes.size();
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh gaiaMesh = meshes.get(i);
            RenderableMesh renderableMesh = getRenderableMesh(gaiaMesh);
            renderableNode.addRenderableMesh(renderableMesh);
        }

        // check for children.
        List<GaiaNode> children = gaiaNode.getChildren();
        int childrenCount = children.size();
        for (int i = 0; i < childrenCount; i++) {
            GaiaNode child = children.get(i);
            RenderableNode renderableChildNode = getRenderableNode(child);
            renderableNode.addChild(renderableChildNode);
        }

        return renderableNode;
    }

    public static RenderableMesh getRenderableMesh(GaiaMesh gaiaMesh) {
        RenderableMesh renderableMesh = new RenderableMesh();

        List<GaiaPrimitive> primitives = gaiaMesh.getPrimitives();
        int primitivesCount = primitives.size();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();

        for (int i = 0; i < primitivesCount; i++) {
            GaiaPrimitive gaiaPrimitive = primitives.get(i);
            GaiaBufferDataSet bufferDataSet = gaiaPrimitive.toGaiaBufferSet(transformMatrix);
            RenderablePrimitive renderablePrimitive = getRenderablePrimitive(bufferDataSet);
            renderableMesh.addRenderablePrimitive(renderablePrimitive);
        }

        return renderableMesh;
    }

    public static RenderablePrimitive getRenderablePrimitive(GaiaBufferDataSet bufferDataSet) {
        RenderablePrimitive renderablePrimitive = new RenderablePrimitive();

        Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            buffer.setAttributeType(attributeType); // set the attribute type to the buffer.***
            RenderableBuffer renderableBuffer = getRenderableBuffer(buffer);
            renderablePrimitive.setAttribTypeRenderableBuffer(attributeType, renderableBuffer);
        }

        return renderablePrimitive;

    }

    public static RenderableBuffer getRenderableBuffer(GaiaBuffer buffer) {
        //try (MemoryStack stack = MemoryStack.stackPush()) {

            AttributeType attributeType = buffer.getAttributeType();
            int glType = buffer.getGlType();
            byte glDimension = buffer.getGlDimension();
            int elemsCount = buffer.getElementsCount();
            int glTarget = buffer.getGlTarget();

            RenderableBuffer renderableBuffer = new RenderableBuffer(attributeType, glType, glDimension, elemsCount, glTarget);

            if (attributeType == AttributeType.POSITION) {
                float[] positions = buffer.getFloats();
                // Positions VBO
                int vboId = glGenBuffers();
                FloatBuffer positionsBuffer = BufferUtils.createFloatBuffer(positions.length);
                //FloatBuffer positionsBuffer = stack.callocFloat(positions.length);
                positionsBuffer.put(0, positions);
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(0);
                glVertexAttribPointer(0, glDimension, glType, false, 0, 0);

                renderableBuffer.setVboId(vboId);

            } else if (attributeType == AttributeType.NORMAL) {

                // Normal VBO
                int vboId = glGenBuffers();

                if (glType == GL20.GL_FLOAT) {
                    float[] normals = buffer.getFloats();
                    FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(normals.length);
                    normalsBuffer.put(0, normals);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(1);
                    glVertexAttribPointer(1, glDimension, glType, false, 0, 0);
                } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                    // TODO :
                } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                    byte[] normals = buffer.getBytes();
                    ByteBuffer normalsBuffer = BufferUtils.createByteBuffer(normals.length);
                    normalsBuffer.put(0, normals);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(1);
                    glVertexAttribPointer(1, glDimension, glType, true, 0, 0);
                }

                renderableBuffer.setVboId(vboId);

            } else if (attributeType == AttributeType.TEXCOORD) {
                // Texture coordinates VBO
                int vboId = glGenBuffers();

                if (glType == GL20.GL_FLOAT) {
                    float[] texcoords = buffer.getFloats();
                    FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(texcoords.length);
                    normalsBuffer.put(0, texcoords);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(1);
                    glVertexAttribPointer(1, glDimension, glType, false, 0, 0);
                } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                    // TODO :
                } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                    byte[] texcoords = buffer.getBytes();
                    ByteBuffer normalsBuffer = BufferUtils.createByteBuffer(texcoords.length);
                    normalsBuffer.put(0, texcoords);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(1);
                    glVertexAttribPointer(1, glDimension, glType, true, 0, 0);
                }

                renderableBuffer.setVboId(vboId);

            } else if (attributeType == AttributeType.COLOR) {
                // Color VBO
                int vboId = glGenBuffers();

                if (glType == GL20.GL_FLOAT) {
                    float[] colors = buffer.getFloats();
                    FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(colors.length);
                    normalsBuffer.put(0, colors);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(1);
                    glVertexAttribPointer(1, glDimension, glType, false, 0, 0);
                } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                    // TODO :
                } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                    byte[] colors = buffer.getBytes();
                    ByteBuffer normalsBuffer = BufferUtils.createByteBuffer(colors.length);
                    normalsBuffer.put(0, colors);
                    glBindBuffer(GL_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
                    glEnableVertexAttribArray(1);
                    glVertexAttribPointer(1, glDimension, glType, true, 0, 0);
                }

                renderableBuffer.setVboId(vboId);

            } else if (attributeType == AttributeType.INDICE) {
                // Index VBO
                int vboId = glGenBuffers();

                if (glType == GL20.GL_INT || glType == GL20.GL_UNSIGNED_INT) {
                    int[] indices = buffer.getInts();
                    IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.length);
                    indicesBuffer.put(0, indices);
                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
                } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                    short[] indices = buffer.getShorts();
                    ShortBuffer indicesBuffer = BufferUtils.createShortBuffer(indices.length);
                    indicesBuffer.put(0, indices);
                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
                } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                    byte[] indices = buffer.getBytes();
                    ByteBuffer indicesBuffer = BufferUtils.createByteBuffer(indices.length);
                    indicesBuffer.put(0, indices);
                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
                }

                renderableBuffer.setVboId(vboId);
            }

            return renderableBuffer;
        //}

    }
}
