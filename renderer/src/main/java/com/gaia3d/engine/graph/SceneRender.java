package com.gaia3d.engine.graph;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.engine.Window;

import com.gaia3d.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.engine.scene.Entity;
import com.gaia3d.engine.scene.Scene;
import com.gaia3d.renderable.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class SceneRender {
    private ShaderProgram shaderProgram;
    private UniformsMap uniformsMap;

    private int testVboId = -1;

    public SceneRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        String vertexShaderPath = "D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/scene.vert";
        String fragmentShaderPath = "D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/scene.frag";

        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderPath, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderPath, GL20.GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);
        createUniforms();
    }

    private void createUniforms() {
        uniformsMap = new UniformsMap(shaderProgram.getProgramId());
        //uniformsMap.createUniform("projectionMatrix");
        //uniformsMap.createUniform("modelMatrix");
        //uniformsMap.createUniform("txtSampler");
        //uniformsMap.createUniform("viewMatrix");
    }

    public void cleanup() {
        shaderProgram.cleanup();
    }

//    private void renderGaiaScene(RenderableGaiaScene renderableGaiaScene) {
//        try (MemoryStack stack = MemoryStack.stackPush()) {
//            List<RenderableNode> renderableNodes = renderableGaiaScene.getRenderableNodess();
//            for (RenderableNode renderableNode : renderableNodes) {
//                renderGaiaNode(renderableNode);
//            }
//        }
//    }

//    private void renderGaiaNode(RenderableNode renderableNode) {
//        List<RenderableMesh> renderableMeshes = renderableNode.getRenderableMeshes();
//        for (RenderableMesh renderableMesh : renderableMeshes) {
//            List<RenderablePrimitive> renderablePrimitives = renderableMesh.getRenderablePrimitives();
//            for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
//                int vertexCount = 0;
//                Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer = renderablePrimitive.getMapAttribTypeRenderableBuffer();
//                for (Map.Entry<AttributeType, RenderableBuffer> entry : mapAttribTypeRenderableBuffer.entrySet()) {
//                    AttributeType attributeType = entry.getKey();
//                    RenderableBuffer renderableBuffer = entry.getValue();
//                    bindBuffer(renderableBuffer);
//
//                    if (attributeType == AttributeType.POSITION) {
//                        vertexCount = renderableBuffer.getElementsCount();
//                    }
//                }
//                try (MemoryStack stack = MemoryStack.stackPush()) {
//
//                    RenderableBuffer renderableBuffer = mapAttribTypeRenderableBuffer.get(AttributeType.INDICE);
//                    GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, renderableBuffer.getVboId());
//
//                    //uniformsMap.setUniform("modelMatrix", entity.getModelMatrix());
//                    GL20.glPointSize(10.0f);
//                    GL20.glDrawElements(GL20.GL_POINTS, 1, GL20.GL_UNSIGNED_INT, 0);
//                    //glDrawArrays(GL_TRIANGLES, 0, 3);
//                }
//            }
//        }
//
//        // check for children
//        List<RenderableNode> children = renderableNode.getChildren();
//        for (RenderableNode child : children) {
//            renderGaiaNode(child);
//        }
//
//    }
//
//    private void bindBuffer(RenderableBuffer renderableBuffer) {
//        try (MemoryStack stack = MemoryStack.stackPush()) {
//            AttributeType attributeType = renderableBuffer.getAttributeType();
//            int vboId = renderableBuffer.getVboId();
//            int elementsCount = renderableBuffer.getElementsCount();
//            byte glDimension = renderableBuffer.getGlDimension();
//            int glType = renderableBuffer.getGlType();
//
//            if (attributeType == AttributeType.POSITION) {
//                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
//                GL20.glEnableVertexAttribArray(0);
//                GL20.glVertexAttribPointer(0, glDimension, glType, false, 0, 0);
//            }
//
//        }
//    }

//    public void renderGaiaScenes(GaiaScenesContainer renderableGaiaScenesContainer)
//    {
//        // http://forum.lwjgl.org/index.php?topic=4483.0
//        try (MemoryStack stack = MemoryStack.stackPush()) {
//            shaderProgram.bind();
//            shaderProgram.validate();
//
//            int linkStatus = GL20.glGetProgrami(shaderProgram.getProgramId(), GL20.GL_LINK_STATUS);
//            if (linkStatus == GL_FALSE) {
//                System.err.println("Program failed to link");
//            }
//
//            Matrix4f projectionMatrix = renderableGaiaScenesContainer.getProjection().getProjMatrix();
//            Matrix4f viewMatrix = renderableGaiaScenesContainer.getCamera().getViewMatrix();
//            Matrix4f modelMatrix = new Matrix4f();
//            modelMatrix.identity();
////            uniformsMap.setUniform("projectionMatrix", projectionMatrix);
////            uniformsMap.setUniform("viewMatrix", viewMatrix);
////            uniformsMap.setUniform("modelMatrix", modelMatrix);
//            //uniformsMap.setUniform("txtSampler", 0);
//
//            // test render.***
//            if(testVboId == -1) {
//                float[] positions = new float[]{
//                        // V0
//                        -0.5f, 0.5f, 0.5f,
//                        // V1
//                        -0.5f, -0.5f, 0.5f,
//                        // V2
//                        0.5f, -0.5f, 0.5f,
//                        // V3
//                        0.5f, 0.5f, 0.5f,
//                        // V4
//                        -0.5f, 0.5f, -0.5f,
//                        // V5
//                        0.5f, 0.5f, -0.5f,
//                        // V6
//                        -0.5f, -0.5f, -0.5f,
//                        // V7
//                        0.5f, -0.5f, -0.5f,
//
//                        // For text coords in top face
//                        // V8: V4 repeated
//                        -0.5f, 0.5f, -0.5f,
//                        // V9: V5 repeated
//                        0.5f, 0.5f, -0.5f,
//                        // V10: V0 repeated
//                        -0.5f, 0.5f, 0.5f,
//                        // V11: V3 repeated
//                        0.5f, 0.5f, 0.5f,
//
//                        // For text coords in right face
//                        // V12: V3 repeated
//                        0.5f, 0.5f, 0.5f,
//                        // V13: V2 repeated
//                        0.5f, -0.5f, 0.5f,
//
//                        // For text coords in left face
//                        // V14: V0 repeated
//                        -0.5f, 0.5f, 0.5f,
//                        // V15: V1 repeated
//                        -0.5f, -0.5f, 0.5f,
//
//                        // For text coords in bottom face
//                        // V16: V6 repeated
//                        -0.5f, -0.5f, -0.5f,
//                        // V17: V7 repeated
//                        0.5f, -0.5f, -0.5f,
//                        // V18: V1 repeated
//                        -0.5f, -0.5f, 0.5f,
//                        // V19: V2 repeated
//                        0.5f, -0.5f, 0.5f,
//                };
//
//                testVboId = glGenBuffers();
//                FloatBuffer positionsBuffer = stack.callocFloat(positions.length);
//                positionsBuffer.put(0, positions);
//                glBindBuffer(GL_ARRAY_BUFFER, testVboId);
//                glBufferData(GL_ARRAY_BUFFER, positions, GL_STATIC_DRAW);
//                glEnableVertexAttribArray(0);
//                glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
//            }
//
//            glBindBuffer(GL_ARRAY_BUFFER, testVboId);
//            glEnableVertexAttribArray(0);
//            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
//
//            GL20.glPointSize(5.0f);
//            //GL20.glDrawArrays(GL20.GL_POINTS, 0, 3);
//            GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, 12);
//
////            GL20.glDisableVertexAttribArray(1);
////
////            Collection<RenderableGaiaScene> renderableGaiaScenes = renderableGaiaScenesContainer.getRenderableGaiaScenes();
////            TextureCache textureCache = renderableGaiaScenesContainer.getTextureCache();
////            for (RenderableGaiaScene renderableGaiaScene : renderableGaiaScenes) {
////                renderGaiaScene(renderableGaiaScene);
////            }
//
//            shaderProgram.unbind();
//        }
//    }

    public void render(Scene scene) {
//        shaderProgram.bind();
//
//        uniformsMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
//        uniformsMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
//        //uniformsMap.setUniform("txtSampler", 0);
//
//        Collection<Model> models = scene.getModelMap().values();
//        TextureCache textureCache = scene.getTextureCache();
//        for (Model model : models) {
//            List<Entity> entities = model.getEntitiesList();
//
//            for (Material material : model.getMaterialList()) {
//                Texture texture = textureCache.getTexture(material.getTexturePath());
//                GL20.glActiveTexture(GL20.GL_TEXTURE0);
//                texture.bind();
//
//                for (Mesh mesh : material.getMeshList()) {
//                    //GL20.glBindVertexArray(mesh.getVaoId());
//                    for (Entity entity : entities) {
//                        uniformsMap.setUniform("modelMatrix", entity.getModelMatrix());
//                        GL20.glDrawElements(GL20.GL_TRIANGLES, mesh.getNumVertices(), GL20.GL_UNSIGNED_INT, 0);
//                    }
//                }
//            }
//        }
//
//        //glBindVertexArray(0);
//
//        shaderProgram.unbind();
    }
}
