package geometry.structure;

public enum GaiaMaterialType {
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
    GaiaMaterialType(int value) {
        this.value = value;
    }
}
