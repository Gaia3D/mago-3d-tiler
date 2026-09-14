package com.gaia3d.converter.kml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeReaderNumberTest {
    @Test
    void numericValuesReplaceTheDefaultInsteadOfAddingToIt() {
        assertEquals(4.0, AttributeReader.parseNumber(4, 1.0));
        assertEquals(3.7, AttributeReader.parseNumber(3.7, 1.0));
        assertEquals(3.7, AttributeReader.parseNumber("3.7", 1.0));
    }

    @Test
    void invalidValuesUseTheDefault() {
        assertEquals(1.0, AttributeReader.parseNumber(null, 1.0));
        assertEquals(1.0, AttributeReader.parseNumber(Double.NaN, 1.0));
        assertEquals(1.0, AttributeReader.parseNumber(Double.POSITIVE_INFINITY, 1.0));
        assertEquals(1.0, AttributeReader.parseNumber("invalid", 1.0));
    }

    @Test
    void nonPositiveScaleUsesTheDefault() {
        assertEquals(3.7, AttributeReader.positiveOrDefault(3.7, 1.0));
        assertEquals(1.0, AttributeReader.positiveOrDefault(0.0, 1.0));
        assertEquals(1.0, AttributeReader.positiveOrDefault(-2.0, 1.0));
        assertEquals(1.0, AttributeReader.positiveOrDefault(Double.NaN, 1.0));
    }
}
