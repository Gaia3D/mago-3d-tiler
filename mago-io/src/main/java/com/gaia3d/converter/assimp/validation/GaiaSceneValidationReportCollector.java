package com.gaia3d.converter.assimp.validation;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Thread-safe accumulator for GaiaSceneValidationReport instances collected during a tiling run.
 * <p>
 * When --validation-report is active, each loader calls {@link #collect} with the pre-repair report
 * and the temp directory. An individual JSON file is written per failure, and at the end of the
 * pipeline {@link #writeSummary} writes a consolidated batch report to the output directory.
 */
@Slf4j
public class GaiaSceneValidationReportCollector {
    private static final GaiaSceneValidationReportCollector INSTANCE = new GaiaSceneValidationReportCollector();

    private final List<GaiaSceneValidationReport> reports = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> reservedFilenames = Collections.synchronizedSet(new HashSet<>());

    private GaiaSceneValidationReportCollector() {}

    public static GaiaSceneValidationReportCollector getInstance() {
        return INSTANCE;
    }

    public void collect(GaiaSceneValidationReport report, File tempDir) {
        reports.add(report);
        if (tempDir != null && tempDir.isDirectory()) {
            String filename = uniqueFilename(report);
            try {
                report.writeJsonFile(new File(tempDir, filename));
            } catch (IOException e) {
                log.warn("[WARN][Validation] Failed to write individual report {}: {}", filename, e.getMessage());
            }
        }
    }

    private String uniqueFilename(GaiaSceneValidationReport report) {
        String base = (report.getSourceFile() != null)
                ? report.getSourceFile().getName().replaceFirst("\\.[^.]+$", "")
                : "unknown";
        String candidate = base + "_validation.json";
        synchronized (reservedFilenames) {
            int n = 1;
            while (reservedFilenames.contains(candidate)) {
                candidate = base + "_validation_" + n++ + ".json";
            }
            reservedFilenames.add(candidate);
        }
        return candidate;
    }

    public boolean hasReports() {
        return !reports.isEmpty();
    }

    public int getReportCount() {
        return reports.size();
    }

    public GaiaSceneBatchValidationReport toBatchReport() {
        GaiaSceneBatchValidationReport batch = new GaiaSceneBatchValidationReport();
        synchronized (reports) {
            reports.forEach(batch::add);
        }
        return batch;
    }

    public void writeSummary(File outputDir) throws IOException {
        if (reports.isEmpty()) {
            log.info("[Validation] No validation issues found. Summary report skipped.");
            return;
        }
        GaiaSceneBatchValidationReport summary = toBatchReport();
        File summaryFile = new File(outputDir, "validation_report.json");
        summary.writeJsonFile(summaryFile);
        log.info("[Validation] Summary report written: {} ({} file(s) with issues)",
                summaryFile.getAbsolutePath(), summary.getInvalidFileCount());
    }

    public void reset() {
        reports.clear();
        reservedFilenames.clear();
    }
}
