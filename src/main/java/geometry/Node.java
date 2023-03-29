package geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private Node parent;
    private ArrayList<Mesh> meshes;
    private ArrayList<Node> children;
    //tm double[16] or
}
