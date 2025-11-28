package com.gaia3d.converter.pointcloud;

import com.gaia3d.util.GlobeUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
    private CoordinateReferenceSystem sourceCrs = GlobeUtils.wgs84;
    private Path tempDirectory;
}
