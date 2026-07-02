package com.gaia3d.process.tileprocess.tile.tileset.subtree;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubtreeBuffer {
    private String uri;
    private int byteLength;

    public SubtreeBuffer() {
    }

    public SubtreeBuffer(String uri, int byteLength) {
        this.uri = uri;
        this.byteLength = byteLength;
    }
}
