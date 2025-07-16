package com.gaia3d.converter.assimp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AssimpConverterOptions {
    private boolean isSplitByNode = false;
    private boolean isGenerateNormals = true;
}
