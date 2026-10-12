package com.pulse.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ping_logs")
@Getter
@Setter
@NoArgsConstructor
public class PingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hangi monitöre ait olduğu
    private Long monitorId;

    // HTTP durum kodu (200, 404, 500 vb.)
    private int statusCode;

    // Yanıt süresi (milisaniye cinsinden ping süresi)
    private long responseTimeMs;

    // "UP" veya "DOWN"
    private String status;

    private LocalDateTime checkedAt = LocalDateTime.now();

    public PingLog(Long monitorId, int statusCode, long responseTimeMs, String status) {
        this.monitorId = monitorId;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.status = status;
        this.checkedAt = LocalDateTime.now();
    }
}