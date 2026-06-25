package com.gaia3d.converter.assimp.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("release")
class GaiaSceneValidationReportIntegrationTest {
    private static final String SAMPLE_DIR = "sample-3ds";

    private GaiaSceneValidator validator;
    private AssimpConverter converter;
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = new GaiaSceneValidator();
        converter = new AssimpConverter(AssimpConverterOptions.builder().build());
    }

    // --- 개별 파일 테스트 ---

    @Test
    void bd001_validFilePassesWithNoIssues() {
        File file = sampleFile("a_bd001.3ds");
        List<GaiaScene> scenes = converter.load(file);

        GaiaSceneValidationReport report = validator.validate(file, scenes);

        assertTrue(report.isSourceFileExists());
        assertFalse(report.hasIssues(), report.toDetailString());
        assertTrue(report.getVertexCount() > 0);
        assertTrue(report.getFaceCount() > 0);
        assertNotNull(report.getBoundingBox());
        assertTrue(report.getBoundingBox().isInit());
    }

    @Test
    void bd003_validFilePassesWithNoIssues() {
        File file = sampleFile("a_bd003.3ds");
        List<GaiaScene> scenes = converter.load(file);

        GaiaSceneValidationReport report = validator.validate(file, scenes);

        assertTrue(report.isSourceFileExists());
        assertFalse(report.hasIssues(), report.toDetailString());
        assertTrue(report.getVertexCount() > 0);
        assertTrue(report.getFaceCount() > 0);
        assertNotNull(report.getBoundingBox());
        assertTrue(report.getBoundingBox().isInit());
    }

    @Test
    void bd002_missingFileReportsSourceNotFound() {
        File file = new File(sampleDir(), "a_bd002.3ds");
        assertFalse(file.exists(), "a_bd002.3ds should not exist for this test");

        GaiaSceneValidationReport report = validator.validate(file, Collections.emptyList());

        assertFalse(report.isSourceFileExists());
        assertTrue(report.hasIssues());
        assertTrue(report.getDetails().stream().anyMatch(d -> d.contains("does not exist")));
    }

    @Test
    void bd004_missingTextureIsDetectedByValidator() {
        // AssimpConverter는 더 이상 텍스처를 silent drop하지 않으므로 validator가 누락을 감지
        File file = sampleFile("a_bd004.3ds");
        List<GaiaScene> scenes = converter.load(file);

        GaiaSceneValidationReport report = validator.validate(file, scenes);

        assertTrue(report.isSourceFileExists());
        assertTrue(report.hasIssues(), "a_bd004.3ds의 누락 텍스처가 감지되어야 합니다");
        assertTrue(report.getMissingExternalResourceCount() > 0, report.toDetailString());
    }

    @Test
    void bd004_repairResolvesOrDropsMissingTexture() {
        // Repair 후에는 이슈가 없어야 함 (찾거나 드롭)
        File file = sampleFile("a_bd004.3ds");
        List<GaiaScene> scenes = converter.load(file);
        GaiaSceneValidationReport report = validator.validate(file, scenes);
        assertTrue(report.hasIssues(), "repair 전 이슈가 있어야 합니다");

        GaiaSceneRepair repair = new GaiaSceneRepair();
        repair.repair(report, scenes);

        GaiaSceneValidationReport reportAfterRepair = validator.validate(file, scenes);
        assertFalse(reportAfterRepair.hasIssues(),
                "repair 후 이슈가 없어야 합니다: " + reportAfterRepair.toDetailString());
        assertEquals(0, reportAfterRepair.getMissingExternalResourceCount());
    }

    // --- JSON 직렬화 테스트 ---

    @Test
    void bd001_toJsonContainsExpectedFields() throws IOException {
        File file = sampleFile("a_bd001.3ds");
        GaiaSceneValidationReport report = validator.validate(file, converter.load(file));

        JsonNode root = mapper.readTree(report.toJson());

        assertTrue(root.get("source").asText().endsWith("a_bd001.3ds"));
        assertFalse(root.get("hasIssues").asBoolean());
        assertTrue(root.get("counts").get("vertices").asInt() > 0);
        assertFalse(root.get("boundingBox").isNull());
    }

    @Test
    void bd002_toJsonMarksSourceAsNotExisting() throws IOException {
        File file = new File(sampleDir(), "a_bd002.3ds");
        GaiaSceneValidationReport report = validator.validate(file, Collections.emptyList());

        JsonNode root = mapper.readTree(report.toJson());

        assertFalse(root.get("sourceFileExists").asBoolean());
        assertTrue(root.get("hasIssues").asBoolean());
        assertTrue(root.get("issueCount").asInt() > 0);
        assertTrue(root.get("details").size() > 0);
    }

    @Test
    void bd004_toJsonReportsMissingTexture() throws IOException {
        // AssimpConverter가 더 이상 silent drop하지 않으므로 JSON 리포트에 누락 텍스처가 기록됨
        File file = sampleFile("a_bd004.3ds");
        GaiaSceneValidationReport report = validator.validate(file, converter.load(file));

        JsonNode root = mapper.readTree(report.toJson());

        assertTrue(root.get("hasIssues").asBoolean());
        assertTrue(root.get("invalidCounts").get("missingExternalResources").asInt() > 0);
    }

    // --- 배치 리포트 테스트 ---

    @Test
    void batchReport_aggregatesAllFourFiles() throws IOException {
        GaiaSceneBatchValidationReport batch = new GaiaSceneBatchValidationReport();
        batch.add(loadAndValidate("a_bd001.3ds"));
        batch.add(loadAndValidate("a_bd002.3ds"));
        batch.add(loadAndValidate("a_bd003.3ds"));
        batch.add(loadAndValidate("a_bd004.3ds"));

        assertEquals(4, batch.getTotalFileCount());
        assertEquals(2, batch.getValidFileCount());   // bd001, bd003
        assertEquals(2, batch.getInvalidFileCount()); // bd002(파일 없음), bd004(텍스처 누락)
        assertTrue(batch.getTotalIssueCount() > 0);
        assertTrue(batch.hasAnyIssues());
    }

    @Test
    void batchReport_toJsonStructureIsCorrect() throws IOException {
        GaiaSceneBatchValidationReport batch = new GaiaSceneBatchValidationReport();
        batch.add(loadAndValidate("a_bd001.3ds"));
        batch.add(loadAndValidate("a_bd002.3ds"));
        batch.add(loadAndValidate("a_bd003.3ds"));
        batch.add(loadAndValidate("a_bd004.3ds"));

        String json = batch.toJson();
        JsonNode root = mapper.readTree(json);

        JsonNode summary = root.get("summary");
        assertEquals(4, summary.get("totalFiles").asInt());
        assertEquals(2, summary.get("validFiles").asInt());   // bd001, bd003
        assertEquals(2, summary.get("invalidFiles").asInt()); // bd002, bd004
        assertTrue(summary.get("totalIssues").asInt() > 0);

        JsonNode reports = root.get("reports");
        assertTrue(reports.isArray());
        assertEquals(4, reports.size());

        // 순서대로 검증
        assertFalse(reports.get(0).get("hasIssues").asBoolean()); // bd001 정상
        assertTrue(reports.get(1).get("hasIssues").asBoolean());  // bd002 파일 없음
        assertFalse(reports.get(2).get("hasIssues").asBoolean()); // bd003 정상
        assertTrue(reports.get(3).get("hasIssues").asBoolean());  // bd004 텍스처 누락
    }

    @Test
    void batchReport_writeJsonFileCreatesReadableOutput() throws IOException {
        GaiaSceneBatchValidationReport batch = new GaiaSceneBatchValidationReport();
        batch.add(loadAndValidate("a_bd001.3ds"));
        batch.add(loadAndValidate("a_bd002.3ds"));
        batch.add(loadAndValidate("a_bd003.3ds"));
        batch.add(loadAndValidate("a_bd004.3ds"));

        File outputFile = tempDir.resolve("batch_validation.json").toFile();
        batch.writeJsonFile(outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        JsonNode root = mapper.readTree(outputFile);
        assertEquals(4, root.get("summary").get("totalFiles").asInt());
        assertEquals(4, root.get("reports").size());
    }

    // --- 헬퍼 ---

    private GaiaSceneValidationReport loadAndValidate(String filename) {
        File file = new File(sampleDir(), filename);
        try {
            List<GaiaScene> scenes = converter.load(file);
            return validator.validate(file, scenes);
        } catch (RuntimeException e) {
            return validator.validate(file, Collections.emptyList());
        }
    }

    private File sampleDir() {
        URL resource = getClass().getClassLoader().getResource(SAMPLE_DIR);
        assertNotNull(resource, "Sample directory not found on classpath: " + SAMPLE_DIR);
        return new File(resource.getFile());
    }

    private File sampleFile(String filename) {
        File file = new File(sampleDir(), filename);
        assertTrue(file.exists(), "Sample file not found: " + filename);
        return file;
    }
}
