package command;

import assimp.DataLoader;
import geometry.structure.GaiaScene;
import tiler.GltfWriter;
import util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Command {
    static boolean isQuiet = false;

    public static void main(String[] args) {
        boolean isHelp = false;
        CommandOption commandOption = new CommandOption();
        commandOption.setInputType(CommandOption.InputType.IN_3DS);
        commandOption.setOutputType(CommandOption.OutputType.OUT_GLB);

        for (int i = 0 ; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains("-help") || arg.contains("--help")) {
                isHelp = true;
            }
            if (arg.contains("-input=")) {
                String path = arg.substring(arg.indexOf("=") + 1);
                commandOption.setInputPath(new File(path).toPath());
            } else if (arg.contains("-output=")) {
                String path = arg.substring(arg.indexOf("=") + 1);
                commandOption.setOutputPath(new File(path).toPath());
            } else if (arg.contains("-inputType=")) {
                String value = arg.substring(arg.indexOf("=") + 1);
                commandOption.setInputType(CommandOption.InputType.fromExtension(value));
            } else if (arg.contains("-outputType=")) {
                String value = arg.substring(arg.indexOf("=") + 1);
                commandOption.setOutputType(CommandOption.OutputType.fromExtension(value));
            } else if (arg.contains("-quiet")) {
                isQuiet = true;
            }
        }

        printLogo();
        if (isHelp) {
            printHelp();
            return;
        } else if (commandOption.getInputPath() == null) {
            errlog("inputPath is not defined.");
            return;
        }  else if (commandOption.getOutputPath() == null) {
            errlog("outputPath is not defined.");
            return;
        }
        logWithoutTime("inputPath : " + commandOption.getInputPath().toAbsolutePath().toString());
        logWithoutTime("outputPath : " + commandOption.getOutputPath().toAbsolutePath().toString());
        logWithoutTime("inputType : " + commandOption.getInputType().getExtension());
        logWithoutTime("outputType : " + commandOption.getOutputType().getExtension());

        File inputPathFile = commandOption.getInputPath().toFile();
        File outputPathFile = commandOption.getOutputPath().toFile();
        String inputExtension = commandOption.getInputType().getExtension();
        String outputExtension = commandOption.getOutputType().getExtension();
        if (inputPathFile.isDirectory() && outputPathFile.isDirectory()) {
            File[] Children = inputPathFile.listFiles();
            for (File child : Children) {
                if (child.isFile() && child.getName().endsWith("." + inputExtension)) {
                    String outputFile = FileUtils.changeExtension(child.getName(), outputExtension);
                    File output = new File(commandOption.getOutputPath().toAbsolutePath().toString() + File.separator + outputFile);
                    log("convert : " + child.getAbsolutePath() + " -> " + output.getAbsolutePath());
                    GaiaScene scene = DataLoader.load(child.getAbsolutePath(), null);
                    if (commandOption.getOutputType() == CommandOption.OutputType.OUT_GLB) {
                        GltfWriter.writeGlb(scene, output.getAbsolutePath());
                    } else if (commandOption.getOutputType() == CommandOption.OutputType.OUT_GLTF) {
                        GltfWriter.writeGltf(scene, output.getAbsolutePath());
                    }
                } else {
                    log("skip : " + child.getName());
                }
            }
        }
        log("=============[END][Plasma Gltf Converter]=============");
    }

    private static void printLogo() {
        if (isQuiet) {
            return;
        }
        System.out.println(
                " _______  ___      _______  _______  __   __  _______ \n" +
                "|       ||   |    |   _   ||       ||  |_|  ||   _   |\n" +
                "|    _  ||   |    |  |_|  ||  _____||       ||  |_|  |\n" +
                "|   |_| ||   |    |       || |_____ |       ||       |\n" +
                "|    ___||   |___ |       ||_____  ||       ||       |\n" +
                "|   |    |       ||   _   | _____| || ||_|| ||   _   |\n" +
                "|___|    |_______||__| |__||_______||_|   |_||__| |__|\n" +
                "===============[Plasma Gltf Converter]================");
    }

    private static void printHelp() {
        logWithoutTime("==========================[HELP][How to use]==========================");
        logWithoutTime("Usage: java -jar tiler.jar -input=<inputPath> -output=<outputPath> -inputType=<inputType> -outputType=<outputType>");
        logWithoutTime("inputType : 3ds, obj, dae, fbx, gltf, glb");
        logWithoutTime("outputType : gltf, glb");
        logWithoutTime("Example: java -jar tiler.jar -input=C:\\data\\sample\\a_bd001.3ds -output=C:\\data\\sample\\a_bd001.gltf -inputType=3ds -outputType=gltf");
        logWithoutTime("DefaultValue: inputType = 3ds, outputType = gltf");
        logWithoutTime("Author: Gaia3D-znkim");
        logWithoutTime("Version: 0.1.1");
        logWithoutTime("==========================[HELP][How to use]==========================");
    }

    private static void logWithoutTime(String message) {
        if (isQuiet) {
            return;
        }
        System.out.println("[P] " + message);
    }

    private static void log(String message) {
        if (isQuiet) {
            return;
        }
        String nowDate = getStringDate();
        System.out.println("[P]["+ nowDate +"] " + message);
    }

    private static void errlog(String message) {
        if (isQuiet) {
            return;
        }
        String nowDate = getStringDate();
        System.out.println("[P]["+ nowDate +"] " + message);
    }

    private static String getStringDate() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(date);
    }
}
