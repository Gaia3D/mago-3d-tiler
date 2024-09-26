package com.gaia3d;

import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.Configurator;
import com.gaia3d.converter.EasySceneCreator;
import com.gaia3d.converter.jgltf.GltfWriter;
import it.geosolutions.imageio.stream.output.ImageOutputStreamAdapter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.EmptyFeatureCollection;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Envelopes;
import org.geotools.image.ImageWorker;
import org.geotools.process.vector.VectorToRasterException;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.styling.RasterSymbolizerImpl;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.media.jai.ImageLayout;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.operator.MosaicDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class GltfCreator {

    @Test
    public void run() {
        Configurator.initConsoleLogger();

        File file = new File("D:/workspaces/scene.gltf");
        EasySceneCreator easySceneCreator = new EasySceneCreator();
        GaiaScene gaiaScene = easySceneCreator.createScene(file);
        GaiaNode rootNode = gaiaScene.getNodes().get(0);

        int gridSize = 512;
        GaiaNode gridNode = easySceneCreator.createGridNode(gridSize, gridSize);
        rootNode.getChildren().add(gridNode);

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(gaiaScene, file);
    }

    @Test
    public void convertVectorToRaster() {
        File file = new File("D:/workspaces/seoul-parts-real.shp");
        ShpFiles shpFiles = null;
        ShapefileReader reader = null;

        try {
            shpFiles = new ShpFiles(file);
            reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory());
            DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
            String typeName = dataStore.getTypeNames()[0];
            ContentFeatureSource source = (ContentFeatureSource) dataStore.getFeatureSource(typeName);
            var query = new Query(typeName, Filter.INCLUDE);
            int totalCount = source.getCount(query);
            System.out.println("Total count: " + totalCount);



            SimpleFeatureCollection features = source.getFeatures();

            FeatureIterator<SimpleFeature> iterator = features.features();

            List<SimpleFeature> simpleFeatures = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Geometry buffered = geom.buffer(0.000025);
                feature.setDefaultGeometry(buffered);
                simpleFeatures.add(feature);

                Integer height = (Integer) feature.getAttribute("HEIGHT");
                feature.setAttribute("HEIGHT", height + 1);
            }

            simpleFeatures.sort((o1, o2) -> {
                Integer height1 = (Integer) o1.getAttribute("HEIGHT");
                Integer height2 = (Integer) o2.getAttribute("HEIGHT");
                return height1.compareTo(height2);
            });
            //Collections.reverse(simpleFeatures);

            ListFeatureCollection listFeatureCollection = new ListFeatureCollection(features.getSchema(), simpleFeatures);

            int width = 1024;
            int height = 1024;

            double maxLon = 126.96656100367117;
            double maxLat = 37.51055300294337;
            double minLon = 126.95497970896422;
            double minLat = 37.50132670958266;

            double lonOffset = 0.000005;
            double latOffset = -0.000005;

            minLon += lonOffset;
            maxLon += lonOffset;

            minLat += latOffset;
            maxLat += latOffset;

            System.out.println("Start converting vector to raster");
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
            ReferencedEnvelope envelope = new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, sourceCRS);

            Dimension dimension = new Dimension(width, height);
            ProgressListener progressListener = new DefaultProgressListener();

            String title = "title";
            String attribute = "HEIGHT";
            VectorToRasterProcess vectorToRasterProcess = new VectorToRasterProcess();
            GridCoverage2D gridCoverage2D = vectorToRasterProcess.execute(listFeatureCollection, width, height, title, attribute, envelope, progressListener);

            ImageWorker imageWorker = new ImageWorker(gridCoverage2D.getRenderedImage());
            imageWorker = imageWorker.rescaleToBytes();
            imageWorker.forceComponentColorModel();

            ReferencedEnvelope envelope2 = new ReferencedEnvelope(minLat, maxLat, minLon, maxLon, sourceCRS);
            gridCoverage2D = new GridCoverageFactory().create("one", imageWorker.getRenderedImage(), envelope2);


            System.out.println("End converting vector to raster");

            File outputFile = new File("D:/workspaces/sample.tiff");
            GeoTiffWriter writer = new GeoTiffWriter(outputFile);

            GeoTiffWriteParams params = new GeoTiffWriteParams();
            ParameterValue<GeoToolsWriteParams> value = GeoTiffFormat.GEOTOOLS_WRITE_PARAMS.createValue();
            value.setValue(params);

            writer.write(gridCoverage2D, new GeneralParameterValue[]{value});
            writer.dispose();

            File output = new File("D:/workspaces/sample.png");
            ImageIO.write(imageWorker.getBufferedImage(), "png", output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAuthorityCodeException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (VectorToRasterException e) {
            throw new RuntimeException(e);
        }
    }
}
