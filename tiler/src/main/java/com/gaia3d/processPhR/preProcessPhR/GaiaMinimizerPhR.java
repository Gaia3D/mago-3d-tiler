package com.gaia3d.processPhR.preProcessPhR;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.halfEdgeStructure.HalfEdgeScene;
import com.gaia3d.basic.halfEdgeStructure.HalfEdgeUtils;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GaiaSceneUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class GaiaMinimizerPhR implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        if (scene != null) {
            log.info("Welding vertices in GaiaScene");

            // 1rst, must weld vertices.***
            boolean checkTexCoord = true;
            boolean checkNormal = false;
            boolean checkColor = false;
            boolean checkBatchId = false;
            double error = 1e-8;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);

//            // Test create a rectangularNet scene.***
//            boolean calculateTexCoords = true;
//            int numCols = 157;
//            int numRows = 214;
//            double width = 100;
//            double height = 100;
//            GaiaScene rectScene = GaiaSceneUtils.getSceneRectangularNet(numCols, numRows, width, height, calculateTexCoords);
//            GaiaNode rootNode = rectScene.getNodes().get(0);
//            GaiaNode node = rootNode.getChildren().get(0);
//            GaiaMesh mesh = node.getMeshes().get(0);
//            GaiaPrimitive primitive = mesh.getPrimitives().get(0);
//            Vector3d translate = new Vector3d(0, 0, 20);
//            primitive.translate(translate);
//            primitive.setMaterialIndex(0);
//            scene.getNodes().clear();
//            scene.getNodes().add(rootNode);
//            // End test.------------------------

            // Test.************************************************
//            GaiaNode rootNode = scene.getNodes().get(0);
//            List<GaiaNode> reducedChildren = new ArrayList<>();
//            int childrenCount = rootNode.getChildren().size();
//
//            reducedChildren.add(rootNode.getChildren().get(100));
//            rootNode.getChildren().clear();
//            rootNode.setChildren(reducedChildren);
            // End test.--------------------------------------------

            log.info("Making HalfEdgeScene from GaiaScene");
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            log.info("Doing triangles reduction in HalfEdgeScene");
            halfEdgeScene.doTrianglesReduction();

            log.info("Making GaiaScene from HalfEdgeScene");
            GaiaScene newScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
            halfEdgeScene.deleteObjects();

            GaiaSet tempSet = GaiaSet.fromGaiaScene(newScene);
            Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tempPath);
            if (tempSet != null) {
                tempSet.clear();
                tempSet = null;
            }

            if (scene != null) {
                scene.clear();
                scene = null;
            }

        }
        return tileInfo;
    }
}
