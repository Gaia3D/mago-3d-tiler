package com.gaia3d.converter.geometry.indoorgml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.converter.geometry.tessellator.GaiaTessellator;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        //ConvexHullTessellator tessellator = new ConvexHullTessellator();
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

                        double scale = 0.0254d;
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

                    GaiaBuildingSurface buildingSurface = GaiaBuildingSurface.builder().id(cellSpace.getId()).name(cellSpace.getName()).boundingBox(boundingBox).positions(vertices).build();

                    gaiaBuildingSurfaces.add(buildingSurface);
                }

                if (!gaiaBuildingSurfaces.isEmpty()) {
                    buildingSurfacesList.add(gaiaBuildingSurfaces);
                }
            }

            for (List<GaiaBuildingSurface> surfaces : buildingSurfacesList) {
                //log.info("Building Surface Size: {}", surfaces.size());

                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());
                GaiaMaterial material = scene.getMaterials().get(0);
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
                for (GaiaBuildingSurface buildingSurface : surfaces) {
                    GaiaBoundingBox localBoundingBox = buildingSurface.getBoundingBox();
                    globalBoundingBox.addBoundingBox(localBoundingBox);
                }

                Vector3d center = globalBoundingBox.getCenter();
                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();


//                GaiaNode node = new GaiaNode();
//                node.setTransformMatrix(new Matrix4d().identity());
//                GaiaMesh mesh = new GaiaMesh();
//                node.getMeshes().add(mesh);



                List<List<Vector3d>> polygons = new ArrayList<>();
                for (GaiaBuildingSurface buildingSurface : surfaces) {
                    List<Vector3d> polygon = new ArrayList<>();

                    List<Vector3d> localPositions = new ArrayList<>();
                    for (Vector3d position : buildingSurface.getPositions()) {
                        Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                        Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                        localPosition.z = position.z;
                        localPositions.add(localPosition);
                        polygon.add(new OnlyHashEqualsVector3d(localPosition));
                    }

//                    List<List<Vector3d>> polygons = new ArrayList<>();
                    polygons.add(polygon);
//
//                    GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);
//                    primitive.setMaterialIndex(0);
//                    mesh.getPrimitives().add(primitive);
                }

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);

                GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);
                primitive.setMaterialIndex(0);
                mesh.getPrimitives().add(primitive);

                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }
        } catch (Exception e) {
            log.info("Failed to load IndoorGML file: {}", file.getAbsolutePath());
            e.printStackTrace();
        }

        return scenes;
    }

    protected GaiaPrimitive createPrimitiveFromPolygons(List<List<Vector3d>> polygons) {
        GaiaTessellator tessellator = new GaiaTessellator();

        GaiaPrimitive primitive = new GaiaPrimitive();
        List<GaiaVertex> vertexList = new ArrayList<>();
        //Map<GaiaVertex, Integer> vertexMap = new HashMap<>();
        Map<Vector3d, Integer> pointsMap = new HashMap<>();

        int polygonCount = polygons.size();
        for (List<Vector3d> polygon : polygons) {

            Vector3d normal = new Vector3d();
            tessellator.calculateNormal3D(polygon, normal);

            for (Vector3d vector3d : polygon) {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(vector3d);
                vertex.setNormal(normal);

                //vertex.setNormal(new Vector3d(0, 0, 1));
                vertexList.add(vertex);
            }
        }

        int vertexCount = vertexList.size();
        for (int m = 0; m < vertexCount; m++) {
            GaiaVertex vertex = vertexList.get(m);
            //vertexMap.put(vertex, m);
            pointsMap.put(vertex.getPosition(), m);
        }

        primitive.setVertices(vertexList); // total vertex list.***

        List<Integer> resultTrianglesIndices = new ArrayList<>();

        for (int m = 0; m < polygonCount; m++) {
            GaiaSurface surface = new GaiaSurface();
            primitive.getSurfaces().add(surface);

            int idx1Local = -1;
            int idx2Local = -1;
            int idx3Local = -1;

            List<Vector3d> polygon = polygons.get(m);
            resultTrianglesIndices.clear();
            tessellator.tessellate3D(polygon, resultTrianglesIndices);

            int indicesCount = resultTrianglesIndices.size();
            int trianglesCount = indicesCount / 3;
            for (int n = 0; n < trianglesCount; n++) {
                idx1Local = resultTrianglesIndices.get(n * 3);
                idx2Local = resultTrianglesIndices.get(n * 3 + 1);
                idx3Local = resultTrianglesIndices.get(n * 3 + 2);

                Vector3d point1 = polygon.get(idx1Local);
                Vector3d point2 = polygon.get(idx2Local);
                Vector3d point3 = polygon.get(idx3Local);

                int idx1 = pointsMap.get(point1);
                int idx2 = pointsMap.get(point2);
                int idx3 = pointsMap.get(point3);

                GaiaFace face = new GaiaFace();
                int[] indicesArray = new int[3];
                indicesArray[0] = idx1;
                indicesArray[1] = idx2;
                indicesArray[2] = idx3;
                face.setIndices(indicesArray);
                surface.getFaces().add(face);
            }
        }

        return primitive;
    }
}
