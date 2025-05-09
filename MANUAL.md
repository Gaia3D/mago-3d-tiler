📕 mago-3d-tiler manual
===

## Overview

mago 3DTiler converts various data formats such as `3D Model/GIS Vector/BIM/CityModel/point-cloud` into 3D Tiles.

`3D Tiles` is a format developed by `Cesium, Inc.` and is currently registered as an `OGC standard`. 
It is currently under development as of version 1.0

3D Tiles Github Repo : https://github.com/CesiumGS/3d-tiles
OGC 3D Tiles Standard : https://www.ogc.org/standards/3dtiles

## Prior knowledge

### What's B3DM, I3DM?

3D Tiles :  
Batched 3D Model :   
Instanced 3D Model :   

## Supported Formats

| Category      | Format Name              | Extension(s)              | Support Level |
|---------------|--------------------------|----------------------------|----------------|
| 3D Modeling   | 3D Studio                | `.3ds`, `.ase`             | Full           |
|               | Wavefront                | `.obj`                     | Full           |
|               | Collada                  | `.dae`                     | Full           |
|               | glTF                     | `.glb`, `.gltf`            | Full           |
|               | FBX                      | `.fbx`                     | Full           |
| BIM           | IFC-STEP                 | `.ifc`                     | Partial        |
| Point Cloud   | PLY                      | `.ply`                     | Full           |
|               | Lidar LASer              | `.las`, `.laz`             | Full           |
| City Models   | CityGML 3.0              | `.gml`                     | Partial        |
| GIS Vector    | Esri Shapefile           | `.shp`                     | Full           |
|               | GeoJSON                  | `.geojson`, `.json`        | Full           |
|               | GeoPackage               | `.gpkg`                    | Full           |


## Default Usage

```
java -jar mago-3d-tiler.jar --input "/data/input/sample" --output "/data/output/sample"
```

## Command Line Options

| Option            | Argument Required | Description |
|-------------------|-------------------|-------------|
| `-aa`, `--absoluteAltitude` | Yes | Absolute altitude value for extrusion model |
| `-ac`, `--altitudeColumn`   | Yes | Altitude column for extrusion model |
| `-af`, `--attributeFilter`  | Yes | Attribute filter for extrusion model (e.g. `"classification=window,door;type=building"`) |
| `-c`, `--crs`               | Yes | Coordinate Reference System (EPSG codes like 4326, 3857, etc.) |
| `-d`, `--debug`             | No  | Show detailed logs and halt on multi-threading bugs |
| `-dc`, `--diameterColumn`   | Yes | Diameter column for extrusion model (unit: mm; default: `diameter`) |
| `-f4`, `--force4ByteRGB`    | No  | Force 4-byte RGB for point cloud tiles |
| `-fc`, `--flipCoordinate`   | No  | Flip X and Y in 2D data |
| `--glb`                     | No  | Create `.glb` file with `.b3dm` |
| `-h`, `--help`              | No  | Print help message |
| `-hc`, `--heightColumn`     | Yes | Height column for extrusion model |
| `-hd`, `--headingColumn`    | Yes | Heading column for I3DM conversion |
| `-i`, `--input`             | Yes | Input directory path |
| `-if`, `--instance`         | Yes | I3DM instance file path (default: `{OUTPUT}/instance.dae`) |
| `-igtx`, `--ignoreTextures` | No  | Ignore diffuse textures |
| `-it`, `--inputType`        | Yes | Input file type (`kml`, `3ds`, `fbx`, etc.) |
| `-l`, `--log`               | Yes | Output log file path |
| `-lat`, `--latitude`        | Yes | Latitude for coordinate transformation *(must be used with `--longitude`)* |
| `-lm`, `--largeMesh`        | No  | [Experimental] Enable large mesh splitting |
| `-lon`, `--longitude`       | Yes | Longitude for coordinate transformation *(must be used with `--latitude`)* |
| `-lt`, `--leaveTemp`        | No  | Leave temporary files |
| `-m`, `--merge`             | No  | Merge multiple `tileset.json` files |
| `-mc`, `--multiThreadCount` | Yes | Number of threads to use |
| `-mg`, `--maxGeometricError`| Yes | Maximum geometric error |
| `-mh`, `--minimumHeight`    | Yes | Minimum height for extrusion model |
| `-mp`, `--maxPoints`        | Yes | Max number of points per tile |
| `-mx`, `--maxCount`         | Yes | Max triangles per node |
| `-nc`, `--nameColumn`       | Yes | Name column for extrusion model |
| `-ng`, `--minGeometricError`| Yes | Minimum geometric error |
| `-nl`, `--minLod`           | Yes | Minimum Level of Detail |
| `-o`, `--output`            | Yes | Output directory path |
| `-ot`, `--outputType`       | Yes | Output 3D Tiles type (`b3dm`, `i3dm`, `pnts`) |
| `-p`, `--proj`              | Yes | Proj4 CRS parameters (e.g. `+proj=tmerc ...`) |
| `-pcr`, `--pointRatio`      | Yes | Ratio of points from original data |
| `-pg`, `--photogrammetry`   | No  | [Experimental][GPU] Generate `.b3dm` from photogrammetry |
| `-q`, `--quiet`             | No  | Silent mode (suppress logs) |
| `-qt`, `--quantize`         | No  | Quantize mesh using `KHR_mesh_quantization` |
| `-r`, `--recursive`         | No  | Process subdirectories recursively |
| `-ra`, `--refineAdd`        | No  | Use 'ADD' refine mode for 3D Tiles |
| `-ru`, `--flipUpAxis`       | No  | Rotate matrix 180° about X-axis |
| `-rx`, `--rotateXAxis`      | Yes | Rotate X-axis by degrees |
| `-sh`, `--skirtHeight`      | Yes | Building skirt height for extrusion model |
| `-sp`, `--sourcePrecision`  | No  | Use original precision for point cloud |
| `-su`, `--swapUpAxis`       | No  | Rotate matrix -90° about X-axis |
| `-te`, `--terrain`          | Yes | GeoTIFF terrain file path (used with `clampToGround`) |
| `-vl`, `--voxelLod`         | No  | [Experimental] Voxel LOD setting for `.i3dm` |
| `-xl`, `--maxLod`           | Yes | Maximum Level of Detail |
| `-xo`, `--xOffset`          | Yes | X offset for coordinate transformation |
| `-yo`, `--yOffset`          | Yes | Y offset for coordinate transformation |
| `-zo`, `--zeroOrigin`       | No  | [Experimental] Fix root transform matrix origin to (0, 0, 0) |



## Mesh Data Conversion Cases

### Converting General Mesh Data
Basic mesh data conversion commands that can be used when converting data.
When converting, the default coordinate system is projected to the EPSG:3857 coordinate system.

```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path"
```

Same case :
```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path" -crs 3857
```

### Batched 3D Model (b3dm)

Can be used to convert common data.
Except for point cloud data, if you do not enter an outputType, it will be generated as b3dm.

```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

Same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

### DEM elevation application case
This is the case of putting 3D data on the terrain height of a single channel such as GeoTiff.

```
java -jar mago-3d-tiler.jar -input "/input_path/sample" -output "/output_path/sample" -terrain "/input_path/sample/terrain.tif"
```

### Converting 3D data with an applied coordinate system
The example below is a sample of converting 3ds (3D MAX) data. It converts to the case where the data has a coordinate system applied to it by adding the `crs` option.
In this case, we have entered the EPSG:5186 coordinate system.

```
java -jar mago-3d-tiler.jar -input "/input_path/3ds" -inputType "3ds" -output "/output_path/3ds" -crs "5186"
```

---
## GIS Vector Data Conversion Cases

### Converting 2D GIS Polygon Data (Shp, GeoJson)
The example below extrudes 2D GIS polygon data.
The extrusion height can be specified to reference a customized attribute name using the `-heightColumn <arg>` attribute.
Similarly, the extrusion start height defaults to 0, and the height of the base plane can be specified via `-altitudeColumn <arg>`.

```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```
same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186" -heightAttribute "height"
```

geojson case :
```
java -jar mago-3d-tiler.jar -input "/input_path/geojson" -inputType "geojson" -output "/output_path/geojson" -crs "5186"
```

### Converting 2D GIS Polyline Data (Shp)
Convert polyline data to pipe. Polyline data with a z-axis can be converted via the `diameter` property.
The default dimension for a pipe in mago 3DTiler is diameter and The length is in millimeters (mm)
```
java -jar mago-3d-tiler.jar -input "/input_path/shp" -inputType "shp" -output "/output_path/shp" -crs "5186"
```

## Instance Model Conversion Cases

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

## Point-Clouds Data Conversion Cases

### Converting Point-Clouds data (Point Clouds)

When converting point-clouds data, the following default options are available for conversion.
If the input data is "las", the "-outputType" will automatically be "pnts".

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las"
```

same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -outputType "pnts"
```

## Other Conversion Examples

### Up-Axis Swap Example
mago3dTiler converts mesh z-up axis data to y-up axis. If your original data is y-up axis, you will need to add the `-rotateX <degree>` option to avoid converting it.
```
java -jar mago-3d-tiler.jar -input "/input_path/y-up-fbx" -inputType "fbx" -output "/output_path/y-up-fbx" -rotateX "90"
```

### Data flipped upside down
If the converted data is flipped upside down, add the `-rotateX <degree>` option to convert it.
```
java -jar mago-3d-tiler.jar -input "/input_path/flip-y-up-fbx" -inputType "fbx" -output "/output_path/flip-y-up-fbx" -rotateX "180"
```

### Converting CityGML
When converting to CityGML, it is recommended to give the InputType as ‘citygml’.
This is because Citygml data can have different extensions: ‘.xml’, ‘.gml’, etc.
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