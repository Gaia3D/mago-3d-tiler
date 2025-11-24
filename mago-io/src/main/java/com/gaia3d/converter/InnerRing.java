package com.gaia3d.converter;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

import java.util.List;

@Getter
@Setter
public class InnerRing {
    private List<Vector2d> coordinates;
    private Vector2d leftDown;
    private double minValue;

    public InnerRing(List<Vector2d> coordinates) {
        this.coordinates = coordinates;
        this.leftDown = findLeftDown();
        this.minValue = leftDown.x + leftDown.y;
        this.coordinates = this.changeOrder(coordinates, coordinates.indexOf(leftDown));
    }

    public double cross(Vector2d a, Vector2d b, Vector2d c) {
        Vector2d ab = a.sub(b, new Vector2d());
        Vector2d bc = b.sub(c, new Vector2d());
        return cross(ab, bc);
    }

    public double cross(Vector2d a, Vector2d b) {
        return (a.x * b.y) - (a.y * b.x);
    }

    private Vector2d findLeftDown() {
        return coordinates.stream().sorted((a, b) -> {
            double positionA = a.x + a.y;
            double positionB = b.x + b.y;
            return Double.compare(positionA, positionB);
        }).findFirst().orElse(null);
    }

    public List<Vector2d> changeOrder(List<Vector2d> list, int index) {
        if (list.get(0).equals(list.get(list.size() - 1))) {
            list.remove(list.size() - 1);
        }

        List<Vector2d> result = list.subList(index, list.size());
        result.addAll(list.subList(0, index));

        if (!result.isEmpty()) {
            result.add(new Vector2d(result.get(0)));
        }
        return result;
    }
}
