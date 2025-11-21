package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;

import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

@Getter
@Setter
@Slf4j
public class IntegralReMeshParameters {
    private Map<String, Fbo> colorFboMap = new java.util.HashMap<>();
    private Map<String, Fbo> colorCodeFboMap = new java.util.HashMap<>();
    private Vector4f backgroundColor = new Vector4f(0.5f, 0.0f, 0.0f, 1.0f);

    public void clear() {
        colorFboMap.clear();
        colorCodeFboMap.clear();
    }

    public void createFBOsObliqueCamera(FboManager fboManager, int fboWidth, int fboHeight) {
        Fbo colorFbo_ZNEG = fboManager.getOrCreateFbo("ZNEG", fboWidth, fboHeight);
        Fbo colorFbo_XPOS_ZNEG = fboManager.getOrCreateFbo("XPOS_ZNEG", fboWidth, fboHeight);
        Fbo colorFbo_XNEG_ZNEG = fboManager.getOrCreateFbo("XNEG_ZNEG", fboWidth, fboHeight);
        Fbo colorFbo_YPOS_ZNEG = fboManager.getOrCreateFbo("YPOS_ZNEG", fboWidth, fboHeight);
        Fbo colorFbo_YNEG_ZNEG = fboManager.getOrCreateFbo("YNEG_ZNEG", fboWidth, fboHeight);

        colorFboMap.put("ZNEG", colorFbo_ZNEG);
        colorFboMap.put("XPOS_ZNEG", colorFbo_XPOS_ZNEG);
        colorFboMap.put("XNEG_ZNEG", colorFbo_XNEG_ZNEG);
        colorFboMap.put("YPOS_ZNEG", colorFbo_YPOS_ZNEG);
        colorFboMap.put("YNEG_ZNEG", colorFbo_YNEG_ZNEG);

        Fbo colorCodedFbo_ZNEG = fboManager.getOrCreateFbo("ColorCoded_ZNEG", fboWidth, fboHeight);
        Fbo colorCodedFbo_XPOS_ZNEG = fboManager.getOrCreateFbo("ColorCoded_XPOS_ZNEG", fboWidth, fboHeight);
        Fbo colorCodedFbo_XNEG_ZNEG = fboManager.getOrCreateFbo("ColorCoded_XNEG_ZNEG", fboWidth, fboHeight);
        Fbo colorCodedFbo_YPOS_ZNEG = fboManager.getOrCreateFbo("ColorCoded_YPOS_ZNEG", fboWidth, fboHeight);
        Fbo colorCodedFbo_YNEG_ZNEG = fboManager.getOrCreateFbo("ColorCoded_YNEG_ZNEG", fboWidth, fboHeight);

        colorCodeFboMap.put("ZNEG", colorCodedFbo_ZNEG);
        colorCodeFboMap.put("XPOS_ZNEG", colorCodedFbo_XPOS_ZNEG);
        colorCodeFboMap.put("XNEG_ZNEG", colorCodedFbo_XNEG_ZNEG);
        colorCodeFboMap.put("YPOS_ZNEG", colorCodedFbo_YPOS_ZNEG);
        colorCodeFboMap.put("YNEG_ZNEG", colorCodedFbo_YNEG_ZNEG);

        // initialize the fbos
        Vector4f clearColor = backgroundColor;
        initFbo(colorFbo_ZNEG, clearColor, true);
        initFbo(colorFbo_XPOS_ZNEG, clearColor, true);
        initFbo(colorFbo_XNEG_ZNEG, clearColor, true);
        initFbo(colorFbo_YPOS_ZNEG, clearColor, true);
        initFbo(colorFbo_YNEG_ZNEG, clearColor, true);

        Vector4f colorCodeClearColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        initFbo(colorCodedFbo_ZNEG, colorCodeClearColor, true);
        initFbo(colorCodedFbo_XPOS_ZNEG, colorCodeClearColor, true);
        initFbo(colorCodedFbo_XNEG_ZNEG, colorCodeClearColor, true);
        initFbo(colorCodedFbo_YPOS_ZNEG, colorCodeClearColor, true);
        initFbo(colorCodedFbo_YNEG_ZNEG, colorCodeClearColor, true);
    }

    private void initFbo(Fbo fbo, Vector4f clearColor, boolean clearDepth) {
        fbo.bind();

        int[] width = new int[1];
        int[] height = new int[1];
        width[0] = fbo.getFboWidth();
        height[0] = fbo.getFboHeight();

        glViewport(0, 0, width[0], height[0]);
        glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);

        int clearMask = GL20.GL_COLOR_BUFFER_BIT;
        if (clearDepth) {
            clearMask |= GL20.GL_DEPTH_BUFFER_BIT;
            glEnable(GL20.GL_DEPTH_TEST);
        } else {
            glDisable(GL20.GL_DEPTH_TEST);
        }

        glClear(clearMask);

        fbo.unbind();
    }

    public void deleteFBOs(FboManager fboManager) {
        for (Fbo fbo : colorFboMap.values()) {
            fbo.cleanup();
        }
        colorFboMap.clear();

        for (Fbo fbo : colorCodeFboMap.values()) {
            fbo.cleanup();
        }
        colorCodeFboMap.clear();
    }
}
