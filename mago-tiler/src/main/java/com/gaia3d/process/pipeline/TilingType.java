package com.gaia3d.process.pipeline;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TilingType {
    UNKNOWN,
    BATCHING,
    GPU_INSTANCING,
    POINTCLOUD,
    PHOTOGRAMMETRY
}
