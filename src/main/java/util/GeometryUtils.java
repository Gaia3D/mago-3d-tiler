package util;

import geometry.structure.*;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import geometry.types.AccessorType;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GeometryUtils {
    public static boolean isIdentity(float[] matrix) {
        return matrix[0] == 1 && matrix[1] == 0 && matrix[2] == 0 && matrix[3] == 0 &&
                matrix[4] == 0 && matrix[5] == 1 && matrix[6] == 0 && matrix[7] == 0 &&
                matrix[8] == 0 && matrix[9] == 0 && matrix[10] == 1 && matrix[11] == 0 &&
                matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1;
    }

    public static Vector3d calcNormal(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP1 = new Vector3d(p3).sub(p1);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP1);
        normal.normalize();
        return normal;
    }

    public static Vector3d calcNormal(GaiaVertex v1, GaiaVertex v2, GaiaVertex v3) {
        return calcNormal(v1.getPosition(), v2.getPosition(), v3.getPosition());
    }

    public static void genNormals(GaiaVertex v1, GaiaVertex v2, GaiaVertex v3) {
        Vector3d normal = calcNormal(v1, v2, v3);
        if (Double.isNaN(normal.x()) || Double.isNaN(normal.y()) || Double.isNaN(normal.z())) {
            System.out.println("NaN normal: " + normal.x() + ", " + normal.y() + ", " + normal.z());
        }
        v1.setNormal(normal);
        v2.setNormal(normal);
        v3.setNormal(normal);
    }

    private static Rectangle2D getTextureCoordinatesBoundingRectangle(GaiaPrimitive primitive) {
        Rectangle2D rect = new Rectangle2D.Double();
        for (GaiaVertex vertex : primitive.getVertices()) {
            Vector2d textureCoordinates = vertex.getTextureCoordinates();
            if (textureCoordinates.x < rect.getMinX()) {
                rect.setRect(textureCoordinates.x, rect.getMinY(), rect.getWidth(), rect.getHeight());
            }
            if (textureCoordinates.y < rect.getMinY()) {
                rect.setRect(rect.getMinX(), textureCoordinates.y, rect.getWidth(), rect.getHeight());
            }
            if (textureCoordinates.x > rect.getMaxX()) {
                rect.setRect(rect.getMinX(), rect.getMinY(), textureCoordinates.x, rect.getHeight());
            }
            if (textureCoordinates.y > rect.getMaxY()) {
                rect.setRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), textureCoordinates.y);
            }
        }
        return rect;
    }

    public static Float[] getMin(ArrayList<Float> numbers, AccessorType accessorType) {
        if (accessorType == AccessorType.SCALAR) {
            Float[] returnArray = new Float[1];
            float min = numbers.isEmpty() ? -1 : Collections.min(numbers);
            returnArray[0] = min;
            return returnArray;
        } else if (accessorType == AccessorType.VEC2) {
            Float[] returnArray = new Float[2];
            List<Float> xNumbers = new ArrayList<>();
            List<Float> yNumbers = new ArrayList<>();
            AtomicInteger atomicInteger = new AtomicInteger();
            numbers.stream().forEach((number) -> {
                int index = atomicInteger.getAndIncrement();
                int mod = index % 2;
                if (mod == 0) {
                    xNumbers.add(number);
                } else if (mod == 1) {
                    yNumbers.add(number);
                }
            });
            float minX = numbers.isEmpty() ? -1 : Collections.min(xNumbers);
            float minY = numbers.isEmpty() ? -1 : Collections.min(yNumbers);
            returnArray[0] = minX;
            returnArray[1] = minY;
            return returnArray;
        } else if (accessorType == AccessorType.VEC3) {
            Float[] returnArray = new Float[3];
            List<Float> xNumbers = new ArrayList<>();
            List<Float> yNumbers = new ArrayList<>();
            List<Float> zNumbers = new ArrayList<>();
            AtomicInteger atomicInteger = new AtomicInteger();
            numbers.stream().forEach((number) -> {
                int index = atomicInteger.getAndIncrement();
                int mod = index % 3;
                if (mod == 0) {
                    xNumbers.add(number);
                } else if (mod == 1) {
                    yNumbers.add(number);
                } else if (mod == 2) {
                    zNumbers.add(number);
                }
            });
            float minX = numbers.isEmpty() ? -1 : Collections.min(xNumbers);
            float minY = numbers.isEmpty() ? -1 : Collections.min(yNumbers);
            float minZ = numbers.isEmpty() ? -1 : Collections.min(zNumbers);
            returnArray[0] = minX;
            returnArray[1] = minY;
            returnArray[2] = minZ;
            return returnArray;
        } else if (accessorType == AccessorType.VEC4) {
            Float[] returnArray = new Float[4];
            List<Float> xNumbers = new ArrayList<>();
            List<Float> yNumbers = new ArrayList<>();
            List<Float> zNumbers = new ArrayList<>();
            List<Float> wNumbers = new ArrayList<>();
            AtomicInteger atomicInteger = new AtomicInteger();
            numbers.stream().forEach((number) -> {
                int index = atomicInteger.getAndIncrement();
                int mod = index % 4;
                if (mod == 0) {
                    xNumbers.add(number);
                } else if (mod == 1) {
                    yNumbers.add(number);
                } else if (mod == 2) {
                    zNumbers.add(number);
                } else if (mod == 3) {
                    wNumbers.add(number);
                }
            });
            float minX = numbers.isEmpty() ? -1 : Collections.min(xNumbers);
            float minY = numbers.isEmpty() ? -1 : Collections.min(yNumbers);
            float minZ = numbers.isEmpty() ? -1 : Collections.min(zNumbers);
            float minW = numbers.isEmpty() ? -1 : Collections.min(wNumbers);
            returnArray[0] = minX;
            returnArray[1] = minY;
            returnArray[2] = minZ;
            returnArray[3] = minW;
            return returnArray;
        }
        return null;
    }

    public static Short[] getMaxIndices(ArrayList<Short> numbers) {
        Short[] returnArray = new Short[1];
        short max = numbers.isEmpty() ? -1 : Collections.max(numbers);
        returnArray[0] = max;
        return returnArray;
    }

    public static Short[] getMinIndices(ArrayList<Short> numbers) {
        Short[] returnArray = new Short[1];
        short min = numbers.isEmpty() ? -1 : Collections.min(numbers);
        returnArray[0] = min;
        return returnArray;
    }

    public static Float[] getMax(ArrayList<Float> numbers, AccessorType accessorType) {
        if (accessorType == AccessorType.SCALAR) {
            Float[] returnArray = new Float[1];
            float max = numbers.isEmpty() ? -1 : Collections.max(numbers);
            returnArray[0] = max;
            return returnArray;
        } else if (accessorType == AccessorType.VEC2) {
            Float[] returnArray = new Float[2];
            List<Float> xNumbers = new ArrayList<>();
            List<Float> yNumbers = new ArrayList<>();
            AtomicInteger atomicInteger = new AtomicInteger();
            numbers.stream().forEach((number) -> {
                int index = atomicInteger.getAndIncrement();
                int mod = index % 2;
                if (mod == 0) {
                    xNumbers.add(number);
                } else if (mod == 1) {
                    yNumbers.add(number);
                }
            });
            float maxX = numbers.isEmpty() ? -1 : Collections.max(xNumbers);
            float maxY = numbers.isEmpty() ? -1 : Collections.max(yNumbers);
            returnArray[0] = maxX;
            returnArray[1] = maxY;
            return returnArray;
        } else if (accessorType == AccessorType.VEC3) {
            Float[] returnArray = new Float[3];
            List<Float> xNumbers = new ArrayList<>();
            List<Float> yNumbers = new ArrayList<>();
            List<Float> zNumbers = new ArrayList<>();
            AtomicInteger atomicInteger = new AtomicInteger();
            numbers.stream().forEach((number) -> {
                int index = atomicInteger.getAndIncrement();
                int mod = index % 3;
                if (mod == 0) {
                    xNumbers.add(number);
                } else if (mod == 1) {
                    yNumbers.add(number);
                } else if (mod == 2) {
                    zNumbers.add(number);
                }
            });
            float maxX = numbers.isEmpty() ? -1 : Collections.max(xNumbers);
            float maxY = numbers.isEmpty() ? -1 : Collections.max(yNumbers);
            float maxZ = numbers.isEmpty() ? -1 : Collections.max(zNumbers);
            returnArray[0] = maxX;
            returnArray[1] = maxY;
            returnArray[2] = maxZ;
            return returnArray;
        } else if (accessorType == AccessorType.VEC4) {
            Float[] returnArray = new Float[4];
            List<Float> xNumbers = new ArrayList<>();
            List<Float> yNumbers = new ArrayList<>();
            List<Float> zNumbers = new ArrayList<>();
            List<Float> wNumbers = new ArrayList<>();
            AtomicInteger atomicInteger = new AtomicInteger();
            numbers.stream().forEach((number) -> {
                int index = atomicInteger.getAndIncrement();
                int mod = index % 4;
                if (mod == 0) {
                    xNumbers.add(number);
                } else if (mod == 1) {
                    yNumbers.add(number);
                } else if (mod == 2) {
                    zNumbers.add(number);
                } else if (mod == 3) {
                    wNumbers.add(number);
                }
            });
            float maxX = numbers.isEmpty() ? -1 : Collections.max(xNumbers);
            float maxY = numbers.isEmpty() ? -1 : Collections.max(yNumbers);
            float maxZ = numbers.isEmpty() ? -1 : Collections.max(zNumbers);
            float maxW = numbers.isEmpty() ? -1 : Collections.max(wNumbers);
            returnArray[0] = maxX;
            returnArray[1] = maxY;
            returnArray[2] = maxZ;
            returnArray[3] = maxW;
            return returnArray;
        }
        return null;
    }


    public static GaiaScene sampleScene() {
        GaiaScene scene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        GaiaNode childNode = new GaiaNode();
        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = new GaiaPrimitive();

        GaiaVertex vertex1 = new GaiaVertex();
        vertex1.setPosition(new Vector3d(0.0, 0.0, 0.0));
        vertex1.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex2 = new GaiaVertex();
        vertex2.setPosition(new Vector3d(256.0, 0.0, 0.0));
        vertex2.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex3 = new GaiaVertex();
        vertex3.setPosition(new Vector3d(256.0, 256.0, 0.0));
        vertex3.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex4 = new GaiaVertex();
        vertex4.setPosition(new Vector3d(0.0, 256.0, 0.0));
        vertex4.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        primitive.getVertices().add(vertex1);
        primitive.getVertices().add(vertex2);
        primitive.getVertices().add(vertex3);
        primitive.getVertices().add(vertex4);

        primitive.getIndices().add(0);
        primitive.getIndices().add(1);
        primitive.getIndices().add(2);
        primitive.getIndices().add(0);
        primitive.getIndices().add(2);
        primitive.getIndices().add(3);

        mesh.getPrimitives().add(primitive);
        childNode.getMeshes().add(mesh);
        rootNode.getChildren().add(childNode);
        scene.getNodes().add(rootNode);
        return scene;
    }

    public static GaiaScene sampleTriangleScene() {
        GaiaScene scene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        GaiaNode childNode = new GaiaNode();
        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = new GaiaPrimitive();

        GaiaVertex vertex1 = new GaiaVertex();
        vertex1.setPosition(new Vector3d(0.0, 0.0, 0.0));
        vertex1.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex2 = new GaiaVertex();
        vertex2.setPosition(new Vector3d(256.0, 0.0, 0.0));
        vertex2.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex3 = new GaiaVertex();
        vertex3.setPosition(new Vector3d(256.0, 256.0, 0.0));
        vertex3.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        primitive.getVertices().add(vertex1);
        primitive.getVertices().add(vertex2);
        primitive.getVertices().add(vertex3);

        primitive.getIndices().add(0);
        primitive.getIndices().add(1);
        primitive.getIndices().add(2);

        mesh.getPrimitives().add(primitive);
        childNode.getMeshes().add(mesh);
        rootNode.getChildren().add(childNode);
        scene.getNodes().add(rootNode);
        return scene;
    }

    public static ProjCoordinate transform(CoordinateReferenceSystem source, CoordinateReferenceSystem target, Double x, Double y) {
        //CRSFactory factory = new CRSFactory();
        //CoordinateReferenceSystem grs80 = factory.createFromName("EPSG:5179");
        //CoordinateReferenceSystem wgs84 = factory.createFromName("EPSG:4326");
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, target);
        ProjCoordinate beforeCoord = new ProjCoordinate(x, y);
        ProjCoordinate afterCoord = new ProjCoordinate();
        transformer.transform(beforeCoord, afterCoord);

        //변환된 좌표
        //System.out.println(afterCoord.x + "," + afterCoord.y);
        return afterCoord;
    }
}
