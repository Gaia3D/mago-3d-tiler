package com.gaia3d.converter.gltf;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class GltfWriterOptions {
    @Builder.Default
    private boolean isUseQuantization = false;
    @Builder.Default
    private boolean isUseByteNormal = false;
    @Builder.Default
    private boolean isUseShortTexCoord = false;
    @Builder.Default
    private boolean isUriImage = false;
    @Builder.Default
    private boolean isForceJpeg = false;
    @Builder.Default
    private boolean isDoubleSided = false;
}
