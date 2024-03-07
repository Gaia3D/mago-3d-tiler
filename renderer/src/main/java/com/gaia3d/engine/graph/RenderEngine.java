package com.gaia3d.engine.graph;

import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.engine.scene.Camera;
import com.gaia3d.renderable.*;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import java.util.List;
import java.util.Map;

public class RenderEngine {

    public RenderEngine() {
    }

    public void render(GaiaScenesContainer gaiaScenesContainer, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        Matrix4f projectionMatrix = gaiaScenesContainer.getProjection().getProjMatrix();

        uniformsMap.setUniformMatrix4fv("uProjectionMatrix", projectionMatrix);
        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        int scenesCount = gaiaScenesContainer.getRenderableGaiaScenes().size();
        for (int i = 0; i < scenesCount; i++) {
            RenderableGaiaScene renderableGaiaScene = gaiaScenesContainer.getRenderableGaiaScenes().get(i);
            renderGaiaScene(renderableGaiaScene, shaderProgram);
        }
    }

    private void renderGaiaScene(RenderableGaiaScene renderableGaiaScene, ShaderProgram shaderProgram) {
        List<RenderableNode> renderableNodes = renderableGaiaScene.getRenderableNodess();
            for (RenderableNode renderableNode : renderableNodes) {
                renderGaiaNode(renderableNode, shaderProgram);
            }
    }

        private void renderGaiaNode(RenderableNode renderableNode, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        Matrix4f objectMatrixTemp = new Matrix4f();
            objectMatrixTemp.identity();
        List<RenderableMesh> renderableMeshes = renderableNode.getRenderableMeshes();
        for (RenderableMesh renderableMesh : renderableMeshes) {
            List<RenderablePrimitive> renderablePrimitives = renderableMesh.getRenderablePrimitives();
            for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
                int vertexCount = 0;
                Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer = renderablePrimitive.getMapAttribTypeRenderableBuffer();
                for (Map.Entry<AttributeType, RenderableBuffer> entry : mapAttribTypeRenderableBuffer.entrySet()) {
                    AttributeType attributeType = entry.getKey();
                    RenderableBuffer renderableBuffer = entry.getValue();
                    bindBuffer(renderableBuffer);

                    if (attributeType == AttributeType.POSITION) {
                        vertexCount = renderableBuffer.getElementsCount();
                    }
                }


                RenderableBuffer renderableBuffer = mapAttribTypeRenderableBuffer.get(AttributeType.INDICE);
                int elemsCount = renderableBuffer.getElementsCount();
                GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, renderableBuffer.getVboId());

                uniformsMap.setUniformMatrix4fv("uObjectMatrix", objectMatrixTemp);
                GL20.glPointSize(10.0f);
                GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, GL20.GL_UNSIGNED_INT, 0);
                //glDrawArrays(GL_TRIANGLES, 0, 3);

            }
        }

        // check for children
        List<RenderableNode> children = renderableNode.getChildren();
        for (RenderableNode child : children) {
            renderGaiaNode(child, shaderProgram);
        }

    }

    private void bindBuffer(RenderableBuffer renderableBuffer) {

        AttributeType attributeType = renderableBuffer.getAttributeType();
        int vboId = renderableBuffer.getVboId();
        int elementsCount = renderableBuffer.getElementsCount();
        byte glDimension = renderableBuffer.getGlDimension();
        int glType = renderableBuffer.getGlType();

        if (attributeType == AttributeType.POSITION) {
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, glDimension, glType, false, 0, 0);
        } else if (attributeType == AttributeType.NORMAL) {
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, glDimension, glType, false, 0, 0);
        } else if (attributeType == AttributeType.COLOR) {
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, glDimension, glType, false, 0, 0);
        } else if (attributeType == AttributeType.INDICE) {
            GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId);
        }
    }
}
