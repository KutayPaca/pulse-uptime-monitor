# Pulse Uptime — Gerçek Zamanlı Servis ve API İzleme Paneli

**Pulse Uptime**, web sitelerinin ve API uç noktalarının (endpoints) ayakta olup olmadığını (`UP` / `DOWN`) ve yanıt sürelerini (latency) gerçek zamanlı olarak takip etmek için geliştirilen tam kapsamlı (full-stack) bir izleme uygulamasıdır.

Bu projenin temel amacı; sadece CRUD işlemleri yapan standart bir uygulama yerine, **arka planda asenkron görevlerin çalıştığı**, **WebSocket ile canlı veri akışının sağlandığı**, **önbellekleme** ve **konteynerizasyon (Docker)** gibi sektörde sıkça kullanılan yapıların bir arada bulunduğu uçtan uca çalışan bir sistem kurgulamaktır.

## Neler Yapabiliyor? 

* **Esnek Periyotlarla İzleme:** İzlenecek her servis veya API için birbirinden bağımsız kontrol sıklığı (10 saniye, 30 saniye, 1 dakika, 5 dakika) belirlenebilir.

* **Canlı Veri Akışı (Sıfır Sayfa Yenileme):** Arka planda bir servise ping atıldığı anda sonuçlar **WebSocket (STOMP)** üzerinden doğrudan açık olan tarayıcıya iletilir; durum ışıkları, loglar ve grafikler anında güncellenir.

* **Gecikme Süresi (Latency) Grafiği:** Seçilen servisin son 20 kontrolüne ait yanıt süreleri (ms), **Recharts** ile anlık olarak çizgi/alan grafiğine dönüştürülür.

* **Uptime ve Performans Metrikleri:** Her servis için toplam çalışma yüzdesi (`% Uptime`) ve ortalama yanıt süresi (`ms`) otomatik hesaplanır.

* **Veritabanı Boyut Takibi:** PostgreSQL'in sistem fonksiyonları kullanılarak biriken ping kayıtlarının sayısı ve tabloların diskte kapladığı gerçek boyut (`kB` / `MB`) doğrudan arayüzde gösterilir.

* **Sürükle & Bırak (Drag & Drop) Sıralama:** Arayüzdeki servis listesi sürükle-bırak yöntemiyle yeniden sıralanabilir ve kullanıcının sıralama tercihi `localStorage` üzerinde saklanır.

## Sistem Nasıl Çalışıyor ve Neden Bu Teknolojiler Kullanıldı?

Projeyi **Backend (Spring Boot)** ve **Frontend (Next.js)** olarak iki ana katman halinde tasarlandı. Her bir teknolojiyi projeye dahil etme nedeni ve çalışma mantığı şu şekildedir:

### 1. Dinamik Görev Zamanlama (`ThreadPoolTaskScheduler`)

Spring Boot'ta periyodik işlemler için genellikle `@Scheduled(fixedRate = ...)` anotasyonu kullanılır. Ancak bu yaklaşım sabit bir süreye bağlıdır ve tüm siteleri aynı anda kontrol etmeye zorlar.
Bunun yerine projede `ThreadPoolTaskScheduler` ve `ConcurrentHashMap<Long, ScheduledFuture<?>>` kullanarak **Dinamik Bir Zamanlayıcı (`DynamicPingScheduler`)** kullanıldı:

* Her monitör kendi belirlenen saniye periyoduna göre bağımsız bir görev olarak zamanlanır.

* Kullanıcı arayüzden yeni bir site eklediğinde, uygulama yeniden başlatılmadan o site için arka plan görevi anında başlatılır.

* Bir site silindiğinde ise `ScheduledFuture.cancel(false)` çağrılarak arka planda gereksiz yere ping atmaya devam etmesi engellenir.

### 2. Java 21 ve Sanal İş Parçacıkları (Virtual Threads)

Uygulamanın ana işi dış dünyadaki sunuculara HTTP isteği atıp yanıt beklemektir (I/O-bound işlem). İzlenen site sayısı arttığında klasik işletim sistemi thread'lerinin yanıt beklerken bloklanması performans darboğazı yaratır. `application.properties` içerisinde `spring.threads.virtual.enabled=true` ayarını aktif ederek **Java 21 Virtual Threads** özelliğini kullanıldı. Bu sayede çok düşük bellek tüketimiyle eşzamanlı sağlık kontrolleri bloklanmadan gerçekleştirilebildi.

### 3. Hafif ve Hızlı Sağlık Kontrolü (`Java HttpClient`)

`PingService` katmanında hedef URL'lere `GET` isteği atılırken 5 saniyelik zaman aşımı (timeout) uygulanır. Burada önemli bir optimizasyon olarak `HttpResponse.BodyHandlers.discarding()` kullanıldı. Çünkü amacımız karşı sitenin HTML/JSON içeriğini okumak değil, sadece HTTP durum kodunu (`200-399` arası `UP`, diğer durumlar `DOWN`) ve yanıt süresini (`ms`) ölçmektir. Yanıt gövdesini belleğe almamak RAM kullanımını ciddi oranda korur.

### 4. Gerçek Zamanlı Bildirimler (`WebSocket & STOMP`)

Frontend'in "yeni ping sonucu var mı?" diye sürekli backend'e HTTP isteği atması (polling) hem sunucuyu yorar hem de gecikmeye sebep olur. Bunun yerine **Spring WebSocket**  kurgulandı. Backend bir sitenin kontrolünü bitirdiği an `/topic/pings` kanalına sonucu yayınlar (`SimpMessagingTemplate`). Frontend tarafında yazılan `useMonitorWebSocket` custom hook'u bu kanalı dinler ve gelen veriyi doğrudan React state'ine yansıtır.

### 5. Önbellekleme (`Redis & Spring Cache`)

Kullanıcı sayfayı her açtığında veya yenilediğinde monitör listesini veritabanından tekrar tekrar çekmek yerine, `MonitorService` üzerinde `@Cacheable(value = "monitors")` kullanarak listeyi **Redis** üzerinde önbelleğe (60 saniye TTL ile) alındı. Yeni bir monitör eklendiğinde veya silindiğinde `@CacheEvict` anotasyonu devreye girerek eski önbelleği temizler; böylece hem hız kazanılır hem de bayat veri (stale data) sorunu yaşanmaz.

### 6. Veritabanı ve Birim Testleri (`PostgreSQL & JUnit 5 / Mockito`)

* **PostgreSQL:** Monitörlerin ana bilgileri `monitors` tablosunda, geçmiş ping kayıtları ise `ping_logs` tablosunda tutulur. Ayrıca native SQL sorguları (`pg_size_pretty(pg_total_relation_size('ping_logs'))`) ile tablo boyutu doğrudan veritabanı motorundan sorgulanır.

* **JUnit 5 & Mockito:** Servis katmanındaki iş mantığını doğrulamak için `MonitorServiceTest` sınıfında *Given-When-Then* yaklaşımıyla birim testi yazıldı. Veritabanı ve zamanlayıcı bağımlılıklarını mock'layarak kayıt oluşturma, listeleme ve hatalı silme (`ResourceNotFoundException`) senaryolar izole şekilde test edildi.

## Teknoloji Yığını (Tech Stack)

| Katman | Kullanılan Teknolojiler | 
| ----- | ----- | 
| **Backend** | Java 21, Spring Boot, Spring Data JPA (Hibernate), Spring WebSocket (STOMP), Spring Cache, Bean Validation, Lombok | 
| **Frontend** | Next.js (App Router), React 19, TypeScript, Tailwind CSS v4, Recharts, SockJS & StompJS, Lucide Icons | 
| **Veritabanı & Cache** | PostgreSQL 16, Redis 7 | 
| **Test & DevOps** | JUnit 5, Mockito, Docker, Docker Compose  | 

## Proje Dizin Yapısı

```
pulse-uptime/
├── backend/
│   ├── src/main/java/com/pulse/backend/
│   │   ├── config/          # WebSocket (STOMP) ve ThreadPoolTaskScheduler ayarları
│   │   ├── controller/      # REST API uç noktaları (MonitorController)
│   │   ├── dto/             # İstek ve yanıt nesneleri (Request / Response DTOs)
│   │   ├── exception/       # Özel hata sınıfları (ResourceNotFoundException)
│   │   ├── model/           # Veritabanı Entity'leri (Monitor, PingLog) ve modeller
│   │   ├── repository/      # JPA ve Native SQL sorguları
│   │   └── service/         # İş mantığı, PingService ve DynamicPingScheduler
│   ├── src/test/            # JUnit 5 & Mockito birim testleri
│   └── Dockerfile           # Maven + Eclipse Temurin 21 Multi-stage build
├── frontend/
│   ├── src/
│   │   ├── app/             # Next.js sayfa yapısı ve ana Dashboard
│   │   ├── components/      # Grafik (LatencyChart) ve Modal (AddMonitorModal) bileşenleri
│   │   ├── hooks/           # WebSocket bağlantı yönetimi (useMonitorWebSocket)
│   │   ├── lib/             # Backend API istemcisi (api.ts)
│   │   └── types/           # TypeScript veri tipleri ve arayüzler
│   └── Dockerfile           # Node 20 Alpine Standalone Multi-stage build
├── docker-compose.yml       # PostgreSQL, Redis, Backend ve Frontend orkestrasyonu
└── .env.example             # Örnek ortam değişkenleri dosyası

```

## Kurulum ve Çalıştırma

Projeyi kendi bilgisayarınızda çalıştırmak için yerel ortamınıza Java, Node.js, PostgreSQL veya Redis kurmanıza gerek yoktur. **Docker** ve **Docker Compose** yüklü olması yeterlidir.

### 1. Projeyi Klonlayın

```
git clone https://github.com/kullanici-adiniz/pulse-uptime.git
cd pulse-uptime

```

### 2. Ortam Değişkenlerini (`.env`) Oluşturun

Ana dizinde bulunan `.env.example` dosyasını `.env` adıyla kopyalayın:

```
cp .env.example .env

```

*(İsterseniz `.env` içerisindeki `DB_PASSWORD` alanını kendi belirlediğiniz bir şifreyle değiştirebilirsiniz.)*

### 3. Docker Compose ile Başlatın

```
docker compose up --build -d

```

> **Not:** `docker-compose.yml` içerisinde `healthcheck` tanımları bulunduğu için önce PostgreSQL ve Redis ayağa kalkıp hazır hale gelir, ardından Backend ve Frontend servisleri başlatılır.

### 4. Uygulamaya Erişin

* **Frontend (Kontrol Paneli):** `http://localhost:3000`

* **Backend REST API:** `http://localhost:8080/api/monitors`

* **WebSocket Endpoint:** `http://localhost:8080/ws`

## REST API Uç Noktaları (Endpoints)

| Metot | Uç Nokta (Endpoint) | Açıklama | 
| ----- | ----- | ----- | 
| `GET` | `/api/monitors` | Kayıtlı tüm monitörleri listeler (Redis önbellek destekli). | 
| `POST` | `/api/monitors` | Yeni bir monitör ekler ve dinamik zamanlayıcıyı başlatır. | 
| `DELETE` | `/api/monitors/{id}` | Monitörü siler, zamanlayıcı görevini iptal eder ve önbelleği temizler. | 
| `GET` | `/api/monitors/{id}/logs` | İlgili monitöre ait son 20 ping kaydını getirir. | 
| `GET` | `/api/monitors/{id}/stats` | İlgili monitörün Uptime yüzdesini ve ortalama gecikme süresini döner. | 
| `GET` | `/api/monitors/stats` | Veritabanı boyutunu, tablo boyutunu ve toplam ping sayısını döner. | 

## Testleri Çalıştırma

Backend birim testlerini yerel ortamınızda çalıştırmak isterseniz `backend` klasörü içerisindeki Maven Wrapper'ı kullanabilirsiniz:

```
cd backend
./mvnw test
```