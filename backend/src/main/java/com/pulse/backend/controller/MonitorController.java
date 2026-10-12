package com.pulse.backend.controller;

import com.pulse.backend.model.Monitor;
import com.pulse.backend.model.PingLog;
import com.pulse.backend.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @RestController: Bu sınıfın bir REST API uç noktası olduğunu ve
 * döneceği verilerin doğrudan JSON formatında tarayıcıya/istemciye gideceğini belirtir.
 *
 * @RequestMapping("/api/monitors"): Bu sınıftaki tüm endpoint'lerin başına "/api/monitors" ekler.
 *
 * @CrossOrigin: İleride Next.js (port 3000) ile konuşurken tarayıcının CORS hatası vermesini engeller.
 */
@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MonitorController {

    private final MonitorService monitorService;

    // GET http://localhost:8080/api/monitors -> Kayıtlı tüm siteleri JSON listesi olarak döner
    @GetMapping
    public ResponseEntity<List<Monitor>> getAllMonitors() {
        return ResponseEntity.ok(monitorService.getAllMonitors());
    }

    // POST http://localhost:8080/api/monitors -> JSON gövdesiyle gelen yeni siteyi veritabanına ekler
    @PostMapping
    public ResponseEntity<Monitor> createMonitor(@RequestBody Monitor monitor) {
        Monitor created = monitorService.createMonitor(monitor);
        return ResponseEntity.ok(created);
    }

    // DELETE http://localhost:8080/api/monitors/{id} -> Belirtilen ID'deki siteyi siler
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.noContent().build();
    }

    // Sitenin son ping geçmişi (Örn: GET /api/monitors/2/logs)
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<PingLog>> getLogsForMonitor(@PathVariable Long id) {
        return ResponseEntity.ok(monitorService.getLogsForMonitor(id));
    }
}