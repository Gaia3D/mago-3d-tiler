package com.gaia3d.command.mago;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("default")
class OutputArchiveOptionTest {
    private static final String TRANSFORM = "1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1";

    @Test
    void rejectsArchiveWithMerge() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "-oa", "3tz", "-merge")));
        assertThrows(IllegalArgumentException.class, () -> GlobalOptions.checkOutputArchiveSupported(parse("--input", "in", "--output", "out", "--outputArchive", "3TZ", "--merge")));
    }

    @Test
    void rejectsArchiveWithUpdateRootTransform() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "-oa", "3tz", "-urt", TRANSFORM)));
        assertThrows(IllegalArgumentException.class, () -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "--outputArchive", "3tz", "--updateRootTransform", TRANSFORM)));
    }

    @Test
    void acceptsFolderOutputWithMergeAndUpdateRootTransform() throws Exception {
        assertDoesNotThrow(() -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "-merge")));
        assertDoesNotThrow(() -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "-oa", "none", "-merge")));
        assertDoesNotThrow(() -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "-urt", TRANSFORM)));
        assertDoesNotThrow(() -> GlobalOptions.checkOutputArchiveSupported(parse("-i", "in", "-o", "out", "-oa", "3tz")));
    }

    private static CommandLine parse(String... args) throws ParseException {
        DefaultCommandLineConfiguration configuration = new DefaultCommandLineConfiguration();
        return configuration.createCommandLine(configuration.createOptions(), args);
    }
}
