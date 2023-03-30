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
    private ArrayList<Integer> indices = new ArrayList<>();
    private ArrayList<Vertex> vertices = new ArrayList<>();
    private ArrayList<Surface> surfaces = new ArrayList<>();
}
