# mago-3d-tiler v1.17.0

This release focuses on the **texture quality, memory use and size of batched models**. Batched textures are now sized by a texel density budget per level of detail instead of a fixed atlas size, only the part of each source texture that is actually used is packed, textures can be written as **KTX2** for about one eighth of the GPU memory, and geometry can be **Draco**-compressed. The compression options are reorganized into `--meshCompression` and `--textureCompression`.

The runtime now requires **Java 25**.

## [Feature Additions]

- **KTX2 Texture Compression**  
  Encodes textures as KTX2 (Basis Universal ETC1S with mipmaps) through `KHR_texture_basisu`. Viewers transcode them to a GPU block format, about 1/8 of the GPU memory of the same JPEG/PNG texture once decoded.  
  Usage example:  
  `--textureCompression ktx2`

- **Unlit Materials**  
  Writes `KHR_materials_unlit` materials, so textures show their own colors without scene lighting.  
  Usage example:  
  `--unlit`

- **Draco Mesh Compression**  
  Encodes the geometry of batched models with `KHR_draco_mesh_compression` (pure Java encoder, Openize.Drako). Positions are quantized to 16 bits, texture coordinates to 14 and colors to 8; normals and feature IDs are kept exact. Geometry shrank by 31–37% on the samples tested (14.3 MB to 9.1 MB on the OBJ district), against about 9% with `quantize`, and decodes with Google's Draco decoder used by CesiumJS. Instanced (i3dm) and point cloud (pnts) content is written uncompressed with a warning.  
  Usage example:  
  `--meshCompression draco`

- **Mesh Compression Option**  
  `--meshCompression` selects the geometry compression: `none`, `quantize` (`KHR_mesh_quantization`, the same output as the former `--quantize`) or `draco`.  
  Usage example:  
  `--meshCompression quantize`

## [Feature Improvements]

- **Texel Density Budget for Batched Textures**  
  Batched texture atlases are sized by a texel density budget instead of a fixed size per LOD: 200 texels/m² for leaf tiles and `(1.5 × 16 / geometricError)²` for coarser tiles, measured against the full-resolution source and never upscaled.

- **Used-Region Atlas Packing**  
  Each material contributes only the region of its texture referenced by UVs (plus a 4 px border), instead of the whole image.

- **Multiple Atlases per Tile**  
  A tile whose textures do not fit one atlas within the budget gets up to four atlases (per single/double-sided group) instead of one heavily downscaled atlas. The atlas cap is now 4096 px for leaf tiles and 2048 px for coarser tiles.

- **Batched Texture Results**  
  On a 426-model OBJ district (Yeongdeungpo-dong 2-ga, Seoul), texels per m² of surface for leaf / LOD1 / LOD2 / LOD3 tiles improved from 122 / 12 / 4 / 2 to 143 / 95 / 33 / 9. With `--textureCompression ktx2` the estimated GPU texture memory dropped from 2,744 MB (JPEG) to 344 MB.

- **Runtime Standardization**  
  The project now targets and requires **Java 25** (Gradle 9.9, Lombok 1.18.48). JDK 21 builds bundle different MSVC runtimes on Windows, and those before 21.0.5 crash native libraries such as the KTX2 encoder; every JDK 25 build works. The Docker image is based on `eclipse-temurin:25-jre`.

## [Behavior Changes]

- Batched outputs differ from 1.16.x even without new options: textures are sharper and output files are larger (about 1.5–1.8× on the samples tested), because of the texel budget, used-region packing and multiple atlases.
- OBJ materials with `map_Kd` no longer multiply the texture by `Kd`. Exporters such as 3ds Max write `Kd 0.588` next to every texture, which made textured OBJ models about 40% darker.
- A batched atlas no longer takes the color factor of its first material; each material's color factor is baked into its own atlas region.
- `--quantize` is deprecated; use `--meshCompression quantize`. Combining `--quantize` with a different `--meshCompression` value is an error.

## [Bug Fixes]

- Fixed textured OBJ models rendering darker than their textures (`Kd` multiplied into `map_Kd`).
- Fixed batched tiles taking a random brightness from the first material of their atlas.
- Fixed batched material lookup using a `WeakHashMap` keyed by boxed material IDs, whose entries could be collected while in use.
