package com.gaia3d.converter;

import com.gaia3d.basic.geometry.parametric.GaiaExtrusionModel;
import com.gaia3d.basic.geometry.parametric.GaiaSurfaceModel;
import com.gaia3d.basic.pipe.GaiaPipeLineString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.feature.simple.SimpleFeatureImpl;

import java.util.List;

@Slf4j
@NoArgsConstructor
public class Parametric3DConverter {

    /**
     * Surface Polygon (Polygon with Z)
     * @param feature
     * @return
     */
    public List<GaiaSurfaceModel> convertSurfaceModel(SimpleFeatureImpl feature) {

        return null;
    }

    /**
     * Extrusion Polygon (for Building Footprint)
     * @param feature
     * @return
     */
    public List<GaiaExtrusionModel> convertExtrusionModel(SimpleFeatureImpl feature) {
        return null;
    }

    /**
     * For LineString (Pipeline)
     * @param feature
     * @return
     */
    public List<GaiaPipeLineString> convertPipeLineString(SimpleFeatureImpl feature) {
        return null;
    }

    /**
     * For I3DM (Point, Polygon, LineString)
     * @param feature
     * @return
     */
    public List<?> convertTileTransformInfo(SimpleFeatureImpl feature) {
        return null;
    }
}
