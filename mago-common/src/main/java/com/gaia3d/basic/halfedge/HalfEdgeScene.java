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
import org.joml.Vector2d;
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
public class HalfEdgeScene implements Serializable {
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public static HalfEdgeScene readFile(String folderPathString, String fileName) throws FileNotFoundException {
        Path folderPath = Paths.get(folderPathString);
        Path filePath = folderPath.resolve(fileName);
        File file = filePath.toFile();
        try {
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
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Read Error : ", e);
        }

        return null;
    }

    public List<GaiaMaterial> getCopyMaterials() {
        List<GaiaMaterial> copyMaterials = new ArrayList<>();
        for (GaiaMaterial material : materials) {
            copyMaterials.add(material.clone());
        }
        return copyMaterials;
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
        int materialsCount = this.materials.size();
        for (GaiaMaterial material : this.materials) {
            material.clear();
        }
        materials.clear();
    }

    public void deleteNoUsedMaterials() {
        List<Integer> usedMaterialsIds = getUsedMaterialsIds(null);
        List<GaiaMaterial> noUsedMaterials = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            if (!usedMaterialsIds.contains(i)) {
                noUsedMaterials.add(materials.get(i));
            }
        }
        for (GaiaMaterial material : noUsedMaterials) {
            material.deleteTextures();
            List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
            if (textures != null) {
                for (GaiaTexture texture : textures) {
                    texture.deleteObjects();
                    texture.setParentPath("");
                    texture.setName("");
                    texture.setPath("");
                }
            }
        }
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

    public void deleteFacesWithClassifyId(int classifyId) {
        for (HalfEdgeNode node : nodes) {
            node.deleteFacesWithClassifyId(classifyId);
        }
    }

    public void TEST_cutScene() {
        // Test
        GaiaBoundingBox bbox = getBoundingBox();
        Vector3d center = bbox.getCenter();
        double error = 1e-8;
//        if (error < 1)
//        {
//            return;
//        }
        PlaneType planeType = PlaneType.YZ;
        cutByPlane(planeType, center, error);
        classifyFacesIdByPlane(planeType, center);

        // check if there are no used vertices
        List<HalfEdgeSurface> resultHalfEdgeSurfaces = new ArrayList<>();
        extractSurfaces(resultHalfEdgeSurfaces);
        List<HalfEdgeVertex> noUsedVertices = new ArrayList<>();
        for (HalfEdgeSurface surface : resultHalfEdgeSurfaces) {
            noUsedVertices.clear();
            if (surface.existNoUsedVertices(noUsedVertices)) {
                log.error("[ERROR] existNoUsedVertices.");
            }
        }
    }

    public List<HalfEdgeSurface> extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces) {
        if (resultHalfEdgeSurfaces == null) {
            resultHalfEdgeSurfaces = new ArrayList<>();
        }
        for (HalfEdgeNode node : nodes) {
            resultHalfEdgeSurfaces = node.extractSurfaces(resultHalfEdgeSurfaces);
        }
        return resultHalfEdgeSurfaces;
    }

    public List<HalfEdgePrimitive> extractPrimitives(List<HalfEdgePrimitive> resultPrimitives) {
        if (resultPrimitives == null) {
            resultPrimitives = new ArrayList<>();
        }
        for (HalfEdgeNode node : nodes) {
            node.extractPrimitives(resultPrimitives);
        }

        return resultPrimitives;
    }

    public void removeDeletedObjects() {
        for (HalfEdgeNode node : nodes) {
            node.removeDeletedObjects();
        }
    }

    public boolean cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        // 1rst check if the plane intersects the bbox
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

    public void getIntersectedFacesByPlane(PlaneType planeType, Vector3d planePosition, List<HalfEdgeFace> resultFaces, double error) {
        for (HalfEdgeNode node : nodes) {
            node.getIntersectedFacesByPlane(planeType, planePosition, resultFaces, error);
        }
    }

    public double calculateArea() {
        double area = 0;
        for (HalfEdgeNode node : nodes) {
            area += node.calculateArea();
        }
        return area;
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if (resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgeNode node : nodes) {
            resultBBox = node.calculateBoundingBox(resultBBox);
        }
        return resultBBox;
    }

    public void translate(Vector3d translation) {
        for (HalfEdgeNode node : nodes) {
            node.translate(translation);
        }
    }

    public void splitFacesByBestObliqueCameraDirectionToProject() {
        for (HalfEdgeNode node : nodes) {
            node.splitFacesByBestObliqueCameraDirectionToProject();
        }
    }

    public void translateToOrigin() {
        GaiaBoundingBox bbox = getBoundingBox();
        Vector3d center = bbox.getCenter();
        center.negate();
        translate(center);
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition) {
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
                log.error("[ERROR] Texture Input Image Path is not exists. {}", diffusePath);
            } else {
                FileUtils.copyFile(imageFile, outputImageFile);
            }
        }
    }

    public void writeFile(String folderPathString, String fileName) throws FileNotFoundException {
        Path folderPath = Paths.get(folderPathString);
        Path filePath = folderPath.resolve(fileName);
        File file = filePath.toFile();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            ObjectOutputStream outputStream = new ObjectOutputStream(bufferedOutputStream);

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

        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Write Error : ", e);
            file.delete();
        }
    }

    public HalfEdgeScene clone() {
        HalfEdgeScene clonedScene = new HalfEdgeScene();
        clonedScene.originalPath = originalPath;
        clonedScene.gaiaBoundingBox = gaiaBoundingBox;
        clonedScene.attribute = attribute;
        for (HalfEdgeNode node : nodes) {
            clonedScene.nodes.add(node.clone());
        }
        for (GaiaMaterial material : materials) {
            clonedScene.materials.add(material.clone());
        }
        return clonedScene;
    }

    public HalfEdgeScene cloneByClassifyId(int classifyId) {
        HalfEdgeScene clonedScene = null;

        for (HalfEdgeNode node : nodes) {
            HalfEdgeNode clonedNode = node.cloneByClassifyId(classifyId);
            if (clonedNode != null) {
                if (clonedScene == null) {
                    clonedScene = new HalfEdgeScene();
                    clonedScene.originalPath = originalPath;
                    clonedScene.attribute = attribute;
                }
                clonedScene.nodes.add(clonedNode);
            }
        }
        if (clonedScene != null) {
            for (GaiaMaterial material : materials) {
                clonedScene.materials.add(material.clone());
            }
        }

        return clonedScene;
    }

    public int deleteDegeneratedFaces() {
        int deletedFacesCount = 0;
        for (HalfEdgeNode node : nodes) {
            deletedFacesCount += node.deleteDegeneratedFaces();
        }
        removeDeletedObjects();
        return deletedFacesCount;
    }


    public void scissorTextures() {
        boolean hasTextures = false;
        for (GaiaMaterial material : materials) {
            if (material.hasTextures()) {
                hasTextures = true;
                break;
            }
        }

        if (!hasTextures) {
            return;
        }

        for (HalfEdgeNode node : nodes) {
            node.scissorTextures(materials);
        }
    }

    public void scissorTexturesByMotherScene(List<GaiaMaterial> motherMaterials) {
        boolean hasTextures = false;
        for (GaiaMaterial material : materials) {
            if (material.hasTextures()) {
                hasTextures = true;
                break;
            }
        }

        if (!hasTextures) {
            return;
        }

        for (HalfEdgeNode node : nodes) {
            node.scissorTexturesByMotherScene(materials, motherMaterials);
        }
    }

    public int getTrianglesCount() {
        int trianglesCount = 0;
        for (HalfEdgeNode node : nodes) {
            trianglesCount += node.getTrianglesCount();
        }
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        for (HalfEdgeNode node : nodes) {
            node.setBoxTexCoordsXY(box);
        }
    }

    public List<Integer> getUsedMaterialsIds(List<Integer> resultMaterialsIds) {
        if (resultMaterialsIds == null) {
            resultMaterialsIds = new ArrayList<>();
        }
        for (HalfEdgeNode node : nodes) {
            node.getUsedMaterialsIds(resultMaterialsIds);
        }
        return resultMaterialsIds;
    }

    public void calculateNormals() {
        for (HalfEdgeNode node : nodes) {
            node.calculateNormals();
        }
    }

    public List<GaiaMaterial> getUsingMaterialsWithTextures(List<GaiaMaterial> resultMaterials) {
        // Usually, there are materials that are not using
        // This function returns the materials that are using and has textures
        if (resultMaterials == null) {
            resultMaterials = new ArrayList<>();
        }

        List<Integer> usedMaterialsIds = getUsedMaterialsIds(null);
        for (int materialId : usedMaterialsIds) {
            GaiaMaterial material = materials.get(materialId);
            if (material.hasTextures()) {
                resultMaterials.add(material);
            }
        }

        return resultMaterials;
    }

    public void setMaterialId(int materialId) {
        for (HalfEdgeNode node : nodes) {
            node.setMaterialId(materialId);
        }
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        for (HalfEdgeNode node : nodes) {
            node.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }
    }

    public void decimate(DecimateParameters decimateParameters) {
        for (HalfEdgeNode node : nodes) {
            node.decimate(decimateParameters);
        }
    }

    public void updateVerticesList() {
        for (HalfEdgeNode node : nodes) {
            node.updateVerticesList();
        }
    }

    public void updateFacesList() {
        for (HalfEdgeNode node : nodes) {
            node.updateFacesList();
        }
    }
    
    public void makeHorizontalSkirt() {
        GaiaBoundingBox bbox = getBoundingBox();
        if (bbox == null) {
            log.info("Making skirt : Error: bbox is null");
            return;
        }

        double error = 0.01; // 0.001
        List<HalfEdgeVertex> westVertices = new ArrayList<>();
        List<HalfEdgeVertex> eastVertices = new ArrayList<>();
        List<HalfEdgeVertex> southVertices = new ArrayList<>();
        List<HalfEdgeVertex> northVertices = new ArrayList<>();
        for (HalfEdgeNode node : nodes) {
            node.getWestEastSouthNorthVertices(bbox, westVertices, eastVertices, southVertices, northVertices, error);
        }

        double bboxLengthX = bbox.getLengthX();
        double bboxLengthY = bbox.getLengthY();
        double bboxMaxSize = Math.max(bboxLengthX, bboxLengthY);
        double expandDistance = bboxMaxSize * 0.005;

        Vector3d axisZ = new Vector3d(0, 0, 1);

        // only expand the perimeter vertices
        double dotLimit = 0.35;
        if (westVertices.size() > 1) {
            for (HalfEdgeVertex vertex : westVertices) {
                Vector3d normal = vertex.calculateNormal();

                if (Math.abs(normal.dot(-1, 0, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.x -= expandDistance;
                }
            }
        }

        if (eastVertices.size() > 1) {
            for (HalfEdgeVertex vertex : eastVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(1, 0, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.x += expandDistance;
                }
            }
        }

        if (southVertices.size() > 1) {
            for (HalfEdgeVertex vertex : southVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(0, -1, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.y -= expandDistance;
                }
            }
        }

        if (northVertices.size() > 1) {
            for (HalfEdgeVertex vertex : northVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(0, 1, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.y += expandDistance;
                }
            }
        }
    }

    public void translateTexCoordsToPositiveQuadrant() {
        for (HalfEdgeNode node : nodes) {
            node.translateTexCoordsToPositiveQuadrant();
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (HalfEdgeNode node : nodes) {
            facesCount += node.getFacesCount();
        }
        return facesCount;
    }
}
