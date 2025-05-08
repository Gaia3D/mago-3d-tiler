mago 3DTiler
===

## 개요
### 최고의 OGC 3D Tiles 솔루션!
mago 3DTiler는 오픈 소스를 기반으로 한 OGC 3DTiles 생성기입니다.  
다양한 공간 정보 데이터를 OGC 3D Tiles로 변환하여 디지털 트윈 서비스를 위한 기반을 제공합니다.  
Java 기반으로 개발된 mago 3DTiler는 높은 이식성, 유연성, 그리고 빠른 성능을 자랑합니다.

![Static Badge](https://img.shields.io/badge/Gaia3D%2C%20Inc-blue?style=flat-square)
![Static Badge](https://img.shields.io/badge/3DTiles-green?style=flat-square&logo=Cesium)
![Static Badge](https://img.shields.io/badge/Jdk17-red?style=flat-square&logo=openjdk)
![Static Badge](https://img.shields.io/badge/Gradle-darkorange?style=flat-square&logo=gradle)
![Static Badge](https://img.shields.io/badge/Docker%20Image-blue?style=flat-square&logo=docker)

![tiler-images](https://github.com/user-attachments/assets/1c496ac5-053a-42c0-a6a7-e2c3b1de219e)

### 왜 mago 3DTiler인가?
mago 3DTiler는 단순한 변환기가 아닙니다.  
Java로 개발된 이 오픈 소스 프로젝트는 3D 데이터 변환 분야에서 유연성과 성능을 동시에 제공합니다.

## 주요 기능
- **다양한 형식 지원**: ***3DS, OBJ, FBX, Collada DAE, GlTF, GLB, IFC*** 등 다양한 3D 형식을 OGC 3D Tiles로 변환 가능
- **포인트 클라우드 변환**: ***LAS, LAZ*** 등의 포인트 클라우드 데이터를 정밀하게 변환
- **2D 데이터를 3D 모델로 변환**: ***ESRI SHP, GeoJSON*** 등 2D 공간 데이터를 3D 모델로 변환하여 생동감 있는 표현 가능
- **좌표 변환 지원**: Proj4 라이브러리를 활용한 실시간 좌표 변환 기능과 다중 스레딩 기능 제공

## 사용법
릴리즈된 jar 파일을 다운로드하여 사용하거나, 직접 Gradle 스크립트를 이용해 빌드할 수 있습니다.  
빌드된 jar 파일은 ```/dist``` 디렉터리에 생성됩니다.

```
gradlew jar
```
###### 릴리즈에 사용된 Java 버전은 openjdk 17입니다.

#### 도움말 명령어 예시
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -help
```
콘솔 출력 예시:
```
┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┏┳┓┳┓ ┏┓┳┓
┃┃┃┣┫┃┓┃┃   ┫┃┃   ┃ ┃┃ ┣ ┣┫
┛ ┗┛┗┗┛┗┛  ┗┛┻┛   ┻ ┻┗┛┗┛┛┗
3d-tiler(dev) by Gaia3D, Inc.
----------------------------------------
usage: mago 3DTiler help
```

이 명령을 사용하여 KML 또는 Collada 데이터를 3D Tiles로 변환할 수 있습니다.
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -input C:\data\kml-input-dir -inputType kml -output C:\data\kml-output-dir
```
또는
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -i C:\data\kml-input-dir -o C:\data\kml-output-dir
```

## Docker 버전 사용법
Docker를 이용하여 mago-3d-tiler를 간편하게 사용할 수도 있습니다.

#### 설치 명령어:
```
docker pull gaia3d/mago-3d-tiler
```

#### 실행 명령어:
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -inputType 3ds -input /workspace/3ds-samples -output /workspace/sample-3d-tiles -crs 5186
```

## 지원하는 Java 버전
***JDK17*** 및 ***JDK21*** 등 장기 지원(LTS) 버전을 지원합니다.

## mago 3DTiler 체험하기
![image](https://github.com/Gaia3D/mago-3d-tiler/assets/87691347/c778f7e1-771c-4df6-8d4c-b46412c80c19)  
<https://seoul.gaia3d.com:10903>

## 커뮤니티 및 라이선스
- **오픈소스 커뮤니티**: GitHub에서 활발한 커뮤니티 활동 및 기여 가능
- **자유로운 개발**: MPL2.0 라이선스를 통해 자유롭게 사용, 수정, 배포 가능 (<https://www.mozilla.org/en-US/MPL/2.0/>)
- **상용 라이선스 옵션**: MPL2.0 조건을 원하지 않을 경우 상용 라이선스를 선택할 수 있으며, 관련 문의는 sales@gaia3d.com으로 연락 바랍니다.

## 라이브러리 의존성
- **LWJGL3** (경량 Java 게임 라이브러리 3 OpenGL, Assimp): <https://github.com/LWJGL/lwjgl3>
- **JOML** (Java OpenGL 수학 라이브러리): <https://github.com/JOML-CI/JOML>
- **jgltf** (glTF를 위한 Java 라이브러리): <https://github.com/javagl/JglTF>
- **laszip4j** (LASzip Java 포트): <https://github.com/mreutegg/laszip4j>
- **geotools** (지리 공간 데이터 처리 라이브러리): <https://github.com/geotools/geotools>
- **proj4j** (좌표 변환 라이브러리): <https://github.com/locationtech/proj4j>
- **citygml4j** (CityGML을 위한 Java API): <https://github.com/citygml4j/citygml4j>

