package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Content {
    private String uri = null;
    BoundingVolume boundingVolume;
}
