package com.gaia3d.basic.exception;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Getter
@AllArgsConstructor
public enum ReportLevel {
    INFO("info"),
    WARN("warn"),
    ERROR("error"),
    FATAL("fatal");

    private final String level;
}
