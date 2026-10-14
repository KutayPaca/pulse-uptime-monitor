package com.pulse.backend.repository;

import com.pulse.backend.model.PingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PingLogRepository extends JpaRepository<PingLog, Long> {

    List<PingLog> findTop20ByMonitorIdOrderByCheckedAtDesc(Long monitorId);

    @Query(value = "SELECT pg_size_pretty(pg_database_size(current_database()))", nativeQuery = true)
    String getDatabaseSize();

    @Query(value = "SELECT pg_size_pretty(pg_total_relation_size('ping_logs'))", nativeQuery = true)
    String getPingLogsTableSize();

    @Query(value = "SELECT COALESCE(pg_total_relation_size(to_regclass('public.ping_logs')), 0)", nativeQuery = true)
    long getPingLogsTableSizeBytes();
}