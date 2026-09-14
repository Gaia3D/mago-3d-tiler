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
