package command;

import geometry.DataLoader;
import geometry.GaiaScene;
import tiler.GltfWriter;
import util.FileUtil;

import java.io.File;

public class Command {
    public static void main(String[] args) {
        boolean isHelp = false;
        CommandOption commandOption = new CommandOption();
        commandOption.setInputType(CommandOption.InputType.IN_3DS);
        commandOption.setOutputType(CommandOption.OutputType.OUT_GLB);

        //File absolutePath = new File("").getAbsoluteFile();
        //System.out.println("absolutePath : " + absolutePath);

        for (int i = 0 ; i < args.length; i++) {
            String arg = args[i];
            System.out.println("arg ["+ i +"]: " + arg);
            if (arg.contains("-help") || arg.contains("--help")) {
                isHelp = true;
            }
            if (arg.contains("-input=")) {
                String path = arg.substring(arg.indexOf("=") + 1);
                commandOption.setInputPath(new File(path).toPath());
            }
            if (arg.contains("-output=")) {
                String path = arg.substring(arg.indexOf("=") + 1);
                commandOption.setOutputPath(new File(path).toPath());
            }
            if (arg.contains("-inputType=")) {
                String value = arg.substring(arg.indexOf("=") + 1);
                commandOption.setInputType(CommandOption.InputType.fromExtension(value));
            }
            if (arg.contains("-outputType=")) {
                String value = arg.substring(arg.indexOf("=") + 1);
                commandOption.setOutputType(CommandOption.OutputType.fromExtension(value));
            }
        }

        if (isHelp) {
            System.out.println("=============[HELP][Plasma Gltf Converter]=============");
            System.out.println("Usage: java -jar tiler.jar -input=<inputPath> -output=<outputPath> -inputType=<inputType> -outputType=<outputType>");
            System.out.println("inputType : 3ds, obj, dae, fbx, gltf, glb");
            System.out.println("outputType : gltf, glb");
            System.out.println("Example: java -jar tiler.jar -input=C:\\data\\sample\\a_bd001.3ds -output=C:\\data\\sample\\a_bd001.gltf -inputType=3ds -outputType=gltf");
            System.out.println("Author: ZNKIM");
            System.out.println("Version: 0.1.0");
            System.out.println("=============[HELP][Plasma Gltf Converter]=============");
            return;
        }
        if (commandOption.getInputPath() == null) {
            System.err.println("inputPath is not defined.");
            return;
        }
        if (commandOption.getOutputPath() == null) {
            System.err.println("outputPath is not defined.");
            return;
        }

//        System.out.println("inputPath : " + commandOption.getInputPath().toAbsolutePath().toString());
//        System.out.println("outputPath : " + commandOption.getOutputPath().toAbsolutePath().toString());
//        System.out.println("root : " + commandOption.getInputPath().getRoot());
//        System.out.println("parent : " + commandOption.getInputPath().getParent());
//        System.out.println("fileName : " + commandOption.getInputPath().getFileName());
//        System.out.println("nameCount : " + commandOption.getInputPath().getNameCount());

        File inputPathFile = commandOption.getInputPath().toFile();
        File outputPathFile = commandOption.getOutputPath().toFile();
        String inputExtension = commandOption.getInputType().getExtension();
        String outputExtension = commandOption.getOutputType().getExtension();
        if (inputPathFile.isDirectory() && outputPathFile.isDirectory()) {
            File[] Children = inputPathFile.listFiles();
            for (File child : Children) {
                if (child.isFile() && child.getName().endsWith("." + inputExtension)) {
                    String outputFile = FileUtil.changeExtension(child.getName(), outputExtension);
                    File output = new File(commandOption.getOutputPath().toAbsolutePath().toString() + File.separator + outputFile);
                    System.out.println("convert : " + child.getAbsolutePath() + " -> " + output.getAbsolutePath());
                    GaiaScene scene = DataLoader.load(child.getAbsolutePath(), null);
                    GltfWriter.writeGltf(scene, output.getAbsolutePath());
                } else {
                    System.out.println("skip : " + child.getName());
                }
            }
        }

        //GaiaScene scene = DataLoader.load(commandOption.getInputPath().toAbsolutePath().toString(), null);
        //GltfWriter.writeGltf(scene, commandOption.getOutputPath().toAbsolutePath().toString());
    }
}
