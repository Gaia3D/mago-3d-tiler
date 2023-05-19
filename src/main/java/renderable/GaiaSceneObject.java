package renderable;

import geometry.structure.GaiaScene;
import org.lwjgl.system.MemoryStack;

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
