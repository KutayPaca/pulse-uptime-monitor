package com.pulse.backend.dto;

import com.pulse.backend.model.Monitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String url;
    private String status;
    private Long pingIntervalSeconds;
    private LocalDateTime createdAt;

    public static MonitorResponse fromEntity(Monitor monitor) {
        return MonitorResponse.builder()
                .id(monitor.getId())
                .name(monitor.getName())
                .url(monitor.getUrl())
                .status(monitor.getStatus())
                .pingIntervalSeconds(monitor.getPingIntervalSeconds())
                .createdAt(monitor.getCreatedAt())
                .build();
    }
}