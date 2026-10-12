package com.pulse.backend.service;

import com.pulse.backend.dto.CreateMonitorRequest;
import com.pulse.backend.dto.MonitorResponse;
import com.pulse.backend.exception.ResourceNotFoundException;
import com.pulse.backend.model.Monitor;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.repository.MonitorRepository;
import com.pulse.backend.repository.PingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;
    private final DynamicPingScheduler dynamicPingScheduler; // Dinamik zamanlayıcı eklendi

    @Cacheable(value = "monitors")
    public List<MonitorResponse> getAllMonitors() {
        log.info("[CACHE MISS] Monitörler veritabanından çekiliyor...");
        return monitorRepository.findAll()
                .stream()
                .map(MonitorResponse::fromEntity)
                .toList();
    }

    @CacheEvict(value = "monitors", allEntries = true)
    public MonitorResponse createMonitor(CreateMonitorRequest request) {
        Monitor monitor = new Monitor();
        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        if (request.getPingIntervalSeconds() != null) {
            monitor.setPingIntervalSeconds(request.getPingIntervalSeconds());
        }
        monitor.setStatus("PENDING");

        Monitor saved = monitorRepository.save(monitor);
        log.info("[CACHE EVICT] Yeni monitör eklendi, önbellek temizlendi: {}", saved.getName());

        // Dinamik zamanlayıcıya kaydet
        dynamicPingScheduler.scheduleMonitor(saved);

        return MonitorResponse.fromEntity(saved);
    }

    @CacheEvict(value = "monitors", allEntries = true)
    public void deleteMonitor(Long id) {
        if (!monitorRepository.existsById(id)) {
            throw new ResourceNotFoundException("ID'si " + id + " olan monitör bulunamadı.");
        }

        // Önce arka plandaki ping görevini durdur
        dynamicPingScheduler.cancelMonitor(id);

        log.info("[CACHE EVICT] Monitör silindi (ID: {}), önbellek temizlendi.", id);
        monitorRepository.deleteById(id);
    }

    public List<PingLog> getLogsForMonitor(Long monitorId) {
        return pingLogRepository.findTop20ByMonitorIdOrderByCheckedAtDesc(monitorId);
    }

    public String getDatabaseSize() {
        return pingLogRepository.getDatabaseSize();
    }

    public String getPingLogsTableSize() {
        return pingLogRepository.getPingLogsTableSize();
    }

    public long getTotalLogsCount() {
        return pingLogRepository.count();
    }
}