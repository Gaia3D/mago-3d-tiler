package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.AbstractGeometryConverter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Polygon;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class GeoJsonConverter extends AbstractGeometryConverter implements Converter {

    @Override
    public List<GaiaScene> load(String path) {
        return convert(new File(path));
    }

    @Override
    public List<GaiaScene> load(File file) {
        return convert(file);
    }

    @Override
    public List<GaiaScene> load(Path path) {
        return convert(path.toFile());
    }

    @Override
    protected List<GaiaScene> convert(File file) {
        try {
            GeometryJSON gjson = new GeometryJSON();
            String json = Files.readString(file.toPath());
            Reader reader = new StringReader(json);
            Polygon polygon = gjson.readPolygon(reader);
            log.info(polygon.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;


    }
}
