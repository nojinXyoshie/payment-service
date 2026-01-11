# Payment Service - Microservice Pembayaran

## Gambaran Arsitektur

Ini adalah microservice Spring Boot yang menangani pemrosesan pembayaran dengan jaminan idempotensi dan mekanisme retry. Sistem dirancang untuk menangani callback dari payment gateway, mencegah double charge, dan menangani network timeout dengan efektif.

### Komponen Utama
- **Payment Service**: Memproses pembayaran dan menangani callback payment gateway
- **Payment Gateway Integration**: Berkomunikasi dengan external payment gateway untuk pemrosesan pembayaran
- **Sistem Notifikasi**: Mengirim notifikasi sukses/gagal pembayaran
- **H2 Database**: Database in-memory untuk pembayaran dan notifikasi

## Alur Pembayaran

1. **Inisiasi Pembayaran**
   - Client mengirim request pembayaran ke Payment Service
   - Sistem membuat record Payment dengan status `INITIATED`
   - Payment Gateway dipanggil untuk memproses pembayaran
   - Mengembalikan paymentId ke client

2. **Pemrosesan Pembayaran**
   - Payment Gateway memproses pembayaran dan mengirim callback ke `POST /api/payments/callback`
   - Sistem memvalidasi callback dan update status pembayaran
   - Mengirim notifikasi ke customer tentang status pembayaran

3. **Notifikasi**
   - Ketika pembayaran berhasil, notifikasi dikirim ke customer
   - Sistem memastikan notifikasi hanya dikirim sekali (idempotent)

## Implementasi Idempotensi

### Idempotensi Callback Pembayaran
- Setiap pembayaran memiliki `paymentId` yang unik
- Sistem memeriksa status pembayaran saat ini sebelum memproses
- Jika status sama dengan request yang masuk, update diabaikan
- Log: `"Idempotent update ignored for payment {} with status {}"`

### Idempotensi Notifikasi
- Sistem memeriksa apakah notifikasi sudah dikirim untuk pembayaran
- `notificationRepository.existsByPaymentIdAndStatus(paymentId, NotificationStatus.SENT)`
- Jika sudah dikirim, pembuatan notifikasi baru dilewati

## Penanganan Callback Ganda

1. **Pemeriksaan Status**: Sistem membandingkan status yang masuk dengan status pembayaran saat ini
2. **Return Awal**: Jika status tidak berubah, callback diabaikan
3. **Perlindungan Konflik**: Jika pembayaran sudah `SUCCESS`, update yang konflik ditolak
4. **Logging**: Semua percobaan duplikat dicatat untuk audit

```java
if (status == currentStatus) {
    log.info("Idempotent update ignored for payment {} with status {}", payment.getPaymentId(), status);
    return;
}

if (currentStatus == PaymentStatus.SUCCESS) {
    log.warn("Ignoring conflicting update for already successful payment {} with status {}", payment.getPaymentId(), status);
    return;
}
```

## Pencegahan Double Charge

1. **Satu Record per Payment**: Setiap paymentId menghasilkan tepat satu record pembayaran
2. **Kontrol Transisi Status**: Hanya transisi `INITIATED` → `SUCCESS/FAILED` yang diizinkan
3. **Pemrosesan Idempotent**: Callback duplikat dengan status yang sama diabaikan
4. **Resolusi Konflik**: Setelah pembayaran `SUCCESS`, tidak ada perubahan status lebih lanjut yang diizinkan
5. **Constraint Database**: Record pembayaran unik berdasarkan paymentId

## Penanganan Network Timeout

1. **Mekanisme Retry**: Menggunakan Spring Retry dengan exponential backoff
2. **Circuit Breaker Pattern**: Mencegah kegagalan berantai
3. **Konfigurasi Timeout**: Timeout yang dapat dikonfigurasi untuk panggilan external service
4. **Graceful Degradation**: Sistem terus berfungsi meskipun ada kegagalan external service

## Cara Menjalankan Service

### Prasyarat
- Java 17+
- Maven 3.6+

### Langkah-langkah
1. **Clone dan build**
   ```bash
   git clone <repository-url>
   cd payment-service
   mvn clean install
   ```

2. **Jalankan aplikasi**
   ```bash
   mvn spring-boot:run
   ```
   
   Aplikasi akan berjalan di `http://localhost:8083`

3. **Akses H2 Console** (opsional)
   - URL: `http://localhost:8083/h2-console`
   - JDBC URL: `jdbc:h2:mem:paymentdb`
   - Username: `sa`
   - Password: (kosong)

## Contoh Skenario Test

### 1. Buat Pembayaran
```bash
curl -X POST http://localhost:8083/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId":"merchant-001",
    "customerId":"cust-001",
    "amount":150000,
    "currency":"IDR",
    "description":"Pembelian produk"
  }'
```

**Respons:**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440001",
  "merchantId": "merchant-001",
  "customerId": "cust-001",
  "amount": 150000,
  "currency": "IDR",
  "description": "Pembelian produk",
  "status": "INITIATED"
}
```

### 2. Simulasikan Callback Sukses Pembayaran
```bash
curl -X POST http://localhost:8083/api/payments/callback \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId":"550e8400-e29b-41d4-a716-446655440001",
    "status":"SUCCESS",
    "amount":150000
  }'
```

### 3. Cek Status Pembayaran
```bash
curl http://localhost:8083/api/payments/550e8400-e29b-41d4-a716-446655440001
```

**Respons yang Diharapkan:**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440001",
  "merchantId": "merchant-001",
  "customerId": "cust-001",
  "amount": 150000,
  "currency": "IDR",
  "description": "Pembelian produk",
  "status": "SUCCESS",
  "createdAt": "2026-01-11T12:00:00",
  "updatedAt": "2026-01-11T12:01:00"
}
```

### 4. Test Idempotensi (Callback Duplikat)
Kirim callback yang sama lagi - status harus tetap `SUCCESS` dan sistem mencatat idempotent ignore.

### 5. Test Pencegahan Double Charge
Coba kirim callback lain dengan status berbeda - sistem harus menolak jika pembayaran sudah `SUCCESS`.

## API Endpoints

### Pemrosesan Pembayaran
- `POST /api/payments` - Buat pembayaran baru
- `GET /api/payments/{paymentId}` - Dapatkan detail pembayaran

### Callback Pembayaran
- `POST /api/payments/callback` - Terima status pembayaran dari gateway

### Database Console
- `GET /h2-console` - Akses H2 database console (http://localhost:8083/h2-console)

## Keputusan Desain Utama

1. **HTTP Callback over Message Broker**: Dipilih untuk kesederhanaan dalam skenario assessment sambil tetap menunjukkan persyaratan inti
2. **Idempotensi Level Database**: Menggunakan state database untuk memastikan konsistensi
3. **Optimistic Locking**: Field versi mencegah masalah modifikasi konkuren
4. **Logging Komprehensif**: Semua perubahan state dicatat untuk debugging dan audit
5. **Retry dengan Exponential Backoff**: Menangani network timeout dengan baik
6. **Pemisahan Tanggung Jawab**: Pemisahan yang jelas antara pemrosesan pembayaran dan notifikasi

## Teknologi Stack

- **Framework**: Spring Boot 3.5.9
- **Database**: H2 (in-memory)
- **Build Tool**: Maven
- **Java Version**: 17
- **Validation**: Jakarta Bean Validation
- **Retry**: Spring Retry untuk penanganan network timeout
- **HTTP Client**: RestTemplate untuk komunikasi external service

## Konfigurasi

Service dapat dikonfigurasi melalui `application.properties`:

```properties
# Payment Gateway Configuration
payment.gateway.base-url=http://localhost:8083
payment.gateway.callback.timeout=10000
payment.gateway.retry.max-attempts=3

# Retry Configuration for Network Timeout
retry.max-attempts=3
retry.delay=1000
```

## Cakupan Persyaratan Assessment

✅ **Penanganan Callback Payment Gateway**: Menangani multiple callback dari payment gateway
✅ **Penanganan Network Timeout**: Implementasi mekanisme retry dengan exponential backoff
✅ **Pencegahan Double Charge**: Idempotensi memastikan tidak ada charge duplikat
✅ **Arsitektur Microservice**: Pemisahan tanggung jawab yang jelas
✅ **Error Handling yang Kuat**: Penanganan error dan logging yang komprehensif