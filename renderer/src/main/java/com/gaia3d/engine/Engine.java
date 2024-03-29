package com.gaia3d.engine;

//import com.gaia3d.converter.*;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.*;

import com.gaia3d.converter.geometry.OnlyHashEqualsVector3d;
import com.gaia3d.converter.geometry.indoorgml.IndoorGmlConverter;
import com.gaia3d.converter.geometry.tessellator.GaiaTessellator;
import com.gaia3d.converter.geometry.tessellator.Point2DTess;
import com.gaia3d.converter.geometry.tessellator.Polygon2DTess;
import com.gaia3d.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.engine.fbo.Fbo;
import com.gaia3d.engine.fbo.FboManager;
import com.gaia3d.engine.graph.RenderEngine;
import com.gaia3d.engine.graph.ShaderManager;
import com.gaia3d.engine.graph.ShaderProgram;
import com.gaia3d.engine.scene.Camera;
import com.gaia3d.engine.screen.ScreenQuad;
import com.gaia3d.renderable.RenderableGaiaScene;
import edu.stem.indoor.IndoorFeatures;
import edu.stem.space.*;
import lombok.Getter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static edu.stem.debug.Main.unmarshall;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;


@Getter
public class Engine {
    private Window window;
    private ShaderManager shaderManager;
    private RenderEngine renderer;
    private FboManager fboManager;
    private ScreenQuad screenQuad;

    private Camera camera;

    GaiaScenesContainer gaiaScenesContainer;

    private double midButtonXpos = 0;
    private double midButtonYpos = 0;
    private double leftButtonXpos = 0;
    private double leftButtonYpos = 0;
    private boolean leftButtonClicked = false;
    private boolean midButtonClicked = false;

    private boolean checkGlError()
    {
        int glError = GL20.glGetError();
        if(glError != GL20.GL_NO_ERROR) {
            System.out.println("glError: " + glError);
            return true;
        }
        return false;
    }

    public Engine(String windowTitle, Window.WindowOptions opts, IAppLogic appLogic) {
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });

    }

    private void resize() {
        //scene.resize(window.getWidth(), window.getHeight());
    }

    public void run() throws JAXBException, IOException {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // 창의 콜백을 해제하고 창을 삭제합니다.
        long windowHandle = window.getWindowHandle();
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        // GLFW를 종료하고 에러 콜백을 해제합니다.
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() throws JAXBException, IOException {
        // 에러 콜백을 설정합니다. System.err의 에러 메세지를 출력 기본으로 구현합니다.
        GLFWErrorCallback.createPrint(System.err).set();


        long windowHandle = window.getWindowHandle();
        // 마우스 위치 콜백
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (this.midButtonClicked) {
                Vector3d pivot = new Vector3d(0.0d,0.0d,-1.0d);
                float xoffset = (float) (this.midButtonXpos - xpos) * 0.01f;
                float yoffset = (float) (this.midButtonYpos - ypos) * 0.01f;
                camera.rotationOrbit(xoffset, yoffset, pivot);
            }
            this.midButtonXpos = xpos;
            this.midButtonYpos = ypos;

            if(this.leftButtonClicked)
            {
                // translate camera
                Vector3d translation = new Vector3d((xpos - this.leftButtonXpos) * 0.01f, (ypos - this.leftButtonYpos) * 0.01f, 0);
                //translation.y *= -1;
                camera.translate(translation);
            }

            this.leftButtonXpos = xpos;
            this.leftButtonYpos = ypos;
        });


        // 마우스 버튼 이벤트
        glfwSetMouseButtonCallback(windowHandle, (window, key, action, mode) -> {
            if (key == GLFW_MOUSE_BUTTON_3 && action == GLFW_PRESS) {
                this.midButtonClicked = true;
            } else if (key == GLFW_MOUSE_BUTTON_3 && action == GLFW_RELEASE) {
                this.midButtonClicked = false;
            }

            // check left button
            if (key == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                this.leftButtonClicked = true;
            } else if (key == GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE) {
                this.leftButtonClicked = false;
            }

        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            camera.moveFront((float)yoffset * 0.2f);
        });

        // 키보드 콜백 이벤트를 설정합니다. 키를 눌렀을 때, 누르고 있을 때, 떼었을 때에 따라 바꿔줍니다.
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }

            float rotationOffset = 0.1f;
            Vector3d pivot = new Vector3d(0.0d,0.0d,-1.0d);
            if (key == GLFW_KEY_W) {
                //camera.rotationOrbit(0, -rotationOffset, pivot);
                camera.moveFront(-0.1f);
            }
            if (key == GLFW_KEY_A) {
                camera.rotationOrbit(rotationOffset, 0, pivot);
            }
            if (key == GLFW_KEY_S) {
                camera.rotationOrbit(0, rotationOffset, pivot);
            }
            if (key == GLFW_KEY_D) {
                camera.rotationOrbit(-rotationOffset, 0, pivot);
            }
        });

        setupShader();

        renderer = new RenderEngine();
        camera = new Camera();
        fboManager = new FboManager();

        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        fboManager.createFbo("colorRender", windowWidth, windowHeight);

        screenQuad = new ScreenQuad();


        gaiaScenesContainer = new GaiaScenesContainer(windowWidth, windowHeight);
        gaiaScenesContainer.setCamera(camera);

        // Test load a 3d file.***
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\DC_library_del_3DS\\DC_library_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\CK_del_3DS\\CK_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\dogok_library_del_3DS\\dogok_library_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\Dongdaemoongu_center_del_3DS\\Dongdaemoongu_center_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\Edumuseum_del_150417_02_3DS\\Edumuseum_del_150417_02.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\gangbuk_cultur_del_3DS\\gangbuk_cultur_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\gangil_del_3DS\\gangil_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\gangnam_del_3DS\\gangnam_del.3ds";
//        String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\Gangseo_cultural_del_3DS\\Gangseo_cultural_del.3ds";
//
//        Converter assimpConverter = new AssimpConverter();
//        List<GaiaScene> gaiaScenes = assimpConverter.load(filePath);
//        RenderableGaiaScene renderableGaiaScene = InternDataConverter.getRenderableGaiaScene(gaiaScenes.get(0));
//        gaiaScenesContainer.addRenderableGaiaScene(renderableGaiaScene);
        // end test load a 3d file.***

        // tessellation test.***************
        List<Vector2d> point2dArray = new ArrayList<>();
        Vector2d pos;
//        pos = new Vector2d(73.7835386317617,	18.627465100050586);polygon.add(pos);
//        pos = new Vector2d(73.783540458349,	18.627454699201696);polygon.add(pos);
//        pos = new Vector2d(73.78357031516362,	18.627459465587727);polygon.add(pos);
//        pos = new Vector2d(73.78357223063935,	18.627448561081255);polygon.add(pos);
//        pos = new Vector2d(73.7835772262741,	18.627420123256442);polygon.add(pos);
//        pos = new Vector2d(73.78358236615054,	18.627390862242997);polygon.add(pos);
//        pos = new Vector2d(73.78358332934158,	18.627385376138328);polygon.add(pos);
//        pos = new Vector2d(73.78353552248302,	18.62737774390691);polygon.add(pos);
//        pos = new Vector2d(73.78351210002472,	18.627374004091774);polygon.add(pos);
//        pos = new Vector2d(73.7835095226704,	18.62738867702051);polygon.add(pos);
//        pos = new Vector2d(73.78350133489192,	18.627387369821314);polygon.add(pos);
//        pos = new Vector2d(73.78346781593767,	18.627382018582406);polygon.add(pos);
//        pos = new Vector2d(73.78344779810621,	18.627378823080118);polygon.add(pos);
//        pos = new Vector2d(73.78345081704364,	18.627361636366768);polygon.add(pos);
//        pos = new Vector2d(73.78342826373225,	18.62735803590075);polygon.add(pos);
//        pos = new Vector2d(73.78340821845012,	18.627354835698693);polygon.add(pos);
//        pos = new Vector2d(73.78339819297236,	18.6273532346749);polygon.add(pos);
//        pos = new Vector2d(73.78338394105451,	18.62735095931115);polygon.add(pos);
//        pos = new Vector2d(73.7833756917355,	18.62734964267222);polygon.add(pos);
//        pos = new Vector2d(73.78337465346952,	18.62735555480873);polygon.add(pos);
//        pos = new Vector2d(73.78337155746397,	18.627373181095436);polygon.add(pos);
//        pos = new Vector2d(73.78336437663198,	18.62741405695941);polygon.add(pos);
//        pos = new Vector2d(73.7833635873281,	18.627418549277973);polygon.add(pos);
//        pos = new Vector2d(73.78337438159902,	18.627420272748974);polygon.add(pos);
//        pos = new Vector2d(73.78339760902179,	18.62742398149997);polygon.add(pos);
//        pos = new Vector2d(73.78339463850719,	18.627440889299997);polygon.add(pos);
//        pos = new Vector2d(73.7833925165495,	18.627452969913488);polygon.add(pos);
//        pos = new Vector2d(73.78337968290091,	18.627450921498443);polygon.add(pos);
//        pos = new Vector2d(73.78337649110419,	18.6274690884467);polygon.add(pos);
//        pos = new Vector2d(73.78337456769249,	18.627480040792157);polygon.add(pos);
//        pos = new Vector2d(73.78339926075319,	18.627483982221126);polygon.add(pos);
//        pos = new Vector2d(73.78341442064699,	18.627486402616793);polygon.add(pos);
//        pos = new Vector2d(73.78343246255342,	18.62748928262552);polygon.add(pos);
//        pos = new Vector2d(73.78346650036404,	18.62749471674736);polygon.add(pos);
//        pos = new Vector2d(73.78349408868596,	18.62749912147804);polygon.add(pos);
//        pos = new Vector2d(73.78349288942928,	18.62750594615568);polygon.add(pos);
//        pos = new Vector2d(73.7834976820504,	18.62750671090361);polygon.add(pos);
//        pos = new Vector2d(73.78351658742346,	18.62750972932326);polygon.add(pos);
//        pos = new Vector2d(73.78351414044998,	18.627523662108914);polygon.add(pos);
//        pos = new Vector2d(73.78351905330928,	18.627524446610295);polygon.add(pos);
//        pos = new Vector2d(73.78353420374361,	18.62752686512809);polygon.add(pos);
//        pos = new Vector2d(73.78354616351167,	18.627528775047008);polygon.add(pos);
//        pos = new Vector2d(73.7835538057019,	18.627485264428543);polygon.add(pos);
//        pos = new Vector2d(73.78355076462998,	18.62748477938929);polygon.add(pos);
//        pos = new Vector2d(73.78353637068052,	18.627482481430665);polygon.add(pos);
//        pos = new Vector2d(73.78353560000107,	18.627482358070274);polygon.add(pos);
//        pos = new Vector2d(73.7835386317617,	18.627465100050586);polygon.add(pos);
//        pos = new Vector2d(73.78350026221025,	18.6274431500904);polygon.add(pos);
//        pos = new Vector2d(73.78350345393198,	18.627426057568453);polygon.add(pos);
//        pos = new Vector2d(73.78350351895,	18.627425711894233);polygon.add(pos);
//        pos = new Vector2d(73.78350501201479,	18.627425951977465);polygon.add(pos);
//        pos = new Vector2d(73.7835192865407,	18.62742824826238);polygon.add(pos);
//        pos = new Vector2d(73.78351863280012,	18.62743193902481);polygon.add(pos);
//        pos = new Vector2d(73.78351619549012,	18.627445712832266);polygon.add(pos);
//        pos = new Vector2d(73.78350026221025,	18.6274431500904);polygon.add(pos);
//        pos = new Vector2d(73.78344409837119,	18.627410963366728);polygon.add(pos);
//        pos = new Vector2d(73.78343996408256,	18.627432373716147);polygon.add(pos);
//        pos = new Vector2d(73.78342155358034,	18.627428944621744);polygon.add(pos);
//        pos = new Vector2d(73.78342565049117,	18.627407725603117);polygon.add(pos);
//        pos = new Vector2d(73.78344409837119,	18.627410963366728);polygon.add(pos);

        pos = new Vector2d(73.78392871829372,	18.619806951154604);point2dArray.add(pos);
        pos = new Vector2d(73.783983561916,	18.619792400862387);point2dArray.add(pos);
        pos = new Vector2d(73.78397946650888,	18.619778386018012);point2dArray.add(pos);
        pos = new Vector2d(73.78401138375489,	18.61976991818869);point2dArray.add(pos);
        pos = new Vector2d(73.7839112695151,	18.619427302343002);point2dArray.add(pos);
        pos = new Vector2d(73.78390515696168,	18.61940638419072);point2dArray.add(pos);
        pos = new Vector2d(73.78387681029746,	18.61941390456686);point2dArray.add(pos);
        pos = new Vector2d(73.78387236421938,	18.6193986892249);point2dArray.add(pos);
        pos = new Vector2d(73.78385060591371,	18.61940446205583);point2dArray.add(pos);
        pos = new Vector2d(73.78380377423255,	18.6194168863143);point2dArray.add(pos);
        pos = new Vector2d(73.783808612626,	18.619433445197902);point2dArray.add(pos);
        pos = new Vector2d(73.78377620449409,	18.61944204265881);point2dArray.add(pos);
        pos = new Vector2d(73.78378471026565,	18.619472159092744);point2dArray.add(pos);
        pos = new Vector2d(73.783878497593,	18.619804242821704);point2dArray.add(pos);
        pos = new Vector2d(73.7839082113956,	18.61979441073009);point2dArray.add(pos);
        pos = new Vector2d(73.78391418550595,	18.619810806161944);point2dArray.add(pos);
        pos = new Vector2d(73.78392871829372,	18.619806951154604);point2dArray.add(pos);
        pos = new Vector2d(73.78391743631656,	18.619739905713093);point2dArray.add(pos);
        pos = new Vector2d(73.78394177368295,	18.619733464581145);point2dArray.add(pos);
        pos = new Vector2d(73.78394827172019,	18.619755756952134);point2dArray.add(pos);
        pos = new Vector2d(73.7839238187249,	18.6197617970241);point2dArray.add(pos);
        pos = new Vector2d(73.78391743631656,	18.619739905713093);point2dArray.add(pos);
        pos = new Vector2d(73.78389685156762,	18.61966059014329);point2dArray.add(pos);
        pos = new Vector2d(73.78388809571848,	18.61962404991229);point2dArray.add(pos);
        pos = new Vector2d(73.78387920568343,	18.61962598318847);point2dArray.add(pos);
        pos = new Vector2d(73.78387177469259,	18.619627599677848);point2dArray.add(pos);
        pos = new Vector2d(73.78386484054785,	18.61959865910166);point2dArray.add(pos);
        pos = new Vector2d(73.78387134313283,	18.619597245354402);point2dArray.add(pos);
        pos = new Vector2d(73.78388075907348,	18.619595197099805);point2dArray.add(pos);
        pos = new Vector2d(73.78387176206402,	18.619557649555972);point2dArray.add(pos);
        pos = new Vector2d(73.78389034616762,	18.619553608374346);point2dArray.add(pos);
        pos = new Vector2d(73.78389784918213,	18.619584923783062);point2dArray.add(pos);
        pos = new Vector2d(73.78391340507909,	18.619581540762848);point2dArray.add(pos);
        pos = new Vector2d(73.78392172370997,	18.619616253714117);point2dArray.add(pos);
        pos = new Vector2d(73.78390649056624,	18.619619566529057);point2dArray.add(pos);
        pos = new Vector2d(73.78391516999602,	18.619655790894473);point2dArray.add(pos);
        pos = new Vector2d(73.78389685156762,	18.61966059014329);point2dArray.add(pos);
        pos = new Vector2d(73.78383638434661,	18.61945786848401);point2dArray.add(pos);
        pos = new Vector2d(73.78385844464422,	18.619451614158038);point2dArray.add(pos);
        pos = new Vector2d(73.78386519297194,	18.61947322770627);point2dArray.add(pos);
        pos = new Vector2d(73.78384337457305,	18.6194802525956);point2dArray.add(pos);
        pos = new Vector2d(73.78383638434661,	18.61945786848401);point2dArray.add(pos);


        int pointsCount = point2dArray.size();
        GaiaRectangle gaiaRectangle = new GaiaRectangle();
        for(int i=0; i<pointsCount; i++)
        {
            Vector2d pos2dAux = point2dArray.get(i);
            if(i == 0)
            {
                gaiaRectangle.setInit(pos2dAux);
            }
            else {
                gaiaRectangle.addPoint(pos2dAux);
            }
        }

        Vector2d leftBottom = gaiaRectangle.getLeftBottomPoint();
        Vector2d rightTop = gaiaRectangle.getRightTopPoint();
        double width = rightTop.x - leftBottom.x;
        double height = rightTop.y - leftBottom.y;
        double centerX = (rightTop.x + leftBottom.x) / 2.0;
        double centerY = (rightTop.y + leftBottom.y) / 2.0;

        for(int i=0; i<pointsCount; i++)
        {
            Vector2d pos2d = point2dArray.get(i);
            pos2d.sub(centerX, centerY);
            pos2d.mul(20000.0, 20000.0);
        }

        

        GaiaTessellator gaiaTessellator = new GaiaTessellator();
        List<Polygon2DTess> exteriorPolygons = new ArrayList<>();
        List<Polygon2DTess> interiorPolygons = new ArrayList<>();
        double error = 1.0e-7;
        gaiaTessellator.getExteriorAndInteriorsPolygons_TEST(point2dArray, exteriorPolygons, interiorPolygons, error);
        List<Integer> resultIndices = new ArrayList<>();
        Polygon2DTess resultPolygon = gaiaTessellator.tessellateHoles(exteriorPolygons.get(0), interiorPolygons, resultIndices);

        int hola = 0;

//        point3dArray = point3dArray.stream().map(p -> new OnlyHashEqualsVector3d(p)).collect(Collectors.toList());
//
        //gaiaTessellator.tessellate3D(point3dArray, resultIndices);
        //GaiaScene scene = makeGaiaSceneFromPoint3dArray(point3dArray, resultIndices);
        GaiaScene scene = makeGaiaSceneFromPolygon2DTess(resultPolygon, resultIndices);
        RenderableGaiaScene renderableGaiaScene = InternDataConverter.getRenderableGaiaScene(scene);
        gaiaScenesContainer.addRenderableGaiaScene(renderableGaiaScene);
        // end tessellation test.***************




//        IndoorGmlConverter indoorGmlConverter = new IndoorGmlConverter();
//        String indoorGMLPath = "D:\\data\\military\\withOutLAS\\IndoorGML\\B00100000005WM8IR.gml";
//        List<GaiaScene> gaiaScenes = indoorGmlConverter.load(indoorGMLPath);
//        int scenesCount = gaiaScenes.size();
//        for(int i=0; i<scenesCount; i++)
//        {
//            RenderableGaiaScene renderableGaiaScene = InternDataConverter.getRenderableGaiaScene(gaiaScenes.get(i));
//            gaiaScenesContainer.addRenderableGaiaScene(renderableGaiaScene);
//            break;
//        }
        int hola2 = 0;
    }

    private GaiaScene makeGaiaSceneFromPolygon2DTess(Polygon2DTess polygon, List<Integer> indices)
    {
        GaiaScene gaiaScene = new GaiaScene();
        GaiaMaterial gaiaMaterial = new GaiaMaterial();
        gaiaMaterial.setId(0);
        gaiaMaterial.setDiffuseColor(new Vector4d(1, 0, 0, 1));
        gaiaScene.getMaterials().add(gaiaMaterial);

        GaiaNode rootNode = new GaiaNode();
        gaiaScene.getNodes().add(rootNode);

        GaiaNode gaiaNode = new GaiaNode();
        rootNode.getChildren().add(gaiaNode);

        GaiaMesh gaiaMesh = new GaiaMesh();
        gaiaNode.getMeshes().add(gaiaMesh);

        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        gaiaMesh.getPrimitives().add(gaiaPrimitive);
        gaiaPrimitive.setMaterial(gaiaMaterial);
        gaiaPrimitive.setMaterialIndex(0);

        int verticesCount = polygon.getPointsCount();
        double scale = 1000.0;
        for(int i=0; i<verticesCount; i++)
        {
            Point2DTess point2DTess = polygon.getPoints().get(i);
            Vector2d vertex = point2DTess.getPoint();
            GaiaVertex gaiaVertex = new GaiaVertex();
            gaiaVertex.setPosition(new Vector3d(vertex.x * scale, vertex.y * scale, 0.0));
            gaiaPrimitive.getVertices().add(gaiaVertex);
        }

        GaiaSurface surface = new GaiaSurface();
        gaiaPrimitive.getSurfaces().add(surface);

        int indicesCount = indices.size();
        int trianglesCount = indicesCount / 3;
        for(int i=0; i<trianglesCount; i++)
        {
            int idx1 = indices.get(i*3);
            int idx2 = indices.get(i*3+1);
            int idx3 = indices.get(i*3+2);

            GaiaFace face = new GaiaFace();
            int faceIndices[] = new int[3];
            faceIndices[0] = idx1;
            faceIndices[1] = idx2;
            faceIndices[2] = idx3;

            face.setIndices(faceIndices);
            surface.getFaces().add(face);
        }

        // now, translate the gaiaScene to origin.***
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        Vector3d center = boundingBox.getCenter();
        center.negate();
        Matrix4d translationMatrix = rootNode.getTransformMatrix();
        translationMatrix.translate(center);

        return gaiaScene;

    }

    private GaiaScene makeGaiaSceneFromPoint3dArray(List<Vector3d> points, List<Integer> indices)
    {
        GaiaScene gaiaScene = new GaiaScene();
        GaiaMaterial gaiaMaterial = new GaiaMaterial();
        gaiaMaterial.setId(0);
        gaiaMaterial.setDiffuseColor(new Vector4d(1, 0, 0, 1));
        gaiaScene.getMaterials().add(gaiaMaterial);

        GaiaNode rootNode = new GaiaNode();
        gaiaScene.getNodes().add(rootNode);

        GaiaNode gaiaNode = new GaiaNode();
        rootNode.getChildren().add(gaiaNode);

        GaiaMesh gaiaMesh = new GaiaMesh();
        gaiaNode.getMeshes().add(gaiaMesh);

        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        gaiaMesh.getPrimitives().add(gaiaPrimitive);
        gaiaPrimitive.setMaterial(gaiaMaterial);
        gaiaPrimitive.setMaterialIndex(0);

        int verticesCount = points.size();
        double scale = 1000.0;
        for(int i=0; i<verticesCount; i++)
        {
            Vector3d vertex = points.get(i);
            GaiaVertex gaiaVertex = new GaiaVertex();
            gaiaVertex.setPosition(new Vector3d(vertex.x * scale, vertex.y * scale, vertex.z * 1.0));
            gaiaPrimitive.getVertices().add(gaiaVertex);
        }

        GaiaSurface surface = new GaiaSurface();
        gaiaPrimitive.getSurfaces().add(surface);

        int indicesCount = indices.size();
        int trianglesCount = indicesCount / 3;
        for(int i=0; i<trianglesCount; i++)
        {
            int idx1 = indices.get(i*3);
            int idx2 = indices.get(i*3+1);
            int idx3 = indices.get(i*3+2);

            GaiaFace face = new GaiaFace();
            int faceIndices[] = new int[3];
            faceIndices[0] = idx1;
            faceIndices[1] = idx2;
            faceIndices[2] = idx3;

            face.setIndices(faceIndices);
            surface.getFaces().add(face);
        }

        // now, translate the gaiaScene to origin.***
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        Vector3d center = boundingBox.getCenter();
        center.negate();
        Matrix4d translationMatrix = rootNode.getTransformMatrix();
        translationMatrix.translate(center);

        return gaiaScene;
    }


    private void setupShader() {
        shaderManager = new ShaderManager();

        GL.createCapabilities();

        // create a scene shader program
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/sceneV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/sceneV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram sceneShaderProgram = shaderManager.createShaderProgram("scene", shaderModuleDataList);


        List<String> uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        uniformNames.add("texture0");
        uniformNames.add("uColorMode");
        uniformNames.add("uOneColor");
        sceneShaderProgram.createUniforms(uniformNames);
        sceneShaderProgram.validate();
        //sceneShaderProgram.getUniformsMap().setUniform1i("texture0", 0); // texture channel 0

        // create a screen shader program
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/screenV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/screenV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram screenShaderProgram = shaderManager.createShaderProgram("screen", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("texture0");
        screenShaderProgram.createUniforms(uniformNames);
        screenShaderProgram.validate();

    }

    private void draw() {
        // render into frame buffer.***
        Fbo colorRenderFbo = fboManager.getFbo("colorRender");
        colorRenderFbo.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // render scene objects.***
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");
        sceneShaderProgram.bind();
        renderer.render(gaiaScenesContainer, sceneShaderProgram);
        sceneShaderProgram.unbind();

        colorRenderFbo.unbind();

        // now render to windows using screenQuad.***
        int colorRenderTextureId = colorRenderFbo.getColorTextureId();
        GL20.glEnable(GL20.GL_TEXTURE_2D);
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, colorRenderTextureId);

        ShaderProgram screenShaderProgram = shaderManager.getShaderProgram("screen");
        screenShaderProgram.bind();
        screenQuad.render();
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        screenShaderProgram.unbind();
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.

        // 사용자가 창을 닫거다 esc키를 누를 때까지 랜더링 루프를 실행합니다.
        long windowHandle = window.getWindowHandle();
        while (!glfwWindowShouldClose(windowHandle)) {
            int[] width = new int[1];
            int[] height = new int[1];
            glfwGetWindowSize(windowHandle, width, height);
            glViewport(0, 0, width[0], height[0]);

            glEnable(GL_DEPTH_TEST);
            glPointSize(5.0f);
            // 클리어 컬러를 적용합니다.
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            glClearDepth(1.0f);
            // 프레임 버퍼 클리어
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            draw();
            // 색상버퍼 교체
            glfwSwapBuffers(windowHandle);
            // 이벤트를 폴링상태로 둡니다. key 콜백이 실행되려면 폴링상태가 활성화 되어있어야 합니다.
            glfwPollEvents();
        }
    }

}
