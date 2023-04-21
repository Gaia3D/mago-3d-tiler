package geometry.structure;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaFace {
    private ArrayList<Integer> indices = new ArrayList<>();
    private Vector3d faceNormal = new Vector3d();

    public void calculateFaceNormal(List<GaiaVertex> vertices) {
        if (indices.size() < 3) {
            System.err.println("[calculateFaceNormal Error] : indices.size() < 3");
            return;
        }

        for (int i = 0; i < indices.size(); i+=3) {
            int indices1 = indices.get(i);
            int indices2 = indices.get(i + 1);
            int indices3 = indices.get(i + 2);
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);

            Vector3d normal1 = vertex1.getNormal();
            Vector3d normal2 = vertex2.getNormal();
            Vector3d normal3 = vertex3.getNormal();

            boolean isNull = normal1 == null || normal2 == null || normal3 == null;
            boolean isZero = normal1.length() != 1.0f || normal2.length() != 1.0f || normal3.length() != 1.0f;
            isZero = false;
            if (true) {
                calcNormal(vertex1, vertex2, vertex3);
            }
        }

        Vector3d firstNormal = vertices.get(0).getNormal();
        this.faceNormal = new Vector3d(firstNormal);
    }

    public boolean validateNormal(Vector3d normal) {
        boolean result = true;
        if (Double.isNaN(normal.lengthSquared()) || Double.isNaN(normal.x()) || Double.isNaN(normal.y()) || Double.isNaN(normal.z()) || Float.isNaN((float) normal.x()) || Float.isNaN((float) normal.y()) || Float.isNaN((float) normal.z())) {
            System.err.println("[calcNormal Error] : " + normal.x() + ", " + normal.y() + ", " + normal.z());
            result = false;
        } else if (normal.lengthSquared() != 1.0f) {
            System.out.println("[calcNormal Wraning] : " + normal.lengthSquared());
            //result = false;
        } else {
            System.out.println("[calcNormal Correct] : " + normal.x() + ", " + normal.y() + ", " + normal.z());
        }
        return result;
    }

    public static Vector3d calcNormal(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP1 = new Vector3d(p3).sub(p1);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP1);
        normal.normalize();
        return normal;
    }

    public void calcNormal(GaiaVertex vertex1, GaiaVertex vertex2, GaiaVertex vertex3) {
        Vector3d position1 = vertex1.getPosition();
        Vector3d position2 = vertex2.getPosition();
        Vector3d position3 = vertex3.getPosition();

        //Vector3d vector1 = new Vector3d(position2);
        //Vector3d vector2 = new Vector3d(position3);
        //Vector3d resultNormal = new Vector3d();

        Vector3d resultNormal = calcNormal(position1, position2, position3);

//        position2.sub(position1, vector1);
//        position3.sub(position1, vector2);
//        vector2.cross(vector1, resultNormal);
//        resultNormal.normalize();

        if (!validateNormal(resultNormal)) {
            /*if (position1.equals(position2) || position1.equals(position3) || position2.equals(position3)) {
                System.err.println(" ======= position is same.");
            } else {
                System.err.println(" ======= other error. " + position1 + ", " + position2 + ", " + position3);
            }*/
            System.err.print(" ======= " + resultNormal + " -> ");
            resultNormal = new Vector3d(1.0, 1.0, 1.0);
            resultNormal.normalize();
            System.err.println(resultNormal);
        }
        vertex1.setNormal(new Vector3d(resultNormal));
        vertex2.setNormal(new Vector3d(resultNormal));
        vertex3.setNormal(new Vector3d(resultNormal));
    }
}
