package com.pulse.backend.repository;

import com.pulse.backend.model.PingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PingLogRepository extends JpaRepository<PingLog, Long> {
    // Belirli bir sitenin son kontrollerini çekmek için
    List<PingLog> findTop20ByMonitorIdOrderByCheckedAtDesc(Long monitorId);
}