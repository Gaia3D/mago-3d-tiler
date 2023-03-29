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
public class Primitive {
    private ArrayList<Integer> indices;
    private ArrayList<Vertex> vertices;

    private ArrayList<Surface> surfaces;
}
