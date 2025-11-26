package com.gaia3d.basic.exception;

import com.gaia3d.util.DecimalUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to report exceptions for multi thread processing.
 */
@Slf4j
@Getter
@Setter
@Deprecated
public class Reporter {
    private final String REPORT_FILE_NAME = "report";
    private final String REPORT_FILE_EXTENSION = ".log";
    private final String REPORT_FILE_ENCODING = "UTF-8";
    private List<Report> reportList = new ArrayList<>();
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;
    private String name;
    private String version;
    private String javaVersion = System.getProperty("java.version");

    private int infoCount = 0;
    private int warningCount = 0;
    private int errorCount = 0;
    private int fatalCount = 0;

    public Reporter(String name, String version) {
        if (version == null) {
            version = "Unknown";
        }
        this.name = name;
        this.version = version;
        this.startTime = LocalDateTime.now();
    }

    synchronized public void addReport(Report report) {
        reportList.add(report);
    }

    public void addReport(Exception e) {
        this.addReport(e, ReportLevel.ERROR);
    }

    public void addReport(String message, ReportLevel level) {
        Report report = new Report();
        report.setLevel(level);
        report.setMessage(message);
        report.setDetailMessage(message);
        report.setUpdateTime(LocalDateTime.now());
        report.setException(null);
        this.addReport(report);

        switch (level) {
            case WARN:
                warningCount++;
                break;
            case ERROR:
                errorCount++;
                break;
            case FATAL:
                fatalCount++;
                break;
            default:
                infoCount++;
                break;
        }
    }

    public void addReport(Exception e, ReportLevel level) {
        Report report = new Report();
        report.setLevel(level);
        report.setMessage(e.getMessage());
        report.setDetailMessage(e.toString());
        report.setUpdateTime(LocalDateTime.now());
        report.setException(e);
        this.addReport(report);

        switch (level) {
            case WARN:
                warningCount++;
                break;
            case ERROR:
                errorCount++;
                break;
            case FATAL:
                fatalCount++;
                break;
            default:
                infoCount++;
                break;
        }
    }

    public void writeReportFile(File outputPath) {
        setEndTime();
        String formatedDuration = DecimalUtils.millisecondToDisplayTime(this.duration);

        StringBuilder stringBuilder = new StringBuilder();
        addHeader(stringBuilder);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SS");
        String formattedStartTime = dateTimeFormatter.format(this.startTime);
        String formattedEndTime = dateTimeFormatter.format(this.endTime);

        stringBuilder.append("[Report Information]\n");
        stringBuilder.append("Name : ").append(name).append("\n");
        stringBuilder.append("Version : ").append(version).append("\n");
        stringBuilder.append("Java Version : ").append(javaVersion).append("\n");
        stringBuilder.append("Start Time : ").append(formattedStartTime).append("\n");
        stringBuilder.append("End Time : ").append(formattedEndTime).append("\n");
        stringBuilder.append("Duration : ").append(formatedDuration).append("\n");
        addLine(stringBuilder);
        stringBuilder.append("[Report Summary]\n");
        stringBuilder.append("Info Count : ").append(infoCount).append("\n");
        stringBuilder.append("Warning Count : ").append(warningCount).append("\n");
        stringBuilder.append("Error Count : ").append(errorCount).append("\n");
        stringBuilder.append("Fatal Count : ").append(fatalCount).append("\n");
        stringBuilder.append("Total Report Count : ").append(reportList.size()).append("\n");
        addLine(stringBuilder);
        if (!reportList.isEmpty()) {
            stringBuilder.append("[Detail Report]\n");
        } else {
            stringBuilder.append("[No Detail Report]\n");
        }
        int index = 1;
        for (Report report : reportList) {
            stringBuilder.append("{").append(report.getLevel().toString()).append(":");
            stringBuilder.append(index++).append(" / ");
            stringBuilder.append(dateTimeFormatter.format(report.getUpdateTime())).append("}\n");

            stringBuilder.append(report.getMessage()).append("\n");
            if (report.getDetailMessage() != null) {
                stringBuilder.append(report.getDetailMessage()).append("\n");
            }
            if (report.getException() != null) {
                for (StackTraceElement element : report.getException().getStackTrace()) {
                    stringBuilder.append(element.toString()).append("\n");
                }
            }
            addLine(stringBuilder);
        }
        addFooter(stringBuilder);

        File reportFile = new File(outputPath.getAbsolutePath(), REPORT_FILE_NAME + REPORT_FILE_EXTENSION);
        try {
            log.info("[Report][I/O] writing the report file: {}", reportFile.getAbsolutePath());
            FileUtils.writeStringToFile(reportFile, stringBuilder.toString(), REPORT_FILE_ENCODING);
        } catch (Exception e) {
            log.error("[ERROR][Report][I/O] failed to write the report file: {}", reportFile.getAbsolutePath());
        }
    }

    private void setEndTime() {
        this.endTime = LocalDateTime.now();

        Duration duration = Duration.between(startTime, endTime);
        this.duration = (duration.getSeconds() * 1000) + (duration.getNano() / 1000000);
    }

    private void addLine(StringBuilder stringBuilder) {
        stringBuilder.append("-----------------------------------------------\n");
    }

    private void addDoubleLine(StringBuilder stringBuilder) {
        stringBuilder.append("===============================================\n");
    }

    private void addHeader(StringBuilder stringBuilder) {
        stringBuilder.append("===============================================\n");
    }

    private void addFooter(StringBuilder stringBuilder) {
        stringBuilder.append("===============================================\n");
    }
}
