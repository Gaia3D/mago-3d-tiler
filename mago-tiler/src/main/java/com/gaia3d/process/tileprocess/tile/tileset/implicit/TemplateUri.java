package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateUri {
    private String uri;

    public TemplateUri() {
    }

    public TemplateUri(String uri) {
        this.uri = uri;
    }
}
