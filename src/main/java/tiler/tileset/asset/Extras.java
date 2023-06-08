package tiler.tileset.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Extras {
    private Ion ion;
    private Cesium cesium;
}
