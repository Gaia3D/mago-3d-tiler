package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class GaiaBatcher {
    private final static int SHORT_LIMIT = 65535;
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final List<GaiaBufferDataSet> batchedDataSets = new ArrayList<>();
    private final List<GaiaMaterial> batchedMaterials = new ArrayList<>();

    private void reassignMaterialsToGaiaBufferDataSetWithSameMaterial(List<GaiaBufferDataSet> dataSets, LevelOfDetail lod) {
        int datasetsCount = dataSets.size();
        Map<GaiaBufferDataSet, Boolean> visitedMap = new HashMap<>();
        for (int i = 0; i < datasetsCount; i++) {
            GaiaBufferDataSet dataSet = dataSets.get(i);
            if (visitedMap.containsKey(dataSet)) {
                continue;
            }
            GaiaMaterial material = batchedMaterials.get(dataSet.getMaterialId());
            for (int  j= i + 1; j < datasetsCount; j++) {
                GaiaBufferDataSet dataSet2 = dataSets.get(j);
                GaiaMaterial material2 = batchedMaterials.get(dataSet2.getMaterialId());
                if (visitedMap.containsKey(dataSet2)) {
                    continue;
                }
                if (dataSet == dataSet2) continue;
                if (material2 == material) {
                    visitedMap.put(dataSet2, true); // set visited.***
                    continue;
                }
                if (areEqualMaterials(material2, material, lod.getTextureScale())) {
                    dataSet2.setMaterialId(material.getId());
                    visitedMap.put(dataSet2, true); // set visited.***
                }
            }
        }
    }

    /** compare two materials.***
     * @param materialA
     * @param materialB
     * @param scaleFactor
     */
    private boolean areEqualMaterials(GaiaMaterial materialA, GaiaMaterial materialB, float scaleFactor) {
        // This function determines if two materials are equal.
        if (materialA == null && materialB == null) {
            return true;
        } else if (materialA == null || materialB == null) {
            return false;
        } else if (materialA == materialB) {
            return true;
        }

        Map<TextureType, List<GaiaTexture>> textureMapA = materialA.getTextures();
        Set<TextureType> keysA = textureMapA.keySet();
        Map<TextureType, List<GaiaTexture>> textureMapB = materialB.getTextures();
        Set<TextureType> keysB = textureMapB.keySet();

        if (keysA.size() != keysB.size()) {
            return false;
        }


        boolean hasTexture = false;
        boolean hasTextureAreEquals = true;
        for (TextureType key : keysA) {
            if (key != TextureType.DIFFUSE) {
                continue;
            }
            List<GaiaTexture> listTexturesA = textureMapA.get(key);
            List<GaiaTexture> listTexturesB = materialB.getTextures().get(key);
            if (listTexturesA == null && listTexturesB == null) {
                continue;
            } else if (listTexturesA == null || listTexturesB == null) {
                hasTextureAreEquals = false;
            }
            if (listTexturesA.size() != listTexturesB.size()) {
                hasTextureAreEquals = false;
            }

            for (int i = 0; i < listTexturesA.size() && i < listTexturesB.size(); i++) {
                GaiaTexture textureA = listTexturesA.get(i);
                GaiaTexture textureB = listTexturesB.get(i);
                hasTexture = true;

                // check if the fullPath of the textures are equal.***
                String fullPathA = textureA.getFullPath();
                String fullPathB = textureB.getFullPath();
                if (fullPathA.equals(fullPathB)) {
                    hasTextureAreEquals = true;
                } else if (!textureA.isEqualTexture(textureB, scaleFactor)) {
                    hasTextureAreEquals = false;
                }
            }
        }

        if (!hasTexture) {
            Vector4d colorA = materialA.getDiffuseColor();
            Vector4d colorB = materialB.getDiffuseColor();
            return colorA.equals(colorB);
        }
        return hasTextureAreEquals;
    }

    private void setBatchId(List<GaiaSet> sets) {
        for (int i = 0; i < sets.size(); i++) {
            GaiaSet set = sets.get(i);
            List<GaiaBufferDataSet> dataSets = set.getBufferDataList();
            for (GaiaBufferDataSet dataSet : dataSets) {
                Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
                GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
                int elementsCount = positionBuffer.getElementsCount();
                float[] batchIdList = new float[elementsCount];
                Arrays.fill(batchIdList, i);

                GaiaBuffer batchIdBuffer = new GaiaBuffer();
                batchIdBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
                batchIdBuffer.setGlType(GL20.GL_FLOAT);
                batchIdBuffer.setElementsCount(elementsCount);
                batchIdBuffer.setGlDimension((byte) 1);
                batchIdBuffer.setFloats(batchIdList);

                buffers.put(AttributeType.BATCHID, batchIdBuffer);
            }
        }
    }

    public GaiaSet runBatching(List<TileInfo> tileInfos, String nodeCode, LevelOfDetail lod) {
        List<GaiaSet> sets = tileInfos.stream()
                .map(TileInfo::getSet)
                .collect(Collectors.toList());

        sets = sets.stream().filter((set -> {
            List<GaiaBufferDataSet> dataSets = set.getBufferDataList();
            for (GaiaBufferDataSet dataSet : dataSets) {
                Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
                GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
                if (positionBuffer == null) {
                    log.error("Position buffer is null");
                    return false;
                }
            }
            return true;
        })).collect(Collectors.toList());

        setBatchId(sets);

        sets.forEach((set) -> {
            List<GaiaBufferDataSet> dataSets = set.getBufferDataList();
            List<GaiaMaterial> materials = set.getMaterials();
            int materialIdOffset = batchedMaterials.size();
            dataSets.forEach((dataSet) -> {
                int materialId = dataSet.getMaterialId();
                dataSet.setMaterialId(materialIdOffset + materialId);
            });
            materials.forEach((material) -> {
                int materialId = material.getId();
                material.setId(materialIdOffset + materialId);
            });
            batchedDataSets.addAll(dataSets);
            batchedMaterials.addAll(materials);
        });

        // check has materials textures
        batchedMaterials.forEach((material) -> {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            Set<TextureType> keys = textures.keySet();
            for (TextureType key : keys) {
                List<GaiaTexture> listTexturesA = textures.get(key);
                if (listTexturesA != null && !listTexturesA.isEmpty()) {
                    List<GaiaTexture> newTextures = listTexturesA.stream().filter((texture) -> {
                        File textureFile = new File(texture.getFullPath());
                        return textureFile.exists() && textureFile.isFile();
                    }).collect(Collectors.toList());
                    textures.put(key, newTextures);
                }
            }
        });

        // check if exist equal materials.***
        reassignMaterialsToGaiaBufferDataSetWithSameMaterial(batchedDataSets, lod);
        List<GaiaMaterial> filteredMaterials = getMaterialsListOfBufferDataSet(batchedDataSets, new ArrayList<>());

        // batch dataSets with same material.***
        List<GaiaBufferDataSet> filteredDataSets = batchDataSetsWithTheSameMaterial(batchedDataSets, filteredMaterials);
        setMaterialsIndexInList(filteredMaterials, filteredDataSets);
        checkIsRepeatMaterial(filteredMaterials, filteredDataSets);

        /* only diffuse-color use materials */
        List<GaiaMaterial> colorMaterials = filteredMaterials.stream().filter((material) -> {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return diffuseTextures == null || diffuseTextures.isEmpty() || globalOptions.isIgnoreTextures();
        }).collect(Collectors.toList());
        List<GaiaBufferDataSet> colorDataSet = filteredDataSets.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = findMaterial(filteredMaterials, bufferDataSet.getMaterialId());
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return diffuseTextures == null || diffuseTextures.isEmpty() || globalOptions.isIgnoreTextures();
        }).collect(Collectors.toList());

        /* clear Textures */
        colorMaterials.stream().forEach((material) -> {
            material.setName("COLOR_MATERIAL");
            material.getTextures().remove(TextureType.DIFFUSE);
            material.getTextures().put(TextureType.DIFFUSE, new ArrayList<>());
        });
        colorDataSet.stream().forEach((bufferDataSet) -> {
            createColorBuffer(colorMaterials, bufferDataSet, lod);
            bufferDataSet.getBuffers().remove(AttributeType.TEXCOORD);
        });

        /* texture use materials */
        List<GaiaMaterial> textureMaterials = filteredMaterials.stream().filter((material) -> {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return diffuseTextures != null && !diffuseTextures.isEmpty() && !globalOptions.isIgnoreTextures();
        }).collect(Collectors.toList());
        List<GaiaBufferDataSet> textureDataSet = filteredDataSets.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = findMaterial(filteredMaterials, bufferDataSet.getMaterialId());
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return diffuseTextures != null && !diffuseTextures.isEmpty() && !globalOptions.isIgnoreTextures();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(textureMaterials, textureDataSet);

        /* 1. clamp texture use materials */
        List<GaiaMaterial> clampMaterials = textureMaterials.stream().filter((material) -> !material.isRepeat()).collect(Collectors.toList());
        List<GaiaBufferDataSet> clampDataSets = textureDataSet.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = findMaterial(textureMaterials, bufferDataSet.getMaterialId());
            return !material.isRepeat();
        }).collect(Collectors.toList());

        /* 2. repeat pattern texture use materials */
        List<GaiaMaterial> repeatMaterials = textureMaterials.stream().filter(GaiaMaterial::isRepeat).collect(Collectors.toList());
        List<GaiaBufferDataSet> repeatDataSets = textureDataSet.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = findMaterial(textureMaterials, bufferDataSet.getMaterialId());
            return material.isRepeat();
        }).collect(Collectors.toList());

        setMaterialsIndexInList(clampMaterials, clampDataSets);
        setMaterialsIndexInList(repeatMaterials, repeatDataSets);

        /* batching textures for clamp texture material */
        List<GaiaBufferDataSet> resultBufferDatas = new ArrayList<>();
        List<GaiaMaterial> resultMaterials = new ArrayList<>();
        if (!clampDataSets.isEmpty() && !clampMaterials.isEmpty()) {
            atlasTextures(lod, nodeCode, clampDataSets, clampMaterials);
            List<List<GaiaBufferDataSet>> splitedDataSets = divisionByMaxVerticesCount(clampDataSets);
            List<GaiaBufferDataSet> batchedClampDataSets = batchClampMaterial(splitedDataSets);
            clampMaterials.removeIf((clampMaterial) -> {
                return clampMaterial.getId() > 0;
            });
            clampMaterials.get(0).setName("ATLAS");
            resultMaterials.addAll(clampMaterials);
            resultBufferDatas.addAll(batchedClampDataSets);
        }

        if (!repeatDataSets.isEmpty() && !repeatMaterials.isEmpty()) {
            rearrangeRepeatMaterial(repeatDataSets, repeatMaterials, resultMaterials.size());
            resultMaterials.addAll(repeatMaterials);
            resultBufferDatas.addAll(repeatDataSets);
        }
        if (!colorMaterials.isEmpty() && !colorDataSet.isEmpty()) {
            rearrangeRepeatMaterial(colorDataSet, colorMaterials, resultMaterials.size());
            resultMaterials.addAll(colorMaterials);
            resultBufferDatas.addAll(colorDataSet);
        }

        GaiaSet batchedSet = new GaiaSet();
        batchedSet.setProjectName("BatchedSet");
        batchedSet.setBufferDataList(resultBufferDatas);
        batchedSet.setMaterials(resultMaterials);
        if (resultBufferDatas.isEmpty() || resultMaterials.isEmpty()) {
            log.error("Batched Set is empty");
            batchedSet = null;
        }

        Matrix4d transform = new Matrix4d();
        transform.identity();
        return batchedSet;
    }

    private List<GaiaMaterial> getMaterialsListOfBufferDataSet(List<GaiaBufferDataSet> bufferDataSets, List<GaiaMaterial> materials) {
        // first, make a map to avoid duplicate materials
        Map<Integer, GaiaMaterial> materialMap = new WeakHashMap<>();
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            Integer materialId = bufferDataSet.getMaterialId();
            GaiaMaterial material = findMaterial(batchedMaterials, bufferDataSet.getMaterialId());
            materialMap.put(materialId, material);
        }
        // second, make a list from the map
        materials.addAll(materialMap.values());
        return materials;
    }

    private void setMaterialsIndexInList(List<GaiaMaterial> materials, List<GaiaBufferDataSet> dataSets) {
        List<GaiaMaterial> tempMaterials = dataSets.stream().map((dataSet) -> {
            int materialId = dataSet.getMaterialId();
            return materials.stream()
                    .filter((material) -> material.getId() == materialId)
                    .findFirst()
                    .orElseThrow();
        }).collect(Collectors.toList());

        for (int i = 0; i < materials.size(); i++) {
            GaiaMaterial material = materials.get(i);
            GaiaBufferDataSet bufferDataSet = dataSets.get(i);
            material.setId(i);
            bufferDataSet.setMaterialId(i);
        }

        for (int i = 0; i < dataSets.size(); i++) {
            int materialId = tempMaterials.get(i).getId();
            dataSets.get(i).setMaterialId(materialId);
        }
    }

    // Material Id 재정렬
    private void rearrangeRepeatMaterial(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials, int offset) {
        dataSets.forEach((batchedBufferData) -> {
            for (int i = 0; i < materials.size(); i++) {
                GaiaMaterial material = materials.get(i);
                if (material.getId() == batchedBufferData.getMaterialId()) {
                    batchedBufferData.setMaterialId(i + offset);
                    break;
                }
            }
        });
        for (int i = 0; i < materials.size(); i++) {
            GaiaMaterial material = materials.get(i);
            material.setId(i + offset);
        }
    }

    private List<List<GaiaBufferDataSet>> divisionByMaxVerticesCount(List<GaiaBufferDataSet> dataSets) {
        int count = 0;
        List<List<GaiaBufferDataSet>> result = new ArrayList<>();
        List<GaiaBufferDataSet> splitList = new ArrayList<>();
        result.add(splitList);
        for (GaiaBufferDataSet dataSet : dataSets) {
            Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
            GaiaBuffer posBuffer = buffers.get(AttributeType.POSITION);
            if (posBuffer != null) {
                int posLength = posBuffer.getFloats().length / 3;
                if ((count + posLength) >= SHORT_LIMIT) {
                    if (!splitList.isEmpty()) {
                        splitList = new ArrayList<>();
                        result.add(splitList);
                    }
                    count = posLength;
                } else {
                    count += posLength;
                }
                splitList.add(dataSet);
            }
        }
        return result;
    }

    private GaiaMaterial findMaterial(List<GaiaMaterial> materials, int materialId) {
        GaiaMaterial resultMaterial = materials.stream()
                .filter((material) -> material.getId() == materialId)
                .findFirst().orElseThrow();
        return resultMaterial;
    }

    private List<GaiaBufferDataSet> batchDataSetsWithTheSameMaterial(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> filteredMaterials) {
        List<GaiaBufferDataSet> filterdBufferDataList = new ArrayList<>();
        // make map : key = GaiaMaterial, value = array<GaiaBufferDataSet>.
        Map<GaiaMaterial, List<GaiaBufferDataSet>> dataSetsMap = new WeakHashMap<>();
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaMaterial material = findMaterial(filteredMaterials, dataSet.getMaterialId());
            List<GaiaBufferDataSet> bufferDataSetDataList = dataSetsMap.computeIfAbsent(material, k -> new ArrayList<>());
            bufferDataSetDataList.add(dataSet);
        }
        // make batched buffer data.
        dataSetsMap.forEach((material, bufferDataSetDataList) -> {
            List<List<GaiaBufferDataSet>> splitedDataSets = divisionByMaxVerticesCount(bufferDataSetDataList);
            splitedDataSets.forEach((splitedDataSet) -> {
                GaiaBufferDataSet batchedBufferData = batchVertices(splitedDataSet);
                batchedBufferData.setMaterialId(material.getId());
                filterdBufferDataList.add(batchedBufferData);
            });
        });
        return filterdBufferDataList;
    }

    private void checkIsRepeatMaterial(List<GaiaMaterial> materials, List<GaiaBufferDataSet> dataSets) {
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaMaterial material = findMaterial(materials, dataSet.getMaterialId());
            if (material != null) {
                Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
                List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
                if (textures == null || textures.isEmpty()) {
                    Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
                    buffers.remove(AttributeType.TEXCOORD);
                }
                material.setRepeat(checkRepeat(material, dataSet));
            }
        }
    }

    // 스트레치 텍스쳐들 배칭
    private List<GaiaBufferDataSet> batchClampMaterial(List<List<GaiaBufferDataSet>> splitedDataSets) {
        return splitedDataSets.stream().map((splitedIndicesLimit) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(splitedIndicesLimit);
            batchedBufferData.setMaterialId(0);
            return batchedBufferData;
        }).collect(Collectors.toList());
    }

    // 각 Material의 Texture들을 하나의 이미지로 변경
    private void atlasTextures(LevelOfDetail lod, String codeName, List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials) {
        GaiaTextureCoordinator textureCoordinator = new GaiaTextureCoordinator(codeName, materials, dataSets);
        textureCoordinator.batchTextures(lod);
    }

    private boolean checkRepeat(GaiaMaterial material, GaiaBufferDataSet dataSet) {
        if (material.isRepeat()) {
            return true;
        }
        Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
        GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
        if (texCoordBuffer == null) {
            return false;
        }
        GaiaRectangle boundingRectangle = dataSet.getTexcoordBoundingRectangle();
        if (boundingRectangle != null) {
            Vector2d range = boundingRectangle.getRange();
            return range.x > 1.1f || range.y > 1.1f;
        }
        return false;
    }

    private void createColorBuffer(List<GaiaMaterial> materials, GaiaBufferDataSet bufferDataSet, LevelOfDetail lod) {
        GaiaMaterial material = findMaterial(materials, bufferDataSet.getMaterialId());
        Vector4d diffuseColor = material.getDiffuseColor();

        if (globalOptions.isDebugLod()) {
            float[] debugColor = lod.getDebugColor();
            diffuseColor.x = debugColor[0];
            diffuseColor.y = debugColor[1];
            diffuseColor.z = debugColor[2];
            diffuseColor.w = 1.0f;
        }

        Map<AttributeType, GaiaBuffer> bufferMap = bufferDataSet.getBuffers();
        GaiaBuffer positionBuffer = bufferMap.get(AttributeType.POSITION);
        int elementsCount = positionBuffer.getElementsCount();
        int length = elementsCount * 4;
        byte[] colorList = new byte[length];
        for (int i = 0; i < length; i+=4) {
            colorList[i] = (byte) (diffuseColor.x * 255);
            colorList[i + 1] = (byte) (diffuseColor.y * 255);
            colorList[i + 2] = (byte) (diffuseColor.z * 255);
            colorList[i + 3] = (byte) (diffuseColor.w * 255);
        }

        GaiaBuffer colorBuffer = new GaiaBuffer();
        colorBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
        colorBuffer.setGlType(GL20.GL_UNSIGNED_BYTE);
        colorBuffer.setElementsCount(elementsCount);
        colorBuffer.setGlDimension((byte) 4);
        colorBuffer.setBytes(colorList);
        bufferDataSet.getBuffers().put(AttributeType.COLOR, colorBuffer);
    }

    private GaiaBufferDataSet batchVertices(List<GaiaBufferDataSet> bufferDataSets) {
        GaiaBufferDataSet dataSet = new GaiaBufferDataSet();
        int totalIndicesCount = 0;
        int totalPositionCount = 0;
        int totalNormalCount = 0;
        int totalColorCount = 0;
        int totalTexCoordCount = 0;
        int totalBatchIdCount = 0;

        int indicesIndex = 0;
        int positionIndex = 0;
        int normalIndex = 0;
        int colorIndex = 0;
        int texCoordIndex = 0;
        int batchIdIndex = 0;

        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            GaiaBuffer normalBuffer = buffers.get(AttributeType.NORMAL);
            GaiaBuffer colorBuffer = buffers.get(AttributeType.COLOR);
            GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
            GaiaBuffer batchIdBuffer = buffers.get(AttributeType.BATCHID);

            if (indicesBuffer != null) {
                totalIndicesCount += indicesBuffer.getInts().length;
            }
            if (positionBuffer != null) {
                totalPositionCount += positionBuffer.getFloats().length;
            }
            if (normalBuffer != null) {
                totalNormalCount += normalBuffer.getFloats().length;
            }
            if (colorBuffer != null) {
                totalColorCount += colorBuffer.getBytes().length;
            }
            if (texCoordBuffer != null) {
                totalTexCoordCount += texCoordBuffer.getFloats().length;
            }
            if (batchIdBuffer != null) {
                totalBatchIdCount += batchIdBuffer.getFloats().length;
            }
        }

        int[] indices = new int[totalIndicesCount];
        float[] positions = new float[totalPositionCount];
        float[] normals = new float[totalNormalCount];
        float[] texCoords = new float[totalTexCoordCount];
        float[] batchIds = new float[totalBatchIdCount];
        byte[] colors = new byte[totalColorCount];

        GaiaRectangle batchedBoundingRectangle = null;
        int totalIndicesMax = 0;
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            GaiaBuffer normalBuffer = buffers.get(AttributeType.NORMAL);
            GaiaBuffer colorBuffer = buffers.get(AttributeType.COLOR);
            GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
            GaiaBuffer batchIdBuffer = buffers.get(AttributeType.BATCHID);

            if (indicesBuffer != null) {
                int indicesMax = 0;
                for (int indice : indicesBuffer.getInts()) {
                    int intIndice = indice < 0 ? indice + 65536 : indice;
                    indicesMax = Math.max(indicesMax, intIndice);
                    //short value = (short) (totalIndicesMax + intIndice);
                    int value = (totalIndicesMax + intIndice);
                    indices[indicesIndex++] = value;
                }
                totalIndicesMax = totalIndicesMax + (indicesMax + 1);
                totalIndicesCount += indicesBuffer.getInts().length;
            } else {
                log.error("indicesBuffer is null");
            }

            if (positionBuffer != null) {
                for (float position : positionBuffer.getFloats()) {
                    positions[positionIndex++] = position;
                }
            } else {
                log.error("positionBuffer is null");
            }

            if (normalBuffer != null) {
                for (float normal : normalBuffer.getFloats()) {
                    normals[normalIndex++] = normal;
                }
            }

            if (colorBuffer != null) {
                for (byte color : colorBuffer.getBytes()) {
                    colors[colorIndex++] = color;
                }
            }

            if (texCoordBuffer != null) {
                for (float texCoord : texCoordBuffer.getFloats()) {
                    texCoords[texCoordIndex++] = texCoord;
                }
            }

            if (batchIdBuffer != null) {
                for (float batchId : batchIdBuffer.getFloats()) {
                    batchIds[batchIdIndex++] = batchId;
                }
            }

            GaiaRectangle boundingRectangle = bufferDataSet.getTexcoordBoundingRectangle();
            if (boundingRectangle != null) {
                if (batchedBoundingRectangle == null) {
                    batchedBoundingRectangle = boundingRectangle;
                } else {
                    batchedBoundingRectangle.addBoundingRectangle(boundingRectangle);
                }
            }
        }

        dataSet.setTexcoordBoundingRectangle(batchedBoundingRectangle);
        if (totalPositionCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalPositionCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(positions);
            dataSet.getBuffers().put(AttributeType.POSITION, buffer);
        }
        if (totalNormalCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalNormalCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(normals);
            dataSet.getBuffers().put(AttributeType.NORMAL, buffer);
        }
        if (totalColorCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_BYTE);
            buffer.setElementsCount(totalColorCount / 4);
            buffer.setGlDimension((byte) 4);
            buffer.setBytes(colors);
            dataSet.getBuffers().put(AttributeType.COLOR, buffer);
        }
        if (totalTexCoordCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalTexCoordCount / 2);
            buffer.setGlDimension((byte) 2);
            buffer.setFloats(texCoords);
            dataSet.getBuffers().put(AttributeType.TEXCOORD, buffer);
        }
        if (totalBatchIdCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalBatchIdCount);
            buffer.setGlDimension((byte) 1);
            buffer.setFloats(batchIds);
            dataSet.getBuffers().put(AttributeType.BATCHID, buffer);
        }
        if (totalIndicesCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_INT);
            buffer.setElementsCount(totalIndicesCount);
            buffer.setGlDimension((byte) 1);
            buffer.setInts(indices);
            dataSet.getBuffers().put(AttributeType.INDICE, buffer);
        }
        return dataSet;
    }
}