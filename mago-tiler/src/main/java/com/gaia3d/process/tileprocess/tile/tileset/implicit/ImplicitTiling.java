package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImplicitTiling {
    private SubdivisionScheme subdivisionScheme;
    private int availableLevels;
    private int subtreeLevels;
    private TemplateUri subtrees;
}
