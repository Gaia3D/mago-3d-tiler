package com.gaia3d.converter.jgltf;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
@Setter
@NoArgsConstructor
public class ImageBuffer {
    int imageId = -1;
    int byteBufferLength = -1;
    ByteBuffer byteBuffer = null;
}
