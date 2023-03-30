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
    private Node parent = null;
    private ArrayList<Mesh> meshes = new ArrayList<>();
    private ArrayList<Node> children = new ArrayList<>();
    //tm double[16]
}
