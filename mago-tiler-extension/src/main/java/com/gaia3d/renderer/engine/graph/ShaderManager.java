package com.gaia3d.renderer.engine.graph;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class ShaderManager {
    final Map<String, ShaderProgram> mapNameShaderProgram = new HashMap<>();

    public ShaderProgram createShaderProgram(String shaderProgramName, List<ShaderProgram.ShaderModuleData> shaderModuleDataList) {
        ShaderProgram shaderProgram = new ShaderProgram(shaderModuleDataList);
        mapNameShaderProgram.put(shaderProgramName, shaderProgram);
        return shaderProgram;
    }

    public ShaderProgram getShaderProgram(String shaderProgramName) {
        // 1rst, check if exist the shader program
        if (!mapNameShaderProgram.containsKey(shaderProgramName)) {
            // if not exist, create it.
            if (shaderProgramName.equals("sceneDelimited_v2")) {
                createSceneDelimitedV2Shader();
            } else {
                log.error("[ERROR] Shader program with name {} does not exist!", shaderProgramName);
                return null;
            }
        }
        return mapNameShaderProgram.get(shaderProgramName);
    }

    public void deleteShaderProgram(String shaderProgramName) {
        ShaderProgram shaderProgram = mapNameShaderProgram.get(shaderProgramName);
        if (shaderProgram != null) {
            shaderProgram.cleanup();
            mapNameShaderProgram.remove(shaderProgramName);
        }
    }

    public void deleteAllShaderPrograms() {
        for (ShaderProgram shaderProgram : mapNameShaderProgram.values()) {
            shaderProgram.cleanup();
        }
        mapNameShaderProgram.clear();
    }

    private String readResource(String resourceLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resourceLocation);
        if (resourceAsStream == null) {
            log.error("[ERROR] Resource not found: {}", resourceLocation);
            return "";
        }
        byte[] bytes = null;
        try {
            bytes = resourceAsStream.readAllBytes();
        } catch (IOException e) {
            log.error("[ERROR] Error reading resource: {}", e);
        }
        if (bytes == null) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void setActiveUniformsAndValidate(ShaderProgram shaderProgram) {
        int programId = shaderProgram.getProgramId();
        int uniformCount = GL20.glGetProgrami(programId, GL20.GL_ACTIVE_UNIFORMS);
        java.util.List<String> uniformNames = new ArrayList<>();
        for (int i = 0; i < uniformCount; i++) {
            IntBuffer size = BufferUtils.createIntBuffer(1);
            IntBuffer type = BufferUtils.createIntBuffer(1);
            String name = GL20.glGetActiveUniform(programId, i, size, type);
            uniformNames.add(name);
        }

        shaderProgram.createUniforms(uniformNames);
        shaderProgram.validate();
    }

    private void createSceneDelimitedV2Shader() {
        // create a delimitedScene shader program with normal textures included
        String vertexShaderText = readResource("shaders/sceneDelimitedV330_normalIncluded.vert");
        String fragmentShaderText = readResource("shaders/sceneDelimitedV330_normalIncluded.frag");
        java.util.List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        ShaderProgram shaderProgram = this.createShaderProgram("sceneDelimited_v2", shaderModuleDataList);
        setActiveUniformsAndValidate(shaderProgram);

        // albedo → texture0
        shaderProgram.bind();
        int programId = shaderProgram.getProgramId();
        int locTex0 = GL20.glGetUniformLocation(programId, "albedoTexture");
        GL20.glUniform1i(locTex0, 0); // GL_TEXTURE0

        // normal → texture1
        int locTex1 = GL20.glGetUniformLocation(programId, "normalTexture");
        GL20.glUniform1i(locTex1, 1); // GL_TEXTURE1
        shaderProgram.unbind();
    }
}
