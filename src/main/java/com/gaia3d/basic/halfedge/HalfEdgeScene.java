package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Slf4j
@Getter
@Setter
public class HalfEdgeScene  implements Serializable{
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction() {
        for (HalfEdgeNode node : nodes) {
            node.doTrianglesReduction();
        }
    }

    public GaiaBoundingBox getGaiaBoundingBox() {
        if (gaiaBoundingBox == null) {
            gaiaBoundingBox = calculateBoundingBox(null);
        }
        return gaiaBoundingBox;
    }

    public void deleteObjects() {
        for (HalfEdgeNode node : nodes) {
            node.deleteObjects();
        }
        nodes.clear();
        materials.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeNode node : nodes) {
            node.checkSandClockFaces();
        }
    }

    public void spendTransformationMatrix() {
        for (HalfEdgeNode node : nodes) {
            node.spendTransformationMatrix();
        }
    }

    public void TEST_cutScene()
    {
        // Test.***
        GaiaBoundingBox bbox = getBoundingBox();
        Vector3d center = bbox.getCenter();
        double error = 1e-8;
//        if(error < 1)
//        {
//            return;
//        }
        PlaneType planeType = PlaneType.XZ;
        cutByPlane(planeType, center, error);
        classifyFacesIdByPlane(planeType, center);

        // check if there are no used vertices.***
        List<HalfEdgeSurface> resultHalfEdgeSurfaces = new ArrayList<>();
        extractSurfaces(resultHalfEdgeSurfaces);
        for (HalfEdgeSurface surface : resultHalfEdgeSurfaces) {
            if(surface.existNoUsedVertices())
            {
                log.error("Error: existNoUsedVertices.***");
            }
        }

        // now, remove faces with classifyId = 1.***
//        for (HalfEdgeNode node : nodes) {
//            node.TEST_removeFacesWithClassifyId(1);
//        }

    }

    public List<HalfEdgeSurface> extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces)
    {
        if(resultHalfEdgeSurfaces == null) {
            resultHalfEdgeSurfaces = new ArrayList<>();
        }
        for (HalfEdgeNode node : nodes) {
            resultHalfEdgeSurfaces = node.extractSurfaces(resultHalfEdgeSurfaces);
        }
        return resultHalfEdgeSurfaces;
    }

    public void removeDeletedObjects()
    {
        for (HalfEdgeNode node : nodes) {
            node.removeDeletedObjects();
        }
    }

    public boolean cutByPlane(PlaneType planeType, Vector3d planePosition, double error)
    {
        // 1rst check if the plane intersects the bbox.***
        GaiaBoundingBox bbox = getBoundingBox();

        if (bbox == null) {
            return false;
        }

        if (planeType == PlaneType.XZ) {
            if (planePosition.y < bbox.getMinY() || planePosition.y > bbox.getMaxY()) {
                return false;
            }
        } else if (planeType == PlaneType.YZ) {
            if (planePosition.x < bbox.getMinX() || planePosition.x > bbox.getMaxX()) {
                return false;
            }
        } else if (planeType == PlaneType.XY) {
            if (planePosition.z < bbox.getMinZ() || planePosition.z > bbox.getMaxZ()) {
                return false;
            }
        }

        for (HalfEdgeNode node : nodes) {
            node.cutByPlane(planeType, planePosition, error);
        }

        removeDeletedObjects();

        return true;
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if(resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgeNode node : nodes) {
            resultBBox = node.calculateBoundingBox(resultBBox);
        }
        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition)
    {
        for (HalfEdgeNode node : nodes) {
            node.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    private void copyTextures(GaiaMaterial material, Path copyDirectory) throws IOException {
        Map<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
        List<GaiaTexture> diffuseTextures = materialTextures.get(TextureType.DIFFUSE);
        if (diffuseTextures != null && !diffuseTextures.isEmpty()) {
            GaiaTexture texture = materialTextures.get(TextureType.DIFFUSE).get(0);
            String parentPath = texture.getParentPath();
            File parentFile = new File(parentPath);
            String diffusePath = texture.getPath();
            File diffuseFile = new File(diffusePath);

            File imageFile = ImageUtils.correctPath(parentFile, diffuseFile);

            Path imagesFolderPath = copyDirectory.resolve("images");
            if (imagesFolderPath.toFile().mkdirs()) {
                log.debug("Images Directory created: {}", imagesFolderPath);
            }

            Path outputImagePath = imagesFolderPath.resolve(imageFile.getName());
            File outputImageFile = outputImagePath.toFile();

            texture.setPath(imageFile.getName());

            if (!imageFile.exists()) {
                log.error("Texture Input Image Path is not exists. {}", diffusePath);
            } else {
                FileUtils.copyFile(imageFile, outputImageFile);
            }
        }
    }
    public void writeFile(String folderPathString, String fileName) throws FileNotFoundException {
        Path folderPath = Paths.get(folderPathString);
        Path filePath = folderPath.resolve(fileName);
        File file = filePath.toFile();
        try
        {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            ObjectOutputStream outputStream = new ObjectOutputStream(bufferedOutputStream);
            /*
            private Path originalPath;
            private GaiaBoundingBox gaiaBoundingBox;
            private GaiaAttribute attribute;
            private List<HalfEdgeNode> nodes = new ArrayList<>();
            private List<GaiaMaterial> materials = new ArrayList<>();
            private GaiaBoundingBox boundingBox = null;
             */

            // write originalPath, gaiaBoundingBox, attribute
            String originalPath = this.originalPath.toString();
            outputStream.writeUTF(originalPath);

            outputStream.writeObject(gaiaBoundingBox);
            outputStream.writeObject(attribute);

            // Write nodes
            outputStream.writeInt(nodes.size());
            for (HalfEdgeNode node : nodes) {
                node.writeFile(outputStream);
            }

            // Write materials
            outputStream.writeInt(materials.size());
            for (GaiaMaterial material : materials) {
                outputStream.writeObject(material);
            }

            // Copy images to the temp directory
            for (GaiaMaterial material : materials) {
                copyTextures(material, folderPath);
            }

            outputStream.close();
            bufferedOutputStream.close();
            fileOutputStream.close();

        }
        catch (Exception e)
        {
            log.error("GaiaSet Write Error : ", e);
            file.delete();
        }
    }

    public static HalfEdgeScene readFile(String folderPathString, String fileName) throws FileNotFoundException {
        Path folderPath = Paths.get(folderPathString);
        Path filePath = folderPath.resolve(fileName);
        File file = filePath.toFile();
        try
        {
            HalfEdgeScene halfEdgeScene = new HalfEdgeScene();
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            ObjectInputStream inputStream = new ObjectInputStream(bufferedInputStream);

            // read originalPath, gaiaBoundingBox, attribute
            String originalPath = inputStream.readUTF();
            halfEdgeScene.originalPath = Paths.get(originalPath);

            halfEdgeScene.gaiaBoundingBox = (GaiaBoundingBox) inputStream.readObject();
            halfEdgeScene.attribute = (GaiaAttribute) inputStream.readObject();

            // Read nodes
            int nodesSize = inputStream.readInt();
            for (int i = 0; i < nodesSize; i++) {
                HalfEdgeNode node = new HalfEdgeNode();
                node.readFile(inputStream);
                halfEdgeScene.nodes.add(node);
            }

            // Read materials
            int materialsSize = inputStream.readInt();
            for (int i = 0; i < materialsSize; i++) {
                GaiaMaterial material = (GaiaMaterial) inputStream.readObject();
                halfEdgeScene.materials.add(material);
            }

            inputStream.close();
            bufferedInputStream.close();
            fileInputStream.close();

            return halfEdgeScene;
        }
        catch (Exception e)
        {
            log.error("GaiaSet Read Error : ", e);
        }

        return null;
    }

    public void writeFileSerializable(String folderPathString, String fileName) {
        Path folderPath = Paths.get(folderPathString);
        Path filePath = folderPath.resolve(fileName);
        File file = filePath.toFile();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            outputStream.writeObject(this);

            // Copy images to the temp directory
            for (GaiaMaterial material : materials) {
                copyTextures(material, folderPath);
            }
        } catch (Exception e) {
            log.error("GaiaSet Write Error : ", e);
            file.delete();
        }
    }

    public static HalfEdgeScene readFileSerializable(String folderPathString, String fileName) throws FileNotFoundException {
        Path folderPath = Paths.get(folderPathString);
        Path filePath = folderPath.resolve(fileName);
        Path imagesPath = folderPath.getParent().resolve("images");
        File file = filePath.toFile();
        try (ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            HalfEdgeScene halfEdgeScene = (HalfEdgeScene) inputStream.readObject();
            for (GaiaMaterial material : halfEdgeScene.getMaterials()) {
                material.getTextures().forEach((textureType, textures) -> {
                    for (GaiaTexture texture : textures) {
                        String texturePath = texture.getPath();
                        File fileTex = new File(texturePath);
                        String fileNameTex = fileTex.getName();
                        //Path imagePath = imagesPath.resolve(fileName);

                        texture.setParentPath(imagesPath.toString());
                        texture.setPath(fileName);
                    }
                });
            }
            return halfEdgeScene;
        } catch (Exception e) {
            log.error("GaiaSet Write Error : ", e);
        }
        return null;
    }

}
