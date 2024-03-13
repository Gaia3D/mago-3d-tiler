package com.gaia3d.engine.graph;

import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.engine.scene.Camera;
import com.gaia3d.renderable.*;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLUtil;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL45.glCreateTextures;

public class RenderEngine {

    public RenderEngine() {
    }

    public void render(GaiaScenesContainer gaiaScenesContainer, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        Matrix4f projectionMatrix = gaiaScenesContainer.getProjection().getProjMatrix();

        uniformsMap.setUniformMatrix4fv("uProjectionMatrix", projectionMatrix);
        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
        uniformsMap.setUniform1i("uColorMode", 0);
        Vector4f oneColor = new Vector4f(1, 0.5f, 0.25f, 1);
        uniformsMap.setUniform4fv("uOneColor", oneColor);

        int scenesCount = gaiaScenesContainer.getRenderableGaiaScenes().size();
        for (int i = 0; i < scenesCount; i++) {
            RenderableGaiaScene renderableGaiaScene = gaiaScenesContainer.getRenderableGaiaScenes().get(i);
            renderGaiaScene(renderableGaiaScene, shaderProgram);
        }
    }

    private void renderGaiaScene(RenderableGaiaScene renderableGaiaScene, ShaderProgram shaderProgram) {
        List<RenderableNode> renderableNodes = renderableGaiaScene.getRenderableNodess();
            for (RenderableNode renderableNode : renderableNodes) {
                renderGaiaNode(renderableNode, shaderProgram);
            }
    }

    private void renderGaiaNode(RenderableNode renderableNode, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        Matrix4d transformMatrix = renderableNode.getPreMultipliedTransformMatrix();
        uniformsMap.setUniformMatrix4fv("uObjectMatrix", new Matrix4f(transformMatrix));

        List<RenderableMesh> renderableMeshes = renderableNode.getRenderableMeshes();
        for (RenderableMesh renderableMesh : renderableMeshes) {
            renderGaiaMesh(renderableMesh, shaderProgram);
        }

        // check for children
        List<RenderableNode> children = renderableNode.getChildren();
        for (RenderableNode child : children) {
            renderGaiaNode(child, shaderProgram);
        }

    }


    private boolean checkGlError()
    {
        int glError = GL20.glGetError();
        if(glError != GL20.GL_NO_ERROR) {
            System.out.println("glError: " + glError);
            return true;
        }
        return false;
    }
    private void renderGaiaMesh(RenderableMesh renderableMesh, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();

        List<RenderablePrimitive> renderablePrimitives = renderableMesh.getRenderablePrimitives();
        for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
            GaiaMaterial material = renderablePrimitive.getMaterial();

            // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
            uniformsMap.setUniform1i("uColorMode", 0);
            Map<TextureType, List<GaiaTexture>> mapTextures = material.getTextures();
            if(mapTextures.containsKey(TextureType.DIFFUSE)) {
                List<GaiaTexture> diffuseTextures = mapTextures.get(TextureType.DIFFUSE);
                if(diffuseTextures.size() > 0) {
                    GaiaTexture diffuseTexture = diffuseTextures.get(0);
                    if(diffuseTexture.getTextureId() < 0)
                    {
                        BufferedImage bufferedImage = diffuseTexture.getBufferedImage();
                        int width = diffuseTexture.getWidth();
                        int height = diffuseTexture.getHeight();

                        // resize image to nearest power of two.***
                        int resizeWidth = width;
                        int resizeHeight = height;
                        resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
                        resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);
                        width = resizeWidth;
                        height = resizeHeight;
                        ImageResizer imageResizer = new ImageResizer();
                        bufferedImage = imageResizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);
                        // end resize image to nearest power of two.***


                        //format :
                        // 1 = TYPE_INT_RGB,
                        // 2 = TYPE_INT_ARGB,
                        // 3 = TYPE_INT_ARGB_PRE,
                        // 4 = TYPE_INT_BGR,
                        // 5 = TYPE_3BYTE_BGR,
                        // 6 = TYPE_4BYTE_ABGR,
                        // TYPE_4BYTE_ABGR_PRE,
                        // TYPE_BYTE_GRAY,
                        // TYPE_BYTE_BINARY,
                        // TYPE_BYTE_INDEXED,
                        // TYPE_USHORT_GRAY,
                        // TYPE_USHORT_565_RGB,
                        // TYPE_USHORT_555_RGB,
                        // TYPE_CUSTOM
                        int format = diffuseTexture.getFormat();
                        int glFormat = -1; // GL_RGB, GL_RGBA, GL_BGR, GL_BGRA
                        if(format == 0)
                        {
                            glFormat = GL_RGB;
                        }
                        else if(format == 1)
                        {
                            glFormat = GL_RGBA;
                        }
                        else if(format == 5)
                        {
                            glFormat = GL_RGB;
                        }
                        else if(format == 6)
                        {
                            glFormat = GL_RGBA;
                        }
                        else
                        {
                            int hola = 0;
                        }


                        byte[] rgbaByteArray = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

                        byte[] data = new byte[rgbaByteArray.length];
                        for(int i = 0; i < rgbaByteArray.length; i += 1) {
                            data[i] = rgbaByteArray[i];
                        }

                        ByteBuffer buffer = ByteBuffer.allocateDirect(rgbaByteArray.length);
                        buffer.put(data);
                        buffer.flip();
                        buffer.position(0);




                        int texLoc = shaderProgram.getUniformsMap().getUniformLocation("texture0");
                        int testuniformLocation = glGetUniformLocation(shaderProgram.getProgramId(), "texture0");

                        //diffuseTexture.flipImageY();

                        GL20.glEnable(GL20.GL_TEXTURE_2D);
                        GL20.glEnable(GL20.GL_BLEND);
                        GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                        GL20.glDisable(GL_CULL_FACE);

                        int textureId = GL20.glGenTextures();

                        GL20.glActiveTexture(GL20.GL_TEXTURE0);
                        GL20.glBindTexture(GL20.GL_TEXTURE_2D, textureId);
                        GL20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);

                        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST); // GL_LINEAR
                        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
                        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_REPEAT);
                        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_REPEAT);

                        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                                glFormat, GL_UNSIGNED_BYTE, buffer);

                        diffuseTexture.setTextureId(textureId);
                    }

                    // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
                    uniformsMap.setUniform1i("uColorMode", 2);
                    GL20.glEnable(GL20.GL_TEXTURE_2D);
                    GL20.glActiveTexture(GL20.GL_TEXTURE0);
                    GL20.glBindTexture(GL20.GL_TEXTURE_2D, diffuseTexture.getTextureId());
                }
            }

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);

            Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer = renderablePrimitive.getMapAttribTypeRenderableBuffer();
            for (Map.Entry<AttributeType, RenderableBuffer> entry : mapAttribTypeRenderableBuffer.entrySet()) {
                AttributeType attributeType = entry.getKey();
                RenderableBuffer renderableBuffer = entry.getValue();
                bindBuffer(renderableBuffer, shaderProgram);
            }

            RenderableBuffer renderableBuffer = mapAttribTypeRenderableBuffer.get(AttributeType.INDICE);
            int elemsCount = renderableBuffer.getElementsCount();
            int type = renderableBuffer.getGlType();

            GL20.glPointSize(10.0f);
            GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, type, 0);

            GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0); // unbind texture
        }
    }

    private void bindBuffer(RenderableBuffer renderableBuffer, ShaderProgram shaderProgram) {

        AttributeType attributeType = renderableBuffer.getAttributeType();
        int vboId = renderableBuffer.getVboId();
        int elementsCount = renderableBuffer.getElementsCount();
        byte glDimension = renderableBuffer.getGlDimension();
        int glType = renderableBuffer.getGlType();

        if (attributeType == AttributeType.POSITION) {
            int location = shaderProgram.enableVertexLocation("aPosition");
            if(location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        }
        else if (attributeType == AttributeType.NORMAL) {
//            int location = shaderProgram.enableVertexLocation("aNormal");
//            if(location >= 0) {
//                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
//                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
//            }
        }
        else if (attributeType == AttributeType.COLOR) {
//            int location = shaderProgram.enableVertexLocation("aColor");
//            if(location >= 0) {
//                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
//                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
//            }
        }
        else if (attributeType == AttributeType.TEXCOORD) {
            int location = shaderProgram.enableVertexLocation("aTexCoord");
            if(location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        }
        else if (attributeType == AttributeType.INDICE) {
            GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId);
        }

    }
}
