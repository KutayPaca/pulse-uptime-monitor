package com.pulse.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PingResult {
    private final int statusCode;
    private final long responseTimeMs;
    private final String status;
}