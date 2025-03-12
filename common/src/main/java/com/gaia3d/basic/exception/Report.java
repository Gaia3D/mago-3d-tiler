package com.gaia3d.basic.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class Report {
    private LocalDateTime updateTime;
    private ReportLevel level;
    private String message;
    private String detailMessage;
    private Exception exception;
}
