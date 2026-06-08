package com.gaia3d.converter.assimp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("default")
class AssimpCrashProbeTest {
    private static final Path SAMPLE = Path.of(
            "H:",
            "\uac1c\ud3ec\ub3d9",
            "B001DS60204181_00",
            "B001DS60204181_00.obj");
    private static final Path SAMPLE_ROOT = Path.of("H:", "\uac1c\ud3ec\ub3d9");
    private static final Path PROBE_LOG = Path.of("build", "assimp-crash-probe.log");

    private static final int DEFAULT_FLAGS = Assimp.aiProcess_GenNormals |
            Assimp.aiProcess_Triangulate |
            Assimp.aiProcess_JoinIdenticalVertices |
            Assimp.aiProcess_CalcTangentSpace |
            Assimp.aiProcess_SortByPType;

    @Test
    void defaultFlags() {
        importSample(DEFAULT_FLAGS);
    }

    @Test
    void withoutCalcTangentSpace() {
        importSample(DEFAULT_FLAGS & ~Assimp.aiProcess_CalcTangentSpace);
    }

    @Test
    void withoutCalcTangentSpaceAndJoinIdenticalVertices() {
        importSample(DEFAULT_FLAGS
                & ~Assimp.aiProcess_CalcTangentSpace
                & ~Assimp.aiProcess_JoinIdenticalVertices);
    }

    @Test
    void withoutPostProcessFlags() {
        importSample(0);
    }

    @Test
    void directImportFirst700DefaultFlags() {
        resetProbeLog();
        List<Path> objFiles = listObjFiles();
        int limit = Math.min(700, objFiles.size());
        for (int index = 0; index < limit; index++) {
            Path path = objFiles.get(index);
            logProbe("direct before", index, objFiles.size(), path);
            AIScene scene = Assimp.aiImportFile(path.toAbsolutePath().toString(), DEFAULT_FLAGS);
            assertNotNull(scene, Assimp.aiGetErrorString());
            Assimp.aiReleaseImport(scene);
            logProbe("direct after ", index, objFiles.size(), path);
        }
    }

    @Test
    void directImportAllDefaultFlags() {
        resetProbeLog();
        List<Path> objFiles = listObjFiles();
        for (int index = 0; index < objFiles.size(); index++) {
            Path path = objFiles.get(index);
            logProbe("direct before", index, objFiles.size(), path);
            AIScene scene = Assimp.aiImportFile(path.toAbsolutePath().toString(), DEFAULT_FLAGS);
            assertNotNull(scene, Assimp.aiGetErrorString());
            Assimp.aiReleaseImport(scene);
            logProbe("direct after ", index, objFiles.size(), path);
        }
    }

    @Test
    void directImportAllNoPostProcessFlags() {
        importAll(0);
    }

    @Test
    void directImportAllTriangulateOnly() {
        importAll(Assimp.aiProcess_Triangulate);
    }

    @Test
    void directImportAllTriangulateAndGenNormals() {
        importAll(Assimp.aiProcess_Triangulate | Assimp.aiProcess_GenNormals);
    }

    @Test
    void directImportAllWithoutJoinIdenticalVertices() {
        importAll(DEFAULT_FLAGS & ~Assimp.aiProcess_JoinIdenticalVertices);
    }

    @Test
    void directImportAllWithoutCalcTangentSpaceAndJoinIdenticalVertices() {
        importAll(DEFAULT_FLAGS
                & ~Assimp.aiProcess_CalcTangentSpace
                & ~Assimp.aiProcess_JoinIdenticalVertices);
    }

    @Test
    void converterLoadFirst700DefaultFlags() {
        resetProbeLog();
        AssimpConverter converter = new AssimpConverter(AssimpConverterOptions.builder().build());
        List<Path> objFiles = listObjFiles();
        int limit = Math.min(700, objFiles.size());
        for (int index = 0; index < limit; index++) {
            Path path = objFiles.get(index);
            logProbe("converter before", index, objFiles.size(), path);
            converter.load(path.toFile());
            logProbe("converter after ", index, objFiles.size(), path);
        }
    }

    @Test
    void converterLoadAllDefaultFlags() {
        resetProbeLog();
        AssimpConverter converter = new AssimpConverter(AssimpConverterOptions.builder().build());
        List<Path> objFiles = listObjFiles();
        for (int index = 0; index < objFiles.size(); index++) {
            Path path = objFiles.get(index);
            logProbe("converter before", index, objFiles.size(), path);
            converter.load(path.toFile());
            logProbe("converter after ", index, objFiles.size(), path);
        }
    }

    private void importSample(int flags) {
        String filePath = SAMPLE.toAbsolutePath().toString();
        System.out.printf("Assimp probe file=%s flags=%d%n", filePath, flags);
        AIScene scene = Assimp.aiImportFile(filePath, flags);
        assertNotNull(scene, Assimp.aiGetErrorString());
        Assimp.aiReleaseImport(scene);
    }

    private void importAll(int flags) {
        resetProbeLog();
        List<Path> objFiles = listObjFiles();
        for (int index = 0; index < objFiles.size(); index++) {
            Path path = objFiles.get(index);
            logProbe("direct before flags=" + flags, index, objFiles.size(), path);
            AIScene scene = Assimp.aiImportFile(path.toAbsolutePath().toString(), flags);
            assertNotNull(scene, Assimp.aiGetErrorString());
            Assimp.aiReleaseImport(scene);
            logProbe("direct after  flags=" + flags, index, objFiles.size(), path);
        }
    }

    private List<Path> listObjFiles() {
        try (Stream<Path> stream = Files.walk(SAMPLE_ROOT)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".obj"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void logProbe(String phase, int index, int total, Path path) {
        String message = String.format("PROBE %s [%d/%d] %s%n", phase, index + 1, total, path);
        System.out.print(message);
        try {
            Files.createDirectories(PROBE_LOG.getParent());
            Files.writeString(PROBE_LOG, message,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void resetProbeLog() {
        try {
            Files.deleteIfExists(PROBE_LOG);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
