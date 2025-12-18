# Changelog
All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

---

## [1.15.3] - 2025-12-15

### Added
- CLI: Added temporary directory option (`--temp`) and cleaned up CLI options.
- Runtime: Apply `AsyncAppender` when not in debug mode.
- Point Cloud: Added chunk-unit tiling support.
- Point Cloud: Added and improved point-cloud tile expand feature.
- Point Cloud: Improved point cloud conversion workflow and overall stability.
- Tileset: Improved point cloud tiling including geometric error calculation and tree distribution.
- Dependencies: Added `junit-platform-launcher` dependency.
- Dependencies: Added `gt-epsg-hsql` dependency.
- Photogrammetry: Added four additional oblique camera directions.
- Photogrammetry: Improved photogrammetry conversion speed.
- Architecture: Migrated converter features from `mago-tiler` to `mago-io`.

### Changed
- Performance: Improved heap stability during point cloud conversion.
- Performance: Improved shuffling and bounding-box fitting logic during point cloud conversion.
- Performance: Optimized bounding box calculation in `addPoint()`.
- Configuration: Added default parametric values for diameter and heading.
- Runtime: Removed explicit `System.gc()` call.
- Threading: Changed default thread count from 2 to 3 and added `TreeInstanceTiler`.
- Usability: Applied sorting to `contentInfos` list for immediate usability.
- Build: Updated Gradle configurations and project version.
- Maintenance: Refactored and cleaned source code (formatting, imports, unused annotations, modifiers/traverser refactoring, GaiaScene split, logging cleanup).

### Fixed
- Point Cloud: Fixed point leakage bug during point cloud tile expansion.
- Tileset: Omitted content generation for 3D Tiles nodes with zero points.
- CRS/EPSG: Fixed EPSG lookup failure and reduced related JAI warnings.
- Build/Test: Fixed Gradle build failures caused by incorrect JUnit dependency configuration.
- Mesh: Fixed vertical skirt infinite direction issue.
- Rendering: Reverted `drawImage` bug caused by unintended code change.
- glTF: Fixed `GltfWriter` texture coordinate bug.
- Rendering: Fixed background color regression by reverting related changes.

### Documentation
- Updated documentation and README files.

### Tests
- Fixed and updated release and build test cases.
