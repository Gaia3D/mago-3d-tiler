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
//        if(childrenCount > 30)
//        { childrenCount = 30;} // test.***
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
        RenderableBuffer renderableBuffer;

        AttributeType attributeType = buffer.getAttributeType();
        int glType = buffer.getGlType();
        byte glDimension = buffer.getGlDimension();
        int elemsCount = buffer.getElementsCount();
        int glTarget = buffer.getGlTarget();

        renderableBuffer = new RenderableBuffer(attributeType, elemsCount, glDimension, glType, glTarget);


        if (attributeType == AttributeType.POSITION) {
            float[] positions = buffer.getFloats();
            // Positions VBO
            int[] vboId = new int[1];
            GL20.glGenBuffers(vboId);
            // test scale positions.
            for (int i = 0; i < positions.length; i++) {
                positions[i] *= 0.01;
            }
            //FloatBuffer positionsBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(positions.length);
            //FloatBuffer positionsBuffer = stack.callocFloat(positionsTEST.length);

            //positionsBuffer.put(0, positionsTEST);
            //FloatBuffer positionsBuffer = FloatBuffer.wrap(positions);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
            GL20.glBufferData(GL20.GL_ARRAY_BUFFER, positions, GL20.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, glDimension, glType, false, 0, 0);

            renderableBuffer.setVboId(vboId[0]);

        } else if (attributeType == AttributeType.NORMAL) {

            // Normal VBO
            int[] vboId = new int[1];
            GL20.glGenBuffers(vboId);

            if (glType == GL20.GL_FLOAT) {
                float[] normals = buffer.getFloats();
                //FloatBuffer normalsBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(normals.length);
                //FloatBuffer normalsBuffer = stack.callocFloat(normals.length);
                //normalsBuffer.put(0, normals);
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ARRAY_BUFFER, normals, GL20.GL_STATIC_DRAW);
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, glDimension, glType, false, 0, 0);

            } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                // TODO :
            } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                byte[] normals = buffer.getBytes();
                //ByteBuffer normalsBuffer = org.lwjgl.system.MemoryUtil.memAlloc(normals.length);
                //ByteBuffer normalsBuffer = stack.calloc(normals.length);
                //normalsBuffer.put(0, normals);
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ARRAY_BUFFER, ByteBuffer.wrap(normals), GL20.GL_STATIC_DRAW);
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, glDimension, glType, true, 0, 0);
            }

            renderableBuffer.setVboId(vboId[0]);

        } else if (attributeType == AttributeType.TEXCOORD) {
            // Texture coordinates VBO
            int[] vboId = new int[1];
            GL20.glGenBuffers(vboId);

            if (glType == GL20.GL_FLOAT) {
                float[] texcoords = buffer.getFloats();
                //FloatBuffer texcoordsBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(texcoords.length);
                //FloatBuffer texcoordsBuffer = stack.callocFloat(texcoords.length);
                //texcoordsBuffer.put(0, texcoords);
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ARRAY_BUFFER, texcoords, GL20.GL_STATIC_DRAW);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, glDimension, glType, false, 0, 0);

            } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                // TODO :
            } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                byte[] texcoords = buffer.getBytes();
                //ByteBuffer texcoordsBuffer = org.lwjgl.system.MemoryUtil.memAlloc(texcoords.length);
                //ByteBuffer texcoordsBuffer = stack.calloc(texcoords.length);
                //texcoordsBuffer.put(0, texcoords);
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ARRAY_BUFFER, ByteBuffer.wrap(texcoords), GL20.GL_STATIC_DRAW);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, glDimension, glType, true, 0, 0);
            }

            renderableBuffer.setVboId(vboId[0]);

        } else if (attributeType == AttributeType.COLOR) {
            // Color VBO
            int[] vboId = new int[1];
            GL20.glGenBuffers(vboId);

            if (glType == GL20.GL_FLOAT) {
                float[] colors = buffer.getFloats();
                //FloatBuffer colorsBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(colors.length);
                //FloatBuffer colorsBuffer = stack.callocFloat(colors.length);
                //colorsBuffer.put(0, colors);
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ARRAY_BUFFER, colors, GL20.GL_STATIC_DRAW);
                GL20.glEnableVertexAttribArray(3);
                GL20.glVertexAttribPointer(3, glDimension, glType, false, 0, 0);

            } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                // TODO :
            } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                byte[] colors = buffer.getBytes();
                //ByteBuffer colorsBuffer = org.lwjgl.system.MemoryUtil.memAlloc(colors.length);
                //ByteBuffer colorsBuffer = stack.calloc(colors.length);
                //colorsBuffer.put(0, colors);
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ARRAY_BUFFER, ByteBuffer.wrap(colors), GL20.GL_STATIC_DRAW);
                GL20.glEnableVertexAttribArray(3);
                GL20.glVertexAttribPointer(3, glDimension, glType, true, 0, 0);
            }

            renderableBuffer.setVboId(vboId[0]);

        } else if (attributeType == AttributeType.INDICE) {
            // Index VBO
            int[] vboId = new int[1];
            GL20.glGenBuffers(vboId);

            if (glType == GL20.GL_INT || glType == GL20.GL_UNSIGNED_INT) {
                int[] indices = buffer.getInts();
                //IntBuffer indicesBuffer = org.lwjgl.system.MemoryUtil.memAllocInt(indices.length);
                //IntBuffer indicesBuffer = stack.callocInt(indicesTEST.length);
                //indicesBuffer.put(0, indicesTEST).flip();
                //IntBuffer indicesBuffer = IntBuffer.wrap(indices);
                GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);

            } else if (glType == GL20.GL_SHORT || glType == GL20.GL_UNSIGNED_SHORT) {
                short[] indices = buffer.getShorts();
                //ShortBuffer indicesBuffer = org.lwjgl.system.MemoryUtil.memAllocShort(indices.length);
                //ShortBuffer indicesBuffer = stack.callocShort(indices.length);
                //indicesBuffer.put(0, indices);
                GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);

            } else if (glType == GL20.GL_BYTE || glType == GL20.GL_UNSIGNED_BYTE) {
                byte[] indices = buffer.getBytes();
                //ByteBuffer indicesBuffer = org.lwjgl.system.MemoryUtil.memAlloc(indices.length);
                //ByteBuffer indicesBuffer = stack.calloc(indices.length);
                //indicesBuffer.put(0, indices);
                GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId[0]);
                GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, ByteBuffer.wrap(indices), GL20.GL_STATIC_DRAW);
            }

            renderableBuffer.setVboId(vboId[0]);
        }

        return renderableBuffer;

    }
}
