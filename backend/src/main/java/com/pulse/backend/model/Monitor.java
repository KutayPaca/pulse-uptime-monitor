package com.pulse.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * @Entity: Spring Boot ve Hibernate'e bu sınıfın sıradan bir nesne olmadığını,
 * veritabanında bir tabloya karşılık geleceğini bildirir (ORM mantığı).
 *
 * @Table(name = "monitors"): PostgreSQL içinde açılacak tablonun adını "monitors" olarak belirler.
 *
 * @Getter, @Setter: Lombok kütüphanesidir. Değişkenlerin get/set metodlarını
 * arka planda gizlice üretir, dosyamızı onlarca satır gereksiz koddan kurtarır.
 *
 * @NoArgsConstructor: Hibernate'in veritabanından veri çekerken boş bir nesne
 * oluşturabilmesi için zorunlu olan parametresiz varsayılan yapıcı metodu (constructor) ekler.
 */
@Entity
@Table(name = "monitors")
@Getter
@Setter
@NoArgsConstructor
public class Monitor {

    /**
     * @Id: Bu alanın tablonun birincil anahtarı (Primary Key) olduğunu söyler.
     * Her sitenin veritabanında kendine ait eşsiz bir numarası olur.
     *
     * @GeneratedValue(strategy = GenerationType.IDENTITY): ID numarasını elle girmeyiz;
     * PostgreSQL yeni kayıt geldikçe sırayla (1, 2, 3...) kendisi otomatik artırır.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Column(nullable = false): Bu alanın boş bırakılamayacağını (NOT NULL) belirtir.
     * Kullanıcı panelden bir site eklerken isim girmek zorundadır.
     * Örnek: "Google Test", "Ana Sayfa Servisi"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Takip edilecek adres. Yine boş bırakılamaz olarak ayarlanmıştır.
     * Örnek: "https://www.google.com" veya "https://api.github.com"
     */
    @Column(nullable = false)
    private String url;

    /**
     * Sitenin son durumunu tutar:
     * "UP" (çalışıyor), "DOWN" (çöktü) veya henüz kontrol edilmediyse "PENDING" (beklemede).
     */
    private String status = "PENDING";

    /**
     * Bu sitenin kaç saniyede bir pinglenip kontrol edileceğini tutar.
     * Varsayılan değer olarak 60 saniye atanmıştır.
     */
    private Long pingIntervalSeconds = 60L;

    /**
     * Bu sitenin sisteme ilk kaydedildiği tarih ve saat bilgisidir.
     * Kayıt oluşturulduğu anda sistemin güncel saatini otomatik alır.
     */
    private LocalDateTime createdAt = LocalDateTime.now();
}