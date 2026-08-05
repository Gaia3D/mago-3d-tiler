package com.gaia3d.process.tileprocess.tile.tileset.subtree;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Subtree {
    private List<SubtreeBuffer> buffers;
    private List<SubtreeBufferView> bufferViews;
    private Availability tileAvailability;
    private List<Availability> contentAvailability;
    private Availability childSubtreeAvailability;
}
