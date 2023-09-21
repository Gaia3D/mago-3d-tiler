package com.gaia3d.converter.geometry.citygml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.converter.geometry.AbstractGeometryConverter;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.citygml4j.core.model.building.Building;
import org.citygml4j.core.model.core.AbstractCityObject;
import org.citygml4j.core.model.core.AbstractCityObjectProperty;
import org.citygml4j.core.model.core.CityModel;
import org.citygml4j.xml.CityGMLContext;
import org.citygml4j.xml.CityGMLContextException;
import org.citygml4j.xml.reader.CityGMLInputFactory;
import org.citygml4j.xml.reader.CityGMLReadException;
import org.citygml4j.xml.reader.CityGMLReader;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.primitives.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class CityGmlConverter extends AbstractGeometryConverter implements Converter {

    private final CommandLine command;

    public CityGmlConverter(CommandLine command) {
        this.command = command;
    }

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

    protected List<GaiaScene> convert(File file) {
        List<GaiaScene> scenes = new ArrayList<>();

        boolean flipCoordnate = this.command.hasOption(ProcessOptions.FLIP_COORDINATE.getArgName());

        try {
            Tessellator tessellator = new Tessellator();
            Extruder extruder = new Extruder(tessellator);

            CityGMLContext context = CityGMLContext.newInstance();
            CityGMLInputFactory factory = context.createCityGMLInputFactory();
            CityGMLReader reader = factory.createCityGMLReader(file);
            CityModel cityModel = (CityModel) reader.next();

            List<GaiaBuilding> gaiaBuildings = new ArrayList<>();

            List<AbstractCityObjectProperty> cityObjectMembers = cityModel.getCityObjectMembers();
            for (AbstractCityObjectProperty cityObjectProperty : cityObjectMembers) {
                AbstractCityObject cityObject = cityObjectProperty.getObject();

                Building building = (Building) cityObject;
                SolidProperty solidProperty= building.getLod1Solid();
                AbstractSolid solid = solidProperty.getObject();

                Shell shell = ((Solid) solid).getExterior().getObject();
                List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();

                if (building.getHeights().isEmpty()) {
                    continue;
                }

                double height = getHeight(building);
                GaiaBuilding gaiaBuilding = GaiaBuilding.builder()
                        .id(cityObject.getId())
                        .floorHeight(0)
                        .roofHeight(height)
                        .build();

                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                for (SurfaceProperty surfaceProperty : surfaceProperties) {
                    List<Vector3d> polygon = new Vector<>();

                    Polygon surface = (Polygon) surfaceProperty.getObject();
                    LinearRing linearRing = (LinearRing)surface.getExterior().getObject();
                    DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
                    List<Double> values = directPositionList.getValue();

                    double value = 0d;
                    for (int i = 0; i < values.size(); i+=3) {
                        double x, y, z;
                        if (flipCoordnate) {
                            x = values.get(i + 1);
                            y = values.get(i);
                        } else {
                            x = values.get(i);
                            y = values.get(i + 1);
                        }
                        z = 0.0d;
                        value += values.get(i + 2);
                        Vector3d position = new Vector3d(x, y, z);
                        polygon.add(position);
                        boundingBox.addPoint(position);
                    }

                    double floorHeight = value / values.size();
                    gaiaBuilding.setPositions(polygon);
                    gaiaBuilding.setFloorHeight(floorHeight);
                    gaiaBuilding.setRoofHeight(floorHeight + height);
                    break;
                }
                gaiaBuilding.setBoundingBox(boundingBox);
                gaiaBuildings.add(gaiaBuilding);
            }

            for (GaiaBuilding gaiaBuilding : gaiaBuildings) {
                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());
                GaiaMaterial material = scene.getMaterials().get(0);
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaBoundingBox boundingBox = gaiaBuilding.getBoundingBox();
                Vector3d center = boundingBox.getCenter();

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();
                for (Vector3d position : gaiaBuilding.getPositions()) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                    localPosition.z = 0.0d;
                    localPositions.add(localPosition);
                }

                Extrusion extrusion = extruder.extrude(localPositions, gaiaBuilding.getRoofHeight(), gaiaBuilding.getFloorHeight());
                GaiaNode node = createNode(material, extrusion.getPositions(), extrusion.getTriangles());
                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }
        } catch (CityGMLContextException | CityGMLReadException e) {
            throw new RuntimeException(e);
        }

        return scenes;
    }

    protected double getHeight(Building building) {
        return building.getHeights().get(0).getObject().getValue().getValue();
    }
}