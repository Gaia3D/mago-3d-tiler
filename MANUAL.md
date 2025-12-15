📕 mago-3d-tiler manual
===
updated at 2025-12-15 by znkim

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
docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -input /workspace/3ds-samples -output /workspace/sample-3d-tiles -inputType 3ds -crs 5186
```

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
    volumes:
      - C:/input:/input
      - D:/output:/output
    command: >
      -input /input/INPUT_DATA_DIR
      -output /output/OUTPUT_DATA_DIR
```

### Execute mago 3DTiler with Java
You can also run mago 3DTiler directly using the jar file.   
Download the latest `mago-3d-tiler.jar` file from the releases page or build it from the source code.  
Currently, **JDK21** or higher is required.

```
java -jar mago-3d-tiler.jar --input "/data/input/sample" --output "/data/output/sample"
```

or using short options

```
java -jar mago-3d-tiler.jar -i "/data/input/sample" -o "/data/output/sample"
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
| `-h`, `--help`       |     ⚪    | Print help                                           |
| `-q`, `--quiet`      |     ⚪    | Quiet / silent mode                                  |
| `-d`, `--debug`      |   ⚪ 🐞   | Enable verbose logging and stop on multi-thread bugs |
| `-l`, `--log <arg>`  |     ⚪    | Output log file path                                 |
| `-t`, `--temp <arg>` |     ⚪    | Temporary directory path (Default: `{OUTPUT}/temp`)  |
| `-lt`, `--leaveTemp` |     ⚪    | Leave temporary files                                |
| `-r`, `--recursive`  |     ⚪    | Recursive directory traversal                        |
| `-m`, `--merge`      |     ⚪    | Merge multiple `tileset.json` files                  |

### Input/Output Options
Options for input and output data paths and types.
when inputType is not automatically detected, specify inputType.
if outputType is not specified, all data except point clouds will be generated as b3dm by default.

| Option                      | Required | Description                               |
| --------------------------- | :------: | ----------------------------------------- |
| `-i`, `--input <arg>`       |     ✅    | Input directory path                      |
| `-o`, `--output <arg>`      |     ✅    | Output directory path                     |
| `-it`, `--inputType <arg>`  |     ⚪    | Input file type                           |
| `-ot`, `--outputType <arg>` |     ⚪    | Output 3D Tiles type `[b3dm, i3dm, pnts]` |
| `-te`, `--terrain <arg>`       |     ⚪    | GeoTIFF terrain for `clampToGround` |
| `-if`, `--instance <arg>`      |     ⚪    | I3DM instance file path             |
| `-pg`, `--photogrammetry`      |   ⚪ 🧪   | GPU-based photogrammetry tiling     |


### Coordinate System / Transform Options
Options for coordinate system and coordinate transformation.

| Option                       | Required | Description                            |
| ---------------------------- | :------: | -------------------------------------- |
| `-c`, `--crs <arg>`          |     ⚪    | CRS EPSG code (4326, 3857, 5186, …)    |
| `-p`, `--proj <arg>`         |     ⚪    | Proj4 parameters                       |
| `-xo`, `--xOffset <arg>`     |     ⚪    | X offset for coordinate transform      |
| `-yo`, `--yOffset <arg>`     |     ⚪    | Y offset for coordinate transform      |
| `-zo`, `--zOffset <arg>`     |     ⚪    | Z offset for coordinate transform      |
| `-lon`, `--longitude <arg>`  |     ⚪    | Longitude (must be used with latitude) |
| `-lat`, `--latitude <arg>`   |     ⚪    | Latitude (must be used with longitude) |
| `-rx`, `--rotateXAxis <arg>` |     ⚪    | Rotate X-axis in degrees               |
| `-fc`, `--flipCoordinate`    |     ⚪    | Flip X/Y coordinates for 2D GIS data   |
| `-cc`, `--curvatureCorrection` |   ⚪ 🧪   | Ellipsoid curvature correction      |

### Tileset Options
Options for 3D Tiles tileset generation.

| Option                             | Required | Description                                  |
| ---------------------------------- | :------: | -------------------------------------------- |
| `-ra`, `--refineAdd`               |     ⚪    | Set tileset refine mode to `ADD`             |
| `-mx`, `--maxCount <arg>`          |     ⚪    | Maximum triangles per node                   |
| `-nl`, `--minLod <arg>`            |     ⚪    | Minimum LOD                                  |
| `-xl`, `--maxLod <arg>`            |     ⚪    | Maximum LOD                                  |
| `-ng`, `--minGeometricError <arg>` |     ⚪    | Minimum geometric error                      |
| `-mg`, `--maxGeometricError <arg>` |     ⚪    | Maximum geometric error                      |
| `-tv`, `--tilesVersion <arg>`      |   ⚪ 🧪   | 3D Tiles version `[1.0, 1.1]` (Default: 1.1) |
| `-sbn`, `--splitByNode`        |   ⚪ 🧪   | Split tiles by scene graph nodes    |

### Point-Cloud Options
Options for point-cloud data conversion.

| Option                       | Required | Description                  |
| ---------------------------- | :------: | ---------------------------- |
| `-mp`, `--maxPoints <arg>`   |     ⚪    | Maximum points per tile      |
| `-pcr`, `--pointRatio <arg>` |     ⚪    | Percentage of sampled points |
| `-sp`, `--sourcePrecision`   |     ⚪    | Preserve original precision  |
| `-f4`, `--force4ByteRGB`     |     ⚪    | Force 4-byte RGB             |

### GIS Vector Options
Options for GIS vector data conversion.

| Option                            | Required | Description                               |
| --------------------------------- | :------: | ----------------------------------------- |
| `-af`, `--attributeFilter <arg>`  |     ⚪    | Attribute filter for extrusion            |
| `-nc`, `--nameColumn <arg>`       |     ⚪    | Name column                               |
| `-hc`, `--heightColumn <arg>`     |     ⚪    | Height column                             |
| `-ac`, `--altitudeColumn <arg>`   |     ⚪    | Altitude column                           |
| `-hd`, `--headingColumn <arg>`    |     ⚪    | Heading column (I3DM)                     |
| `-scl`, `--scaleColumn <arg>`     |     ⚪    | Scale column (I3DM)                       |
| `-den`, `--densityColumn <arg>`   |     ⚪    | Density column                            |
| `-dc`, `--diameterColumn <arg>`   |     ⚪    | Diameter column (mm, default: `diameter`) |
| `-mh`, `--minimumHeight <arg>`    |     ⚪    | Minimum extrusion height                  |
| `-aa`, `--absoluteAltitude <arg>` |     ⚪    | Absolute altitude                         |
| `-sh`, `--skirtHeight <arg>`      |     ⚪    | Building skirt height                     |

### Deprecated Options
It is planned to be deprecated soon.

| Option                            | Required | Description                      |
| --------------------------------- | :------: | -------------------------------- |
| `-mc`, `--multiThreadCount <arg>` |    ⚠️    | Deprecated thread count option   |
| `-glb`, `--glb`                   |    ⚠️    | Deprecated GLB generation        |
| `-igtx`, `--ignoreTextures`       |    ⚠️    | Deprecated texture ignore option |


## Batched Model Conversion Cases

### Batched 3D Model (b3dm)
The example below shows how to convert various modeled 3D building data.
Can be used to convert common data.
Except for point cloud data, if you do not enter an outputType, it will be generated as b3dm.

```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

### DEM elevation application case
This is the case of putting 3D data on the terrain height of a single channel such as GeoTiff.
```
java -jar mago-3d-tiler.jar -input "/input_path/sample" -output "/output_path/sample" -terrain "/input_path/sample/terrain.tif"
```

multiple terrain files case :
```
java -jar mago-3d-tiler.jar -input "/input_path/sample" -output "/output_path/sample" -terrain "/input_path/sample/terrain_dir"
```

### Converting 3D data with an applied coordinate system
This is the case of converting 3D data that already has a coordinate system applied to it.
The example below is a sample of converting 3ds (3D MAX) data. It converts to the case where the data has a coordinate system applied to it by adding the `crs` option.
In this case, we have entered the EPSG:5186 coordinate system.

```
java -jar mago-3d-tiler.jar -input "/input_path/3ds" -inputType "3ds" -output "/output_path/3ds" -crs "5186"
```

### Converting 3D data with curvature correction
When converting 3D data with a wide range, you may need to apply earth curvature correction.
In this case, you can apply curvature correction by adding the `-cc` option.
```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -inputType "obj" -output "/output_path/kml_with_collada" -crs "5186" -cc
```

---

## GPU Instance Model Conversion Cases

### Instanced 3D Model (i3dm)

When converting instance model data, the following options are available for conversion.
(kml with collada) data, and the `outputType` option is required in the current version.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -outputType "i3dm"
```

### Converting i3dm data to Shape

To converting i3dm as a Shape file with Point geometry type, you can convert it with the following options.
You need to specify `inputType` as shp and specify the path to the instance file through the 'instance' option.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -inputType "shp" -outputType "i3dm" -instance "/input_path/instance.gltf"
```

---
## GIS Vector Data Conversion Cases

### Converting 2D GIS Polygon Data
The example below extrudes 2D GIS polygon data.
The extrusion height can be specified to reference a customized attribute name using the `-heightColumn <arg>` attribute.
Similarly, the extrusion start height defaults to 0, and the height of the base plane can be specified via `-altitudeColumn <arg>`.

```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```
or
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186" -heightColumn "height"
```

### Converting 2D GIS Polyline Data
Convert polyline data to pipe. Polyline data with a z-axis can be converted via the `diameter` property.
The default dimension for a pipe in mago 3DTiler is diameter and The length is in millimeters (mm)
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```
or
```
java -jar mago-3d-tiler.jar -input "/input_path/geotiff" -inputType "geotiff" -output "/output_path/geotiff" -crs "5186" -diameterColumn "diameter"
```

### Converting 2D GIS Point Data
When converting point data, you can convert it to i3dm by specifying the instance model through the `-instance <arg>` option.
```
java -jar mago-3d-tiler.jar -input "/input_path/geopackage" -inputType "gpkg" -output "/output_path/geopackage" -crs "5186" -outputType "i3dm" -instance "instance.glb"
```

## Point-Clouds Data Conversion Cases

### Converting Point-Clouds data (Point Clouds)

When converting point-clouds data, the following default options are available for conversion.
If the input data is "las", the "-outputType" will automatically be "pnts".

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las"
```
or
```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -outputType "pnts"
```

## Special Conversion Cases

### Up-Axis Swap Example
mago3dTiler converts mesh z-up axis data to y-up axis. If your original data is y-up axis, you will need to add the `-rotateXAxis <degree>` option to avoid converting it.
```
java -jar mago-3d-tiler.jar -input "/input_path/y-up-fbx" -inputType "fbx" -output "/output_path/y-up-fbx" -rotateXAxis "90"
```

### Translating Data to Origin
When converting data that is not located at the origin, you can use the `-xOffset`, `-yOffset`, and `-zOffset` options to translate it to the origin.
```
java -jar mago-3d-tiler.jar -input "/input_path/translated-model" -inputType "gltf" -output "/output_path/translated-model" -zOffset "50.0" -crs "5186"
```

### Converting CityGML
When converting to CityGML, it is recommended to give the InputType as ‘citygml’.
This is because Citygml data can have different extensions: ‘.xml’, ‘.gml’, etc.
```
java -jar mago-3d-tiler.jar -input "/input_path/citygml" -inputType "citygml" -output "/output_path/citygml" -crs "5186"
```

### Photogrammetry Tiling
When converting photogrammetry data, you can use the `-photogrammetry` option to enable GPU-based photogrammetry tiling as follows.
```
java -jar mago-3d-tiler.jar -input "/input_path/photogrammetry" -output "/output_path/photogrammetry" -inputType "obj" -outputType "b3dm" -crs "5186" -photogrammetry
```
can be omitted `-outputType "b3dm"` option like below.
```
java -jar mago-3d-tiler.jar -input "/input_path/photogrammetry" -output "/output_path/photogrammetry" -inputType "obj" -crs "5186" -photogrammetry
```

### Converting Large Point-Clouds Data
When converting large point-clouds, you can use the `-pointRatio` option to adjust the percentage of conversion points from the source data as follows.
In the example below, 25% of the points are converted.
```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -pointRatio "25"
```