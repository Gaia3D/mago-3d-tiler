package renderable;

import geometry.structure.GaiaTexture;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL20;

@Setter
@Getter
public class TextureBuffer {
    private int[] vbos;
    private int vboCount = 0;
    private int textureVbo;

    public TextureBuffer() {
        this.initVbo();
    }
    public void initVbo() {
        vbos = new int[1];
        GL20.glGenTextures(vbos);
        vboCount = 0;
    }

    //bind texture
    public void setTextureBind(int vbo) {
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, vbo);
    }
    //unbind texture
    public void setTextureUnbind() {
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0);
    }

    public int createGlTexture(GaiaTexture gaiaTexture) {
        int texture = GL20.glGenTextures();
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texture);
        GL20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_REPEAT);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_REPEAT);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR); // GL_NEAREST
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR); // GL_NEAREST
        GL20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, gaiaTexture.getWidth(), gaiaTexture.getHeight(), 0, gaiaTexture.getFormat(), GL20.GL_UNSIGNED_BYTE, gaiaTexture.getByteBuffer());
        vboCount++;
        return texture;
    }
}
