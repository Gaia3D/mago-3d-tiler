package com.gaia3d.engine.graph;

//import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.engine.scene.Entity;
import com.gaia3d.engine.scene.Scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class GaiaSceneRender {
    private ShaderProgram shaderProgram;
    private UniformsMap uniformsMap;

    public GaiaSceneRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("renderer/src/main/resources/shaders/scene.vert", GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("renderer/src/main/resources/shaders/scene.frag", GL_FRAGMENT_SHADER));
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

    public void render(GaiaScene gaiaScene) {
        shaderProgram.bind();

//        uniformsMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
//        uniformsMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
//        uniformsMap.setUniform("txtSampler", 0);
//
//        Collection<Model> models = scene.getModelMap().values();
//        TextureCache textureCache = scene.getTextureCache();
//        for (Model model : models) {
//            List<Entity> entities = model.getEntitiesList();
//
//            for (Material material : model.getMaterialList()) {
//                Texture texture = textureCache.getTexture(material.getTexturePath());
//                glActiveTexture(GL_TEXTURE0);
//                texture.bind();
//
//                for (Mesh mesh : material.getMeshList()) {
//                    glBindVertexArray(mesh.getVaoId());
//                    for (Entity entity : entities) {
//                        uniformsMap.setUniform("modelMatrix", entity.getModelMatrix());
//                        glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
//                    }
//                }
//            }
//        }

        glBindVertexArray(0);

        shaderProgram.unbind();
    }
}
