package renderable;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;
import util.GeometryUtils;

import java.util.ArrayList;

public class BaseObject extends RenderableObject {
    RenderableBuffer renderableBuffer;
    float size;

    public BaseObject() {
        super();
        this.size = 1024.0f;
        this.setPosition(0.0f, 0.0f, -100.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        RenderableBuffer renderableBuffer = this.getBuffer();
        Matrix4f objectTransformMatrix = getTransformMatrix();
        int uObjectTransformMatrix = GL20.glGetUniformLocation(program, "uObjectTransformMatrix");
        float[] objectTransformMatrixBuffer = new float[16];
        objectTransformMatrix.get(objectTransformMatrixBuffer);
        GL20.glUniformMatrix4fv(uObjectTransformMatrix, false, objectTransformMatrixBuffer);
        int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
        int aVertexNormal = GL20.glGetAttribLocation(program, "aVertexNormal");
        int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");
        renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
        renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
        renderableBuffer.setAttribute(renderableBuffer.getNormalVbo(), aVertexNormal, 3, 0);
        renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);
        GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
    }
    @Override
    public RenderableBuffer getBuffer() {
        if (this.renderableBuffer == null) {
            RenderableBuffer renderableBuffer = new RenderableBuffer();
            ArrayList<Short> indicesList = new ArrayList<>();
            ArrayList<Float> positionList = new ArrayList<>();
            ArrayList<Float> normalList = new ArrayList<>();
            ArrayList<Float> colorList = new ArrayList<>();
            float size = this.size;
            int density = 16;
            float offset = (size * 2) / density;
            int checkPattern = 0;
            for (int x = 0; x < density; x++) {
                float startX = -size + (offset * x);
                float endX = -size + (offset * (x + 1));
                for (int y = 0; y < density; y++) {
                    float startY = -size + (offset * y);
                    float endY = -size + (offset * (y + 1));
                    positionList.add(startX);
                    positionList.add(startY);
                    positionList.add(0.0f);
                    positionList.add(endX);
                    positionList.add(startY);
                    positionList.add(0.0f);
                    positionList.add(endX);
                    positionList.add(endY);
                    positionList.add(0.0f);
                    positionList.add(startX);
                    positionList.add(endY);
                    positionList.add(0.0f);
                    Vector3d normal = GeometryUtils.calcNormal(new Vector3d(startX, startY, 0.0f), new Vector3d(endX, endY, 0.0f), new Vector3d(endX, startY, 0.0f));
                    normalList.add((float) normal.x);
                    normalList.add((float) normal.y);
                    normalList.add((float) normal.z);
                    normalList.add((float) normal.x);
                    normalList.add((float) normal.y);
                    normalList.add((float) normal.z);
                    normalList.add((float) normal.x);
                    normalList.add((float) normal.y);
                    normalList.add((float) normal.z);
                    normalList.add((float) normal.x);
                    normalList.add((float) normal.y);
                    normalList.add((float) normal.z);
                    for (int i = 0; i < 4; i++) {
                        if (checkPattern % 2 == 0) {
                            colorList.add(0.4f);
                            colorList.add(0.4f);
                            colorList.add(0.4f);
                            colorList.add(1.0f);
                        } else {
                            colorList.add(0.2f);
                            colorList.add(0.2f);
                            colorList.add(0.2f);
                            colorList.add(1.0f);
                        }
                    }
                    checkPattern++;
                }
                checkPattern++;
            }
            for (int i = 0; i < positionList.size() / 3; i++) {
                int indicesOffset = i * 4;
                indicesList.add((short) (indicesOffset));
                indicesList.add((short) (1 + indicesOffset));
                indicesList.add((short) (2 + indicesOffset));
                indicesList.add((short) (indicesOffset));
                indicesList.add((short) (2 + indicesOffset));
                indicesList.add((short) (3 + indicesOffset));
                checkPattern++;
            }
            int indicesVbo = renderableBuffer.createIndicesBuffer(indicesList);
            int positionVbo = renderableBuffer.createBuffer(positionList);
            int normalVbo = renderableBuffer.createBuffer(normalList);
            int colorVbo = renderableBuffer.createBuffer(colorList);
            renderableBuffer.setPositionVbo(positionVbo);
            renderableBuffer.setNormalVbo(normalVbo);
            renderableBuffer.setColorVbo(colorVbo);
            renderableBuffer.setIndicesVbo(indicesVbo);
            renderableBuffer.setIndicesLength(indicesList.size());
            this.renderableBuffer = renderableBuffer;
        }
        return this.renderableBuffer;
    }
}
