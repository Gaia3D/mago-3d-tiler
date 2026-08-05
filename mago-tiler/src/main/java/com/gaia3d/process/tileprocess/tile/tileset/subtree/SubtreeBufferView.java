package com.gaia3d.process.tileprocess.tile.tileset.subtree;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubtreeBufferView {
    private Integer buffer;
    private Integer byteOffset;
    private Integer byteLength;

    public SubtreeBufferView() {
    }

    public SubtreeBufferView(int buffer, int byteOffset, int byteLength) {
        this.buffer = buffer;
        this.byteOffset = byteOffset;
        this.byteLength = byteLength;
    }
}
