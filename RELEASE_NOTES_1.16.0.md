# mago-3d-tiler v1.16.0

This is the first feature release in approximately eight months. The release took longer than expected due to a wide range of changes and improvements made throughout the project.

This release adds **Implicit 3D Tiles**, expands built-in **geoid support**, improves **glTF material interpretation**, and introduces scene **validation, reporting, and automatic repair**. The runtime has also been standardized on **Java 21**.

As more features have been added, the project structure and command-line interface have gradually become more complex. Version 1.16.0 is intended to provide a stable and reliable release of the current architecture before the project moves toward a larger renewal.

For a future successor, tentatively referred to as **mago-3d-tiler 2**, we plan to separate the project into smaller modules with clearer responsibilities and improve how features are composed and integrated. The major processing areas under consideration are **photogrammetry**, **point clouds**, **batched models**, **GPU instancing**, and **BIM**.

## [Feature Additions]

- **Implicit 3D Tiles (Experimental)**  
  Adds support for implicit tileset hierarchies. Explicit tiling remains the default.  
  Usage example:  
  `--tilingMode implicit --implicitSubtreeLevels 4`

- **Expanded Geoid Support**  
  Adds built-in **EGM84** and **EGM2008** models alongside EGM96. Custom GeoTIFF geoid files remain supported.  
  Usage example:  
  `--geoid EGM84`  
  `--geoid EGM2008`

- **Validation, Reporting & Automatic Repair**  
  Improves input-scene validation and automatically repairs supported consistency issues. Optional JSON reports can be generated for individual files and the complete batch.  
  Usage example:  
  `--validationReport`

- **Quantized Mesh Terrain Support**  
  The `--terrain` option now accepts a local Quantized Mesh `layer.json` in addition to GeoTIFF terrain data.  
  Usage example:  
  `--terrain {quantized_mesh_directory}/layer.json`

- **Tileset Root Transform Update (Experimental)**  
  Adds an option to apply a 4x4 transform matrix to the root node of an existing `tileset.json`.  
  Usage example:  
  `--updateRootTransform {16_comma_separated_values}`

- **Verbose Logging**  
  Adds `--verbose` for debug-level logs without enabling the additional behavior of `--debug`.

## [Feature Improvements]

- **Core Conversion Stability**  
  Improved conversion reliability, memory management, tileset validation, bounding volumes, geometric errors, terrain sampling, and temporary-resource handling.

- **glTF Material & Geometry Interpretation**  
  Improved material, texture, transform, texture-coordinate, and normal interpretation. Added fallback normal generation and improved quantized glTF output.

- **Photogrammetry Conversion (Experimental)**  
  Reworked processing to make better use of CPU resources for broader compatibility, with additional performance and visual-quality improvements.

- **Runtime Standardization**  
  The project now targets and requires **Java 21**.

## [Bug Fixes]

- Fixed photogrammetry alignment, duplicated meshes, Z-offset handling, and texture projection issues
- Fixed GeoTIFF terrain sampling at pixel boundaries and explicit `Ellipsoid` geoid handling
- Fixed missing or empty instance handling in i3dm conversion
- Fixed glTF normal quantization, transform baking, and material-processing issues
- Fixed multiple mesh decimation, remeshing, tileset merge, and validation issues
- Fixed temporary-file collisions and improved cleanup of textures, images, and intermediate resources
