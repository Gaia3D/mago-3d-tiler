package renderable;

import assimp.DataLoader;
import geometry.structure.GaiaScene;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.file.Path;

public class GaiaSceneObject extends RenderableObject {
    GaiaScene scene;
    File file;
    Path path;

    public GaiaSceneObject(String filePath) {
        super();
        this.file = new File(filePath);
        this.path = file.toPath();
        this.setPosition(0.0f, 0.0f, 0.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GaiaScene gaiaScene;
            if (this.scene == null) {
                gaiaScene = DataLoader.load(file.getAbsolutePath(), null);
                this.scene = gaiaScene;
            } else {
                gaiaScene = this.scene;
            }
            gaiaScene.renderScene(program);
        }
    }

    @Override
    public RenderableBuffer getBuffer() {
        return null;
    }
}
