package geometry.structure;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector4d;

import java.util.LinkedHashMap;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaMaterial {
    private Vector4d diffuseColor = new Vector4d(0.0, 0.0, 0.0, 1.0);
    private Vector4d ambientColor = new Vector4d(0.0, 0.0, 0.0, 1.0);
    private Vector4d specularColor = new Vector4d(0.0, 0.0, 0.0, 1.0);
    private float shininess = 0.0f;

    private int id = -1;
    private String name = "GaiaDefaultMaterial";
    private LinkedHashMap<GaiaMaterialType, GaiaTexture> textures = new LinkedHashMap<>();
}
