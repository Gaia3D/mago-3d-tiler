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

    @JsonIgnore
    private Node parent;

    private List<Node> children;

    private BoundingVolume boundingVolume;
    private RefineType refine = RefineType.ADD;
    private float geometricError = 0.0f;
    private float[] transform;

    private Content content;

    public enum RefineType {
        ADD,
        REPLACE,
    }
}
