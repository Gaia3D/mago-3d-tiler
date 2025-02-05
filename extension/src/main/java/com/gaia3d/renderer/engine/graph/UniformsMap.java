package com.gaia3d.renderer.engine.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class UniformsMap {
    private final int programId;
    private final Map<String, Integer> uniforms;

    public UniformsMap(int programId) {
        this.programId = programId;
        uniforms = new HashMap<>();
    }

    public int getUniformLocation(String uniformName) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            throw new RuntimeException("Could not find uniform [" + uniformName + "]");
        }
        return location.intValue();
    }

    public void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new RuntimeException("Could not find uniform [" + uniformName + "] in shader program [" + programId + "]");
        }
        uniforms.put(uniformName, uniformLocation);
    }

    public boolean setUniform1i(String uniformName, int value) {
        // check if exist uniform.
        int location = glGetUniformLocation(programId, uniformName);
        if (location >= 0) {
            glUniform1i(location, value);
            return true;
        }
        return false;
    }

    public void setUniform4fv(String uniformName, Vector4f value) {
        // check if exist uniform.
        int location = glGetUniformLocation(programId, uniformName);
        if (location >= 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniform4fv(location, value.get(stack.mallocFloat(4)));
            }
        }
    }

    public void setUniformMatrix4fv(String uniformName, Matrix4f value) {
        // check if exist uniform.
        int location = glGetUniformLocation(programId, uniformName);
        if (location >= 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix4fv(location, false, value.get(stack.mallocFloat(16)));
            }
        }
    }

    public void setUniform1f(String uniformName, float value) {
        // check if exist uniform.
        int location = glGetUniformLocation(programId, uniformName);
        if (location >= 0) {
            glUniform1f(location, value);
        }
    }

    public void setUniform3fv(String uBackgroundColor, Vector3f vector3f) {
        // check if exist uniform.
        int location = glGetUniformLocation(programId, uBackgroundColor);
        if (location >= 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniform3fv(location, vector3f.get(stack.mallocFloat(3)));
            }
        }
    }
}
