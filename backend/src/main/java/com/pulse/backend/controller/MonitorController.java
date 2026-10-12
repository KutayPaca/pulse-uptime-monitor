package com.pulse.backend.controller;

import com.pulse.backend.dto.CreateMonitorRequest;
import com.pulse.backend.dto.DatabaseStatsResponse;
import com.pulse.backend.dto.MonitorResponse;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.service.MonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping
    public ResponseEntity<List<MonitorResponse>> getAllMonitors() {
        return ResponseEntity.ok(monitorService.getAllMonitors());
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(@Valid @RequestBody CreateMonitorRequest request) {
        MonitorResponse created = monitorService.createMonitor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<PingLog>> getLogsForMonitor(@PathVariable Long id) {
        return ResponseEntity.ok(monitorService.getLogsForMonitor(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<DatabaseStatsResponse> getDatabaseStats() {
        String dbSize = monitorService.getDatabaseSize(); // Bunu service'e ekleyeceğiz
        String tableSize = monitorService.getPingLogsTableSize(); // Bunu service'e ekleyeceğiz
        long totalLogs = monitorService.getTotalLogsCount(); // Bunu service'e ekleyeceğiz

        DatabaseStatsResponse stats = DatabaseStatsResponse.builder()
                .databaseSize(dbSize)
                .pingLogsTableSize(tableSize)
                .totalPingLogsCount(totalLogs)
                .build();

        return ResponseEntity.ok(stats);
    }
}