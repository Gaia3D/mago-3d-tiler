package com.gaia3d.renderer.engine.graph;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class ShaderManager {
    final Map<String, ShaderProgram> mapNameShaderProgram = new HashMap<>();

    public ShaderProgram createShaderProgram(String shaderProgramName, List<ShaderProgram.ShaderModuleData> shaderModuleDataList) {
        ShaderProgram shaderProgram = new ShaderProgram(shaderModuleDataList);
        mapNameShaderProgram.put(shaderProgramName, shaderProgram);
        return shaderProgram;
    }

    public ShaderProgram getShaderProgram(String shaderProgramName) {
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
}
