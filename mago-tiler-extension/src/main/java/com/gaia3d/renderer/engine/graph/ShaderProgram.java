package com.gaia3d.renderer.engine.graph;

import lombok.Getter;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;

@Getter
public class ShaderProgram {
    private final int programId;
    private UniformsMap uniformsMap;

    public ShaderProgram(List<ShaderModuleData> shaderModuleDataList) {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create Shader");
        }

        List<Integer> shaderModules = new ArrayList<>();
        shaderModuleDataList.forEach(s -> shaderModules.add(createShader(s.shaderSource, s.shaderType)));

        link(shaderModules);
    }

    public int getAttribLocation(String attribName) {
        return GL20.glGetAttribLocation(programId, attribName);
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    protected int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    private void link(List<Integer> shaderModules) {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        shaderModules.forEach(s -> glDetachShader(programId, s));
        shaderModules.forEach(GL30::glDeleteShader);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void validate() {
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            throw new RuntimeException("Error validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
    }

    public void createUniforms(List<String> uniformNames) {
        uniformsMap = new UniformsMap(programId);
        uniformNames.forEach(uniformsMap::createUniform);
    }

    public int enableAttribLocation(String attribName) {
        int location = getAttribLocation(attribName);
        if (location >= 0) {
            glEnableVertexAttribArray(location);
        }

        return location;
    }

    public boolean disableAttribLocation(String attribName) {
        int location = getAttribLocation(attribName);
        if (location == -1) {
            return false;
        }
        glDisableVertexAttribArray(location);
        return true;
    }

    public record ShaderModuleData(String shaderSource, int shaderType) {

    }
}
