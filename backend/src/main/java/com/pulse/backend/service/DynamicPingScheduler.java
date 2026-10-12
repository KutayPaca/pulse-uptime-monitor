package com.pulse.backend.service;

import com.pulse.backend.model.Monitor;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.model.PingResult;
import com.pulse.backend.model.WebSocketPingPayload;
import com.pulse.backend.repository.MonitorRepository;
import com.pulse.backend.repository.PingLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicPingScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;
    private final PingService pingService;
    private final SimpMessagingTemplate messagingTemplate;

    // Çalışan arka plan görevlerini ID bazında hafızada tutar
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // Uygulama ilk açıldığında veritabanındaki tüm monitörler için zamanlayıcıları başlatır
    @PostConstruct
    public void initScheduler() {
        var monitors = monitorRepository.findAll();
        log.info("Sistem ayağa kalktı. {} adet monitör dinamik zamanlayıcıya ekleniyor...", monitors.size());
        for (Monitor monitor : monitors) {
            scheduleMonitor(monitor);
        }
    }

    // Bir monitörü kendi ping aralığına göre zamanlar
    public void scheduleMonitor(Monitor monitor) {
        cancelMonitor(monitor.getId()); // Varsa eski görevi iptal et

        long interval = (monitor.getPingIntervalSeconds() != null && monitor.getPingIntervalSeconds() > 0)
                ? monitor.getPingIntervalSeconds()
                : 60L;

        ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
                () -> executePing(monitor.getId()),
                Duration.ofSeconds(interval)
        );

        scheduledTasks.put(monitor.getId(), future);
        log.info("[ZAMANLANDI] Site: {} | Periyot: {} saniye", monitor.getName(), interval);
    }

    // Bir monitör silindiğinde arka plandaki ping döngüsünü durdurur
    public void cancelMonitor(Long monitorId) {
        ScheduledFuture<?> future = scheduledTasks.remove(monitorId);
        if (future != null) {
            future.cancel(false);
            log.info("[İPTAL EDİLDİ] Monitör ID: {} için zamanlayıcı durduruldu.", monitorId);
        }
    }

    // Tek bir sitenin ping kontrolünü yapıp WS yayını gönderir
    private void executePing(Long monitorId) {
        var monitorOpt = monitorRepository.findById(monitorId);
        if (monitorOpt.isEmpty()) {
            cancelMonitor(monitorId);
            return;
        }

        Monitor monitor = monitorOpt.get();
        PingResult result = pingService.checkHealth(monitor.getUrl());
        LocalDateTime now = LocalDateTime.now();

        monitor.setStatus(result.getStatus());
        monitorRepository.save(monitor);

        PingLog pingLog = new PingLog(
                monitor.getId(),
                result.getStatusCode(),
                result.getResponseTimeMs(),
                result.getStatus()
        );
        pingLogRepository.save(pingLog);

        WebSocketPingPayload payload = new WebSocketPingPayload(
                monitor.getId(),
                result.getStatus(),
                result.getStatusCode(),
                result.getResponseTimeMs(),
                now
        );
        messagingTemplate.convertAndSend("/topic/pings", payload);

        log.info("[DİNAMİK KONTROL] {} -> {} ({} ms)", monitor.getName(), result.getStatus(), result.getResponseTimeMs());
    }
}