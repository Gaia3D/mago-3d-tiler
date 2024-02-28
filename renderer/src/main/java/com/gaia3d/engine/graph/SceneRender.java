package com.gaia3d.engine.graph;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.engine.Window;

import com.gaia3d.engine.scene.Entity;
import com.gaia3d.engine.scene.Scene;

import java.util.*;

import static org.lwjgl.opengl.GL30.*;
public class SceneRender {
    private ShaderProgram shaderProgram;
    private UniformsMap uniformsMap;

    public SceneRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        String vertexShaderPath = "D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/scene.vert";
        String fragmentShaderPath = "D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/scene.frag";

        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderPath, GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderPath, GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);
        createUniforms();
    }

    private void createUniforms() {
        uniformsMap = new UniformsMap(shaderProgram.getProgramId());
        uniformsMap.createUniform("projectionMatrix");
        uniformsMap.createUniform("modelMatrix");
        uniformsMap.createUniform("txtSampler");
        uniformsMap.createUniform("viewMatrix");
    }

    public void cleanup() {
        shaderProgram.cleanup();
    }

    public void renderGaiaScene(GaiaScene gaiaScene)
    {

    }

    public void render(Scene scene) {
        shaderProgram.bind();

        uniformsMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        uniformsMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
        uniformsMap.setUniform("txtSampler", 0);

        Collection<Model> models = scene.getModelMap().values();
        TextureCache textureCache = scene.getTextureCache();
        for (Model model : models) {
            List<Entity> entities = model.getEntitiesList();

            for (Material material : model.getMaterialList()) {
                Texture texture = textureCache.getTexture(material.getTexturePath());
                glActiveTexture(GL_TEXTURE0);
                texture.bind();

                for (Mesh mesh : material.getMeshList()) {
                    glBindVertexArray(mesh.getVaoId());
                    for (Entity entity : entities) {
                        uniformsMap.setUniform("modelMatrix", entity.getModelMatrix());
                        glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
                    }
                }
            }
        }

        glBindVertexArray(0);

        shaderProgram.unbind();
    }
}
