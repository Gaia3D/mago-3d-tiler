package com.gaia3d.converter.kml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.gaia3d.converter.kml.kml.Document;
import com.gaia3d.converter.kml.kml.KmlRoot;
import com.gaia3d.converter.kml.kml.Model;
import com.gaia3d.converter.kml.kml.Placemark;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@NoArgsConstructor
public class JacksonKmlReader implements AttributeReader {

    @Override
    public TileTransformInfo read(File file) {
        TileTransformInfo tileTransformInfo = null;
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            KmlRoot root = xmlMapper.readValue(file, KmlRoot.class);
            Document document = root.getDocument();
            for (Placemark placemark : document.getPlacemark()) {
                String name = placemark.getName();
                String description = placemark.getDescription();
                List<Model> models = placemark.getModel();
                Model model = models.get(0);
                String altitudeMode = model.getAltitudeMode();
                String href = model.getLink().getHref();
                double longitude = model.getLocation().getLongitude();
                double latitude = model.getLocation().getLatitude();
                double altitude = model.getLocation().getAltitude();
                double heading = model.getOrientation().getHeading();
                double tilt = model.getOrientation().getTilt();
                double roll = model.getOrientation().getRoll();
                double x = model.getScale().getX();
                double y = model.getScale().getY();
                double z = model.getScale().getZ();
                tileTransformInfo = TileTransformInfo.builder().name(name).position(new Vector3d(longitude, latitude, altitude)).altitudeMode(altitudeMode).heading(heading).tilt(tilt).roll(roll).href(href).scaleX(x).scaleY(y).scaleZ(z).build();
            }
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
        return tileTransformInfo;
    }


    public List<TileTransformInfo> readAll(File file) {
        List<TileTransformInfo> tileTransformInfos = new ArrayList<>();
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            KmlRoot root = xmlMapper.readValue(file, KmlRoot.class);
            Document document = root.getDocument();
            for (Placemark placemark : document.getPlacemark()) {
                String name = placemark.getName();
                String description = placemark.getDescription();
                List<Model> models = placemark.getModel();
                for (Model model : models) {
                    String altitudeMode = model.getAltitudeMode();
                    String href = model.getLink().getHref();
                    double longitude = model.getLocation().getLongitude();
                    double latitude = model.getLocation().getLatitude();
                    double altitude = model.getLocation().getAltitude();
                    double heading = model.getOrientation().getHeading();
                    double tilt = model.getOrientation().getTilt();
                    double roll = model.getOrientation().getRoll();
                    double x = model.getScale().getX();
                    double y = model.getScale().getY();
                    double z = model.getScale().getZ();

                    HashMap<String, String> properties = new HashMap<>();
                    properties.put("name", name);
                    properties.put("description", description);
                    TileTransformInfo tileTransformInfo = TileTransformInfo.builder()
                            .name(name)
                            .position(new Vector3d(longitude, latitude, altitude))
                            .altitudeMode(altitudeMode)
                            .heading(heading)
                            .tilt(tilt)
                            .roll(roll)
                            .href(href)
                            .scaleX(x)
                            .scaleY(y)
                            .scaleZ(z)
                            .properties(properties)
                            .build();
                    tileTransformInfos.add(tileTransformInfo);
                }
            }
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
        return tileTransformInfos;
    }
}
