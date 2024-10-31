package com.gaia3d.converter.kml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.gaia3d.converter.kml.kml.KmlRoot;
import com.gaia3d.converter.kml.kml.Model;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@NoArgsConstructor
public class JacksonKmlReader implements AttributeReader {

    @Override
    public KmlInfo read(File file) {
        KmlInfo kmlInfo = null;
        XmlMapper xmlMapper = new XmlMapper();
        try {
            KmlRoot root = xmlMapper.readValue(file, KmlRoot.class);
            String name = root.getDocument().getPlacemark().getName();
            String description = root.getDocument().getPlacemark().getDescription();
            List<Model> models = root.getDocument().getPlacemark().getModel();
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

            kmlInfo = KmlInfo.builder().name(name).position(new Vector3d(longitude, latitude, altitude)).altitudeMode(altitudeMode).heading(heading).tilt(tilt).roll(roll).href(href).scaleX(x).scaleY(y).scaleZ(z).build();
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return kmlInfo;
    }


    public List<KmlInfo> readAll(File file) {
        List<KmlInfo> kmlInfos = new ArrayList<>();
        XmlMapper xmlMapper = new XmlMapper();
        try {
            KmlRoot root = xmlMapper.readValue(file, KmlRoot.class);
            String name = root.getDocument().getPlacemark().getName();
            String description = root.getDocument().getPlacemark().getDescription();
            List<Model> models = root.getDocument().getPlacemark().getModel();
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

                KmlInfo kmlInfo = KmlInfo.builder().name(name).position(new Vector3d(longitude, latitude, altitude)).altitudeMode(altitudeMode).heading(heading).tilt(tilt).roll(roll).href(href).scaleX(x).scaleY(y).scaleZ(z).build();
                kmlInfos.add(kmlInfo);
            }
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return kmlInfos;
    }
}
