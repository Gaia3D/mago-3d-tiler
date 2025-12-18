![mago-3d-tiler](https://github.com/user-attachments/assets/e7f8086d-ab5e-4848-9f51-99d444691f91)
===

## The Premier OGC 3D Tiles Solution!
mago 3DTiler is an open source-based OGC 3DTiles generator.   
It converts various spatial information data into OGC 3D Tiles, the basis of the Digital Twin service.   
Based on Java, mago 3DTiler is highly portable, flexible, and fast.

![Static Badge](https://img.shields.io/badge/Gaia3D%2C%20Inc-blue?style=flat-square)
![Static Badge](https://img.shields.io/badge/3DTiles1.1-green?style=flat-square&logo=Cesium)
![Static Badge](https://img.shields.io/badge/JDK21-red?style=flat-square&logo=openjdk)
![Static Badge](https://img.shields.io/badge/Gradle9-darkorange?style=flat-square&logo=gradle)
![Static Badge](https://img.shields.io/badge/Docker%20Image-blue?style=flat-square&logo=docker)

![tiler-images](https://github.com/user-attachments/assets/1c496ac5-053a-42c0-a6a7-e2c3b1de219e)

### Why mago 3DTiler? 
mago 3DTiler isn’t just a converter;   
developed with Java, this open-source marvel stands as a beacon for flexibility and performance in the world of 3D data conversion.

### Key Features
- **Multi-Format Mastery**: Effortlessly convert an array of 3D formats, including ***3DS, OBJ, FBX, Collada DAE, GlTF/GLB, CityGML, IFC*** and more. ​
- **Point Cloud Precision**: Bring your detailed point cloud data (***LAS, LAZ***) into the fold with pinpoint accuracy.​
- **2D to 3D Extrusion**: Turn 2D geospatial data (***ESRI SHP, GeoJSON, GeoPackage***) into detailed 3D extrusion models, breathing life into flat representations.​
- **On-The-Fly CRS Conversion**: Leverage the power of multi-threading and on-the-fly coordinate conversion with comprehensive PCS and GCS support via the Proj4 library.​

## Quick Start Guide
download the mago-3d-tiler jar file or use the Docker image to get started quickly.

### Using the Jar File
You can easily run mago 3dTiler using the jar file.   
> ⚠️ **Runs on Java versions JDK 21 and above**

```
java -jar mago-3d-tiler-x.x.x.jar -input C:\data\kml-input-dir -output C:\data\kml-output-dir
```

### Using Docker Image
you can run mago-3d-tiler using the Docker image without installing Java or other dependencies.

#### pulling the Docker image:
```
docker pull gaia3d/mago-3d-tiler
```
#### running the Docker image:
```
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -input /workspace/3ds-samples -output /workspace/sample-3d-tiles -crs 5186
```

### Building from Source
If you prefer to build the mago 3DTiler jar file from source,
clone the repository and use Gradle to build the project.

cloning the repository:
```git
git clone https://github.com/Gaia3D/mago-3d-tiler.git
```
then navigate to the project directory and run:
```gradle
gradle jar
```

###### The java version used in the release is openjdk 21

## Example help command
```bash
java -jar mago-3d-tiler.jar --help
```
console output:
```
----------------------------------------
mago-3d-tiler(dev) by Gaia3D, Inc.
----------------------------------------
Usage: command options
 -h, --help                       Print Help
 -q, --quiet                      Quiet mode/Silent mode
 -lt, --leaveTemp                 Leave temporary files
 -m, --merge                      Merge tileset.json files
 -i, --input <arg>                [Required] Input directory path
 -o, --output <arg>               [Required] Output directory path
 -t, --temp <arg>                 Temporary directory path (Default: {OUTPUT}/temp)
 -it, --inputType <arg>           Input files type [kml, 3ds, fbx, obj, gltf/glb, las/laz, citygml, indoorgml, shp, geojson, gpkg]
 -ot, --outputType <arg>          Output 3DTiles Type [b3dm, i3dm, pnts]
 -l, --log <arg>                  Output log file path.
 -r, --recursive                  Tree directory deep navigation.
 -te, --terrain <arg>             GeoTiff Terrain file path, 3D Object applied as clampToGround (Supports GeoTIFF format)
 -ge, --geoid <arg>               Geoid file path for height correction, (Default: Ellipsoid)("Ellipsoid", "EGM96" or GeoTIFF File Path)
 -if, --instance <arg>            Instance file path for I3DM (Default: {OUTPUT}/instance.dae)
 -qt, --quantize                  Quantize glTF 3DMesh via "KHR_mesh_quantization" Extension
 -c, --crs <arg>                  Coordinate Reference Systems, EPSG Code(4326, 3857, 32652, 5186...)
 -p, --proj <arg>                 Proj4 parameters (ex: +proj=tmerc +la...)
 -xo, --xOffset <arg>             X Offset value for coordinate transformation
 -yo, --yOffset <arg>             Y Offset value for coordinate transformation
 -zo, --zOffset <arg>             Z Offset value for coordinate transformation
 -lon, --longitude <arg>          Longitude value for coordinate transformation. (The lon lat option must be used together).
 -lat, --latitude <arg>           Latitude value for coordinate transformation. (The lon lat option must be used together).
 -rx, --rotateXAxis <arg>         Rotate the X-Axis in degrees
 -ra, --refineAdd                 [Tileset] Set 3D Tiles Refine 'ADD' mode
 -mx, --maxCount <arg>            [Tileset] Maximum number of triangles per node.
 -nl, --minLod <arg>              [Tileset] min level of detail
 -xl, --maxLod <arg>              [Tileset] Max Level of detail
 -ng, --minGeometricError <arg>   [Tileset] Minimum geometric error
 -mg, --maxGeometricError <arg>   [Tileset] Maximum geometric error
 -mp, --maxPoints <arg>           [Tileset] Maximum number of points per a tile
 -pcr, --pointRatio <arg>         [PointCloud] Percentage of points from original data
 -sp, --sourcePrecision           [PointCloud] Create pointscloud tile with original precision.
 -f4, --force4ByteRGB             [PointCloud] Force 4Byte RGB for pointscloud tile.
 -fc, --flipCoordinate            [GISVector] Flip x, y coordinate for 2D Original Data.
 -af, --attributeFilter <arg>     [GISVector] Attribute filter setting for extrusion model ex) "classification=window,door;type=building"
 -nc, --nameColumn <arg>          [GISVector] Name column setting for extrusion model
 -hc, --heightColumn <arg>        [GISVector] Height column setting for extrusion model
 -ac, --altitudeColumn <arg>      [GISVector] Altitude Column setting for extrusion model
 -hd, --headingColumn <arg>       [GISVector] Heading column setting for I3DM converting
 -scl, --scaleColumn <arg>        [GISVector] Scale column setting for I3DM converting
 -den, --densityColumn <arg>      [GISVector] Density column setting for I3DM polygon converting
 -dc, --diameterColumn <arg>      [GISVector] Diameter column setting for pipe extrusion model, Specify a length unit for Diameter in millimeters(mm) (Default Column: diameter)
 -mh, --minimumHeight <arg>       [GISVector] Minimum height value for extrusion model
 -aa, --absoluteAltitude <arg>    [GISVector] Absolute altitude value for extrusion model
 -sh, --skirtHeight <arg>         [GISVector] Building Skirt height setting for extrusion model
 -tv, --tilesVersion <arg>        [Experimental] 3DTiles Version [1.0, 1.1][Default: 1.1]
 -pg, --photogrammetry            [Experimental] generate b3dm for photogrammetry model with GPU
 -sbn, --splitByNode              [Experimental] Split tiles by nodes of scene.
 -cc, --curvatureCorrection       [Experimental] Apply curvature correction for ellipsoid surface.
 -mc, --multiThreadCount <arg>    [Deprecated] set thread count
 -glb, --glb                      [Deprecated] Create glb file with B3DM.
 -igtx, --ignoreTextures          [Deprecated] Ignore diffuse textures.
 -d, --debug                      [DEBUG] More detailed log output and stops on Multi-Thread bugs.
```

## Documentation
For detailed documentation, including installation and usage instructions, please refer to the official documentation:
- Manual : [github.com/Gaia3D/mago-3d-tiler](https://github.com/Gaia3D/mago-3d-tiler/blob/main/MANUAL.md)
- JavaDocs : [gaia3d.github.io/mago-3d-tiler](https://gaia3d.github.io/mago-3d-tiler)

## Explore and Experience:
- **Community and Code**: Join our vibrant community on GitHub and contribute to the future of 3D data conversion.
- **Freedom to Innovate**: Embrace the flexibility of MPL2.0 licensing(<https://www.mozilla.org/en-US/MPL/2.0/>),
  ensuring your freedom to use, modify, and distribute without hindrance
- **License**: If you prefer not to share your modified or improved code under the MPL2.0 license, you can opt for a commercial license instead
In this case, please contact us at sales@gaia3d.com

## Dependencies
main dependencies used in the mago-3d-tiler project include
- **LWJGL3** (Lightweight Java Game Library 3): <https://github.com/LWJGL/lwjgl3>
  - **Assimp** (Open Asset Import Library Java binding): <https://github.com/assimp/assimp>
- **JOML** (Java OpenGL Math Library): <https://github.com/JOML-CI/JOML>
- **JglTF** (Java libraries for glTF): <https://github.com/javagl/JglTF>
- **laszip4j** (The LASzip library ported to Java): <https://github.com/mreutegg/laszip4j>
- **GeoTools** (Geospatial data tools library): <https://github.com/geotools/geotools>
- **Proj4J** (Converting coordinate reference systems): <https://github.com/locationtech/proj4j>
- **citygml4j** (The Open Source Java API for CityGML): <https://github.com/citygml4j/citygml4j>

## License
- **MPL2.0**: The Mozilla Public License 2.0 (MPL2.0) governs the use of this software