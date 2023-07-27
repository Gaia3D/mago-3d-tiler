package geometry.batch;

import geometry.basic.GaiaBoundingBox;
import geometry.basic.GaiaRectangle;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaBufferDataSet;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;
import tiler.BatchInfo;
import tiler.LevelOfDetail;
import util.ArrayUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GaiaBatcher implements Batcher {
    private final static int SHORT_LIMIT = 65535;
    private final GaiaSet batchedSet;
    private final GaiaUniverse universe;
    private GaiaBoundingBox globalBBox;
    private final LevelOfDetail lod;
    private final CommandLine command;

    public GaiaBatcher(BatchInfo tileInfo, CommandLine command) {
        GaiaUniverse universe = tileInfo.getUniverse();
        universe.convertGaiaSet();

        this.command = command;
        this.globalBBox = tileInfo.getBoundingBox();
        this.universe = universe;
        this.lod = tileInfo.getLod();
        this.batchedSet = new GaiaSet();
        this.batchedSet.setProjectName(this.universe.getName());
    }

    private void reassignMaterialsToGaiaBufferDataSetWithSameMaterial(List<GaiaBufferDataSet> dataSets) throws IOException {
        int datasetsCount = dataSets.size();
        for(int i=0; i<datasetsCount; i++)
        {
            GaiaBufferDataSet dataSet = dataSets.get(i);
            GaiaMaterial material = dataSet.material;
            for(int j=i+1; j<datasetsCount; j++)
            {
                GaiaBufferDataSet dataSet2 = dataSets.get(j);
                if(dataSet == dataSet2)
                    continue;

                if(dataSet2.material == material)
                {
                    continue;
                }

                if(GaiaMaterial.areEqualMaterials(dataSet2.material, material, lod))
                {
                    //dataSet2.material.deleteTextures();
                    dataSet2.material = material;
                }
            }
        }
    }

    private void setBatchId (List<GaiaSet> sets) {
        for (int i = 0; i < sets.size(); i++) {
            GaiaSet set = sets.get(i);
            List<GaiaBufferDataSet> dataSets = set.getBufferDatas();
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

    public GaiaSet batch() throws IOException {
        log.info("Batching started : {}", universe.getName());
        List<GaiaSet> sets = universe.getSets();
        setBatchId(sets);

        List<GaiaBufferDataSet> batchedDataSets = new ArrayList<>();
        List<GaiaMaterial> batchedMaterials = new ArrayList<>();
        sets.forEach((set) -> {
            List<GaiaBufferDataSet> dataSets = set.getBufferDatas();
            List<GaiaMaterial> materials = set.getMaterials();
            int materialIdOffset = batchedMaterials.size();
            dataSets.forEach((dataSet) -> {
                int materialId = dataSet.getMaterialId();
                dataSet.setTransformMatrix(set.getTransformMatrix());
                dataSet.setMaterialId(materialIdOffset + materialId);
            });
            materials.forEach((material) -> {
                int materialId = material.getId();
                material.setId(materialIdOffset + materialId);
            });
            batchedDataSets.addAll(dataSets);
            batchedMaterials.addAll(materials);
        });

        //log.info("time);
        calcGlobalBBox();
        // check if exist equal materials.***
        reassignMaterialsToGaiaBufferDataSetWithSameMaterial(batchedDataSets);
        List<GaiaMaterial> filteredMaterials = new ArrayList<>();
        GaiaBufferDataSet.getMaterialslIstOfBufferDataSet(batchedDataSets, filteredMaterials);

        // 1rst delete textures of materials to free memory.
        /*filteredMaterials.forEach((material) -> {
            material.deleteTextures();
        });*/

        Vector3d translation = calcTranslation();

        // batch dataSets with same material.***
        List<GaiaBufferDataSet> filteredDataSets = batchDataSetsWithTheSameMaterial(batchedDataSets, translation);
        setMaterialsIndexInList(filteredMaterials, filteredDataSets);
        checkIsRepeatMaterial(filteredDataSets);

        List<GaiaMaterial> clampMaterials = filteredMaterials.stream()
                .filter((material) -> !material.isRepeat())
                .collect(Collectors.toList());
        List<GaiaBufferDataSet> clampDataSets = filteredDataSets.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = bufferDataSet.material;
            return !material.isRepeat();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(clampMaterials, clampDataSets);

        List<GaiaMaterial> repeatMaterials = filteredMaterials.stream()
                .filter(GaiaMaterial::isRepeat)
                .collect(Collectors.toList());
        List<GaiaBufferDataSet> repeatDataSets = filteredDataSets.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = bufferDataSet.material;
            return material.isRepeat();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(repeatMaterials, repeatDataSets);

        List<GaiaBufferDataSet> resultBufferDatas = new ArrayList<>();
        List<GaiaMaterial> resultMaterials = new ArrayList<>();
        if (clampDataSets.size() > 0 && clampMaterials.size() > 0) {
            atlasTextures(clampDataSets, clampMaterials);
            List<List<GaiaBufferDataSet>> splitedDataSets = divisionByMaxIndices(clampDataSets);
            List<GaiaBufferDataSet> batchedClampDataSets = batchClampMaterial(splitedDataSets);
            clampMaterials.removeIf((clampMaterial) -> {
                return clampMaterial.getId() > 0;
            });
            clampMaterials.get(0).setName("ATLAS");
            resultMaterials.addAll(clampMaterials);
            resultBufferDatas.addAll(batchedClampDataSets);
        }

        if (repeatDataSets.size() > 0 && repeatMaterials.size() > 0) {
            rearrangeRepeatMaterial(repeatDataSets, repeatMaterials, resultMaterials.size());
            resultMaterials.addAll(repeatMaterials);
            resultBufferDatas.addAll(repeatDataSets);
        }

        this.batchedSet.setBufferDatas(resultBufferDatas);
        this.batchedSet.setMaterials(resultMaterials);

        if (resultBufferDatas.size() < 1 || resultMaterials.size() < 1) {
            log.error("Batched Set is empty");
        }

        Matrix4d transform = new Matrix4d();
        transform.identity();

        this.batchedSet.setTransformMatrix(transform);
        return this.batchedSet;
    }

    private void setMaterialsIndexInList(List<GaiaMaterial> materials, List<GaiaBufferDataSet> dataSets) {
        for (int i = 0; i < materials.size(); i++) {
            GaiaMaterial material = materials.get(i);
            material.setId(i);
        }

        dataSets.forEach((dataSet) -> {
            int materialId = dataSet.material.getId();
            dataSet.setMaterialId(materialId);
        });
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

    // Vertices 바운더리 구함
    private void calcGlobalBBox() {
        if (this.globalBBox == null) {
            this.globalBBox = new GaiaBoundingBox();
        }
        List<GaiaSet> sets = universe.getSets();
        sets.forEach((set) -> {
            set.getBufferDatas().forEach((dataSet) -> {
                this.globalBBox.addBoundingBox(getBoundingBox(dataSet, set.getTransformMatrix()));
            });
        });
    }

    // 원점이동 구함
    Vector3d calcTranslation() {
        Vector3d temp = this.globalBBox.getCenter();
        Vector3d translation = new Vector3d(temp.x(), temp.y(), this.globalBBox.getMinZ());
        translation.negate();
        return translation;
    }

    // Indices 최대 값 만큼 객체를 나눔
    private List<List<GaiaBufferDataSet>> divisionByMaxIndices(List<GaiaBufferDataSet> dataSets) {
        int count = 0;
        List<List<GaiaBufferDataSet>> result = new ArrayList<>();
        List<GaiaBufferDataSet> splitList = new ArrayList<>();
        result.add(splitList);
        for (GaiaBufferDataSet dataSet : dataSets) {
            Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesLength = indicesBuffer.getShorts().length;
                if ((count + indicesLength) >= SHORT_LIMIT) {
                    splitList = new ArrayList<>();
                    result.add(splitList);
                    count = indicesLength;
                } else {
                    count += indicesLength;
                }
                splitList.add(dataSet);
            }
        }
        return result;
    }

    // 객체의 Indices와 Vertices를 하나로 배칭
    /*rivate List<GaiaBufferDataSet> batchDataSets(List<GaiaBufferDataSet> dataSets, Vector3d translation) {
        Map<Integer, List<GaiaBufferDataSet>> dataSetsMap = new LinkedHashMap<>();
        List<GaiaBufferDataSet> filterdBufferDataList = new ArrayList<>();
        for (GaiaBufferDataSet dataSet : dataSets) {
            int materialId = dataSet.getMaterialId();
            List<GaiaBufferDataSet> bufferDataSetDataList = dataSetsMap.computeIfAbsent(materialId, k -> new ArrayList<>());
            bufferDataSetDataList.add(dataSet);
            translateOrigin(dataSet, translation);
        }
        dataSetsMap.forEach((materialId, bufferDataSetDataList) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(bufferDataSetDataList);
            batchedBufferData.setMaterialId(materialId);
            filterdBufferDataList.add(batchedBufferData);
        });
        return filterdBufferDataList;
    }*/

    private List<GaiaBufferDataSet> batchDataSetsWithTheSameMaterial(List<GaiaBufferDataSet> dataSets, Vector3d translation) {
        List<GaiaBufferDataSet> filterdBufferDataList = new ArrayList<>();
        // make map : key = GaiaMaterial, value = array<GaiaBufferDataSet>.
        Map<GaiaMaterial, List<GaiaBufferDataSet>> dataSetsMap = new LinkedHashMap<>();
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaMaterial material = dataSet.getMaterial();
            List<GaiaBufferDataSet> bufferDataSetDataList = dataSetsMap.computeIfAbsent(material, k -> new ArrayList<>());
            bufferDataSetDataList.add(dataSet);
            translateOrigin(dataSet, translation);
        }
        // make batched buffer data.
        dataSetsMap.forEach((material, bufferDataSetDataList) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(bufferDataSetDataList);
            batchedBufferData.setMaterial(material);
            filterdBufferDataList.add(batchedBufferData);
        });
        return filterdBufferDataList;
    }

    // 모든 객체를 0,0 기준으로 이동
    private void translateOrigin(GaiaBufferDataSet batchedBufferData, Vector3d translations) {
        Map<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
        GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
        Matrix4d transform = batchedBufferData.getTransformMatrix();
        Vector3d translatedPosition = transform.transformPosition(translations, new Vector3d());
        if (positionBuffer != null) {
            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
                Vector3d position = new Vector3d(x, y, z);
                position.add(translatedPosition);
                positions[i] = (float) position.x;
                positions[i + 1] = (float) position.y;
                positions[i + 2] = (float) position.z;
            }
        }
    }

    private void checkIsRepeatMaterial(List<GaiaBufferDataSet> dataSets) {
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaMaterial material = dataSet.material;
            if (material != null) {
                Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
                List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
                if (textures == null || textures.size() == 0) {
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
    private void atlasTextures(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials) {
        GaiaTextureCoordinator textureCoordinator = new GaiaTextureCoordinator(universe.getName(), materials, dataSets);
        textureCoordinator.batchTextures(lod, this.command);
        textureCoordinator.writeBatchedImage(this.universe.getOutputRoot().resolve("images"));
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

    // getBoundingBox
    private GaiaBoundingBox getBoundingBox(GaiaBufferDataSet batchedBufferData, Matrix4d transform) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        Map<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
        GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
        if (positionBuffer != null) {
            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
                Vector3d position = new Vector3d(x, y, z);
                Vector3d transformedPosition = new Vector3d(position);
                if (transform != null) {
                    transform.transformPosition(position, transformedPosition);
                }
                boundingBox.addPoint(transformedPosition);
            }
        }
        return boundingBox;
    }

    private GaiaBufferDataSet batchVertices(List<GaiaBufferDataSet> bufferDataSets) {
        GaiaBufferDataSet dataSet = new GaiaBufferDataSet();
        int totalPositionCount = 0;
        int totalNormalCount = 0;
        int totalTexCoordCount = 0;
        int totalBatchIdCount = 0;
        int totalIndicesCount = 0;

        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> batchIds = new ArrayList<>();
        List<Short> indices = new ArrayList<>();
        GaiaRectangle batchedBoundingRectangle = null;
        int totalIndicesMax = 0;
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesMax = 0;
                for (short indice : indicesBuffer.getShorts()) {
                    int intIndice = indice < 0 ? indice + 65536 : indice;
                    indicesMax = Math.max(indicesMax, intIndice);
                    int value = totalIndicesMax + intIndice;
                    indices.add((short) value);
                }
                totalIndicesMax = totalIndicesMax + (indicesMax + 1);
                totalIndicesCount += indicesBuffer.getShorts().length;
            } else {
                log.error("indicesBuffer is null");
            }

            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            if (positionBuffer != null) {
                totalPositionCount += positionBuffer.getFloats().length;
                for (float position : positionBuffer.getFloats()) {
                    positions.add(position);
                }
            } else {
                log.error("positionBuffer is null");
            }
            GaiaBuffer normalBuffer = buffers.get(AttributeType.NORMAL);
            if (normalBuffer != null) {
                totalNormalCount += normalBuffer.getFloats().length;
                for (float normal : normalBuffer.getFloats()) {
                    normals.add(normal);
                }
            }
            GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
            if (texCoordBuffer != null) {
                totalTexCoordCount += texCoordBuffer.getFloats().length;
                for (float texCoord : texCoordBuffer.getFloats()) {
                    texCoords.add(texCoord);
                }
            }
            GaiaBuffer batchIdBuffer = buffers.get(AttributeType.BATCHID);
            if (batchIdBuffer != null) {
                totalBatchIdCount += batchIdBuffer.getFloats().length;
                for (float batchId : batchIdBuffer.getFloats()) {
                    batchIds.add(batchId);
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
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(positions));
            dataSet.getBuffers().put(AttributeType.POSITION, buffer);
        }
        if (totalNormalCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalNormalCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(normals));
            dataSet.getBuffers().put(AttributeType.NORMAL, buffer);
        }
        if (totalTexCoordCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalTexCoordCount / 2);
            buffer.setGlDimension((byte) 2);
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(texCoords));
            dataSet.getBuffers().put(AttributeType.TEXCOORD, buffer);
        }
        if (totalBatchIdCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalBatchIdCount);
            buffer.setGlDimension((byte) 1);
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(batchIds));
            dataSet.getBuffers().put(AttributeType.BATCHID, buffer);
        }
        if (totalIndicesCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_SHORT);
            buffer.setElementsCount(totalIndicesCount);
            buffer.setGlDimension((byte) 1);
            buffer.setShorts(ArrayUtils.convertShortArrayToList(indices));
            dataSet.getBuffers().put(AttributeType.INDICE, buffer);
        }
        return dataSet;
    }
}