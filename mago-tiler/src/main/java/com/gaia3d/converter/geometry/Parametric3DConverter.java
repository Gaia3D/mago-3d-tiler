package com.gaia3d.converter.geometry;

import com.gaia3d.converter.geometry.pipe.GaiaPipeLineString;
import com.gaia3d.converter.kml.TileTransformInfo;
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
    public List<TileTransformInfo> convertTileTransformInfo(SimpleFeatureImpl feature) {
        return null;
    }
}
