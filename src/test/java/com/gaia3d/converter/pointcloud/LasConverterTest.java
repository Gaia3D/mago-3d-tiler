package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LasConverterTest {
    @Test
    void load() throws URISyntaxException {
        LasConverter lasConverter = new LasConverter();
        lasConverter.load("D:\\temp\\las\\temp.las");
    }
}