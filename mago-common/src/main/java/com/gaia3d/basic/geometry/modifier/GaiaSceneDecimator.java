package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.geometry.modifier.halfedge.HalfEdgeDecimator;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.model.GaiaScene;
import lombok.extern.slf4j.Slf4j;

/**
 * General-purpose edge-collapse mesh decimator that operates directly on a {@link GaiaScene}.
 * <p>
 * This is a thin, reusable convenience wrapper around the existing half-edge based
 * {@link HalfEdgeDecimator}: it converts the GaiaScene to a HalfEdgeScene, runs the
 * edge-collapse decimation (which preserves creases via
 * {@link DecimateParameters#getMaxDiffAngDegrees()} and per-vertex attributes such as color),
 * and converts the result back to a GaiaScene.
 * <p>
 * It was introduced for the marching-cubes iso-surface pipeline but is intentionally generic
 * so any pipeline that produces a {@link GaiaScene} can reuse it.
 */
@Slf4j
public class GaiaSceneDecimator {

    /**
     * Decimates the given scene using the supplied parameters and returns a new, decimated scene.
     * The input scene is left untouched.
     *
     * @param gaiaScene          the scene to decimate (may be {@code null})
     * @param decimateParameters edge-collapse parameters
     * @return the decimated scene, or {@code null} if the input was {@code null}
     */
    public GaiaScene decimate(GaiaScene gaiaScene, DecimateParameters decimateParameters) {
        if (gaiaScene == null) {
            return null;
        }
        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
        HalfEdgeDecimator halfEdgeDecimator = new HalfEdgeDecimator(decimateParameters);
        halfEdgeDecimator.apply(halfEdgeScene);
        return HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
    }
}
