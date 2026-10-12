package com.pulse.backend.service;

import com.pulse.backend.dto.CreateMonitorRequest;
import com.pulse.backend.dto.MonitorResponse;
import com.pulse.backend.exception.ResourceNotFoundException;
import com.pulse.backend.model.Monitor;
import com.pulse.backend.repository.MonitorRepository;
import com.pulse.backend.repository.PingLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private PingLogRepository pingLogRepository;

    @Mock
    private DynamicPingScheduler dynamicPingScheduler;

    @InjectMocks
    private MonitorService monitorService;

    @Test
    @DisplayName("Tüm monitörler başarıyla listelenmeli ve DTO'ya dönüştürülmeli")
    void getAllMonitors_ShouldReturnListOfMonitorResponse() {
        // GIVEN (Ön Koşullar / Sahte Veri)
        Monitor monitor = new Monitor();
        monitor.setId(1L);
        monitor.setName("Test Monitor");
        monitor.setUrl("https://test.com");
        monitor.setStatus("UP");
        monitor.setPingIntervalSeconds(30L);
        monitor.setCreatedAt(LocalDateTime.now());

        when(monitorRepository.findAll()).thenReturn(List.of(monitor));

        // WHEN (Test Edilen Metot Çalıştığında)
        List<MonitorResponse> result = monitorService.getAllMonitors();

        // THEN (Beklenen Sonuçlar)
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Monitor", result.get(0).getName());
        assertEquals("https://test.com", result.get(0).getUrl());
        verify(monitorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Yeni monitör başarıyla kaydedilmeli ve dinamik zamanlayıcıya eklenmeli")
    void createMonitor_ShouldSaveAndSchedule() {
        // GIVEN
        CreateMonitorRequest request = new CreateMonitorRequest();
        request.setName("Yeni Servis");
        request.setUrl("https://yeniservis.com");
        request.setPingIntervalSeconds(15L);

        Monitor savedMonitor = new Monitor();
        savedMonitor.setId(10L);
        savedMonitor.setName(request.getName());
        savedMonitor.setUrl(request.getUrl());
        savedMonitor.setPingIntervalSeconds(15L);
        savedMonitor.setStatus("PENDING");
        savedMonitor.setCreatedAt(LocalDateTime.now());

        when(monitorRepository.save(any(Monitor.class))).thenReturn(savedMonitor);

        // WHEN
        MonitorResponse response = monitorService.createMonitor(request);

        // THEN
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(15L, response.getPingIntervalSeconds());

        verify(monitorRepository, times(1)).save(any(Monitor.class));
        verify(dynamicPingScheduler, times(1)).scheduleMonitor(savedMonitor);
    }

    @Test
    @DisplayName("Var olmayan monitör silinmek istendiğinde ResourceNotFoundException fırlatılmalı")
    void deleteMonitor_WhenNotFound_ShouldThrowException() {
        // GIVEN (Veritabanında bu ID yok diye dublör nesneye talimat veriyoruz)
        Long nonExistentId = 999L;
        when(monitorRepository.existsById(nonExistentId)).thenReturn(false);

        // WHEN & THEN (Hata fırlattığını ve veritabanı silme metodunun hiç çağrılmadığını doğrula)
        assertThrows(ResourceNotFoundException.class, () -> monitorService.deleteMonitor(nonExistentId));
        verify(monitorRepository, never()).deleteById(anyLong());
        verify(dynamicPingScheduler, never()).cancelMonitor(anyLong());
    }

    @Test
    @DisplayName("Var olan monitör silindiğinde zamanlayıcı iptal edilmeli ve veritabanından silinmeli")
    void deleteMonitor_WhenExists_ShouldCancelAndArchive() {
        // GIVEN
        Long validId = 5L;
        when(monitorRepository.existsById(validId)).thenReturn(true);

        // WHEN
        monitorService.deleteMonitor(validId);

        // THEN
        verify(dynamicPingScheduler, times(1)).cancelMonitor(validId);
        verify(monitorRepository, times(1)).deleteById(validId);
    }
}