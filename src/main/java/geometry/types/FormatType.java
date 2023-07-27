package geometry.types;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FormatType {
    KML("kml"),
    MAX_3DS("3ds"),
    OBJ("obj"),
    COLLADA("dae"),
    IFC("ifc"),
    GLTF("gltf"),
    GLB("glb"),
    B3DM("b3dm"),
    I3DM("i3dm"),
    TEMP("mgb");

    final String extension;

    FormatType(String extension) {
        this.extension = extension;
    }

    public static FormatType fromExtension(String extension) {
        return Arrays.stream(FormatType.values())
                .filter(type -> type.getExtension().equals(extension.toLowerCase()))
                .findFirst()
                .orElse(null);
    }
}
