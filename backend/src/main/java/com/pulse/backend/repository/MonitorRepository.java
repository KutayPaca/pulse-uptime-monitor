package com.pulse.backend.repository;

import com.pulse.backend.model.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Repository: Spring'e bu arayüzün veritabanı işlemlerini yürüteceğini bildirir.
 *
 * JpaRepository<Monitor, Long>:
 * - 'Monitor': Hangi tablo ile çalışacağını belirtir.
 * - 'Long': Tablonun Primary Key'inin (ID) veri tipidir.
 *
 * Bu sayede SQL yazmadan veritabanına ekleme, okuma, silme yeteneği kazanırız.
 */
@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {
}