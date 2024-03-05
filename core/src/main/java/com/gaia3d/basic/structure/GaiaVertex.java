package com.gaia3d.basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;

/**
 * A class that represents a vertex of a Gaia object.
 * It contains the texture coordinates, position, normal, color, and batchId.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Vertex_(geometry)">Vertex (geometry)</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaVertex implements Serializable {
    private Vector2d texcoords;
    private Vector3d position;
    private Vector3d normal;
    private byte[] color;
    private float batchId;

    public void clear() {
        texcoords = null;
        position = null;
        normal = null;
        color = null;
        batchId = 0;
    }

    public GaiaVertex clone() {
        GaiaVertex vertex = new GaiaVertex();
        if (texcoords != null) {
            vertex.setTexcoords(new Vector2d(texcoords));
        }
        if (position != null) {
            vertex.setPosition(new Vector3d(position));
        }
        if (normal != null) {
            vertex.setNormal(new Vector3d(normal));
        }
        if (color != null) {
            vertex.setColor(color.clone());
        }
        if (batchId != 0) {
            vertex.setBatchId(batchId);
        }
        return vertex;
    }
}
