package command;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Setter
@Getter
public class CommandOption {
    private Path inputPath = null;
    private InputType inputType = InputType.IN_3DS;
    private Path outputPath = null;
    private OutputType outputType = OutputType.OUT_GLB;
    boolean recursive = false;

    float scaleFactor = 0.0f;
    boolean strictMode = false;
    boolean genNormals = false;
    boolean genTangents = false;
    boolean swapYZ = false;
    boolean ignoreTextures = false;

    public enum InputType {
        IN_3DS("3ds"),
        IN_OBJ("obj"),
        IN_COLLADA("dae"),
        IN_IFC("ifc"),
        IN_GLTF("gltf"),
        IN_GLB("glb");

        String extension;
        InputType(String extension) {
            this.extension = extension;
        }
        public String getExtension() {
            return extension;
        }
        public static InputType fromExtension(String extension) {
            for (InputType inputType : InputType.values()) {
                if (inputType.getExtension().equals(extension)) {
                    return inputType;
                }
            }
            return null;
        }
    }
    public enum OutputType {
        OUT_GLTF("gltf"),
        OUT_GLB("glb"),
        OUT_B3DM("b3dm"),
        OUT_I3DM("i3dm");

        String extension;
        OutputType(String extension) {
            this.extension = extension;
        }
        public String getExtension() {
            return extension;
        }
        public static OutputType fromExtension(String extension) {
            for (OutputType outputType : OutputType.values()) {
                if (outputType.getExtension().equals(extension)) {
                    return outputType;
                }
            }
            return null;
        }
    }
}
