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
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@AllArgsConstructor
/**
 * Save only the essential information of the object as a file.
 */
public class GaiaZUpTransformer implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaScene scene = tileInfo.getScene();
        try {
            if (globalOptions.isAlreadyZUp()) {
                return tileInfo;
            }

            if (isZUpAxis(scene)) {
                return tileInfo; // Already in Z-Up orientation
            }
            UpAxisTransformer.transformToZUp(scene);
        } catch (Exception e) {
            String message = "Failed to transform scene to Z-Up orientation: " + e.getMessage();
            log.error(message, e);
            Reporter reporter = GlobalOptions.getInstance().getReporter();
            reporter.addReport(message, ReportLevel.ERROR);
            throw new RuntimeException(message, e);
        }
        return tileInfo;
    }

    private boolean isZUpAxis(GaiaScene scene) {
        boolean isUpAxisZ = true;
        List<GaiaNode> nodes = scene.getNodes();
        GaiaNode rootNode = nodes.get(0);
        Matrix4d transformMatrix = rootNode.getTransformMatrix();
        Matrix3d rotationMatrix = new Matrix3d(transformMatrix);
        rotationMatrix.normal();

        Matrix3d zUpAxisMatrix = new Matrix3d();
        zUpAxisMatrix.identity();

        isUpAxisZ = zUpAxisMatrix.equals(rotationMatrix, 0.1);
        return isUpAxisZ;
    }
}
