package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import tiler.BatchInfo;

@Getter
@Setter
public class Content {
    private String uri = null;
    BoundingVolume boundingVolume;

    @JsonIgnore
    private BatchInfo batchInfo = null;
}
