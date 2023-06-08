package tiler.tileset.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ion {
    private boolean georeferenced = true;
    private boolean movable = false;
}
