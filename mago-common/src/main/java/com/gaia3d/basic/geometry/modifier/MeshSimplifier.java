package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.remesher.ReMeshParameters;
import org.joml.Matrix4d;

import java.util.List;

public class MeshSimplifier {
    //********************************
    // Simplify = decimate + remesh
    //********************************
    public MeshSimplifier() {
    }

    public void simplify(GaiaScene gaiaScene,
                         int lod,
                         DecimateParameters decimateParameters,
                         ReMeshParameters reMeshParams,
                         GaiaBoundingBox nodeBBox,
                         Matrix4d nodeTMatrix,
                         List<HalfEdgeScene> resultHalfEdgeScenes) {

    }
}
