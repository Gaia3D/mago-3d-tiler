package com.gaia3d.converter.citygml;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.Configurator;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.FileLoader;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.citygml4j.core.model.building.Building;
import org.citygml4j.core.model.core.AbstractCityObject;
import org.citygml4j.core.model.core.CityModel;
import org.citygml4j.xml.CityGMLContext;
import org.citygml4j.xml.CityGMLContextException;
import org.citygml4j.xml.reader.*;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.GeometricPositionList;
import org.xmlobjects.gml.model.geometry.primitives.*;

import java.io.File;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CityGmlConverterTest {

    @Test
    void read() {
        String path = "D:\\workspaces\\cityGML\\moran_6697\\63403779_bldg_6697_op.gml";
        String path2 = "D:\\workspaces\\cityGML\\hawaii\\Hawaii-15001-002.gml";

        Converter converter = new CityGmlConverter();
        GaiaScene scene = converter.load(path);

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(scene, "D:\\sample.gltf");
    }

    @Test
    void load() {
        String path = "D:\\workspaces\\cityGML\\moran_6697\\63403767_bldg_6697_op.gml";
        String path2 = "D:\\workspaces\\cityGML\\hawaii\\Hawaii-15001-002.gml";

        Configurator.initConsoleLogger();
        CityGMLContext ctx = null;
        try {
            CityGMLContext context = CityGMLContext.newInstance();
            CityGMLInputFactory factory = context.createCityGMLInputFactory();
            CityGMLReader reader = factory.createCityGMLReader(new File(path));
            CityModel cityModel = (CityModel) reader.next();

            AbstractCityObject cityObject = cityModel.getCityObjectMembers().get(0).getObject();

            Building building = (Building) cityObject;
            SolidProperty solidProperty= building.getLod1Solid();
            AbstractSolid solid = solidProperty.getObject();

            Shell shell = ((Solid) solid).getExterior().getObject();
            List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();

            String srsName = cityModel.getBoundedBy().getEnvelope().getSrsName();

            //CRSFactory crsFactory = new CRSFactory();
            //CoordinateReferenceSystem source = crsFactory.createFromName("EPSG:6697");

            List<List<Vector3d>> polygons = new Vector<>();


            for (SurfaceProperty surfaceProperty : surfaceProperties) {
                List<Vector3d> polygon = new Vector<>();

                Polygon surface = (Polygon) surfaceProperty.getObject();
                LinearRing linearRing = (LinearRing)surface.getExterior().getObject();
                DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
                List<Double> values = directPositionList.getValue();

                for (int i = 0; i < values.size(); i+=3) {
                    double x = values.get(i);
                    double y = values.get(i+1);
                    double z = values.get(i+2);
                    Vector3d position = new Vector3d(x, y, z);
                    polygon.add(position);

                    log.info("{}A : {}, {}, {}", i, position.x(), position.y(), position.z());
                    //ProjCoordinate centerSource = new ProjCoordinate(x, y, z);
                    //ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
                    //log.info("B : {}, {}, {}", centerWgs84.x, centerWgs84.y, centerWgs84.z);
                }
                //log.info(linearRing.toString());

                polygons.add(polygon);
            }

            log.info("cityModel: {}", cityModel);

        } catch (CityGMLContextException | CityGMLReadException e) {
            throw new RuntimeException(e);
        }
    }
}