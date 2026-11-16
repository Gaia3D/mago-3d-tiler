package com.gaia3d.process;

import com.gaia3d.basic.types.FormatType;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Checks before a run that the temp and output drives have room for the conversion.
 * The need is estimated from the size of the input files: a run that clearly cannot fit stops before writing anything,
 * and a run that may not fit logs a warning.
 */
@Slf4j
public class DiskSpaceChecker {
    // headroom left for the OS, logs and other programs on the same drive
    static final long RESERVED_BYTES = 1L << 30;

    /**
     * @throws IllegalStateException when a drive has less room than the estimated minimum
     */
    public static void check(File tempDir, File outputDir, File input, FormatType inputFormat, FormatType outputFormat, boolean photogrammetry) {
        long inputBytes = sizeOf(input); // the whole input folder, textures next to the models count too
        if (inputBytes <= 0) {
            return;
        }
        Ratio ratio = Ratio.of(inputFormat, outputFormat, photogrammetry);
        Map<FileStore, Need> needs = new LinkedHashMap<>();
        addNeed(needs, tempDir, "temp", inputBytes * ratio.minimumTemp, inputBytes * ratio.expectedTemp);
        addNeed(needs, outputDir, "output", inputBytes * ratio.minimumOutput, inputBytes * ratio.expectedOutput);

        for (Map.Entry<FileStore, Need> entry : needs.entrySet()) {
            Need need = entry.getValue();
            long usable;
            try {
                usable = entry.getKey().getUsableSpace();
            } catch (IOException e) {
                log.warn("[WARN][Disk] Can not read the free space of {}: {}", need.dirs, e.getMessage());
                continue;
            }
            check(usable, inputBytes, need);
        }
    }

    static void check(long usable, long inputBytes, Need need) {
        long minimum = (long) need.minimum + RESERVED_BYTES;
        long expected = (long) need.expected + RESERVED_BYTES;
        String message = String.format("drive of %s has %s free, input is %s and the run needs about %s (at least %s)",
                String.join(", ", need.dirs), toReadable(usable), toReadable(inputBytes), toReadable(expected), toReadable(minimum));
        if (usable < minimum) {
            throw new IllegalStateException("[Disk] Not enough disk space: " + message + ". Free up space or choose another drive with --temp/--output.");
        } else if (usable < expected) {
            log.warn("[WARN][Disk] Disk space may run out: {}. Consider freeing up space or choosing another drive with --temp/--output.", message);
        } else {
            log.info("[Disk] The {}", message);
        }
    }

    private static void addNeed(Map<FileStore, Need> needs, File dir, String name, double minimum, double expected) {
        FileStore store = findStore(dir);
        if (store == null) {
            log.warn("[WARN][Disk] Can not find the drive of the {} directory {}", name, dir.getAbsolutePath());
            return;
        }
        Need need = needs.computeIfAbsent(store, key -> new Need());
        need.minimum += minimum;
        need.expected += expected;
        need.dirs.add(name + " " + dir.getAbsolutePath());
    }

    static long sizeOf(File input) {
        if (input.isFile()) {
            return input.length();
        }
        try (Stream<Path> paths = Files.walk(input.toPath())) {
            return paths.mapToLong(path -> {
                File file = path.toFile();
                return file.isFile() ? file.length() : 0L;
            }).sum();
        } catch (IOException | UncheckedIOException e) {
            log.warn("[WARN][Disk] Can not read the size of the input {}: {}", input.getAbsolutePath(), e.getMessage());
            return 0L;
        }
    }

    // the directory may not exist yet, so look up the drive of its nearest existing parent
    private static FileStore findStore(File dir) {
        Path path = dir.getAbsoluteFile().toPath();
        while (path != null && !Files.exists(path)) {
            path = path.getParent();
        }
        if (path == null) {
            return null;
        }
        try {
            return Files.getFileStore(path);
        } catch (IOException e) {
            return null;
        }
    }

    static String toReadable(long bytes) {
        if (bytes >= 1L << 30) {
            return String.format("%.1f GB", bytes / (double) (1L << 30));
        }
        return String.format("%.1f MB", bytes / (double) (1L << 20));
    }

    static class Need {
        double minimum;
        double expected;
        final List<String> dirs = new ArrayList<>();
    }

    /**
     * Temp and output size per byte of input, from the peak sizes of the release samples (2026-11):
     * mesh 3ds/citygml temp 1.4 output 0.3-0.4, vector gpkg temp 0.75 output 0.1 and pipes temp 27 output 5,
     * i3dm from kml no temp output 19, las/copc temp 14-17 output 2.9, photogrammetry obj temp 9.5 output 0.2.
     * The minimum stays below every sample of the kind, the expectation covers the heaviest one.
     */
    enum Ratio {
        MESH(1.0, 3.0, 0.2, 1.0),
        VECTOR(0.5, 30.0, 0.1, 6.0),
        INSTANCE(0.0, 1.0, 2.0, 25.0),
        POINT_CLOUD(5.0, 20.0, 1.0, 4.0),
        PHOTOGRAMMETRY(4.0, 12.0, 0.1, 0.5);

        final double minimumTemp;
        final double expectedTemp;
        final double minimumOutput;
        final double expectedOutput;

        Ratio(double minimumTemp, double expectedTemp, double minimumOutput, double expectedOutput) {
            this.minimumTemp = minimumTemp;
            this.expectedTemp = expectedTemp;
            this.minimumOutput = minimumOutput;
            this.expectedOutput = expectedOutput;
        }

        static Ratio of(FormatType inputFormat, FormatType outputFormat, boolean photogrammetry) {
            if (inputFormat == FormatType.LAS || inputFormat == FormatType.LAZ) {
                return POINT_CLOUD;
            } else if (photogrammetry) {
                return PHOTOGRAMMETRY;
            } else if (outputFormat == FormatType.I3DM || outputFormat == FormatType.FOREST) {
                return INSTANCE;
            } else if (inputFormat == FormatType.SHP || inputFormat == FormatType.GEOJSON || inputFormat == FormatType.GEO_PACKAGE) {
                return VECTOR;
            }
            return MESH;
        }
    }
}
