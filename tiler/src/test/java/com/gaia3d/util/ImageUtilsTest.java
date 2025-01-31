package com.gaia3d.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    @Test
    void getNearestPowerOfTwo() {
        int size = ImageUtils.getNearestPowerOfTwo(1025);
        assertEquals(1024, size);

        size = ImageUtils.getNearestPowerOfTwo(1024);
        assertEquals(1024, size);

        size = ImageUtils.getNearestPowerOfTwo(3072);
        assertEquals(2048, size);

        size = ImageUtils.getNearestPowerOfTwo(3073);
        assertEquals(4096, size);
    }

    @Test
    void getNearestPowerOfTwoHigher() {
        int size = ImageUtils.getNearestPowerOfTwoHigher(1025);
        assertEquals(2048, size);

        size = ImageUtils.getNearestPowerOfTwoHigher(1024);
        assertEquals(1024, size);

        size = ImageUtils.getNearestPowerOfTwoHigher(3072);
        assertEquals(4096, size);

        size = ImageUtils.getNearestPowerOfTwoHigher(3159);
        assertEquals(2048, size);
    }
}