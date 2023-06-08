package tiler.tileset.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Credit {
    private String html;
    private boolean showOnScreen = true;
}
