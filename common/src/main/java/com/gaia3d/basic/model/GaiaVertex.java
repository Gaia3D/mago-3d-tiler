package com.gaia3d.basic.model;

import com.gaia3d.basic.model.structure.VertexStructure;
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
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaVertex extends VertexStructure implements Serializable {
    private Vector3d position;
    private short[] quantizedPosition;
    private Vector3d normal;
    private Vector2d texcoords;
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

    public boolean isWeldable(GaiaVertex vertex2, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // 1rst, check position.
        double distance = position.distance(vertex2.position);
        if (distance > error) {
            return false;
        }

        // 2nd, check texCoord.
        if (checkTexCoord && texcoords != null && vertex2.texcoords != null) {
            double texCoordDist = texcoords.distance(vertex2.texcoords);
            if (texCoordDist > error) {
                return false;
            }
        }

        // 3rd, check normal.
        if (checkNormal && normal != null && vertex2.normal != null) {
            if (normal.distance(vertex2.normal) > error) {
                return false;
            }
        }

        // 4th, check color.
        if (checkColor && color != null && vertex2.color != null) {
            for (int i = 0; i < color.length; i++) {
                if (Math.abs(color[i] - vertex2.color[i]) > error) {
                    return false;
                }
            }
        }

        // 5th, check batchId.
        if (checkBatchId) {
            return batchId == vertex2.batchId;
        }
        return true;
    }
}
