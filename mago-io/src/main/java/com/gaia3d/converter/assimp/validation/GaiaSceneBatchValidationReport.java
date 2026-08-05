package com.gaia3d.converter.assimp.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GaiaSceneBatchValidationReport {
    private final List<GaiaSceneValidationReport> reports = new ArrayList<>();

    public void add(GaiaSceneValidationReport report) {
        reports.add(report);
    }

    public List<GaiaSceneValidationReport> getReports() {
        return reports;
    }

    public int getTotalFileCount() {
        return reports.size();
    }

    public long getValidFileCount() {
        return reports.stream().filter(r -> !r.hasIssues()).count();
    }

    public long getInvalidFileCount() {
        return reports.stream().filter(GaiaSceneValidationReport::hasIssues).count();
    }

    public int getTotalIssueCount() {
        return reports.stream().mapToInt(GaiaSceneValidationReport::getIssueCount).sum();
    }

    public boolean hasAnyIssues() {
        return reports.stream().anyMatch(GaiaSceneValidationReport::hasIssues);
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        ObjectNode summary = root.putObject("summary");
        summary.put("totalFiles", getTotalFileCount());
        summary.put("validFiles", getValidFileCount());
        summary.put("invalidFiles", getInvalidFileCount());
        summary.put("totalIssues", getTotalIssueCount());

        ArrayNode reportsNode = root.putArray("reports");
        for (GaiaSceneValidationReport report : reports) {
            try {
                reportsNode.add(mapper.readTree(report.toJson()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize validation report for: " + report.sourceName(), e);
            }
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize batch validation report to JSON", e);
        }
    }

    public void writeJsonFile(File outputFile) throws IOException {
        Files.writeString(outputFile.toPath(), toJson(), StandardCharsets.UTF_8);
    }
}
