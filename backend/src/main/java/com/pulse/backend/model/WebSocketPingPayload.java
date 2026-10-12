package com.pulse.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketPingPayload {
    private Long monitorId;
    private String status;
    private int statusCode;
    private long responseTimeMs;
    private LocalDateTime checkedAt;
}