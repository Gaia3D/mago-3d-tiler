다양한 변환 사례
===

## 3D 메시 데이터 변환 (Batched Model)

### 일반적인 메시 데이터 변환
기본적인 메시 데이터를 변환하는 명령어입니다. 변환 시 기본 좌표계는 EPSG:3857로 설정됩니다.

```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path"
```

동일한 경우:
```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path" -crs 3857
```

### 배치 3D 모델 (b3dm)
일반적인 데이터를 변환할 때 사용할 수 있습니다. 포인트 클라우드 데이터를 제외하고, `-outputType`을 입력하지 않으면 기본적으로 b3dm으로 생성됩니다.

```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

동일한 경우:
```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

### DEM 고도 적용 사례
GeoTiff와 같은 단일 채널 지형 높이에 3D 데이터를 배치하는 경우입니다.

```
java -jar mago-3d-tiler.jar -input "/input_path/sample" -output "/output_path/sample" -terrain "/input_path/sample/terrain.tif"
```

### 좌표계를 적용한 3D 데이터 변환
다음 예시는 3ds(3D MAX) 데이터를 변환하는 예제입니다. `crs` 옵션을 추가하여 좌표계를 적용하여 변환할 수 있습니다. 아래의 경우 EPSG:5186 좌표계를 적용했습니다.

```
java -jar mago-3d-tiler.jar -input "/input_path/3ds" -inputType "3ds" -output "/output_path/3ds" -crs "5186"
```

---
# 2D 벡터 데이터 변환

### 2D GIS 폴리곤 데이터(SHP, GeoJSON) 변환
다음 예시는 2D GIS 폴리곤 데이터를 돌출하는(Extrude) 방법입니다. 돌출 높이는 `-heightColumn <arg>` 속성을 사용하여 특정 속성을 참조하도록 지정할 수 있습니다. 기본적으로 돌출 시작 높이는 0이며, `-altitudeColumn <arg>` 옵션을 사용하여 기준면의 높이를 설정할 수 있습니다.

```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```

동일한 경우:
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186" -heightAttribute "height"
```

GeoJSON 변환 사례:
```
java -jar mago-3d-tiler.jar -input "/input_path/geojson" -inputType "geojson" -output "/output_path/geojson" -crs "5186"
```

### 2D GIS 폴리라인 데이터(SHP) 변환
폴리라인 데이터를 파이프 형태로 변환합니다. Z축이 있는 폴리라인 데이터는 `diameter` 속성을 이용해 변환할 수 있습니다. 기본적으로 mago 3DTiler의 파이프 크기는 직경 기준이며 단위는 밀리미터(mm)입니다.
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```

---
# 인스턴스 모델 변환 (Instanced Model)

### 인스턴스 3D 모델 (i3dm)
인스턴스 모델 데이터를 변환할 때 사용할 수 있는 옵션입니다. (kml with collada) 데이터를 변환할 때 `outputType` 옵션이 필요합니다.
```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -outputType "i3dm"
```

### i3dm 데이터를 Shape 파일로 변환
i3dm 데이터를 Point 형식의 Shape 파일로 변환할 수 있습니다. `inputType`을 shp로 지정하고, `instance` 옵션을 사용해 인스턴스 파일 경로를 입력해야 합니다.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -inputType "shp" -outputType "i3dm" -instance "/input_path/instance.gltf"
```

---
# 포인트 클라우드 데이터 변환 (Point Clouds)

### 포인트 클라우드 데이터 변환(Point Clouds)
포인트 클라우드 데이터를 변환할 때 사용할 수 있는 기본 옵션입니다. 입력 데이터가 "las"인 경우, `-outputType`은 자동으로 "pnts"로 설정됩니다.

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las"
```

동일한 경우:
```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -outputType "pnts"
```

---
# 기타 변환 예제

### Up-Axis 변환
mago3dTiler는 기본적으로 Z-Up 축 데이터를 Y-Up 축으로 변환합니다. 원본 데이터가 Y-Up 축을 사용하는 경우, `-rotateX <degree>` 옵션을 추가하여 변환을 방지할 수 있습니다.
```
java -jar mago-3d-tiler.jar -input "/input_path/y-up-fbx" -inputType "fbx" -output "/output_path/y-up-fbx" -rotateX "90"
```

### 뒤집힌 데이터 변환
변환된 데이터가 뒤집혀 있는 경우, `-rotateX <degree>` 옵션을 추가하여 변환할 수 있습니다.
```
java -jar mago-3d-tiler.jar -input "/input_path/flip-y-up-fbx" -inputType "fbx" -output "/output_path/flip-y-up-fbx" -rotateX "180"
```

### CityGML 변환
CityGML 데이터를 변환할 때 `inputType`을 "citygml"로 지정하는 것이 좋습니다. 이는 CityGML 데이터의 확장자가 `.xml`, `.gml` 등 다양할 수 있기 때문입니다.
```
java -jar mago-3d-tiler.jar -input "/input_path/citygml" -inputType "citygml" -output "/output_path/citygml" -crs "5186"
```

### Converting Large Point-Clouds Data
When converting large point-clouds, you can use the `-pointRatio` option to adjust the percentage of conversion points from the source data as follows.

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -pointRatio "100"
```

### How to run with Docker
You can also conveniently convert to mago 3DTiler deployed on docker hub.
Pull the image of gaia3d/mago-3d-tiler and create a container.


```docker
docker pull gaia3d/mago-3d-tiler
```

Specify the input and output data paths through the workspace volume.

```docker
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -input /workspace/3ds-samples -output /workspace/sample-3d-tiles -inputType 3ds -crs 5186
```