package com.gaia3d.converter.kml;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 * @author znkim
 * @since 1.0.0
 * @see FastKmlReader , KmlInfo
 */
@Slf4j
@NoArgsConstructor
public class FastKmlReader {

    public KmlInfo read(File file) {
        KmlInfo kmlInfo = null;
        try {
            String xml = Files.readString(file.toPath());
            Vector3d position = new Vector3d(Double.parseDouble(findValue(xml, "longitude")), Double.parseDouble(findValue(xml, "latitude")), Double.parseDouble(findValue(xml, "altitude")));
            kmlInfo = KmlInfo.builder()
                    .name(findValue(xml, "name"))
                    .position(position)
                    .altitudeMode(findValue(xml, "altitudeMode"))
                    .heading(Double.parseDouble(findValue(xml, "heading")))
                    .tilt(Double.parseDouble(findValue(xml, "tilt")))
                    .roll(Double.parseDouble(findValue(xml, "roll")))
                    .href(findValue(xml, "href"))
                    .scaleX(Double.parseDouble(findValue(xml, "x")))
                    .scaleY(Double.parseDouble(findValue(xml, "y")))
                    .scaleZ(Double.parseDouble(findValue(xml, "z")))
                    .build();
            xml = null;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return kmlInfo;
    }

    private String findValue(String xml, String tag) {
        String result = null;
        int start = xml.indexOf("<" + tag + ">");
        int end = xml.indexOf("</" + tag + ">");
        if (start > -1 && end > -1) {
            result = xml.substring(start + tag.length() + 2, end);
        }
        return result;
    }
}
