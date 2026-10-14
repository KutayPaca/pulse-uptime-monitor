package com.pulse.backend.service;

import com.pulse.backend.dto.CreateMonitorRequest;
import com.pulse.backend.dto.DatabaseStatsResponse;
import com.pulse.backend.dto.MonitorResponse;
import com.pulse.backend.dto.MonitorStatsResponse;
import com.pulse.backend.exception.ResourceNotFoundException;
import com.pulse.backend.model.Monitor;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.repository.MonitorRepository;
import com.pulse.backend.repository.PingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;
    private final DynamicPingScheduler dynamicPingScheduler;

    @Value("${QUBIX_DB_HOST:host.docker.internal}")
    private String qubixDbHost;

    @Value("${QUBIX_DB_PORT:5432}")
    private String qubixDbPort;

    @Value("${QUBIX_DB_USER:postgres}")
    private String qubixDbUser;

    @Value("${QUBIX_DB_PASS:password}")
    private String qubixDbPass;

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

        dynamicPingScheduler.scheduleMonitor(saved);
        return MonitorResponse.fromEntity(saved);
    }

    @CacheEvict(value = "monitors", allEntries = true)
    public void deleteMonitor(Long id) {
        if (!monitorRepository.existsById(id)) {
            throw new ResourceNotFoundException("ID'si " + id + " olan monitör bulunamadı.");
        }
        dynamicPingScheduler.cancelMonitor(id);
        log.info("[CACHE EVICT] Monitör silindi (ID: {}), önbellek temizlendi.", id);
        monitorRepository.deleteById(id);
    }

    public List<PingLog> getLogsForMonitor(Long monitorId) {
        return pingLogRepository.findTop20ByMonitorIdOrderByCheckedAtDesc(monitorId);
    }

    public DatabaseStatsResponse getCombinedDatabaseStats() {
        long pulseBytes = pingLogRepository.getPingLogsTableSizeBytes();
        long pulseCount = pingLogRepository.count();

        long[] orderStats = queryTableMetrics("qubix_order_db", "proposals");
        long[] notificationStats = queryTableMetrics("qubix_notification_db", "notification_logs");
        long[] auditStats = queryTableMetrics("qubix_audit_db", "audit_trail");

        long totalQubixBytes = orderStats[0] + notificationStats[0] + auditStats[0];
        long totalQubixRows = orderStats[1] + notificationStats[1] + auditStats[1];

        long combinedBytes = pulseBytes + totalQubixBytes;
        long combinedCount = pulseCount + totalQubixRows;

        return DatabaseStatsResponse.builder()
                .databaseSize(formatBytes(combinedBytes))
                .pingLogsTableSize(formatBytes(combinedBytes))
                .totalPingLogsCount(combinedCount)
                .orderTableSize(formatBytes(orderStats[0]))
                .notificationTableSize(formatBytes(notificationStats[0]))
                .auditTableSize(formatBytes(auditStats[0]))
                .totalQubixEventsCount(totalQubixRows)
                .build();
    }

    private long[] queryTableMetrics(String dbName, String tableName) {
        String url = String.format("jdbc:postgresql://%s:%s/%s", qubixDbHost, qubixDbPort, dbName);
        String sql = "SELECT COALESCE(pg_total_relation_size(to_regclass('public.' || ?)), 0), " +
                "(SELECT COUNT(*) FROM " + tableName + ")";
        try (Connection conn = DriverManager.getConnection(url, qubixDbUser, qubixDbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new long[]{rs.getLong(1), rs.getLong(2)};
                }
            }
        } catch (Exception e) {
            log.debug("[QUBIX-DB-STATS] {}.{} sorgulanamadı: {}", dbName, tableName, e.getMessage());
        }
        return new long[]{0L, 0L};
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.0f kB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }

    public MonitorStatsResponse getStatsForSingleMonitor(Long monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitör bulunamadı"));

        if (monitor.getTotalChecks() == 0) {
            return new MonitorStatsResponse("0.00", 0);
        }

        double uptime = ((double) monitor.getTotalUpChecks() / monitor.getTotalChecks()) * 100;
        String formattedUptime = String.format(Locale.US, "%.2f", uptime);

        long avgLatency = monitor.getTotalUpChecks() > 0
                ? monitor.getTotalLatencySum() / monitor.getTotalUpChecks()
                : 0;

        return new MonitorStatsResponse(formattedUptime, avgLatency);
    }
}