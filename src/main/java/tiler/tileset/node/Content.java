package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import tiler.ContentInfo;

@Getter
@Setter
public class Content {
    private String uri = null;
    BoundingVolume boundingVolume;

    @JsonIgnore
    private ContentInfo contentInfo = null;
}
