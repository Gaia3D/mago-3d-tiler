package com.gaia3d.renderer.renderable;

import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL20;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RenderableGaiaScene {
    List<RenderableNode> renderableNodes;
    private GaiaScene originalGaiaScene;
    private Path originalPath;
    private List<GaiaMaterial> materials = new ArrayList<>();

    public RenderableGaiaScene() {
        renderableNodes = new ArrayList<>();
        originalGaiaScene = null;
    }

    public void addRenderableNode(RenderableNode renderableNode) {
        renderableNodes.add(renderableNode);
    }

    public void extractRenderablePrimitives(List<RenderablePrimitive> resultRenderablePrimitives) {
        for (RenderableNode renderableNode : renderableNodes) {
            renderableNode.extractRenderablePrimitives(resultRenderablePrimitives);
        }
    }

    public void deleteGLBuffers() {
        for (RenderableNode renderableNode : renderableNodes) {
            renderableNode.deleteGLBuffers();
        }

        // delete textures.***
        for (GaiaMaterial material : materials) {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            for (List<GaiaTexture> gaiaTextures : textures.values()) {
                for (GaiaTexture gaiaTexture : gaiaTextures) {
                    int textureId = gaiaTexture.getTextureId();
                    if (textureId != -1) {
                        GL20.glDeleteTextures(textureId);
                        gaiaTexture.setTextureId(-1);
                    }
                    gaiaTexture.deleteObjects();
                    gaiaTexture.setByteBuffer(null);
                    gaiaTexture.setBufferedImage(null);
                }
            }
        }

        int materialsCount = this.materials.size();
        for (int i = 0; i < materialsCount; i++) {
            GaiaMaterial material = this.materials.get(i);
            material.clear();
        }

        materials.clear();
    }

}
