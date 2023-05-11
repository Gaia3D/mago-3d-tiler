package geometry.types;

public enum FormatType {

    MAX_3DS("3ds"),
    OBJ("obj"),
    COLLADA("dae"),
    IFC("ifc"),
    GLTF("gltf"),
    GLB("glb"),
    B3DM("b3dm"),
    I3DM("i3dm");

    String extension;

    FormatType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static FormatType fromExtension(String extension) {
        for (FormatType type : FormatType.values()) {
            if (type.getExtension().equals(extension)) {
                return type;
            }
        }
        return null;
    }
}
