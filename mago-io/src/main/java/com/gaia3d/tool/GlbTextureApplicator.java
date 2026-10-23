package com.gaia3d.tool;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.creation.ImageModels;
import de.javagl.jgltf.model.creation.MaterialModels;
import de.javagl.jgltf.model.creation.TextureModels;
import de.javagl.jgltf.model.impl.DefaultGltfModel;
import de.javagl.jgltf.model.impl.DefaultImageModel;
import de.javagl.jgltf.model.impl.DefaultTextureModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

public class GlbTextureApplicator {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -cp <classpath> com.gaia3d.tool.GlbTextureApplicator <input.glb> <texture.jpg|png> [output.glb]");
            System.err.println("Example: java -cp <classpath> com.gaia3d.tool.GlbTextureApplicator model.glb texture.jpg model_textured.glb");
            System.exit(1);
        }

        String inputPath = args[0];
        String texturePath = args[1];
        String outputPath = args.length >= 3 ? args[2] : getDefaultOutputPath(inputPath);

        try {
            applyTexture(inputPath, texturePath, outputPath);
            System.out.println("Texture applied successfully!");
            System.out.println("Input model: " + inputPath);
            System.out.println("Texture file: " + texturePath);
            System.out.println("Output file: " + outputPath);
        } catch (Exception e) {
            System.err.println("Failed to apply texture: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void applyTexture(String inputPath, String texturePath, String outputPath) throws Exception {
        File inputFile = new File(inputPath);
        File textureFile = new File(texturePath);
        File outputFile = new File(outputPath);

        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input GLB file not found: " + inputPath);
        }
        if (!textureFile.exists()) {
            throw new IllegalArgumentException("Texture file not found: " + texturePath);
        }

        // 1. Read GLB model
        System.out.println("Reading model: " + inputPath);
        GltfModelReader reader = new GltfModelReader();
        GltfModel model = reader.read(inputFile.toPath());

        // 2. Read texture image
        System.out.println("Reading texture: " + texturePath);
        BufferedImage image = ImageIO.read(textureFile);
        if (image == null) {
            throw new IllegalArgumentException("Unable to read texture image: " + texturePath);
        }

        String extension = getFileExtension(texturePath);
        String mimeType = extension.equalsIgnoreCase("png") ? "image/png" : "image/jpeg";
        String textureName = "applied_texture." + extension;

        // 3. Create ImageModel
        DefaultImageModel imageModel = ImageModels.createFromBufferedImage(
                textureName, mimeType, image);

        // 4. Create TextureModel
        DefaultTextureModel textureModel = TextureModels.createFromImage(imageModel);
        textureModel.setMagFilter(9729);
        textureModel.setMinFilter(9987);
        textureModel.setWrapS(10497);
        textureModel.setWrapT(10497);

        // 5. Create material with the texture
        MaterialModelV2 newMaterial = MaterialModels.createFromBaseColorTexture(
                textureModel, null);
        newMaterial.setName("applied_texture_material");

        // 6. Cast to DefaultGltfModel and add elements
        DefaultGltfModel defaultModel = (DefaultGltfModel) model;
        defaultModel.addImageModel(imageModel);
        defaultModel.addTextureModel(textureModel);
        defaultModel.addMaterialModel(newMaterial);

        // 7. Replace existing material references with the new material
        List<MaterialModel> materials = defaultModel.getMaterialModels();
        System.out.println("Found " + materials.size() + " existing material(s)");

        // 8. Redirect mesh primitives to the new material
        replaceMaterialReferences(defaultModel, newMaterial, materials.size() - 1);

        // 9. Write output GLB
        System.out.println("Writing: " + outputPath);
        GltfModelWriter writer = new GltfModelWriter();
        writer.writeBinary(defaultModel, outputFile);
    }

    private static void replaceMaterialReferences(DefaultGltfModel model, MaterialModel newMaterial, int newMaterialIndex) {
        try {
            // Iterate all MeshModels and point primitives to the new material
            var meshModels = model.getMeshModels();
            for (var mesh : meshModels) {
                var primitives = mesh.getMeshPrimitiveModels();
                if (primitives == null) continue;
                for (var prim : primitives) {
                    setMaterialOnPrimitive(prim, newMaterial, newMaterialIndex);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to replace material references: " + e.getMessage());
        }
    }

    private static void setMaterialOnPrimitive(Object prim, MaterialModel newMaterial, int newMaterialIndex) {
        Class<?> cls = prim.getClass();
        try {
            // Try setMaterialModel(MaterialModel)
            for (Method m : cls.getMethods()) {
                if (m.getName().equals("setMaterialModel") && m.getParameterCount() == 1) {
                    m.invoke(prim, newMaterial);
                    System.out.println("Replaced material reference");
                    return;
                }
            }
            // Try setting the material field directly
            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                if (f.getName().contains("material") || f.getName().contains("Material")) {
                    f.setAccessible(true);
                    if (f.getType().isAssignableFrom(MaterialModel.class) ||
                        f.getType().isAssignableFrom(newMaterial.getClass())) {
                        f.set(prim, newMaterial);
                        System.out.println("Replaced material reference (field)");
                        return;
                    }
                }
            }
            System.err.println("Warning: Unable to find material setter: " + cls.getName());
        } catch (Exception e) {
            System.err.println("Warning: Failed to set material: " + e.getMessage());
        }
    }

    private static String getFileExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < path.length() - 1) {
            return path.substring(dotIndex + 1).toLowerCase();
        }
        return "jpg";
    }

    private static String getDefaultOutputPath(String inputPath) {
        int dotIndex = inputPath.lastIndexOf('.');
        if (dotIndex > 0) {
            return inputPath.substring(0, dotIndex) + "_textured.glb";
        }
        return inputPath + "_textured.glb";
    }
}
