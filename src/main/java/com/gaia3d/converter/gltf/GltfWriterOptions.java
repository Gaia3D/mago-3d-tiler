package com.gaia3d.converter.gltf;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.util.function.Function;

@Setter
@Getter
@Builder
public class GltfWriterOptions {
    @Builder.Default
    private boolean isUseQuantization = false;
    @Builder.Default
    private boolean isUseByteNormal = false;
    @Builder.Default
    private boolean isUseShortNormal = false;
    @Builder.Default
    private boolean isUseShortTexCoord = false;
    @Builder.Default
    private boolean isUriImage = false;
    @Builder.Default
    private boolean isForceJpeg = false;
    @Builder.Default
    private boolean isDoubleSided = false;
    @Builder.Default
    private boolean isUnlit = false;
    // encodes embedded images as KTX2 (KHR_texture_basisu); returns null to keep the original format. null disables it
    @Builder.Default
    private Function<BufferedImage, byte[]> ktx2Encoder = null;
}
