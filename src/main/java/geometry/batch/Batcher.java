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
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;
import tiler.LevelOfDetail;
import util.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Batcher {
    private final GaiaSet batchedSet;
    private final GaiaUniverse universe;
    private GaiaBoundingBox globalBBox;
    private final LevelOfDetail lod;

    public Batcher(GaiaUniverse universe, GaiaBoundingBox boundingBox, LevelOfDetail lod) {
        this.batchedSet = new GaiaSet();
        this.batchedSet.setProjectName(universe.getName());
        this.universe = universe;
        this.globalBBox = boundingBox;
        this.lod = lod;
    }

    public GaiaSet batch() {
        List<GaiaSet> sets = universe.getSets();
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

        //rearrangeMaterial();
        calcGlobalBBox(); // ex
        List<GaiaMaterial> filterdMaterials = batchDuplicateMaterial(batchedDataSets, batchedMaterials);
        rearrangeMaterial(batchedDataSets, filterdMaterials);

        Vector3d translation = calcTranslation();
        List<GaiaBufferDataSet> filterdDataSets = batchDataSets(batchedDataSets, translation);
        checkIsRepeatMaterial(filterdDataSets, filterdMaterials);

        List<GaiaMaterial> clampMaterials = filterdMaterials.stream()
                .filter((material) -> !material.isRepeat())
                .collect(Collectors.toList());
        List<GaiaBufferDataSet> clampDataSets = filterdDataSets.stream().filter((bufferDataSet) -> {
            int materialId = bufferDataSet.getMaterialId();
            GaiaMaterial material = findMaterial(filterdMaterials, materialId);
            return !material.isRepeat();
        }).collect(Collectors.toList());

        List<GaiaMaterial> repeatMaterials = filterdMaterials.stream()
                .filter(GaiaMaterial::isRepeat)
                .collect(Collectors.toList());
        List<GaiaBufferDataSet> repeatDataSets = filterdDataSets.stream().filter((bufferDataSet) -> {
            int materialId = bufferDataSet.getMaterialId();
            GaiaMaterial material = findMaterial(filterdMaterials, materialId);
            return material.isRepeat();
        }).collect(Collectors.toList());

//        log.info("nonRepeatMaterials : " + clampMaterials.size());
//        log.info("nonRepeatBufferDatas : " + clampDataSets.size());
//        log.info("repeatMaterials : " + repeatMaterials.size());
//        log.info("repeatBufferDatas : " + repeatDataSets.size());

        if (clampDataSets.size() > 0 && clampMaterials.size() > 0) {
            atlasTextures(clampDataSets, clampMaterials);
        }

        List<List<GaiaBufferDataSet>> splitedDataSets = divisionByMaxIndices(clampDataSets);
        List<GaiaBufferDataSet> batchedClampDataSets = batchClampMaterial(splitedDataSets);
        clampMaterials.removeIf((clampMaterial) -> {
            return clampMaterial.getId() > 0;
        });

        List<GaiaBufferDataSet> resultBufferDatas = new ArrayList<>();
        resultBufferDatas.addAll(batchedClampDataSets);

        List<GaiaMaterial> resultMaterials = new ArrayList<>();
        resultMaterials.addAll(clampMaterials);

        this.batchedSet.setBufferDatas(resultBufferDatas);
        this.batchedSet.setMaterials(resultMaterials);

        Matrix4d transform = new Matrix4d();
        transform.identity();

        this.batchedSet.setTransformMatrix(transform);

        return this.batchedSet;
    }

    // Material Id 재정렬
    @Deprecated
    private void rearrangeMaterial() {
        List<GaiaSet> sets = universe.getSets();
        int materialIdOffset = 0;
        sets.forEach((set) -> {
            set.getBufferDatas().forEach((dataSet) -> {
                int materialId = dataSet.getMaterialId();
                dataSet.setMaterialId(materialIdOffset + materialId);
            });
            set.getMaterials().forEach((material) -> {
                material.setId((materialIdOffset) + material.getId());
            });
        });
    }

    // Material Id 재정렬
    private void rearrangeMaterial(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials) {
        dataSets.forEach((batchedBufferData) -> {
            for (int i = 0; i < materials.size(); i++) {
                GaiaMaterial material = materials.get(i);
                if (material.getId() == batchedBufferData.getMaterialId()) {
                    batchedBufferData.setMaterialId(i);
                    break;
                }
            }
        });
        for (int i = 0; i < materials.size(); i++) {
            GaiaMaterial material = materials.get(i);
            material.setId(i);
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
    private Vector3d calcTranslation() {
        Vector3d temp = this.globalBBox.getCenter();
        Vector3d translation = new Vector3d(temp.x(), temp.y(), this.globalBBox.getMinZ());
        //Vector3d translation = new Vector3d(temp.x(), temp.y(), temp.z());
        translation.negate();
        return translation;
    }

    // Indices 최대 값 만큼 객체를 나눔
    private List<List<GaiaBufferDataSet>> divisionByMaxIndices(List<GaiaBufferDataSet> dataSets) {
        final int SHORT_LIMIT = 65535;
        int count = 0;
        List<List<GaiaBufferDataSet>> result = new ArrayList<>();
        List<GaiaBufferDataSet> splited = new ArrayList<>();
        result.add(splited);
        for (GaiaBufferDataSet dataSet : dataSets) {
            LinkedHashMap<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesLength = indicesBuffer.getShorts().length;

                if ((count + indicesLength) > SHORT_LIMIT) {
                    splited = new ArrayList<>();
                    result.add(splited);
                    count = indicesLength;
                } else {
                    count += indicesLength;
                }
                splited.add(dataSet);
            }
        }
        return result;
    }

    // 객체의 Indices와 Vertices를 하나로 배칭
    private List<GaiaBufferDataSet> batchDataSets(List<GaiaBufferDataSet> dataSets, Vector3d translation) {
        LinkedHashMap<Integer, List<GaiaBufferDataSet>> dataSetsMap = new LinkedHashMap<>();
        List<GaiaBufferDataSet> filterdBufferDatas = new ArrayList<>();
        for (GaiaBufferDataSet dataSet : dataSets) {
            int materialId = dataSet.getMaterialId();
            List<GaiaBufferDataSet> bufferDataSetDataList = dataSetsMap.computeIfAbsent(materialId, k -> new ArrayList<>());
            bufferDataSetDataList.add(dataSet);
            translateOrigin(dataSet, translation);
        }
        dataSetsMap.forEach((materialId, bufferDataSetDataList) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(bufferDataSetDataList);
            batchedBufferData.setMaterialId(materialId);
            filterdBufferDatas.add(batchedBufferData);
        });
        return filterdBufferDatas;
    }

    // 모든 객체를 0,0 기준으로 이동
    private void translateOrigin(GaiaBufferDataSet batchedBufferData, Vector3d translations) {
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
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

    // 같은 Material 배칭
    private List<GaiaMaterial> batchDuplicateMaterial(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials) {
        List<GaiaMaterial> filterdMaterials = materials.stream().filter((material) -> {
            int materialId = material.getId();
            GaiaMaterial sameMaterial = findDuplicateMaterial(material, materials);
            if (sameMaterial != null) {
                dataSets.forEach((dataSet) -> {
                    int usedMaterialId = dataSet.getMaterialId();
                    if (usedMaterialId == materialId) {
                        dataSet.setMaterialId(sameMaterial.getId());
                    }
                });
                return materialId <= sameMaterial.getId();
            }
            return true;
        }).collect(Collectors.toList());

        // MaterialId를 재정렬
        dataSets.forEach((dataSet) -> {
            for (int i = 0; i < filterdMaterials.size(); i++) {
                GaiaMaterial material = filterdMaterials.get(i);
                if (material.getId() == dataSet.getMaterialId()) {
                    dataSet.setMaterialId(i);
                    break;
                }
            }
        });
        for (int i = 0; i < filterdMaterials.size(); i++) {
            GaiaMaterial material = filterdMaterials.get(i);
            material.setId(i);
        }
        return filterdMaterials;
    }

    // 같은 Material 찾기
    private GaiaMaterial findDuplicateMaterial(GaiaMaterial target, List<GaiaMaterial> materials) {
        return materials.stream().filter((searchMaterial) -> {
            return target.compareTo(searchMaterial);
        }).findFirst().orElse(null);
    }

    // 바둑판 텍스쳐인지 확인
    private void checkIsRepeatMaterial(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials) {
        for (GaiaBufferDataSet dataSet : dataSets) {
            int materialId = dataSet.getMaterialId();
            GaiaMaterial material = findMaterial(materials, materialId);
            if (material != null) {
                LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
                List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
                if (textures == null || textures.size() == 0) {
                    LinkedHashMap<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
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
        textureCoordinator.batchTextures(lod);
        textureCoordinator.writeBatchedImage(this.universe.getOutputRoot().resolve("images"));
    }

    private boolean checkRepeat(GaiaMaterial material, GaiaBufferDataSet dataSet) {
        if (material.isRepeat()) {
            return true;
        }
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
        GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
        if (texCoordBuffer == null) {
            return false;
        }
        GaiaRectangle boundingRectangle = dataSet.getTexcoordBoundingRectangle();
        if (boundingRectangle != null) {
            Vector2d range = boundingRectangle.getRange();
            return range.x > 1.00f || range.y > 1.001f;
        }
        return false;
    }

    // getBoundingBox
    private GaiaBoundingBox getBoundingBox(GaiaBufferDataSet batchedBufferData, Matrix4d transform) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
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
        int totalIndicesCount = 0;

        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Short> indices = new ArrayList<>();
        GaiaRectangle batchedBoundingRectangle = null;
        int totalIndicesMax = 0;
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesMax = 0;
                for (short indice : indicesBuffer.getShorts()) {
                    indicesMax = Math.max(indicesMax, indice);
                    int value = totalIndicesMax + indice;
                    indices.add((short) value);
                }
                totalIndicesMax = totalIndicesMax + (indicesMax + 1);
                totalIndicesCount += indicesBuffer.getShorts().length;
            }

            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            if (positionBuffer != null) {
                totalPositionCount += positionBuffer.getFloats().length;
                for (float position : positionBuffer.getFloats()) {
                    positions.add(position);
                }
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

    private GaiaMaterial findMaterial(List<GaiaMaterial> materials, int materialId) {
        return materials.stream()
                .filter(material -> material.getId() == materialId)
                .findFirst()
                //.orElse(materials.get(0));
                .orElseThrow(() -> new RuntimeException("not found material"));
    }
}
