# mago-3d-tiler v1.17.1

This release makes long conversions **easier on the disks** they run on. Temporary files are now removed when a run fails or is stopped, the free space of the temp and output drives is checked before anything is written, and the point cloud shuffle reads and writes each bucket at most twice instead of 4 to 12 times.

## [Feature Improvements]

- **Fewer Point Cloud Shuffle Passes**  
  Point buckets are shuffled in memory when they fit in a 2 GB section and otherwise scattered into at most 64 random temp files that are each shuffled in memory (Rao-Sandelius), so a bucket is read and written once up to 2 GB, twice up to about 64 GB and three times around 200 GB. The order is a uniform, seeded permutation, so coarse LODs sample the whole bucket evenly and the same input gives the same output.  
  On a 1.2 GB LAS (153,604,527 points) the conversion took 380 s instead of 1,193 s and the peak temp size dropped from 16.6 GB to 9.4 GB, with the same point count.

- **Buffered Point Reads**  
  Point cloud chunks are read through a 4 MB buffer instead of one read call per point.

- **Disk Space Check**  
  Before writing, the free space of the temp and output drives is compared with an estimate from the size of the selected input files (with their side files such as `.mtl`/`.dbf` and nearby textures). The run stops when a drive has less than the minimum estimate and logs a warning when it has less than the expected size. The ratios come from the peak temp and output sizes of the release samples per input kind (mesh, vector, instance, point cloud, photogrammetry).

## [Behavior Changes]

- The temp directory is deleted when a run fails or is stopped (Ctrl+C, `docker stop`), not only on success. Use `--leaveTemp` to keep it for debugging.
- With `--leaveTemp`, only the most recent `temp_backup_*` folder is kept; older backups are removed instead of accumulating.
- A run that clearly does not fit on the temp or output drive now stops at the start with `[Disk] Not enough disk space` instead of failing part way through.
- Point cloud tiles are built from a different (uniform) shuffle order than 1.17.0, so tile contents and counts can differ slightly; the total point count is the same.

## [Bug Fixes]

- Fixed temp files staying behind after failed or interrupted runs.
- Fixed `temp_backup_*` folders piling up on reruns with `--leaveTemp`.
- Fixed the point cloud shuffle keeping two section-sized (about 2 GB each) buffers in memory.
- Fixed photogrammetry `reMeshTemp` being written to `<output>/temp` instead of the `--temp` directory.
