package com.gaia3d.converter;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.Configurator;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.feature.FeatureIterator;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.process.vector.VectorToRasterException;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"ALL", "TryWithIdenticalCatches"})
@Slf4j
public class GltfCreatorTest {

    @Test
    public void createGrid() {
        Configurator.initConsoleLogger();

        GlobalOptions globalOptions = GlobalOptions.getInstance();

        int[] gridSizes = new int[] {8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        for (int gridSize : gridSizes) {
            File file = new File("E:/workspace/", gridSize + "x" + gridSize + ".glb");
            log.info("Creating gltf file: {}", file.getAbsolutePath());
            EasySceneCreator easySceneCreator = new EasySceneCreator();
            GaiaScene gaiaScene = easySceneCreator.createScene(file);
            GaiaNode rootNode = gaiaScene.getNodes().get(0);

            GaiaNode gridNode = easySceneCreator.createGridNode(gridSize, gridSize);
            rootNode.getChildren().add(gridNode);

            GltfWriter gltfWriter = new GltfWriter();
            gltfWriter.writeGlb(gaiaScene, file);
        }
    }

    @SuppressWarnings("TryWithIdenticalCatches")
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
            log.info("Total count: " + totalCount);

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

            log.info("Start converting vector to raster");
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


            log.info("End converting vector to raster");

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
