package com.gaia3d.renderer.engine.graph;

import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.RenderableTexturesUtils;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.renderable.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

@SuppressWarnings("ALL")
@Getter
@Setter
@Slf4j
public class RenderEngine {
    private RenderableBasicAxis renderableBasicAxis;
    private boolean renderWireFrame = false;
    private int colorMode = 2; // 0 = oneColor, 1 = vertexColor, 2 = textureColor

    public RenderEngine() {
        renderableBasicAxis = new RenderableBasicAxis();
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

    public void renderColorCoded(RenderableGaiaScene renderableGaiaScene, SelectionColorManager selectionColorManager, ShaderProgram shaderProgram) {
        if (shaderProgram == null) {
            return;
        }
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();

        // disable blending
        GL20.glDisable(GL20.GL_BLEND);

        Matrix4f identityMatrix = new Matrix4f();
        identityMatrix.identity();

        uniformsMap.setUniformMatrix4fv("uObjectMatrix", identityMatrix);

        List<RenderableNode> renderableNodes = renderableGaiaScene.getRenderableNodes();
        for (RenderableNode renderableNode : renderableNodes) {
            renderColorCodedNode(renderableNode, selectionColorManager, shaderProgram);
        }

        // return blending
        GL20.glEnable(GL20.GL_BLEND);
    }

    private void renderColorCodedNode(RenderableNode renderableNode, SelectionColorManager selectionColorManager, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        Matrix4d transformMatrix = renderableNode.getPreMultipliedTransformMatrix();
        Matrix4f identityMatrix = new Matrix4f();
        identityMatrix.identity();

        //uniformsMap.setUniformMatrix4fv("uObjectMatrix", new Matrix4f(transformMatrix));
        // note : use "identityMatrix" because the renderablePrimitive has transformed vertices
        uniformsMap.setUniformMatrix4fv("uObjectMatrix", identityMatrix);

        List<RenderableMesh> renderableMeshes = renderableNode.getRenderableMeshes();
        for (RenderableMesh renderableMesh : renderableMeshes) {
            renderColorCodedMesh(renderableMesh, selectionColorManager, shaderProgram);
        }

        // check for children
        List<RenderableNode> children = renderableNode.getChildren();
        for (RenderableNode child : children) {
            renderColorCodedNode(child, selectionColorManager, shaderProgram);
        }

    }

    private void renderColorCodedMesh(RenderableMesh renderableMesh, SelectionColorManager selectionColorManager, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();

        List<RenderablePrimitive> renderablePrimitives = renderableMesh.getRenderablePrimitives();
        for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
            GaiaMaterial material = renderablePrimitive.getMaterial();

            int color = selectionColorManager.getAvailableColor();
            byte[] encodedColor = new byte[4];
            selectionColorManager.getEncodedColor4(color, encodedColor);

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);

            int currColor = renderablePrimitive.getColorCode();

            if (renderablePrimitive.getStatus() == 1) {
                // this object is exterior object.
                currColor = -1; // inside the shader, -1 = background color (white).
            }

            uniformsMap.setUniform1i("uColorCode", currColor);

            // bind only "POSITION" & "INDICE" buffer ("INDICE" if exist).
            Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer = renderablePrimitive.getMapAttribTypeRenderableBuffer();
            AttributeType attributeType = AttributeType.POSITION;
            RenderableBuffer posBuffer = mapAttribTypeRenderableBuffer.get(attributeType);
            bindBuffer(posBuffer, shaderProgram);


            RenderableBuffer indicesBuffer = mapAttribTypeRenderableBuffer.get(AttributeType.INDICE);
            if (indicesBuffer == null) {
                // use glDrawArrays.
                GL20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
                GL20.glPolygonOffset(1.0f, 1.0f);
                GL20.glDrawArrays(GL_LINE_STRIP, 0, 16);
                return;
            }
            int elemsCount = indicesBuffer.getElementsCount();
            int type = indicesBuffer.getGlType();

            bindBuffer(indicesBuffer, shaderProgram);

            // enable face cull
            GL20.glEnable(GL20.GL_CULL_FACE);
            GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, type, 0);

        }
    }

    private void renderGaiaScene(RenderableGaiaScene renderableGaiaScene, ShaderProgram shaderProgram) {
        List<RenderableNode> renderableNodes = renderableGaiaScene.getRenderableNodes();
        for (RenderableNode renderableNode : renderableNodes) {
            renderGaiaNode(renderableNode, shaderProgram);
        }
    }

    private void renderGaiaNode(RenderableNode renderableNode, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        Matrix4d transformMatrix = renderableNode.getPreMultipliedTransformMatrix();
        if (transformMatrix == null) {
            transformMatrix = new Matrix4d();
            transformMatrix.identity();
        }
        Matrix4f tMatF = new Matrix4f();
        tMatF.set(transformMatrix);

        //uniformsMap.setUniformMatrix4fv("uObjectMatrix", new Matrix4f(transformMatrix));
        // note : use "identityMatrix" because the renderablePrimitive has transformed vertices.
        uniformsMap.setUniformMatrix4fv("uObjectMatrix", tMatF);

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


    private boolean checkGlError() {
        int glError = GL20.glGetError();
        if (glError != GL20.GL_NO_ERROR) {
            log.error("[ERROR] glError: " + glError);
            return true;
        }
        return false;
    }

    public void renderAxis(ShaderProgram shaderProgram) {
        Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer = renderableBasicAxis.getMapAttribTypeRenderableBuffer();
        for (Map.Entry<AttributeType, RenderableBuffer> entry : mapAttribTypeRenderableBuffer.entrySet()) {
            AttributeType attributeType = entry.getKey();
            RenderableBuffer renderableBuffer = entry.getValue();
            bindBuffer(renderableBuffer, shaderProgram);
        }

        GL20.glLineWidth(5.0f);

        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        uniformsMap.setUniform1i("uColorMode", 0);
        uniformsMap.setUniform4fv("uOneColor", new Vector4f(1, 0, 0, 1));
        GL20.glDrawArrays(GL20.GL_LINES, 0, 2);

        uniformsMap.setUniform4fv("uOneColor", new Vector4f(0, 1, 0, 1));
        GL20.glDrawArrays(GL20.GL_LINES, 2, 2);

        uniformsMap.setUniform4fv("uOneColor", new Vector4f(0, 0, 1, 1));
        GL20.glDrawArrays(GL20.GL_LINES, 4, 2);

        GL20.glLineWidth(1.0f);
    }

    private double linearToSrgb(double linear) {
        if (linear <= 0.0031308) {
            return linear * 12.92;
        } else {
            return (1.055 * Math.pow(linear, 1.0/2.4) - 0.055);
        }
    }

    private void renderGaiaMesh(RenderableMesh renderableMesh, ShaderProgram shaderProgram) {
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();

        List<RenderablePrimitive> renderablePrimitives = renderableMesh.getRenderablePrimitives();
        for (RenderablePrimitive renderablePrimitive : renderablePrimitives) {
            GaiaMaterial material = renderablePrimitive.getMaterial();

            if (material == null) {
                material = new GaiaMaterial();
                material.setDiffuseColor(new Vector4d(0.2, 0.95, 0.2, 1.0));
            }
            int status = renderablePrimitive.getStatus();

            boolean textureBinded = false;
            if (this.colorMode == 2) {
                // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
                uniformsMap.setUniform1i("uColorMode", 0);
                Map<TextureType, List<GaiaTexture>> mapTextures = material.getTextures();
                if (mapTextures.containsKey(TextureType.DIFFUSE)) {
                    List<GaiaTexture> diffuseTextures = mapTextures.get(TextureType.DIFFUSE);
                    if (!diffuseTextures.isEmpty()) {
                        GaiaTexture diffuseTexture = diffuseTextures.get(0);
                        if (diffuseTexture.getTextureId() < 0) {
                            int minFilter = GL_LINEAR; // GL_LINEAR, GL_NEAREST
                            int magFilter = GL_LINEAR;
                            int wrapS = GL_REPEAT; // GL_CLAMP_TO_EDGE
                            int wrapT = GL_REPEAT;
                            BufferedImage bufferedImage = diffuseTexture.getBufferedImage();
                            if (bufferedImage == null) {
                                log.error("[ERROR] bufferedImage is null.");
                                continue;
                            }
                            int textureId = RenderableTexturesUtils.createGlTextureFromBufferedImage(bufferedImage, minFilter, magFilter, wrapS, wrapT, true);

                            diffuseTexture.setTextureId(textureId);
                            diffuseTexture.setBufferedImage(null);
                        }

                        // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
                        uniformsMap.setUniform1i("uColorMode", 2);
                        GL30.glActiveTexture(GL30.GL_TEXTURE0);
                        GL30.glBindTexture(GL30.GL_TEXTURE_2D, diffuseTexture.getTextureId());
                        textureBinded = true;
                    }
                }

                // normal textures if exist
                if (mapTextures.containsKey(TextureType.NORMALS)) {
                    List<GaiaTexture> normalTextures = mapTextures.get(TextureType.NORMALS);
                    if (!normalTextures.isEmpty()) {
                        GaiaTexture normalTexture = normalTextures.get(0);
                        if (normalTexture.getTextureId() < 0) {
                            int minFilter = GL_LINEAR; // GL_LINEAR, GL_NEAREST
                            int magFilter = GL_LINEAR;
                            int wrapS = GL_REPEAT; // GL_CLAMP_TO_EDGE
                            int wrapT = GL_REPEAT;
                            BufferedImage bufferedImage = normalTexture.getBufferedImage();
                            if (bufferedImage == null) {
                                log.error("[ERROR] bufferedImage is null.");
                                continue;
                            }
                            int textureId = RenderableTexturesUtils.createGlTextureFromBufferedImage(bufferedImage, minFilter, magFilter, wrapS, wrapT, true);

                            normalTexture.setTextureId(textureId);
                            normalTexture.setBufferedImage(null);
                        }

                        // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
                        uniformsMap.setUniform1i("uColorMode", 2);
                        GL30.glActiveTexture(GL20.GL_TEXTURE1);
                        GL30.glBindTexture(GL20.GL_TEXTURE_2D, normalTexture.getTextureId());
                        textureBinded = true;
                        // set up uniform to disable normal mapping
                        uniformsMap.setUniform1i("uUseNormalMap", 1);
                    }
                }
                else {
                    // set up uniform to disable normal mapping
                    uniformsMap.setUniform1i("uUseNormalMap", 0);
                }
            }

            if (this.colorMode == 0) {
                uniformsMap.setUniform1i("uColorMode", 0);
                uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.9f, 0.3f, 0.6f, 1.0f));
            } else if (this.colorMode == 1) {
                uniformsMap.setUniform1i("uColorMode", 1);
            } else if (this.colorMode == 2) {
                uniformsMap.setUniform1i("uColorMode", 2);
            }

            if (!textureBinded) {
                // get diffuse color from material
                Vector4d diffuseColor = material.getDiffuseColor();
                Vector4d srgb = new Vector4d();
                srgb.x = linearToSrgb(diffuseColor.x);
                srgb.y = linearToSrgb(diffuseColor.y);
                srgb.z = linearToSrgb(diffuseColor.z);
                diffuseColor = srgb;

                uniformsMap.setUniform1i("uColorMode", 0);
                uniformsMap.setUniform4fv("uOneColor", new Vector4f((float) srgb.x, (float) srgb.y, (float) srgb.z, (float) srgb.w));
            }

            if (status == 1) {
                // this object is exterior object.
//                uniformsMap.setUniform1i("uColorMode", 0);
//                uniformsMap.setUniform4fv("uOneColor", new Vector4f(1.0f, 0.0f, 1.0f, 1.0f));
                //continue;
            }

            if (status == 0) {
                // this object is interior object.
                //continue;
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
            if (renderableBuffer == null) {
                // use glDrawArrays.
                GL20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
                GL20.glPolygonOffset(1.0f, 1.0f);
                uniformsMap.setUniform1i("uColorMode", 0);
                uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
                GL20.glDrawArrays(GL_LINE_STRIP, 0, 16);
                return;
            }
            int elemsCount = renderableBuffer.getElementsCount();
            int type = renderableBuffer.getGlType();

            GL20.glPointSize(10.0f);
            GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, type, 0);

            // render wireframe
            if (renderWireFrame) {
                uniformsMap.setUniform1i("uColorMode", 0);
                uniformsMap.setUniform4fv("uOneColor", new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
                GL20.glPolygonMode(GL20.GL_FRONT_AND_BACK, GL20.GL_LINE);
                // do offset
                GL20.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
                GL20.glPolygonOffset(1.0f, 1.0f);
                GL20.glDrawElements(GL20.GL_TRIANGLES, elemsCount, type, 0);
            }

//            // render the 1rst point of the primitive
//            GL20.glPointSize(10.0f);
//            GL20.glDrawArrays(GL20.GL_POINTS, 0, 1);


            // return polygonMode to fill
            GL30.glPolygonMode(GL20.GL_FRONT_AND_BACK, GL20.GL_FILL);
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
            int location = shaderProgram.enableAttribLocation("aPosition");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        } else if (attributeType == AttributeType.NORMAL) {
            int location = shaderProgram.enableAttribLocation("aNormal");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        } else if (attributeType == AttributeType.COLOR) {
            int location = shaderProgram.enableAttribLocation("aColor");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, true, 0, 0);
            }
        } else if (attributeType == AttributeType.TEXCOORD) {
            int location = shaderProgram.enableAttribLocation("aTexCoord");
            if (location >= 0) {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId);
                GL20.glVertexAttribPointer(location, glDimension, glType, false, 0, 0);
            }
        } else if (attributeType == AttributeType.INDICE) {
            GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vboId);
        }

    }
}
