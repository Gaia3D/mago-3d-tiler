package com.gaia3d.converter.kml.kml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.gaia3d.basic.pointcloud.UniqueRandomNumbers;
import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
class KmlRootTest {
    @Test
    void case01() {
        XmlMapper xmlMapper = new XmlMapper();
        String path = "D:\\Mago3DTiler-UnitTest\\input\\auto-created-i3dm\\sample-instances.kml";
        try {
             KmlRoot root = xmlMapper.readValue(new File(path), KmlRoot.class);
             log.info("{}", xmlMapper.writeValueAsString(root));
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void hashSet() {
        Configurator.initConsoleLogger();

        log.info("start");
        List<Integer> temp = UniqueRandomNumbers.generateUniqueRandomCache(1000000);
        log.info("{}", temp.size());

        temp = UniqueRandomNumbers.generateUniqueRandomCache(100000000);
        log.info("{}", temp.size());
        temp = UniqueRandomNumbers.generateUniqueRandomCache(5000000);
        log.info("{}", temp.size());
        temp = UniqueRandomNumbers.generateUniqueRandomCache(75000000);
        log.info("{}", temp.size());
        temp = UniqueRandomNumbers.generateUniqueRandomCache(4000000);
        log.info("{}", temp.size());

        /*List<Integer> list = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
        HashSet<Integer> hashSet = new HashSet<>(list);
        *//*int count = 1000;
        Random random = new Random();
        while (hashSet.size() < count) {
            int num = random.nextInt(count);
            hashSet.add(num);
        }*//*

        hashSet.iterator().forEachRemaining(i -> {
            log.info("{}", i);
        });*/
    }
}