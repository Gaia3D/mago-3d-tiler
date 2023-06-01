package tiler;

import assimp.AssimpConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Gaia3DTiler {
    private static AssimpConverter assimpConverter = new AssimpConverter(null);
    public void createRoot(Path input, Path output) {

        System.setProperty("org.geotools.referencing.forceXY", "true");

        GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
        List<GaiaScene> sceneList = read(input);
        for (GaiaScene scene : sceneList) {
            GaiaBoundingBox localBoundingBox = scene.getBoundingBox();
            globalBoundingBox.addBoundingBox(localBoundingBox);

            ProjCoordinate minPoint = new ProjCoordinate(localBoundingBox.getMinX(), localBoundingBox.getMinY(), localBoundingBox.getMinZ());
            ProjCoordinate maxPoint = new ProjCoordinate(localBoundingBox.getMaxX(), localBoundingBox.getMaxY(), localBoundingBox.getMaxZ());

            CRSFactory factory = new CRSFactory();
            CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
            CoordinateReferenceSystem grs80 = factory.createFromParameters("EPSG:5186", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs");

            //CoordinateReferenceSystem grs80 = factory.createFromName("EPGS:5179");
            //CoordinateReferenceSystem wgs84 = factory.createFromName("EPSG:4326");
            ProjCoordinate translatedMinPoint = transform(grs80, wgs84, minPoint);
            ProjCoordinate translatedMaxPoint = transform(grs80, wgs84, maxPoint);
            log.info(":");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String test = objectMapper.writeValueAsString(globalBoundingBox);
            log.info(test);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<GaiaScene> read(Path input) {
        List<GaiaScene> sceneList = new ArrayList<>();
        readTree(sceneList, input.toFile(), FormatType.MAX_3DS);
        return sceneList;
    }
    private void readTree(List<GaiaScene> sceneList, File inputFile, FormatType formatType) {
        if (inputFile.isFile() && inputFile.getName().endsWith("." + formatType.getExtension())) {
            GaiaScene scene = assimpConverter.load(inputFile.toPath(), formatType.getExtension());
            sceneList.add(scene);
        } else if (inputFile.isDirectory()){
            for (File child : inputFile.listFiles()) {
                if (sceneList.size() <= 100) {
                    readTree(sceneList, child, formatType);
                }
            }
        }
    }



    public static ProjCoordinate transform(CoordinateReferenceSystem source, CoordinateReferenceSystem target, ProjCoordinate beforeCoord) {
        //CRSFactory factory = new CRSFactory();
        //CoordinateReferenceSystem grs80 = factory.createFromName("EPSG:5179");
        //CoordinateReferenceSystem wgs84 = factory.createFromName("EPSG:4326");
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, target);
        ProjCoordinate afterCoord = new ProjCoordinate();
        transformer.transform(beforeCoord, afterCoord);
        return afterCoord;
    }
}
