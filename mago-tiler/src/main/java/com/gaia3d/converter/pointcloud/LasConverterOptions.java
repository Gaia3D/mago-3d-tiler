package com.gaia3d.converter.pointcloud;

import com.gaia3d.util.GlobeUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.nio.file.Path;

@Getter
@Setter
@Builder
public class LasConverterOptions {
    @Builder.Default
    private int chunkSize = 100000;
    @Builder.Default
    private boolean force4ByteRgb = false;
    @Builder.Default
    private boolean forceCrs = false;
    @Builder.Default
    private CoordinateReferenceSystem sourceCrs = GlobeUtils.wgs84;
    @Builder.Default
    private float pointPercentage = 1.0f; // 1.0 means 100%
    //@Builder.Default
    //private float pointSpacing = 0.0f;
    private Path tempDirectory;
    @Builder.Default
    private Vector3d translation = new Vector3d(0.0, 0.0, 0.0);
}
