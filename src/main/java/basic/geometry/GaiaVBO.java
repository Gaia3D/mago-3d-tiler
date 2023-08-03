package basic.geometry;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GaiaVBO {
    private int indicesLength;
    private int indicesVbo;
    private int positionVbo;
    private int normalVbo;
    private int colorVbo;
    private int textureCoordinateVbo;
    private int textureVbo;
}
