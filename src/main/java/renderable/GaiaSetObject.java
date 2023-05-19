package renderable;

import geometry.exchangable.GaiaSet;
import org.lwjgl.system.MemoryStack;

public class GaiaSetObject extends RenderableObject {
    GaiaSet gaiaSet;

    public GaiaSetObject(GaiaSet gaiaSet) {
        super();
        this.gaiaSet = gaiaSet;
        this.setPosition(0.0f, 0.0f, 0.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GaiaSet set = this.gaiaSet;
            set.renderSet(program);
        }
    }

    @Override
    public RenderableBuffer getBuffer() {
        return null;
    }
}
