package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {
    @DisplayName("2의 거듭제곱 근접 값")
    @Test
    void getNearestPowerOfTwo() {
        int expected = 1024;
        int result = ImageUtils.getNearestPowerOfTwo(900);
        assertEquals(expected, result);

        expected = 512;
        result = ImageUtils.getNearestPowerOfTwo(600);
        assertEquals(expected, result);
    }
}