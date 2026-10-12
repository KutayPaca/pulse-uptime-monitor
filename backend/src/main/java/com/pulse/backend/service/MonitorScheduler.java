package com.pulse.backend.service;

import com.pulse.backend.model.Monitor;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.model.PingResult;
import com.pulse.backend.model.WebSocketPingPayload;
import com.pulse.backend.repository.MonitorRepository;
import com.pulse.backend.repository.PingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;
    private final PingService pingService;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket göndericisi eklendi

    @Scheduled(fixedRate = 15000)
    public void runHealthChecks() {
        List<Monitor> monitors = monitorRepository.findAll();
        if (monitors.isEmpty()) {
            return;
        }

        for (Monitor monitor : monitors) {
            PingResult result = pingService.checkHealth(monitor.getUrl());
            LocalDateTime now = LocalDateTime.now();

            // 1. Monitörün son durumunu güncelle
            monitor.setStatus(result.getStatus());
            monitorRepository.save(monitor);

            // 2. Veritabanına geçmiş log kaydı ekle
            PingLog pingLog = new PingLog(
                    monitor.getId(),
                    result.getStatusCode(),
                    result.getResponseTimeMs(),
                    result.getStatus()
            );
            pingLogRepository.save(pingLog);

            // 3. WebSocket üzerinden frontend'e anlık yayın yap
            WebSocketPingPayload payload = new WebSocketPingPayload(
                    monitor.getId(),
                    result.getStatus(),
                    result.getStatusCode(),
                    result.getResponseTimeMs(),
                    now
            );
            messagingTemplate.convertAndSend("/topic/pings", payload);

            log.info("[KONTROL] Site: {} -> Durum: {} (HTTP: {}, Sure: {} ms) | WS yayını yapıldı.",
                    monitor.getName(), result.getStatus(), result.getStatusCode(), result.getResponseTimeMs());
        }
    }
}