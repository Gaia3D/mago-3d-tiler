package com.gaia3d.converter.assimp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AssimpConverterOptions {
    @Builder.Default
    private boolean isSplitByNode = false;
    @Builder.Default
    private boolean isGenerateNormals = true;
}
