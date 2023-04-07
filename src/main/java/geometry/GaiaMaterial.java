package geometry;

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
    private Vector4d ambientColor = new Vector4d(0.0, 0.0, 0.0, 1.0);
    private Vector4d diffuseColor = new Vector4d(0.0, 0.0, 0.0, 1.0);
    private Vector4d specularColor = new Vector4d(0.0, 0.0, 0.0, 1.0);
    private float shininess = 0.0f;

    private int id = -1;
    private String name;
    private LinkedHashMap<MaterialType, String> textures = new LinkedHashMap<>();

    public enum MaterialType {
        NONE(0),
        DIFFUSE(1),
        SPECULAR(2),
        AMBIENT(3),
        EMISSIVE(4),
        HEIGHT(5),
        NORMALS(6),
        SHININESS(7),
        OPACITY(8),
        DISPLACEMENT(9),
        LIGHTMAP(10),
        REFLECTION(11),
        BASE_COLOR(12),
        NORMAL_CAMERA(13),
        EMISSION_COLOR(14),
        METALNESS(15),
        DIFFUSE_ROUGHNESS(16),
        AMBIENT_OCCLUSION(17),
        UNKNOWN(18);

        private int value;

        MaterialType(int value) {
            this.value = value;
        }
    }
}
