package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.structure.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HalfEdgeVertex {
    private Vector2d texcoords;
    private Vector3d position;
    private Vector3d normal;
    private byte[] color;
    private float batchId;
    private HalfEdge outingHalfEdge = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;

    public HalfEdgeVertex(GaiaVertex vertex)
    {
        copyFromGaiaVertex(vertex);
    }

    public void copyFromGaiaVertex(GaiaVertex vertex)
    {
        Vector3d position = vertex.getPosition();
        Vector3d normal = vertex.getNormal();
        Vector2d texcoords = vertex.getTexcoords();
        byte[] color = vertex.getColor();
        float batchId = vertex.getBatchId();

        this.position = new Vector3d(position);

        if (normal != null)
        {
            this.normal = new Vector3d(normal);
        }

        if (texcoords != null)
        {
            this.texcoords = new Vector2d(texcoords);
        }

        if (color != null)
        {
            this.color = color.clone();
        }

        this.batchId = batchId;
    }

}
