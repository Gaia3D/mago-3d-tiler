package com.gaia3d.basic.magogl.shader;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class MagoFragmentInput {

    public int x;
    public int y;

    public float depth;

    public final Vector3f worldPosition = new Vector3f();
    public final Vector3f normal = new Vector3f();
    public final Vector2f texCoord = new Vector2f();
    public final Vector4f color = new Vector4f(1.0f);
    public int faceCode; // no interpolable.
}