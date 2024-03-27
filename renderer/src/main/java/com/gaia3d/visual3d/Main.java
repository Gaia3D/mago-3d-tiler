package com.gaia3d.visual3d;

import com.gaia3d.engine.IAppLogic;
import com.gaia3d.engine.dataStructure.GaiaScenesContainer;

import com.gaia3d.engine.*;
import com.gaia3d.engine.scene.Camera;

import javax.xml.bind.JAXBException;
import java.io.IOException;


public class Main implements IAppLogic {
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;
//    private Entity cubeEntity;
//    private Vector4f displInc = new Vector4f();
//    private float rotation;

    public static void main(String[] args) throws JAXBException, IOException {
        Main main = new Main();
        Engine gameEng = new Engine("MagoVisual3D", new Window.WindowOptions(), main);
        gameEng.run();
    }

    @Override
    public void cleanup() {
        // Nothing to be done yet
    }

    @Override
    public void init(Window window, GaiaScenesContainer gaiaScenesContainer) {
        // Nothing to be done yet
    }

    @Override
    public void input(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis) {
        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = gaiaScenesContainer.getCamera();
//        if (window.isKeyPressed(GLFW_KEY_W)) {
//            camera.moveForward(move);
//        } else if (window.isKeyPressed(GLFW_KEY_S)) {
//            camera.moveBackwards(move);
//        }
//        if (window.isKeyPressed(GLFW_KEY_A)) {
//            camera.moveLeft(move);
//        } else if (window.isKeyPressed(GLFW_KEY_D)) {
//            camera.moveRight(move);
//        }
//        if (window.isKeyPressed(GLFW_KEY_UP)) {
//            camera.moveUp(move);
//        } else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
//            camera.moveDown(move);
//        }
//
//        MouseInput mouseInput = window.getMouseInput();
//        if (mouseInput.isRightButtonPressed()) {
//            Vector2f displVec = mouseInput.getDisplVec();
//            camera.addRotation((float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY),
//                    (float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY));
//        }
    }

    @Override
    public void update(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis) {

    }

}
