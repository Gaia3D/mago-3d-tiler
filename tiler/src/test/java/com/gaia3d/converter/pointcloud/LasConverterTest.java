package com.gaia3d.converter.pointcloud;

import com.github.mreutegg.laszip4j.CloseablePointIterable;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASReader;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LasConverterTest {

    @Test
    void case00() {
        String path = "D:\\Mago3DTiler-UnitTest\\input\\case21-las-sangjiuni";
        File input = new File(path);
        List<File> inputFiles = (ArrayList<File>) FileUtils.listFiles(input, new String[]{"las"}, true);

        List<Vector3d> pointsList = new ArrayList<>();
        inputFiles.forEach((inputFile) -> {
            LASReader reader = new LASReader(inputFile);
            LASHeader header = reader.getHeader();
            CloseablePointIterable pointIterable = reader.getCloseablePoints();

            pointIterable.forEach((point) -> {
                double x = point.getX();
                double y = point.getY();
                double z = point.getZ();
                Vector3d vector3d = new Vector3d(x, y, z);
                pointsList.add(vector3d);
            });
            System.out.println("pointIterable = " + pointIterable);
        });
    }

}