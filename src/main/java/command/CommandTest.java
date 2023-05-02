package command;

import assimp.DataLoader;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import util.FileUtils;

import java.io.File;

public class CommandTest {
    public static void main(String[] args) {
        //String inputPath = "C:\\data\\sample\\Data3D\\Edumuseum_del_150417_02_3DS\\Edumuseum_del_150417_02.3ds";
        //String inputPath = "C:\\data\\sample\\Data3D\\dogok_library_del_3DS\\dogok_library_del.3ds";
        //String inputPath = "C:\\data\\sample\\DC_library_del_3DS\\DC_library_del.3ds";
        //String inputPath = "C:\\data\\sample\\face.3ds";
        //String inputPath = "C:\\data\\sample\\dolphin.3ds";
        //String inputPath = "C:\\data\\sample\\KSJ_100.ifc";

        //File thePath = new File("C:\\data\\sample\\DC_library_del_3DS\\DC_library_del.3ds");
        File thePath = new File("C:\\data\\sample\\Data3D\\gangbuk_cultur_del_3DS\\gangbuk_cultur_del.3ds");
        //File thePath = new File("C:\\data\\sample\\simpleCube.3ds");
        convert(thePath, "3ds", "C:\\data\\sample");

        //File parentPath = new File("C:\\data\\sample\\Data3D");
        //convert(parentPath, "3ds", "C:\\data\\sample");




        /*if (parentPath.isDirectory()) {
            for (String childPath : parentPath.list()) {
                File child = new File(parentPath, childPath);
                String fileName = FileUtils.getFileNameWithoutExtension(child.getName());
                String extension = FileUtils.getExtension(child.getName());

                GaiaScene scene = DataLoader.load(child.getAbsolutePath(), extension);

                String outputPathGltf = "C:\\data\\sample";
                File output = new File(outputPathGltf, fileName + ".gltf");
                System.out.println(child.getAbsolutePath() + " -> " + output.getAbsolutePath());
                GltfWriter.writeGltf(scene, output.getAbsolutePath());
            }
        }*/


        /*File file = new File(inputPath);
        String fileName = FileUtils.getFileNameWithoutExtension(file.getName());
        String extension = FileUtils.getExtension(file.getName());

        GaiaScene scene = DataLoader.load(inputPath, extension);

        String outputPathGltf = "C:\\data\\sample";
        File output = new File(outputPathGltf, fileName + ".gltf");
        System.out.println(file.getAbsolutePath() + " -> " + output.getAbsolutePath());
        GltfWriter.writeGltf(scene, output.getAbsolutePath());*/

        //String outputPathGlb = "C:\\data\\sample\\test.glb";
        //GltfWriter.writeGlb(scene, outputPathGlb);
    }
    public static void convert(File path, String extension, String outputPath) {
        if (path.isFile() && FileUtils.getExtension(path.getName()).equals(extension)) {
            String fileName = FileUtils.getFileNameWithoutExtension(path.getName());
            GaiaScene scene = DataLoader.load(path.getAbsolutePath(), extension, new CommandOption());

            String outputPathGltf = "C:\\data\\sample";
            File output = new File(outputPathGltf, fileName + ".glb");
            System.out.println(path.getAbsolutePath() + " -> " + output.getAbsolutePath());
            GltfWriter.writeGlb(scene, output.getAbsolutePath());
        }
        if (path.isDirectory()) {
            for (String childPath : path.list()) {
                File child = new File(path, childPath);
                convert(child, extension, outputPath);
            }
        }
    }
}
