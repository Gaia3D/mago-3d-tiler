package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class GaiaBatcherTest {

    @Test
    void separatesDataSetsByTexCoordPresenceBeforeBatching() {
        GaiaBufferDataSet withoutTexCoordA = createDataSet(false);
        GaiaBufferDataSet withTexCoord = createDataSet(true);
        GaiaBufferDataSet withoutTexCoordB = createDataSet(false);

        List<List<GaiaBufferDataSet>> groups = new GaiaBatcher().divisionByMaxVerticesCount(
                List.of(withoutTexCoordA, withTexCoord, withoutTexCoordB));

        assertEquals(2, groups.size());
        assertEquals(2, groups.get(0).size());
        assertSame(withoutTexCoordA, groups.get(0).get(0));
        assertSame(withoutTexCoordB, groups.get(0).get(1));
        assertTrue(groups.get(0).stream().noneMatch(this::hasTexCoord));
        assertEquals(List.of(withTexCoord), groups.get(1));
        assertTrue(groups.get(1).stream().allMatch(this::hasTexCoord));
    }

    @Test
    void keepsDataSetsWithMatchingTexCoordPresenceTogether() {
        GaiaBufferDataSet first = createDataSet(true);
        GaiaBufferDataSet second = createDataSet(true);

        List<List<GaiaBufferDataSet>> groups = new GaiaBatcher().divisionByMaxVerticesCount(List.of(first, second));

        assertEquals(1, groups.size());
        assertEquals(List.of(first, second), groups.getFirst());
        assertFalse(groups.getFirst().isEmpty());
    }

    @Test
    void removesAllTextureReferencesFromMaterialUsedWithoutTexCoords() {
        GaiaMaterial texturedMaterial = new GaiaMaterial();
        texturedMaterial.setId(0);
        texturedMaterial.getTextures().put(TextureType.NORMALS, List.of(new GaiaTexture()));
        GaiaBufferDataSet withTexCoord = createDataSet(true);
        GaiaBufferDataSet withoutTexCoord = createDataSet(false);
        withTexCoord.setMaterialId(0);
        withoutTexCoord.setMaterialId(0);
        List<GaiaMaterial> materials = new java.util.ArrayList<>(List.of(texturedMaterial));

        new GaiaBatcher().assignTexturelessMaterialsToDataSetsWithoutTexCoords(
                List.of(withTexCoord, withoutTexCoord), materials);

        assertEquals(2, materials.size());
        assertEquals(0, withTexCoord.getMaterialId());
        assertFalse(materials.get(withTexCoord.getMaterialId()).getTextures().isEmpty());
        assertEquals(1, withoutTexCoord.getMaterialId());
        assertTrue(materials.get(withoutTexCoord.getMaterialId()).getTextures().isEmpty());
    }

    @Test
    void offsetsIndicesByVertexCountWhenTrailingVerticesAreUnreferenced() {
        // first: 4 vertices, triangle uses 0..2 only (vertex 3 unreferenced)
        GaiaBufferDataSet first = createIndexedDataSet(new float[]{
                0, 0, 0, 1, 0, 0, 0, 1, 0, 9, 9, 9}, new int[]{0, 1, 2});
        GaiaBufferDataSet second = createIndexedDataSet(new float[]{
                10, 0, 0, 11, 0, 0, 10, 1, 0}, new int[]{0, 1, 2});

        GaiaBufferDataSet batched = new GaiaBatcher().batchVertices(List.of(first, second));

        int[] indices = batched.getBuffers().get(AttributeType.INDICE).getInts();
        float[] positions = batched.getBuffers().get(AttributeType.POSITION).getFloats();
        assertEquals(6, batched.getBuffers().get(AttributeType.INDICE).getElementsCount());
        assertEquals(List.of(0, 1, 2, 4, 5, 6), java.util.Arrays.stream(indices).boxed().toList());
        assertEquals(10.0f, positions[indices[3] * 3]);
        assertEquals(11.0f, positions[indices[4] * 3]);
    }

    @Test
    void fillsNormalsAndColorsMissingFromSomeDataSets() {
        GaiaBufferDataSet withAttributes = createIndexedDataSet(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0}, new int[]{0, 1, 2});
        GaiaBuffer normalBuffer = new GaiaBuffer();
        normalBuffer.setFloats(new float[]{1, 0, 0, 1, 0, 0, 1, 0, 0});
        withAttributes.getBuffers().put(AttributeType.NORMAL, normalBuffer);
        GaiaBuffer colorBuffer = new GaiaBuffer();
        colorBuffer.setBytes(new byte[]{1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4});
        withAttributes.getBuffers().put(AttributeType.COLOR, colorBuffer);
        GaiaBufferDataSet withoutAttributes = createIndexedDataSet(new float[]{10, 0, 0, 11, 0, 0}, new int[]{0, 1, 1});

        GaiaBufferDataSet batched = new GaiaBatcher().batchVertices(List.of(withoutAttributes, withAttributes), new byte[]{9, 8, 7, 6});

        float[] normals = batched.getBuffers().get(AttributeType.NORMAL).getFloats();
        byte[] colors = batched.getBuffers().get(AttributeType.COLOR).getBytes();
        assertEquals(5 * 3, normals.length);
        assertEquals(5 * 4, colors.length);
        assertEquals(5, batched.getBuffers().get(AttributeType.NORMAL).getElementsCount());
        assertEquals(1.0f, normals[2]); // filled up normal of vertex 0
        assertEquals(1.0f, normals[2 * 3]); // vertex 2 is the first one with its own normal
        assertEquals(9, colors[0]);
        assertEquals(1, colors[2 * 4]);
        assertEquals(4, colors[5 * 4 - 1]);
    }

    private GaiaBufferDataSet createIndexedDataSet(float[] positions, int[] indices) {
        GaiaBufferDataSet dataSet = new GaiaBufferDataSet();
        GaiaBuffer positionBuffer = new GaiaBuffer();
        positionBuffer.setFloats(positions);
        dataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);
        GaiaBuffer indicesBuffer = new GaiaBuffer();
        indicesBuffer.setInts(indices);
        dataSet.getBuffers().put(AttributeType.INDICE, indicesBuffer);
        return dataSet;
    }

    private GaiaBufferDataSet createDataSet(boolean withTexCoord) {
        GaiaBufferDataSet dataSet = new GaiaBufferDataSet();
        GaiaBuffer positionBuffer = new GaiaBuffer();
        positionBuffer.setFloats(new float[]{0.0f, 0.0f, 0.0f});
        dataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);

        if (withTexCoord) {
            GaiaBuffer texCoordBuffer = new GaiaBuffer();
            texCoordBuffer.setFloats(new float[]{0.0f, 0.0f});
            dataSet.getBuffers().put(AttributeType.TEXCOORD, texCoordBuffer);
        }
        return dataSet;
    }

    private boolean hasTexCoord(GaiaBufferDataSet dataSet) {
        return dataSet.getBuffers().get(AttributeType.TEXCOORD) != null;
    }
}
