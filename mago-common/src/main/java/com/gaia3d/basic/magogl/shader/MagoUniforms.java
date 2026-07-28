package com.gaia3d.basic.magogl.shader;

import com.gaia3d.basic.magogl.texture.MagoTexture2D;
import com.gaia3d.basic.magogl.texture.MagoTextureFilter;
import com.gaia3d.basic.magogl.texture.MagoTextureWrap;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MagoUniforms {

    public final Matrix4f modelMatrix = new Matrix4f();
    public final Matrix4f viewMatrix = new Matrix4f();
    public final Matrix4f projectionMatrix = new Matrix4f();
    public final Matrix4f modelViewProjectionMatrix = new Matrix4f();

    public final Matrix3f normalMatrix = new Matrix3f();

    public final Vector3f cameraPosition = new Vector3f();
    public final Vector3f lightDirection = new Vector3f(0.0f, 0.0f, -1.0f);

    public MagoTexture2D diffuseTexture;
    public MagoTexture2D normalTexture;

    public MagoTextureWrap wrapS =
            MagoTextureWrap.CLAMP_TO_EDGE;

    public MagoTextureWrap wrapT =
            MagoTextureWrap.CLAMP_TO_EDGE;

    public MagoTextureFilter textureFilter = MagoTextureFilter.BILINEAR;

    /*
     * Como ya has comprobado que ahora funciona,
     * mantén esta convención.
     */
    public boolean invertTextureV = false;

    public float alphaCutoff = 0.0f;
    public float minimumAlpha = 0.0f;
}
