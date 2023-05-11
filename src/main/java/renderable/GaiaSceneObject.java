package renderable;

import assimp.AssimpConverter;
import geometry.structure.GaiaScene;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.file.Path;

public class GaiaSceneObject extends RenderableObject {
    GaiaScene scene;

    public GaiaSceneObject(GaiaScene gaiaScene) {
        super();
        this.scene = gaiaScene;
        this.setPosition(0.0f, 0.0f, 0.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.scene.renderScene(program);
        }
    }

    @Override
    public RenderableBuffer getBuffer() {
        return null;
    }
}
