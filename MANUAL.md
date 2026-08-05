mago-3d-tiler Manual
===
updated at 2026-7-29 by znkim

# Overview

mago 3DTiler is a command-line tool for converting various 3D data formats into `3D Tiles` format.   
`3D Tiles` is a format developed by `Cesium, Inc.` and is currently registered as an `OGC standard`.  
mago 3DTiler supports data conversion of `b3dm`, `i3dm`, and `pnts` types.   
`b3dm` represents batched 3D models, `i3dm` represents GPU-instanced 3D models, and `pnts` represents point cloud data.

# Getting Started
mago 3DTiler can be executed using Docker or directly with Java.

## Execute mago 3DTiler with Docker
use the following command to run mago 3DTiler.

### pull docker image
```bash
docker pull gaia3d/mago-3d-tiler
```

Specify the input and output data paths through the workspace volume.

```bash
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler --input /workspace/3ds-samples --output /workspace/sample-3d-tiles --inputType 3ds --crs 5186
```

### Docker memory tuning
For large meshes, point clouds, CityGML, or photogrammetry inputs, set both the container memory limit and JVM options. Java reads `JAVA_TOOL_OPTIONS` automatically before starting mago 3DTiler.

```bash
docker run --rm --memory=24g \
  -e JAVA_TOOL_OPTIONS="-Xms4g -Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication" \
  -v "/workspace:/workspace" \
  gaia3d/mago-3d-tiler \
  --input /workspace/3ds-samples --output /workspace/sample-3d-tiles --inputType 3ds --crs 5186
```

For percentage-based sizing inside a container:

```bash
docker run --rm --memory=24g \
  -e JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=12.5 -XX:MaxRAMPercentage=70 -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication" \
  -v "/workspace:/workspace" \
  gaia3d/mago-3d-tiler \
  --input /workspace/3ds-samples --output /workspace/sample-3d-tiles --inputType 3ds --crs 5186
```

When building a custom Docker image with Jib, JVM options can be set in `mago-tiler/build.gradle`. If a custom `entrypoint` is used, include the JVM options in the entrypoint or pass them at runtime with `JAVA_TOOL_OPTIONS`.

### Using Docker Compose
Also, you can use Docker Compose to run mago 3DTiler.
When using Docker Compose, you can specifically set the input and output data paths through volume mapping.

Create a `docker-compose.yml` file with the following content:
```yaml
version: '3.8'
name: mago-3d-tiler
services:
  mago-tiler:
    image: gaia3d/mago-3d-tiler:latest
    pull_policy: always
    platform: linux/amd64
    container_name: mago-tiler
    mem_limit: 24g
    environment:
      JAVA_TOOL_OPTIONS: "-Xms4g -Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
    volumes:
      - C:/input:/input
      - D:/output:/output
    command: >
      --input /input/INPUT_DATA_DIR
      --output /output/OUTPUT_DATA_DIR
```

### Execute mago 3DTiler with Java
You can also run mago 3DTiler directly using the jar file.   
Download the latest `mago-3d-tiler.jar` file from the releases page or build it from the source code.  
Currently, **JDK 21** is required.

```
java -jar mago-3d-tiler.jar --input "/data/input/sample" --output "/data/output/sample"
```

or using short options

```
java -jar mago-3d-tiler.jar -i "/data/input/sample" -o "/data/output/sample"
```

### JVM memory tuning
mago 3DTiler can require a large Java heap for large meshes, point clouds, CityGML, or photogrammetry inputs. The warning `Maximum memory is less than the recommended 16GB` checks the JVM maximum heap, not the physical RAM installed on the PC.

For large jobs, start the jar with an explicit heap size:

```bash
java -Xms4g -Xmx16g -jar mago-3d-tiler.jar --input "/data/input/sample" --output "/data/output/sample"
```

Recommended JVM options:

```bash
java -Xms4g -Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Djava.awt.headless=true -jar mago-3d-tiler.jar --input "/data/input/sample" --output "/data/output/sample"
```

You can also size the heap as a percentage of available memory instead of a fixed value:

```bash
java -XX:InitialRAMPercentage=12.5 -XX:MaxRAMPercentage=50 -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Djava.awt.headless=true -jar mago-3d-tiler.jar --input "/data/input/sample" --output "/data/output/sample"
```

Use `-Xmx16g` only on machines with enough free RAM. On a 32GB machine, 16GB heap is usually reasonable if no other heavy process is running. For troubleshooting out-of-memory failures, add:

```bash
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heap-dumps
```

## Supported Formats
for input data formats, various 3D model formats, GIS vector formats, point-cloud formats, and BIM/CityModel formats are supported.

### Supported 3D Model Input Types
| Category | Format Name       | Extension(s)            | Support Level             |
|----------| ----------------- | ----------------------- | ------------------------- |
| 3D Model | glTF 2.0          | `.gltf`, `.glb`         | **Full**                  |
|          | FBX               | `.fbx` (ASCII / Binary) | **Full**                  |
|          | Collada           | `.dae`, `.xml`          | **Full**                  |
|          | Wavefront Object  | `.obj`                  | **Full**                  |
|          | Blender 3D        | `.blend`                | **Full**                  |
|          | 3D Studio         | `.3ds`, `.ase`          | **Full**                  |
|          | AutoCAD DXF       | `.dxf`                  | **Full**                  |
|          | Stereolithography | `.stl`                  | **Full**                  |
|          | LightWave         | `.lwo`, `.lws`          | **Full**                  |
|          | DirectX X         | `.x`                    | **Full**                  |
|          | Stanford PLY      | `.ply`                  | **Full** *(3D Mesh only)* |

### Supported GIS Vector Input Types
| Category   | Format Name    | Extension(s)        | Support Level   |
| ---------- | -------------- | ------------------- |-----------------|
| GIS Vector | Esri Shapefile | `.shp`              | **Full**        |
|            | GeoJSON        | `.geojson`, `.json` | **Full**        |
|            | GeoPackage     | `.gpkg`             | **Partial**     |

### Supported Point-Cloud Input Types
| Category    | Format Name | Extension(s)   | Support Level |
| ----------- | ----------- | -------------- | ------------- |
| Point Cloud | LAS / LAZ   | `.las`, `.laz` | **Full**      |

### Supported BIM Input Types
| Category   | Format Name | Extension(s) | Support Level |
| ---------- | ----------- | ------------ | ------------- |
| City Model | CityGML     | `.gml`       | **Partial**   |
| BIM        | IFC (STEP)  | `.ifc`       | **Partial**   |

## Command Line Options

### General Options
Options for general program behavior.

| Option               | Required | Description                                          |
| -------------------- | :------: | ---------------------------------------------------- |
| `-h`, `--help`       |    No    | Print help                                           |
| `-q`, `--quiet`      |    No    | Quiet / silent mode                                  |
| `-v`, `--verbose`    |    No    | Enable debug-level logs without changing log format  |
| `-d`, `--debug`      |    No    | Enable detailed debug output and stop on thread bugs |
| `-l`, `--log <arg>`  |    No    | Output log file path                                 |
| `-t`, `--temp <arg>` |    No    | Temporary directory path (Default: `{OUTPUT}/temp`)  |
| `-lt`, `--leaveTemp` |    No    | Leave temporary files                                |
| `-r`, `--recursive`  |    No    | Recursive directory traversal                        |
| `-m`, `--merge`      |    No    | Merge multiple `tileset.json` files                  |

### Input/Output Options
Options for input and output data paths and types. Specify `--inputType` when the input type is not detected automatically. If `--outputType` is not specified, all data except point clouds is generated as `b3dm` by default.

| Option                      | Required | Description                               |
| --------------------------- | :------: | ----------------------------------------- |
| `-i`, `--input <arg>`       |   Yes    | Input directory path                      |
| `-o`, `--output <arg>`      |   Yes    | Output directory path                     |
| `-it`, `--inputType <arg>`  |    No    | Input file type: `kml`, `3ds`, `fbx`, `obj`, `gltf/glb`, `las/laz`, `citygml`, `indoorgml`, `shp`, `geojson`, `gpkg` |
| `-ot`, `--outputType <arg>` |    No    | Output 3D Tiles type: `b3dm`, `i3dm`, `pnts` |
| `-te`, `--terrain <arg>`    |    No    | GeoTIFF path/directory or local Quantized Mesh `layer.json` for `clampToGround` |
| `-ge`, `--geoid <arg>`      |    No    | Height reference correction: `Ellipsoid`, `EGM84`, `EGM96`, `EGM2008`, or a custom GeoTIFF path |
| `-if`, `--instance <arg>`   |    No    | I3DM instance file path (Default: `{OUTPUT}/instance.dae`) |

### Coordinate System / Transform Options
Options for coordinate system and coordinate transformation.

| Option                       | Required | Description                            |
| ---------------------------- | :------: | -------------------------------------- |
| `-c`, `--crs <arg>`          |    No    | CRS EPSG code, such as `4326`, `3857`, `4978`, `5186` |
| `-p`, `--proj <arg>`         |    No    | Proj4 parameters. When set, `--crs` is ignored |
| `-xo`, `--xOffset <arg>`     |    No    | X offset for coordinate transform      |
| `-yo`, `--yOffset <arg>`     |    No    | Y offset for coordinate transform      |
| `-zo`, `--zOffset <arg>`     |    No    | Z offset for coordinate transform      |
| `-lon`, `--longitude <arg>`  |    No    | Longitude (must be used with latitude) |
| `-lat`, `--latitude <arg>`   |    No    | Latitude (must be used with longitude) |
| `-rx`, `--rotateXAxis <arg>` |    No    | Rotate X-axis in degrees               |
| `-fc`, `--flipCoordinate`    |    No    | Flip X/Y coordinates for 2D GIS data   |

### Tileset Options
Options for 3D Tiles tileset generation.

| Option                             | Required | Description                                  |
| ---------------------------------- | :------: | -------------------------------------------- |
| `-ra`, `--refineAdd`               |    No    | Set tileset refine mode to `ADD`             |
| `-mx`, `--maxCount <arg>`          |    No    | Maximum triangles per node                   |
| `-nl`, `--minLod <arg>`            |    No    | Minimum LOD                                  |
| `-xl`, `--maxLod <arg>`            |    No    | Maximum LOD                                  |
| `-ng`, `--minGeometricError <arg>` |    No    | Minimum geometric error                      |
| `-mg`, `--maxGeometricError <arg>` |    No    | Maximum geometric error                      |
| `-mp`, `--maxPoints <arg>`         |    No    | Maximum number of points per tile            |
| `-qt`, `--quantize`                |    No    | Quantize glTF meshes with `KHR_mesh_quantization` |
| `-tv`, `--tilesVersion <arg>`      |    No    | 3D Tiles version: `1.0` or `1.1` (Default: `1.1`) |
| `-vr`, `--validationReport`        |    No    | Write per-file validation reports and a batch summary |

### Point-Cloud Options
Options for point-cloud data conversion.

| Option                       | Required | Description                  |
| ---------------------------- | :------: | ---------------------------- |
| `-pcr`, `--pointRatio <arg>` |    No    | Percentage of sampled points |
| `-sp`, `--sourcePrecision`   |    No    | Preserve original precision  |
| `-f4`, `--force4ByteRGB`     |    No    | Force 4-byte RGB             |

### GIS Vector Options
Options for GIS vector data conversion.

| Option                            | Required | Description                               |
| --------------------------------- | :------: | ----------------------------------------- |
| `-af`, `--attributeFilter <arg>`  |    No    | Attribute filter for extrusion, such as `classification=window,door;type=building` |
| `-nc`, `--nameColumn <arg>`       |    No    | Name column (Default: `name`)             |
| `-hc`, `--heightColumn <arg>`     |    No    | Height column (Default: `height`, meters) |
| `-ac`, `--altitudeColumn <arg>`   |    No    | Altitude column (Default: `altitude`, meters) |
| `-hd`, `--headingColumn <arg>`    |    No    | Heading column for I3DM (Default: `heading`, degrees) |
| `-scl`, `--scaleColumn <arg>`     |    No    | Scale column for I3DM (Default: `scale`)  |
| `-den`, `--densityColumn <arg>`   |    No    | Density column for I3DM (Default: `density`) |
| `-dc`, `--diameterColumn <arg>`   |    No    | Pipe diameter column (Default: `diameter`, millimeters) |
| `-mh`, `--minimumHeight <arg>`    |    No    | Minimum extrusion height (Default: `0.0`, meters) |
| `-aa`, `--absoluteAltitude <arg>` |    No    | Absolute altitude for all features; overrides altitude column |
| `-sh`, `--skirtHeight <arg>`      |    No    | Building skirt height (Default: `4.0`, meters) |

### Experimental Options
These options are available but may change in future releases.

| Option                                  | Required | Description                                  |
| --------------------------------------- | :------: | -------------------------------------------- |
| `-pg`, `--photogrammetry`               |    No    | Compatibility-focused photogrammetry tiling  |
| `-sbn`, `--splitByNode`                 |    No    | Split tiles by scene graph nodes             |
| `-cc`, `--curvatureCorrection`          |    No    | Apply ellipsoid curvature correction         |
| `-urt`, `--updateRootTransform <arg>`   |    No    | Add 16 comma-separated transform values to the root node of `tileset.json` |
| `-tm`, `--tilingMode <arg>`             |    No    | 3D Tiles hierarchy mode: `explicit` or `implicit` |
| `-isl`, `--implicitSubtreeLevels <arg>` |    No    | Implicit tiling subtree levels (Default: `4`) |

### Deprecated Options
These options remain for compatibility and should be avoided in new workflows.

| Option                            | Required | Description                      |
| --------------------------------- | :------: | -------------------------------- |
| `-mc`, `--multiThreadCount <arg>` |    No    | Deprecated thread count option   |
| `-glb`, `--glb`                   |    No    | Deprecated GLB generation        |
| `-igtx`, `--ignoreTextures`       |    No    | Deprecated texture ignore option |

## Batched Model Conversion Cases

### Batched 3D Model (b3dm)
The example below shows how to convert various modeled 3D building data.
Can be used to convert common data.
Except for point cloud data, if you do not enter an outputType, it will be generated as b3dm.

```
java -jar mago-3d-tiler.jar --input "/input_path/kml_with_collada" --output "/output_path/kml_with_collada"
```

### DEM elevation application case
This is the case of putting 3D data on the terrain height of a single channel such as GeoTIFF.
```
java -jar mago-3d-tiler.jar --input "/input_path/sample" --output "/output_path/sample" --terrain "/input_path/sample/terrain.tif"
```

multiple terrain files case :
```
java -jar mago-3d-tiler.jar --input "/input_path/sample" --output "/output_path/sample" --terrain "/input_path/sample/terrain_dir"
```

### Geoid Height Correction
Use `--geoid` when input heights are referenced to a geoid model and should be corrected during tiling. The default is `Ellipsoid`, which applies no geoid correction.

Built-in geoid models:
```bash
java -jar mago-3d-tiler.jar --input "/input_path/sample" --output "/output_path/sample" --geoid EGM84
java -jar mago-3d-tiler.jar --input "/input_path/sample" --output "/output_path/sample" --geoid EGM96
java -jar mago-3d-tiler.jar --input "/input_path/sample" --output "/output_path/sample" --geoid EGM2008
```

Custom GeoTIFF geoid file or directory:
```bash
java -jar mago-3d-tiler.jar --input "/input_path/sample" --output "/output_path/sample" --geoid "/input_path/geoid/custom-geoid.tif"
```

### Converting 3D data with an applied coordinate system
This is the case of converting 3D data that already has a coordinate system applied to it.
The example below is a sample of converting 3ds (3D MAX) data. It converts to the case where the data has a coordinate system applied to it by adding the `crs` option.
In this case, we have entered the EPSG:5186 coordinate system.

```
java -jar mago-3d-tiler.jar --input "/input_path/3ds" --inputType "3ds" --output "/output_path/3ds" --crs "5186"
```

### Converting 3D data with curvature correction
When converting 3D data with a wide range, you may need to apply earth curvature correction.
In this case, you can apply curvature correction by adding the `--curvatureCorrection` option.
```
java -jar mago-3d-tiler.jar --input "/input_path/kml_with_collada" --inputType "obj" --output "/output_path/kml_with_collada" --crs "5186" --curvatureCorrection
```

---

## GPU Instance Model Conversion Cases

### Instanced 3D Model (i3dm)

When converting instance model data, the following options are available for conversion.
(kml with collada) data, and the `outputType` option is required in the current version.

```
java -jar mago-3d-tiler.jar --input "/input_path/i3dm" --output "/output_path/i3dm" --outputType "i3dm"
```

### Converting i3dm data to Shape

To converting i3dm as a Shape file with Point geometry type, you can convert it with the following options.
You need to specify `inputType` as shp and specify the path to the instance file through the 'instance' option.

```
java -jar mago-3d-tiler.jar --input "/input_path/i3dm" --output "/output_path/i3dm" --inputType "shp" --outputType "i3dm" --instance "/input_path/instance.gltf"
```

---
## GIS Vector Data Conversion Cases

### Converting 2D GIS Polygon Data
The example below extrudes 2D GIS polygon data.
The extrusion height can be specified to reference a customized attribute name using the `--heightColumn <arg>` attribute.
Similarly, the extrusion start height defaults to 0, and the height of the base plane can be specified via `--altitudeColumn <arg>`.

```
java -jar mago-3d-tiler.jar --input "/input_path/shp" --inputType "shp" --output "/output_path/shp" --crs "5186"
```
or
```
java -jar mago-3d-tiler.jar --input "/input_path/shp" --inputType "shp" --output "/output_path/shp" --crs "5186" --heightColumn "height"
```

### Converting 2D GIS Polyline Data
Convert polyline data to pipe. Polyline data with a z-axis can be converted via the `diameter` property.
The default dimension for a pipe in mago 3DTiler is diameter and The length is in millimeters (mm)
```
java -jar mago-3d-tiler.jar --input "/input_path/shp" --inputType "shp" --output "/output_path/shp" --crs "5186"
```
or
```
java -jar mago-3d-tiler.jar --input "/input_path/shp" --inputType "shp" --output "/output_path/shp" --crs "5186" --diameterColumn "diameter"
```

### Converting 2D GIS Point Data
When converting point data, you can convert it to i3dm by specifying the instance model through the `--instance <arg>` option.
```
java -jar mago-3d-tiler.jar --input "/input_path/geopackage" --inputType "gpkg" --output "/output_path/geopackage" --crs "5186" --outputType "i3dm" --instance "instance.glb"
```

## Point-Clouds Data Conversion Cases

### Converting Point-Clouds data (Point Clouds)

When converting point-clouds data, the following default options are available for conversion.
If the input data is "las", the `--outputType` will automatically be `pnts`.

```
java -jar mago-3d-tiler.jar --input "/input_path/las" --inputType "las" --output "/output_path/las"
```
or
```
java -jar mago-3d-tiler.jar --input "/input_path/las" --inputType "las" --output "/output_path/las" --outputType "pnts"
```

## Special Conversion Cases

### Up-Axis Swap Example
mago3dTiler converts mesh z-up axis data to y-up axis. If your original data is y-up axis, you will need to add the `--rotateXAxis <degree>` option to avoid converting it.
```
java -jar mago-3d-tiler.jar --input "/input_path/y-up-fbx" --inputType "fbx" --output "/output_path/y-up-fbx" --rotateXAxis "90"
```

### Translating Data to Origin
When converting data that is not located at the origin, you can use the `--xOffset`, `--yOffset`, and `--zOffset` options to translate it to the origin.
```
java -jar mago-3d-tiler.jar --input "/input_path/translated-model" --inputType "gltf" --output "/output_path/translated-model" --zOffset "50.0" --crs "5186"
```

### Converting CityGML
When converting CityGML, it is recommended to set `--inputType citygml`.
CityGML data can have different extensions, such as `.xml` and `.gml`.
```
java -jar mago-3d-tiler.jar --input "/input_path/citygml" --inputType "citygml" --output "/output_path/citygml" --crs "5186"
```

### Photogrammetry Tiling
When converting photogrammetry data, use `--photogrammetry` to enable the experimental compatibility-focused pipeline. This pipeline makes broader use of CPU processing and includes performance and visual-quality improvements.
```
java -jar mago-3d-tiler.jar --input "/input_path/photogrammetry" --output "/output_path/photogrammetry" --inputType "obj" --outputType "b3dm" --crs "5186" --photogrammetry
```
The `--outputType "b3dm"` option can be omitted as below.
```
java -jar mago-3d-tiler.jar --input "/input_path/photogrammetry" --output "/output_path/photogrammetry" --inputType "obj" --crs "5186" --photogrammetry
```

### Converting Large Point-Clouds Data
When converting large point-clouds, you can use the `--pointRatio` option to adjust the percentage of conversion points from the source data as follows.
In the example below, 25% of the points are converted.
```
java -jar mago-3d-tiler.jar --input "/input_path/las" --inputType "las" --output "/output_path/las" --pointRatio "25"
```
