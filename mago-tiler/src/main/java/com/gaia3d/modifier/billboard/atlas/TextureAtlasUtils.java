package com.gaia3d.modifier.billboard.atlas;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public final class TextureAtlasUtils {

    /**
     * Merge Primitives if same materials are used, to reduce draw calls. This is optional but can improve performance.
     * @param primitives {@code List<GaiaPrimitive>}
     */
    public static GaiaPrimitive createMergedPrimitives(List<GaiaPrimitive> primitives) {
        if (primitives.isEmpty()) {
            log.warn("No primitives to merge.");
            return null;
        }

        GaiaExtractor extractor = new GaiaExtractor();
        GaiaPrimitive firstPrimitive = primitives.getFirst();
        GaiaPrimitive resultPrimitive = new GaiaPrimitive();
        List<GaiaSurface> surfaces = resultPrimitive.getSurfaces();
        List<GaiaVertex> vertices = resultPrimitive.getVertices();

        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> primitiveVertices = primitive.getVertices();
            List<GaiaFace> gaiaFaces = extractor.extractAllFaces(primitive);

            GaiaSurface newSurface = new GaiaSurface();
            List<GaiaFace> newFaces = newSurface.getFaces();
            for (GaiaFace face : gaiaFaces) {
                GaiaFace copiedFace = face.clone();

                int[] indices = copiedFace.getIndices();
                for (int i = 0; i < indices.length; i++) {
                    indices[i] += vertices.size();
                }
                newFaces.add(copiedFace);
            }

            surfaces.add(newSurface);
            vertices.addAll(primitiveVertices);
        }

        resultPrimitive.setMaterialIndex(firstPrimitive.getMaterialIndex());
        resultPrimitive.setAccessorIndices(firstPrimitive.getAccessorIndices());
        return resultPrimitive;
    }

    /**
     * Change the material index of all primitives in the scene to the specified material index.
     * This is useful when you want to apply the same material to all primitives after merging them.
     * @param scene GaiaScene
     * @param materialIndex int
     */
    public static void changePrimitivesMaterialId(GaiaScene scene, int materialIndex) {
        GaiaExtractor extractor = new GaiaExtractor();
        for (GaiaNode node : extractor.extractAllNodes(scene, true)) {
            for (GaiaMesh mesh : node.getMeshes()) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    primitive.setMaterialIndex(materialIndex);
                }
            }
        }
    }

    /**
     * Find all primitives in the scene that use the specified material.
     * This is useful when you want to merge primitives that use the same material.
     * @param scene GaiaScene
     * @param material GaiaMaterial
     * @return {@code List<GaiaPrimitive>}
     */
    public static List<GaiaPrimitive> findPrimitivesUsingMaterial(GaiaScene scene, GaiaMaterial material) {
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = new ArrayList<>();
        for (GaiaNode node : extractor.extractAllNodes(scene, true)) {
            for (GaiaMesh mesh : node.getMeshes()) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    if (primitive.getMaterialIndex() == material.getId()) {
                        primitives.add(primitive);
                    }
                }
            }
        }
        return primitives;
    }

    /**
     * Load the diffuse image from the material.
     * This is useful when you want to create a texture atlas and need to access the diffuse images of the materials.
     * @param material GaiaMaterial
     * @return BufferedImage
     */
    public static BufferedImage loadDiffuseImage(GaiaMaterial material) {
        Map<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
        List<GaiaTexture> diffuseTextures = materialTextures.getOrDefault(TextureType.DIFFUSE, Collections.emptyList());
        if (!diffuseTextures.isEmpty()) {
            GaiaTexture diffuseTexture = diffuseTextures.getFirst();
            diffuseTexture.loadImage();
            return diffuseTexture.getBufferedImage();
        }
        return null;
    }
}
