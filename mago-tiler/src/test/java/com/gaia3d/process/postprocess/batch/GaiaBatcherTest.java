package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.types.AttributeType;
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
