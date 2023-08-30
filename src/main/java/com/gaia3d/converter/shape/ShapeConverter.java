package com.gaia3d.converter.shape;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileHeader;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

@Slf4j
public class ShapeConverter implements Converter {
    private final CommandLine command;

    public ShapeConverter(CommandLine command) {
        log.info("ShapeConverter");
        this.command = command;
    }

    @Override
    public GaiaScene load(String path) {
        return convert(new File(path));
    }

    @Override
    public GaiaScene load(File file) {
        return convert(file);
    }

    @Override
    public GaiaScene load(Path path) {
        return convert(path.toFile());
    }

    private GaiaScene convert(File file) {

        try {
            ShpFiles shpFiles = new ShpFiles(file);
            ShapefileReader reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory());
            ShapefileHeader header = reader.getHeader();
            log.info("ShapefileHeader: {}", header);

            DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            SimpleFeatureType schema = source.getSchema();

            ShapefileDataStore shapeF
            ileDatastore = (ShapefileDataStore) dataStore;

            source = dataStore.getFeatureSource(typeName);
            schema = source.getSchema();


            log.info("SimpleFeatureType: {}", schema);
            //SimpleFeature feature = dataStore.getFeatureSource(dataStore.getTypeNames()[0]).getFeatures().features().next();

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ShapefileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return null;
    }
}
