package process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import process.tileprocess.tile.ContentInfo;

@Getter
@Setter
public class Content {
    private String uri = null;
    BoundingVolume boundingVolume;

    @JsonIgnore
    private ContentInfo contentInfo = null;
}
