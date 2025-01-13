package com.gaia3d.release;

import com.gaia3d.basic.model.ImageCacheQueue;
import com.gaia3d.command.Configurator;
import com.gaia3d.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

@Slf4j
public class ImageIOTest {

    @Test
    void testJpgQuality() {
        Configurator.initConsoleLogger();
        File input = new File("D:\\workspace\\pizza.jpg");
        for (int i = 0; i < 100; i++) {
            log.info("Quality: {}", 0.01f * i);
            File output = new File("D:\\workspace\\compressed_pizza" + i + ".jpg");
            writeImage(input, output, 0.01f * i);
        }
    }

    @Test
    void testDeepCopy() {
        Configurator.initConsoleLogger();
        File input = new File("D:\\workspace\\pizza.jpg");
        try {
            BufferedImage image = ImageIO.read(input);

            ImageCacheQueue imageCacheQueue = ImageCacheQueue.getInstance();
            BufferedImage copy = imageCacheQueue.deepCopy(image);

            log.info("Image: {}", image);
            log.info("Copy: {}", copy);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeImage(File inputFile, File outputFile, float quality) {
        try {
            BufferedImage image = ImageIO.read(inputFile);
            OutputStream os = new FileOutputStream(outputFile);
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg"); // 1
            if (!writers.hasNext()) {
                throw new IllegalStateException("No writers found");
            }
            ImageWriter writer = writers.next();
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam(); // 2
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // 3
            param.setCompressionQuality(quality);  // 4
            writer.write(null, new IIOImage(image, null, null), param); // 5

            os.close();
            ios.close();
            writer.dispose();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
