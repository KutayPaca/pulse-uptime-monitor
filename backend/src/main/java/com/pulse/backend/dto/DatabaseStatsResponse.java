package com.pulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseStatsResponse {
    private String databaseSize;
    private String pingLogsTableSize;
    private long totalPingLogsCount;
}