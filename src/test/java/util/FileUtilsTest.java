package util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @DisplayName("2의 거듭제곱 근접 값")
    @Test
    void getNearestPowerOfTwo() {
        int expected = 1024;
        int result = FileUtils.getNearestPowerOfTwo(900);
        assertEquals(expected, result);

        expected = 512;
        result = FileUtils.getNearestPowerOfTwo(600);
        assertEquals(expected, result);
    }

    @DisplayName("확장자 추출")
    @Test
    void getExtension() {
        String expected = "png";
        String result = FileUtils.getExtension("test.png");
        assertEquals(expected, result);

        expected = "jpg";
        result = FileUtils.getExtension("C:\\data\\test.jpg");
        assertEquals(expected, result);

        expected = "png";
        result = FileUtils.getExtension("C:\\data\\test");
        assertNotEquals(expected, result);

        expected = "";
        result = FileUtils.getExtension("C:\\data\\test");
        assertEquals(expected, result);
    }

    @DisplayName("확장자 변경")
    @Test
    void changeExtension() {
        String expected = "C:\\data\\test.png";
        String result = FileUtils.changeExtension("C:\\data\\test.jpg", "png");
        assertEquals(expected, result);

        expected = "C:\\data\\test.gltf";
        result = FileUtils.changeExtension("C:\\data\\test.3ds", "gltf");
    }

    @DisplayName("파일명만 추출")
    @Test
    void getFileNameWithoutExtension() {
        String expected = "C:\\data\\test";
        String result = FileUtils.getFileNameWithoutExtension("C:\\data\\test.jpg");
        assertEquals(expected, result);

        expected = "test";
        result = FileUtils.getFileNameWithoutExtension("test.3ds");
    }

    @DisplayName("파일 읽기")
    @Test
    void readBytes() {
        String path = "C:\\data\\sample\\nonFile.png";
        File file = new File(path);
        byte[] bytes = FileUtils.readBytes(file);
        Assertions.assertNull(bytes);
    }
}