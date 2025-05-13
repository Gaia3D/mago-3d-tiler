![mago-3d-tiler](https://github.com/user-attachments/assets/e7f8086d-ab5e-4848-9f51-99d444691f91)
===

## Overview

### The Premier OGC 3D Tiles Solution!
mago 3DTiler is an open source-based OGC 3DTiles generator.   
It converts various spatial information data into OGC 3D Tiles, the basis of the Digital Twin service.   
Based on Java, mago 3DTiler is highly portable, flexible, and fast.

![Static Badge](https://img.shields.io/badge/Gaia3D%2C%20Inc-blue?style=flat-square)
![Static Badge](https://img.shields.io/badge/3DTiles-green?style=flat-square&logo=Cesium)
![Static Badge](https://img.shields.io/badge/Jdk17-red?style=flat-square&logo=openjdk)
![Static Badge](https://img.shields.io/badge/Gradle-darkorange?style=flat-square&logo=gradle)
![Static Badge](https://img.shields.io/badge/Docker%20Image-blue?style=flat-square&logo=docker)

![tiler-images](https://github.com/user-attachments/assets/1c496ac5-053a-42c0-a6a7-e2c3b1de219e)

### Why mago 3DTiler? 
mago 3DTiler isn’t just a converter;   
developed with Java, this open-source marvel stands as a beacon for flexibility and performance in the world of 3D data conversion.

## Key Features
- **Multi-Format Mastery**: Effortlessly convert an array of 3D formats, including ***3DS, OBJ, FBX, Collada DAE, GlTF, GLB , IFC*** and more. ​
- **Point Cloud Precision**: Bring your detailed point cloud data (***LAS, LAZ***) into the fold with pinpoint accuracy.​
- **2D to 3D Extrusion**: Turn 2D geospatial data (***ESRI SHP, GeoJSON***) into detailed 3D extrusion models, breathing life into flat representations.​
- **On-The-Fly CRS Conversion**: Leverage the power of multi-threading and on-the-fly coordinate conversion with comprehensive PCS and GCS support via the Proj4 library.​

## Usage
You can download the released jar file or build the jar yourself via the mago-3d-tiler project gradle script.   
The built jar is created in the ```/dist``` directory.

```
gradlew jar
```
###### The java version used in the release is openjdk 17.

## Example help command
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -help
```
console output:
```
----------------------------------------
mago-3d-tiler(dev) by Gaia3D, Inc.
----------------------------------------
usage: mago 3DTiler help
 -aa,--absoluteAltitude <arg>    Absolute altitude value for extrusion model
 -ac,--altitudeColumn <arg>      Altitude Column setting for extrusion model
 -af,--attributeFilter <arg>     Attribute filter setting for extrusion model ex) "classification=window,door;type=building"
 -c,--crs <arg>                  Coordinate Reference Systems, EPSG Code(4326, 3857, 32652, 5186...)
 -d,--debug                      More detailed log output and stops on Multi-Thread bugs.
 -dc,--diameterColumn <arg>      Diameter column setting for extrusion model, Specify a length unit for Diameter in millimeters(mm) (Default Column: diameter)
 -f4,--force4ByteRGB             Force 4Byte RGB for pointscloud tile.
 -fc,--flipCoordinate            Flip x, y coordinate for 2D Original Data.
 -glb,--glb                      Create glb file with B3DM.
 -h,--help                       Print Help
 -hc,--heightColumn <arg>        Height column setting for extrusion model
 -hd,--headingColumn <arg>       Heading column setting for I3DM converting
 -i,--input <arg>                Input directory path
 -if,--instance <arg>            Instance file path for I3DM (Default: {OUTPUT}/instance.dae)
 -igtx,--ignoreTextures          Ignore diffuse textures.
 -it,--inputType <arg>           Input files type [kml, 3ds, fbx, obj, gltf/glb, las/laz, citygml, indoorgml, shp, geojson, gpkg]
 -l,--log <arg>                  Output log file path.
 -lat,--latitude <arg>           Latitude value for coordinate transformation. (The lon lat option must be used together).
 -lm,--largeMesh                 [Experimental] Large Mesh Splitting Mode)
 -lon,--longitude <arg>          Longitude value for coordinate transformation. (The lon lat option must be used together).
 -lt,--leaveTemp                 Leave temporary files
 -m,--merge                      Merge tileset.json files
 -mc,--multiThreadCount <arg>    set Multi-Thread count
 -mg,--maxGeometricError <arg>   Maximum geometric error
 -mh,--minimumHeight <arg>       Minimum height value for extrusion model
 -mp,--maxPoints <arg>           Maximum number of points per a tile
 -mx,--maxCount <arg>            Maximum number of triangles per node.
 -nc,--nameColumn <arg>          Name column setting for extrusion model
 -ng,--minGeometricError <arg>   Minimum geometric error
 -nl,--minLod <arg>              min level of detail
 -o,--output <arg>               Output directory file path
 -ot,--outputType <arg>          Output 3DTiles Type [b3dm, i3dm, pnts]
 -p,--proj <arg>                 Proj4 parameters (ex: +proj=tmerc +la...)
 -pcr,--pointRatio <arg>         Percentage of points from original data
 -pg,--photogrammetry            [Experimental][GPU] generate b3dm for photogrammetry model
 -q,--quiet                      Quiet mode/Silent mode
 -qt,--quantize                  Quantize mesh to reduce glb size via "KHR_mesh_quantization" Extension
 -r,--recursive                  Tree directory deep navigation.
 -ra,--refineAdd                 Set 3D Tiles Refine 'ADD' mode
 -ru,--flipUpAxis                Rotate the matrix 180 degrees about the X-axis.
 -rx,--rotateXAxis <arg>         Rotate the X-Axis in degrees
 -sh,--skirtHeight <arg>         Building Skirt height setting for extrusion model
 -sp,--sourcePrecision           Create pointscloud tile with original precision.
 -su,--swapUpAxis                Rotate the matrix -90 degrees about the X-axis.
 -te,--terrain <arg>             GeoTiff Terrain file path, 3D Object applied as clampToGround (Supports geotiff format)
 -vl,--voxelLod                  [Experimental] Voxel Level Of Detail setting for i3dm
 -xl,--maxLod <arg>              Max Level of detail
 -xo,--xOffset <arg>             X Offset value for coordinate transformation
 -yo,--yOffset <arg>             Y Offset value for coordinate transformation
 -zo,--zeroOrigin                [Experimental] fix 3d root transformed matrix origin to zero point.
```
This is a simple kml/collada -> 3dTiles conversion code with the mandatory argument values.    
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -input C:\data\kml-input-dir -inputType kml -output C:\data\kml-output-dir
```
or
```
java -jar mago-3d-tiler-x.x.x-natives-windows.jar -i C:\data\kml-input-dir -o C:\data\kml-output-dir
```

## Using Docker Image
Alternatively, you can easily use mago-3d-tiler with docker.

#### Installation command: 
```
docker pull gaia3d/mago-3d-tiler
```
#### Running command:
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -inputType 3ds -input /workspace/3ds-samples -output /workspace/sample-3d-tiles -crs 5186
```

## Documentation
For detailed documentation, including installation and usage instructions, please refer to the official documentation:
- JavaDocs : [gaia3d.github.io/mago-3d-tiler](https://gaia3d.github.io/mago-3d-tiler)
- Manual : [github.com/Gaia3D/mago-3d-tiler](https://github.com/Gaia3D/mago-3d-tiler/blob/main/MANUAL.md)

## Supported Java versions:
Supports long-term support (LTS) versions of the JDK, including ***JDK17*** and ***JDK21***.

## Experience the mago 3DTiler:
![image](https://github.com/Gaia3D/mago-3d-tiler/assets/87691347/c778f7e1-771c-4df6-8d4c-b46412c80c19)   
Demo page : [Link](https://seoul.gaia3d.com:10903)

## Explore and Experience:
- **Community and Code**: Join our vibrant community on GitHub and contribute to the future of 3D data conversion.​
- **Freedom to Innovate**: Embrace the flexibility of MPL2.0 licensing(<https://www.mozilla.org/en-US/MPL/2.0/>)​,
  ensuring your freedom to use, modify, and distribute without hindrance.
- **License**: If you prefer not to share your modified or improved code under the MPL2.0 license, you can opt for a commercial license instead.
In this case, please contact us at sales@gaia3d.com

## Library Dependencies:
- **LWJGL3** (Lightweight Java Game Library 3 Opengl, Assimp): <https://github.com/LWJGL/lwjgl3>
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **jgltf** (Java libraries for glTF): <https://github.com/javagl/JglTF>
- **laszip4j** (The LASzip library ported to Java): <https://github.com/mreutegg/laszip4j>
- **geotools** (Geospatial data tools library): <https://github.com/geotools/geotools>
- **proj4j** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>
- **citygml4j** (The Open Source Java API for CityGML): <https://github.com/citygml4j/citygml4j>

## License
- **MPL2.0**: The Mozilla Public License 2.0 (MPL2.0) governs the use of this software.