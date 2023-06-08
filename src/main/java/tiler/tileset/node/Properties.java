package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Properties {
    @JsonProperty("Height")
    private Range height;

    @JsonProperty("Latitude")
    private Range latitude;

    @JsonProperty("Longitude")
    private Range longitude;
}