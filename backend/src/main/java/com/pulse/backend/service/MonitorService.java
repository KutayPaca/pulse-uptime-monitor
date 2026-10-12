package com.pulse.backend.service;

import com.pulse.backend.model.Monitor;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.repository.MonitorRepository;
import com.pulse.backend.repository.PingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;

    // Tüm siteleri listeler
    public List<Monitor> getAllMonitors() {
        return monitorRepository.findAll();
    }

    // Yeni site kaydeder
    public Monitor createMonitor(Monitor monitor) {
        monitor.setStatus("PENDING");
        return monitorRepository.save(monitor);
    }

    // Siteyi siler
    public void deleteMonitor(Long id) {
        monitorRepository.deleteById(id);
    }

    // Bir sitenin son 20 ping sonucunu getirir (Grafik çizimi için)
    public List<PingLog> getLogsForMonitor(Long monitorId) {
        return pingLogRepository.findTop20ByMonitorIdOrderByCheckedAtDesc(monitorId);
    }
}