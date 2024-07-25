package com.gaia3d.converter.geometry.indoorgml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.tessellator.GaiaTessellator;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.AbstractGeometryConverter;
import com.gaia3d.converter.geometry.GaiaBuildingSurface;
import com.gaia3d.converter.geometry.Vector3dsOnlyHashEquals;
import com.gaia3d.util.GlobeUtils;
import edu.stem.indoor.IndoorFeatures;
import edu.stem.space.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IndoorGmlConverter extends AbstractGeometryConverter implements Converter {
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
        List<GaiaScene> scenes = new ArrayList<>();
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        GaiaTessellator tessellator = new GaiaTessellator();

        try {
            JAXBContext context = JAXBContext.newInstance(IndoorFeatures.class);
            IndoorFeatures indoorFeatures = (IndoorFeatures) context.createUnmarshaller().unmarshal(new FileReader(file));

            List<List<GaiaBuildingSurface>> buildingSurfacesList = new ArrayList<>();

            PrimalSpaceFeatures primalSpaceFeatures = indoorFeatures.getPrimalSpaceFeatures();
            PrimalSpaceFeatures primalSpaceFeaturesChild = primalSpaceFeatures.getPrimalSpaceFeatures();
            List<CellSpaceMember> cellSpaceMembers = primalSpaceFeaturesChild.getCellSpaceMember();
            for (CellSpaceMember cellSpaceMember : cellSpaceMembers) {

                //log.info("CellSpaceMember: {}", cellSpaceMember.getCellSpace().getId());
                CellSpace cellSpace = cellSpaceMember.getCellSpace();
                CellSpaceGeometry cellSpaceGeometry = cellSpace.getCellSpaceGeometry();
                Geometry3D geometry3D = cellSpaceGeometry.getGeometry3d();
                Solid solid = geometry3D.getSolid();
                Exterior exterior = solid.getExterior();
                Shell shell = exterior.getShell();
                List<SurfaceMember> surfaceMembers = shell.getSurfaceMembers();

                List<GaiaBuildingSurface> gaiaBuildingSurfaces = new ArrayList<>();

                for (SurfaceMember surfaceMember : surfaceMembers) {
                    GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                    List<Vector3d> vertices = new ArrayList<>();
                    Polygon polygon = surfaceMember.getPolygon();
                    List<Pos> posList = polygon.getExterior().getPos();
                    for (Pos pos : posList) {
                        String[] vectors = pos.getVector().split(" ");

                        //double scale = 0.0254d; // inch to meter
                        double scale = 1.0d;
                        double x = Double.parseDouble(vectors[0]) * scale;
                        double y = Double.parseDouble(vectors[1]) * scale;
                        double z = Double.parseDouble(vectors[2]) * scale;

                        Vector3d wgs84Position = new Vector3d(x, y, z);
                        CoordinateReferenceSystem crs = globalOptions.getCrs();
                        if (crs != null) {
                            ProjCoordinate projCoordinate = new ProjCoordinate(x, y, boundingBox.getMinZ());
                            ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                            wgs84Position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                        }
                        vertices.add(wgs84Position);
                        boundingBox.addPoint(wgs84Position);
                    }

                    // use the first point as the last point to close the polygon
                    if (!vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
                        vertices.add(vertices.get(0));
                        log.info("Polygon is not closed. Adding the first point to the end of the list.");
                    }

                    GaiaBuildingSurface buildingSurface = GaiaBuildingSurface.builder()
                            .id(cellSpace.getId())
                            .name(cellSpace.getName())
                            .boundingBox(boundingBox)
                            .positions(vertices)
                            .build();
                    gaiaBuildingSurfaces.add(buildingSurface);
                }

                if (!gaiaBuildingSurfaces.isEmpty()) {
                    buildingSurfacesList.add(gaiaBuildingSurfaces);
                }
            }

            GaiaScene scene = initScene(file);
            scene.setOriginalPath(file.toPath());
            GaiaNode rootNode = scene.getNodes().get(0);

            GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
            for (List<GaiaBuildingSurface> surfaces : buildingSurfacesList) {
                for (GaiaBuildingSurface buildingSurface : surfaces) {
                    GaiaBoundingBox localBoundingBox = buildingSurface.getBoundingBox();
                    globalBoundingBox.addBoundingBox(localBoundingBox);
                }
            }
            Vector3d center = globalBoundingBox.getCenter();
            Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
            Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
            Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

            for (List<GaiaBuildingSurface> surfaces : buildingSurfacesList) {
                List<List<Vector3d>> polygons = new ArrayList<>();
                for (GaiaBuildingSurface buildingSurface : surfaces) {
                    List<Vector3d> polygon = new ArrayList<>();

                    List<Vector3d> localPositions = new ArrayList<>();
                    for (Vector3d position : buildingSurface.getPositions()) {
                        Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                        Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                        localPositions.add(localPosition);
                        polygon.add(new Vector3dsOnlyHashEquals(localPosition));
                    }
                    polygons.add(polygon);
                }

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);

                GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);
                primitive.setMaterialIndex(0);
                mesh.getPrimitives().add(primitive);

                rootNode.getChildren().add(node);
            }
            Matrix4d rootTransformMatrix = new Matrix4d().identity();
            rootTransformMatrix.translate(center, rootTransformMatrix);
            rootNode.setTransformMatrix(rootTransformMatrix);
            scenes.add(scene);
        } catch (Exception e) {
            log.info("Failed to load IndoorGML file: {}", file.getAbsolutePath());
            e.printStackTrace();
        }

        return scenes;
    }
}
