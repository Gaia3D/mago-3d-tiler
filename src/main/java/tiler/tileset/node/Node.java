package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Node {
    @JsonIgnore
    private String nodeCode;

    private BoundingVolume boundingVolume;
    private float geometricError = 0.0f;
    private String refine = "ADD";
    private float[] transform;

    private Content content = null;
    private List<Node> children;
}
