Different conversion cases
===

## Converting Mesh Data

### Converting General Mesh Data
Basic mesh data conversion commands that can be used when converting data.
When converting, the default coordinate system is projected to the EPSG:3857 coordinate system.

```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path"
```

Same case :
```
java -jar mago-3d-tiler.jar -input "/input_path" -output "/output_path" -crs 3857 -outputType b3dm
```

### Batched 3D Model (b3dm)

Can be used to convert common data.
Except for point cloud data, if you do not enter an outputType, it will be generated as b3dm.

```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada"
```

Same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/kml_with_collada" -output "/output_path/kml_with_collada" -outputType b3dm
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
# Converting 2D Vector Data

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

---
# Converting Instance Model

### Instanced 3D Model (i3dm)

When creating instance model data, the following options are available for conversion.
(kml with collada) data, and the `outputType` option is required in the current version.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -outputType "i3dm"
```

### Converting i3dm data to Shape

To generate i3dm as a Shape file with Point geometry type, you can convert it with the following options.
You need to specify `inputType` as shp and specify the path to the instance file through the 'instance' option.

```
java -jar mago-3d-tiler.jar -input "/input_path/i3dm" -output "/output_path/i3dm" -inputType "shp" -outputType "i3dm" -instance "/input_path/instance.gltf"
```

---
# Converting Point-Clouds Data

### Converting Point-Clouds data (Point Clouds)

When converting point-clouds data, the following default options are available for conversion.

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las"
```

same case :
```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -outputType "pnts"
```


---
# Other examples

### Up-Axis Swap Example
mago3dTiler converts mesh z-up axis data to y-up axis. If your original data is y-up axis, you will need to add the `-swapUpAxis` option to avoid converting it.
```
java -jar mago-3d-tiler.jar -input "/input_path/y-up-fbx" -inputType "fbx" -output "/output_path/y-up-fbx" -swapUpAxis
```

### Data flipped upside down
If the converted data is flipped upside down, add the `-flipUpAxis` option to convert it.
Can be used with the -swapUpAxis option.
```
java -jar mago-3d-tiler.jar -input "/input_path/flip-y-up-fbx" -inputType "fbx" -output "/output_path/flip-y-up-fbx" -swapUpAxis -flipUpAxis
```

### Converting Large 3D Mesh Data
[Warning: Experimental]   
This option tiles large mesh data by breaking it down into smaller units.
This can be specified via the `-largeMesh` option.

```
java -jar mago-3d-tiler.jar -input "/input_path/ifc_large_mesh" -inputType "ifc" -output "/output_path/ifc_large_mesh" -largeMesh
```

### Converting Large Point-Clouds Data
When converting large point-clouds, you can use the `-pointSkip` option to adjust the conversion speed and data size as follows.

```
java -jar mago-3d-tiler.jar -input "/input_path/las" -inputType "las" -output "/output_path/las" -pointSkip 4
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