package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exception.ReportLevel;
import com.gaia3d.basic.exception.Reporter;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.preprocess.sub.UpAxisTransformer;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@AllArgsConstructor
/**
 * Save only the essential information of the object as a file.
 */
public class GaiaZUpTransformer implements PreProcess {

    /*
     * Z-Up axis matrix:
     * 1.0, 0.0, 0.0,
     * 0.0, 1.0, 0.0,
     * 0.0, 0.0, 1.0
     */
    private final Matrix3d zUpAxisMatrix = new Matrix3d(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
    );

    /*
     * Y-Up axis matrix:
     * 1.0, 0.0, 0.0,
     * 0.0, 0.0, 1.0,
     * 0.0, -1.0, 0.0
     */
    private final Matrix3d yUpAxisMatrix = new Matrix3d(
            1.0, 0.0, 0.0,
            0.0, 0.0, -1.0,
            0.0, 1.0, 0.0
    );

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaScene scene = tileInfo.getScene();
        try {
            if (globalOptions.isParametric()) {
                return tileInfo;
            }

            Matrix3d rotationMatrix = createNormalMatrix3d(scene);
            boolean isZUp = isZUpAxis(rotationMatrix);
            boolean isYUp = isYUpAxis(rotationMatrix);
            if (isZUp) {
                return tileInfo;
            } else if (isYUp) {
                UpAxisTransformer.transformToZUp(scene);
                return tileInfo;
            } else {
                log.warn("[PRE] Scene is not in Z-Up or Y-Up orientation. {}", tileInfo.getName());

                // It is assumed that the scene is in Y-Up orientation.
                List<GaiaNode> nodes = scene.getNodes();
                for (GaiaNode node : nodes) {
                    Matrix4d originalTransform = node.getTransformMatrix();
                    Vector3d scale = originalTransform.getScale(new Vector3d());

                    Vector3d translation = originalTransform.getTranslation(new Vector3d());
                    Vector3d rotatedTranslation = new Vector3d(translation.x, -translation.z, translation.y);

                    Matrix4d transform = new Matrix4d();
                    transform.identity();
                    transform.translate(rotatedTranslation);
                    transform.scale(scale);
                    node.setTransformMatrix(transform);
                }
            }
        } catch (Exception e) {
            String message = "Failed to transform scene to Z-Up orientation: " + e.getMessage();
            log.error(message, e);
            Reporter reporter = GlobalOptions.getInstance().getReporter();
            reporter.addReport(message, ReportLevel.ERROR);
            throw new RuntimeException(message, e);
        }

        tileInfo.updateSceneInfo();
        return tileInfo;
    }


    private Matrix3d createNormalMatrix3d(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        GaiaNode rootNode = nodes.get(0);
        Matrix4d transformMatrix = rootNode.getTransformMatrix();

        Vector3d rotationX = new Vector3d(transformMatrix.m00(), transformMatrix.m01(), transformMatrix.m02());
        Vector3d rotationY = new Vector3d(transformMatrix.m10(), transformMatrix.m11(), transformMatrix.m12());
        Vector3d rotationZ = new Vector3d(transformMatrix.m20(), transformMatrix.m21(), transformMatrix.m22());
        rotationX.normalize();
        rotationY.normalize();
        rotationZ.normalize();

        Matrix3d rotationMatrix = new Matrix3d(
                rotationX.x, rotationY.x, rotationZ.x,
                rotationX.y, rotationY.y, rotationZ.y,
                rotationX.z, rotationY.z, rotationZ.z
        );
        rotationMatrix = clampEpsilonMatrix(rotationMatrix);
        return rotationMatrix;
    }

    private boolean isZUpAxis(Matrix3d rotationMatrix) {
        return zUpAxisMatrix.equals(rotationMatrix);
    }

    private boolean isYUpAxis(Matrix3d rotationMatrix) {
        return yUpAxisMatrix.equals(rotationMatrix);
    }

    private Matrix3d clampEpsilonMatrix(Matrix3d matrix) {
        double epsilon = 1e-1;
        Matrix3d clampedMatrix = new Matrix3d(matrix);
        clampedMatrix.m00(clampEpsilon(matrix.m00(), epsilon));
        clampedMatrix.m01(clampEpsilon(matrix.m01(), epsilon));
        clampedMatrix.m02(clampEpsilon(matrix.m02(), epsilon));

        clampedMatrix.m10(clampEpsilon(matrix.m10(), epsilon));
        clampedMatrix.m11(clampEpsilon(matrix.m11(), epsilon));
        clampedMatrix.m12(clampEpsilon(matrix.m12(), epsilon));

        clampedMatrix.m20(clampEpsilon(matrix.m20(), epsilon));
        clampedMatrix.m21(clampEpsilon(matrix.m21(), epsilon));
        clampedMatrix.m22(clampEpsilon(matrix.m22(), epsilon));

        return clampedMatrix;
    }

    public static double clampEpsilon(double value, double epsilon) {
        if (Math.abs(value) < epsilon) {
            return 0.0f;
        } else if (Math.abs(value - 1.0f) < epsilon) {
            return 1.0f;
        } else if (Math.abs(value + 1.0f) < epsilon) {
            return -1.0f;
        } else if (value > 1.0f) {
            return 1.0f;
        } else if (value < -1.0f) {
            return -1.0f;
        }
        return value;
    }

    public void printMatrix(Matrix3d matrix) {
        log.info("Matrix:");
        log.info("{} {} {}", matrix.m00(), matrix.m01(), matrix.m02());
        log.info("{} {} {}", matrix.m10(), matrix.m11(), matrix.m12());
        log.info("{} {} {}", matrix.m20(), matrix.m21(), matrix.m22());
    }
}
