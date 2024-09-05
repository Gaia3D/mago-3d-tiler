![mago_3DTiler_256](https://github.com/Gaia3D/mago-3d-tiler/assets/87691347/792058e4-e41e-4f39-97e5-1a059b8d70b5)
==
mago 3DTiler: The Premier OGC 3D Tiles Solution!
--
Unlock the potential of your geospatial projects with mago 3DTiler   
the robust, versatile tool designed to elevate your 3D data into the realm of OGC 3D Tiles with unprecedented ease.

### Why mago 3DTiler? 
mago 3DTiler isn’t just a converter;   
developed with Java, this open-source marvel stands as a beacon for flexibility and performance in the world of 3D data conversion.

### Key Features:​
- **Multi-Format Mastery**: Effortlessly convert an array of 3D formats, including 3DS, OBJ, FBX, Collada DAE, glTF , IFC and more. ​
- **Point Cloud Precision**: Bring your detailed point cloud data (LAS, LAZ) into the fold with pinpoint accuracy.​
- **2D to 3D Extrusion**: Turn 2D geospatial data (ESRI SHP, GeoJSON) into detailed 3D extrusion models, breathing life into flat representations.​
- **On-The-Fly CRS Conversion**: Leverage the power of multi-threading and on-the-fly coordinate conversion with comprehensive PCS and GCS support via the Proj4 library.​
  
### How to Use:
Basically, when you modify source, you can generate a runnable jar via shadowjar, the tiler project's gradle script.   
There are pre-built jars in the /tiler/dist/ directory.
- mago-3d-tiler-x.x.x-natives-windows.jar   
- mago-3d-tiler-x.x.x-natives-linux.jar   
- mago-3d-tiler-x.x.x-natives-macos.jar

The JAVA version used when building is JDK 17.   
Below is an example of running the Help code.
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar --help
```
console output:
```
┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┏┳┓┳┓ ┏┓┳┓
┃┃┃┣┫┃┓┃┃   ┫┃┃   ┃ ┃┃ ┣ ┣┫
┛ ┗┛┗┗┛┗┛  ┗┛┻┛   ┻ ┻┗┛┗┛┛┗
3d-tiler(dev-version) by Gaia3D, Inc.
----------------------------------------
usage: Gaia3D Tiler
 -aa,--absoluteAltitude <arg>    Absolute altitude value for extrusion
                                 model
 -ac,--altitudeColumn <arg>      Altitude Column setting for extrusion
                                 model ((Default Column: altitude)
 -c,--crs <arg>                  Coordinate Reference Systems, EPSG
                                 Code(4326, 3857, 32652, 5186...)
 -d,--debug                      More detailed log output and stops on
                                 Multi-Thread bugs.
 -dc,--diameterColumn <arg>      Diameter column setting for extrusion
                                 model, Specify a length unit for Diameter
                                 in millimeters(mm) (Default Column:
                                 diameter)
 -fc,--flipCoordinate            Flip x, y Coordinate (Default: false)
 -glb,--glb                      Create glb file with B3DM.
 -h,--help                       Print Gelp
 -hc,--heightColumn <arg>        Height column setting for extrusion model
                                 ((Default Column: height)
 -i,--input <arg>                Input directory path
 -if,--instance <arg>            Instance file path for I3DM (Default:
                                 {OUTPUT}/instance.dae)
 -igtx,--ignoreTextures          Ignore diffuse textures.
 -it,--inputType <arg>           Input files type (kml, 3ds, fbx, obj,
                                 gltf, glb, las, laz, citygml, indoorgml,
                                 shp, geojson)(Default: kml)
 -l,--log <arg>                  Output log file path.
 -lm,--largeMesh                 [Experimental] Large Mesh Splitting Mode
                                 (Default: false)
 -mc,--multiThreadCount <arg>    set Multi-Thread count
 -mg,--maxGeometricError <arg>   Maximum geometric error (Default: Integer
                                 max value)
 -mh,--minimumHeight <arg>       Minimum height value for extrusion model
                                 (Default: 1.0)
 -mp,--maxPoints <arg>           Limiting the maximum number of points in
                                 point cloud data. (Default: 65536)
 -mx,--maxCount <arg>            Maximum number of triangles per node.
 -nc,--nameColumn <arg>          Name column setting for extrusion model
                                 (Default Column: name)
 -ng,--minGeometricError <arg>   Minimum geometric error (Default: 16.0)
 -nl,--minLod <arg>              min level of detail (Default: 0)
 -o,--output <arg>               Output directory file path
 -ot,--outputType <arg>          Output 3DTiles Type (b3dm, i3dm,
                                 pnts)(Default : b3dm)
 -p,--proj <arg>                 Proj4 parameters (ex: +proj=tmerc +la...)
 -pk,--pointSkip <arg>           Number of pointcloud omissions (ex:
                                 1/4)(Default: 4)
 -ps,--pointScale <arg>          Pointscloud geometryError scale setting
                                 (Default: 2)
 -q,--quiet                      Quiet mode/Silent mode
 -r,--recursive                  Tree directory deep navigation.
 -ra,--refineAdd                 Set 3D Tiles Refine 'ADD' mode
 -ru,--flipUpAxis                Rotate the matrix 180 degrees about the
                                 X-axis. (Default: false)
 -sh,--skirtHeight <arg>         Building Skirt height setting for
                                 extrusion model (Default: 4.0)
 -su,--swapUpAxis                Rotate the matrix -90 degrees about the
                                 X-axis. (Default: false)
 -te,--terrain <arg>             GeoTiff Terrain file path, 3D Object
                                 applied as clampToGround (Supports
                                 geotiff format)
 -v,--version                    Print Version Info
 -xl,--maxLod <arg>              Max Level of detail (Default: 3)
 -zo,--zeroOrigin                [Experimental] fix 3d root transformed
                                 matrix origin to zero point.
```
This is a simple kml/collada -> 3dTiles conversion code with the mandatory argument values.    
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar --input C:\data\kml-input-dir --inputType kml --output C:\data\kml-output-dir
```
or
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -i C:\data\kml-input-dir -o C:\data\kml-output-dir
```

### How to use Docker version:
Alternatively, you can easily use mago3dtiler with docker.

Example usage : 
```
docker pull gaia3d/mago-3d-tiler
```
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -it 3ds -i /workspace/3ds-samples -o /workspace/sample-3d-tiles -crs 5186
```

### Supported Java versions:
It supports compatibility with long-term support (LTS) versions of the JDK, such as JDK11, JDK17 and JDK21.   
JDK21 has been found to be partially available.

### Experience the mago 3DTiler:
![image](https://github.com/Gaia3D/mago-3d-tiler/assets/87691347/c778f7e1-771c-4df6-8d4c-b46412c80c19)   
<https://seoul.gaia3d.com:10903>

### Explore and Experience:
- **Community and Code**: Join our vibrant community on GitHub and contribute to the future of 3D data conversion.​
- **Freedom to Innovate**: Embrace the flexibility of MPL2.0 licensing(<https://www.mozilla.org/en-US/MPL/2.0/>)​,
  ensuring your freedom to use, modify, and distribute without hindrance.
- **License**: If you prefer not to share your modified or improved code under the MPL2.0 license, you can opt for a commercial license instead.
In this case, please contact us at sales@gaia3d.com

### Library Dependencies:
- **LWJGL3** (Lightweight Java Game Library 3 Opengl, Assimp): <https://github.com/LWJGL/lwjgl3>
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **jgltf** (Java libraries for glTF): <https://github.com/javagl/JglTF>
- **laszip4j** (The LASzip library ported to Java): <https://github.com/mreutegg/laszip4j>
- **geotools** (Geospatial data library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>
- **citygml4j** (The Open Source Java API for CityGML): <https://github.com/citygml4j/citygml4j>

---


![mago_3DTiler_256](https://github.com/Gaia3D/mago-3d-tiler/assets/87691347/792058e4-e41e-4f39-97e5-1a059b8d70b5)
==
mago 3DTiler: The Premier OGC 3D Tiles Solution!
--

### 개요
mago 3DTiler는 오픈소스 기반의 OGC 3D Tiles 변환기입니다.   
다양한 공간정보 데이터를 디지털트윈 서비스의 근간이 되는 OGC 3D Tiles로 변환해 줍니다.   
mago 3DTiler는 Java 기반으로 뛰어난 이식성, 유연함과 함께 빠른 속도를 자랑합니다.   

### 주요 기능:
- 다양한 포맷 지원: 3DS, OBJ, FBX, Collada DAE, glTF, IFC 등 다양한 3D 형식을 손쉽게 변환합니다.​
- 포인트 클라우드: LAS, LAZ 등의 세밀한 포인트 클라우드 데이터를 정확하게 변환합니다.​
- 2D에서 3D로의 Extrusion 변환: 객체 속성값을 활용해 ESRI SHP, GeoJSON 같은 2D 데이터를 3차원으로 변환합니다. ​
- 실시간 좌표 변환: Proj4 라이브러리를 통해 전 세계 좌표계를 지원하며, 입력 좌표계와 출력 좌표계 설정을 통해 3D Tiles 제작 시 실시간 좌표변환을 지원합니다. 

### 사용법:
기본적으로 코드 수정 시 tiler프로젝트의 gradle script인 shadowjar를 통해 runnable jar를 생성할 수 있습니다.   
/tiler/dist/ 디렉토리에는 미리 빌드된 jar가 준비 되어있습니다.
- mago-3d-tiler-x.x.x-natives-windows.jar   
- mago-3d-tiler-x.x.x-natives-linux.jar   
- mago-3d-tiler-x.x.x-natives-macos.jar
빌드 시 사용된 java 버전은 jdk 17 입니다.

아래는 Help 코드를 실행시킨 예시입니다.
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -h
```
출력 결과물: 
```
┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┏┳┓┳┓ ┏┓┳┓
┃┃┃┣┫┃┓┃┃   ┫┃┃   ┃ ┃┃ ┣ ┣┫
┛ ┗┛┗┗┛┗┛  ┗┛┻┛   ┻ ┻┗┛┗┛┛┗
3d-tiler(dev-version) by Gaia3D, Inc.
----------------------------------------
usage: Gaia3D Tiler
 -aa,--absoluteAltitude <arg>    Absolute altitude value for extrusion
                                 model
 -ac,--altitudeColumn <arg>      Altitude Column setting for extrusion
                                 model ((Default Column: altitude)
 -c,--crs <arg>                  Coordinate Reference Systems, EPSG
                                 Code(4326, 3857, 32652, 5186...)
 -d,--debug                      More detailed log output and stops on
                                 Multi-Thread bugs.
 -dc,--diameterColumn <arg>      Diameter column setting for extrusion
                                 model, Specify a length unit for Diameter
                                 in millimeters(mm) (Default Column:
                                 diameter)
 -fc,--flipCoordinate            Flip x, y Coordinate (Default: false)
 -glb,--glb                      Create glb file with B3DM.
 -h,--help                       Print Gelp
 -hc,--heightColumn <arg>        Height column setting for extrusion model
                                 ((Default Column: height)
 -i,--input <arg>                Input directory path
 -if,--instance <arg>            Instance file path for I3DM (Default:
                                 {OUTPUT}/instance.dae)
 -igtx,--ignoreTextures          Ignore diffuse textures.
 -it,--inputType <arg>           Input files type (kml, 3ds, fbx, obj,
                                 gltf, glb, las, laz, citygml, indoorgml,
                                 shp, geojson)(Default: kml)
 -l,--log <arg>                  Output log file path.
 -lm,--largeMesh                 [Experimental] Large Mesh Splitting Mode
                                 (Default: false)
 -mc,--multiThreadCount <arg>    set Multi-Thread count
 -mg,--maxGeometricError <arg>   Maximum geometric error (Default: Integer
                                 max value)
 -mh,--minimumHeight <arg>       Minimum height value for extrusion model
                                 (Default: 1.0)
 -mp,--maxPoints <arg>           Limiting the maximum number of points in
                                 point cloud data. (Default: 65536)
 -mx,--maxCount <arg>            Maximum number of triangles per node.
 -nc,--nameColumn <arg>          Name column setting for extrusion model
                                 (Default Column: name)
 -ng,--minGeometricError <arg>   Minimum geometric error (Default: 16.0)
 -nl,--minLod <arg>              min level of detail (Default: 0)
 -o,--output <arg>               Output directory file path
 -ot,--outputType <arg>          Output 3DTiles Type (b3dm, i3dm,
                                 pnts)(Default : b3dm)
 -p,--proj <arg>                 Proj4 parameters (ex: +proj=tmerc +la...)
 -pk,--pointSkip <arg>           Number of pointcloud omissions (ex:
                                 1/4)(Default: 4)
 -ps,--pointScale <arg>          Pointscloud geometryError scale setting
                                 (Default: 2)
 -q,--quiet                      Quiet mode/Silent mode
 -r,--recursive                  Tree directory deep navigation.
 -ra,--refineAdd                 Set 3D Tiles Refine 'ADD' mode
 -ru,--flipUpAxis                Rotate the matrix 180 degrees about the
                                 X-axis. (Default: false)
 -sh,--skirtHeight <arg>         Building Skirt height setting for
                                 extrusion model (Default: 4.0)
 -su,--swapUpAxis                Rotate the matrix -90 degrees about the
                                 X-axis. (Default: false)
 -te,--terrain <arg>             GeoTiff Terrain file path, 3D Object
                                 applied as clampToGround (Supports
                                 geotiff format)
 -v,--version                    Print Version Info
 -xl,--maxLod <arg>              Max Level of detail (Default: 3)
 -zo,--zeroOrigin                [Experimental] fix 3d root transformed
                                 matrix origin to zero point.
```

필수 인자 값으로 작성한 간단한 kml/collada -> 3dTiles 변환코드 입니다.
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar --input C:\data\kml-input-dir --inputType kml --output C:\data\kml-output-dir
```
또는
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -i C:\data\kml-input-dir -o C:\data\kml-output-dir
```

### 도커 버전 사용법:
mago 3DTiler 1.3.1 버전부터 도커 버전으로 손쉽게 사용할 수 있습니다.

사용 예시: 
```
docker pull gaia3d/mago-3d-tiler
```
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -it 3ds -i /workspace/3ds-samples -o /workspace/sample-3d-tiles -crs 5186
```

### 지원하는 자바 버전:
JDK11, JDK17, JDK21 등 JDK의 LTS(Long-term support) 버전의 호환을 지원합니다.   
JDK21는 부분적으로 사용이 가능한 것을 확인했습니다.

### 샘플 사이트: 
![image](https://github.com/Gaia3D/mago-3d-tiler/assets/87691347/c778f7e1-771c-4df6-8d4c-b46412c80c19)   
<https://seoul.gaia3d.com:10903>

### 라이선스: 
- mago 3DTiler는 MPL2.0 라이선스를 따릅니다. (<https://www.mozilla.org/en-US/MPL/2.0/>)
- 만약 MPL2.0라이선스에 따라 여러분이 개작, 수정한 코드를 공개하고 싶지 않으면 상업 라이선스를 따르시면 됩니다. 이 경우에는 sales@gaia3d.com으로 연락 주시기 바랍니다.

### 라이브러리 의존성: 
- **LWJGL3** (Lightweight Java Game Library 3 Opengl, Assimp): <https://github.com/LWJGL/lwjgl3>
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **jgltf** (Java libraries for glTF): <https://github.com/javagl/JglTF>
- **laszip4j** (The LASzip library ported to Java): <https://github.com/mreutegg/laszip4j>
- **geotools** (Geospatial data library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>
- **citygml4j** (The Open Source Java API for CityGML): <https://github.com/citygml4j/citygml4j>
