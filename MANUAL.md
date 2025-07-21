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
| Option                             | Argument Required | Description                                                                                                                        |
| ---------------------------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `-h`, `--help`                     | No                | Print Help                                                                                                                         |
| `-q`, `--quiet`                    | No                | Quiet mode/Silent mode                                                                                                             |
| `-lt`, `--leaveTemp`               | No                | Leave temporary files                                                                                                              |
| `-m`, `--merge`                    | No                | Merge tileset.json files                                                                                                           |
| `-i`, `--input <arg>`              | Yes               | Input directory path                                                                                                               |
| `-o`, `--output <arg>`             | Yes               | Output directory file path                                                                                                         |
| `-it`, `--inputType <arg>`         | Yes               | Input files type \[kml, 3ds, fbx, obj, gltf/glb, las/laz, citygml, indoorgml, shp, geojson, gpkg]                                  |
| `-ot`, `--outputType <arg>`        | Yes               | Output 3DTiles Type \[b3dm, i3dm, pnts]                                                                                            |
| `-l`, `--log <arg>`                | Yes               | Output log file path                                                                                                               |
| `-r`, `--recursive`                | No                | Tree directory deep navigation                                                                                                     |
| `-te`, `--terrain <arg>`           | Yes               | GeoTiff Terrain file path, 3D Object applied as clampToGround (Supports geotiff format)                                            |
| `-if`, `--instance <arg>`          | Yes               | Instance file path for I3DM (Default: {OUTPUT}/instance.dae)                                                                       |
| `-qt`, `--quantize`                | No                | Quantize mesh to reduce glb size via "KHR\_mesh\_quantization" Extension                                                           |
| `-c`, `--crs <arg>`                | Yes               | Coordinate Reference Systems, EPSG Code (4326, 3857, 32652, 5186...)                                                               |
| `-p`, `--proj <arg>`               | Yes               | Proj4 parameters (ex: +proj=tmerc +la...)                                                                                          |
| `-xo`, `--xOffset <arg>`           | Yes               | X Offset value for coordinate transformation                                                                                       |
| `-yo`, `--yOffset <arg>`           | Yes               | Y Offset value for coordinate transformation                                                                                       |
| `-zo`, `--zOffset <arg>`           | Yes               | Z Offset value for coordinate transformation                                                                                       |
| `-lon`, `--longitude <arg>`        | Yes               | Longitude value for coordinate transformation. (The lon lat option must be used together).                                         |
| `-lat`, `--latitude <arg>`         | Yes               | Latitude value for coordinate transformation. (The lon lat option must be used together).                                          |
| `-rx`, `--rotateXAxis <arg>`       | Yes               | Rotate the X-Axis in degrees                                                                                                       |
| `-ra`, `--refineAdd`               | No                | Set 3D Tiles Refine 'ADD' mode                                                                                                     |
| `-mx`, `--maxCount <arg>`          | Yes               | Maximum number of triangles per node                                                                                               |
| `-nl`, `--minLod <arg>`            | Yes               | Min level of detail                                                                                                                |
| `-xl`, `--maxLod <arg>`            | Yes               | Max Level of detail                                                                                                                |
| `-ng`, `--minGeometricError <arg>` | Yes               | Minimum geometric error                                                                                                            |
| `-mg`, `--maxGeometricError <arg>` | Yes               | Maximum geometric error                                                                                                            |
| `-mp`, `--maxPoints <arg>`         | Yes               | Maximum number of points per a tile                                                                                                |
| `-pcr`, `--pointRatio <arg>`       | Yes               | Percentage of points from original data                                                                                            |
| `-sp`, `--sourcePrecision`         | No                | Create point cloud tile with original precision                                                                                    |
| `-f4`, `--force4ByteRGB`           | No                | Force 4Byte RGB for point cloud tile                                                                                               |
| `-fc`, `--flipCoordinate`          | No                | Flip x, y coordinate for 2D Original Data                                                                                          |
| `-af`, `--attributeFilter <arg>`   | Yes               | Attribute filter setting for extrusion model ex) "classification=window,door;type=building"                                        |
| `-nc`, `--nameColumn <arg>`        | Yes               | Name column setting for extrusion model                                                                                            |
| `-hc`, `--heightColumn <arg>`      | Yes               | Height column setting for extrusion model                                                                                          |
| `-ac`, `--altitudeColumn <arg>`    | Yes               | Altitude Column setting for extrusion model                                                                                        |
| `-hd`, `--headingColumn <arg>`     | Yes               | Heading column setting for I3DM converting                                                                                         |
| `-scl`, `--scaleColumn <arg>`      | Yes               | Scale column setting for I3DM converting                                                                                           |
| `-den`, `--densityColumn <arg>`    | Yes               | Density column setting for I3DM polygon converting                                                                                 |
| `-dc`, `--diameterColumn <arg>`    | Yes               | Diameter column setting for pipe extrusion model, Specify a length unit for Diameter in millimeters(mm) (Default Column: diameter) |
| `-mh`, `--minimumHeight <arg>`     | Yes               | Minimum height value for extrusion model                                                                                           |
| `-aa`, `--absoluteAltitude <arg>`  | Yes               | Absolute altitude value for extrusion model                                                                                        |
| `-sh`, `--skirtHeight <arg>`       | Yes               | Building Skirt height setting for extrusion model                                                                                  |
| `-tv`, `--tilesVersion <arg>`      | Yes               | \[Experimental] 3DTiles Version \[Default: 1.1]\[1.0, 1.1]                                                                         |
| `-pg`, `--photogrammetry`          | No                | \[Experimental] generate b3dm for photogrammetry model with GPU                                                                    |
| `-mc`, `--multiThreadCount <arg>`  | Yes               | \[Deprecated] set thread count                                                                                                     |
| `-glb`, `--glb`                    | No                | \[Deprecated] Create glb file with B3DM                                                                                            |
| `-igtx`, `--ignoreTextures`        | No                | \[Deprecated] Ignore diffuse textures                                                                                              |
| `-d`, `--debug`                    | No                | \[DEBUG] More detailed log output and stops on Multi-Thread bugs                                                                   |



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
mago3dTiler converts mesh z-up axis data to y-up axis. If your original data is y-up axis, you will need to add the `-rotateXAxis <degree>` option to avoid converting it.
```
java -jar mago-3d-tiler.jar -input "/input_path/y-up-fbx" -inputType "fbx" -output "/output_path/y-up-fbx" -rotateXAxis "90"
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