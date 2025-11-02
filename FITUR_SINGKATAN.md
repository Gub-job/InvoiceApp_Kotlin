# Fitur Singkatan Perusahaan

## Deskripsi
Fitur ini memungkinkan Anda menambahkan singkatan perusahaan yang dapat digunakan dalam penomoran invoice dan proforma.

## Cara Menggunakan

### 1. Mengatur Singkatan Perusahaan
1. Buka menu **Pengaturan** dari aplikasi
2. Isi field **Singkatan Perusahaan** dengan singkatan yang diinginkan
   - Contoh: "PBM" untuk "Putra Bangka Mandiri"
   - Singkatan akan otomatis diubah ke huruf kapital
3. Klik **Simpan Pengaturan**

### 2. Menggunakan Singkatan dalam Format Nomor
Dalam field **Format Nomor Invoice** atau **Format Nomor Proforma**, Anda dapat menggunakan placeholder `{singkatan}`.

#### Contoh Format:
- `{singkatan}/{nomor:3}/{bulan_romawi}/{tahun}`
- Hasil: `PBM/001/XII/2024`

- `INV-{singkatan}-{nomor:4}-{tahun}`
- Hasil: `INV-PBM-0001-2024`

- `{singkatan}-{divisi}-{nomor:3}/{bulan_romawi}/{tahun}`
- Hasil: `PBM-KONSTRUKSI-001/XII/2024`

### 3. Placeholder yang Tersedia
- `{singkatan}` - Singkatan perusahaan
- `{nomor:3}` - Nomor urut dengan 3 digit (001, 002, dst)
- `{divisi}` - Divisi produk
- `{produk}` - Nama produk
- `{bulan_romawi}` - Bulan dalam angka romawi (I-XII)
- `{tahun}` - Tahun (2024)

## Update Database
Sebelum menggunakan fitur ini, jalankan update database dengan menjalankan:
```kotlin
// Jalankan file test/UpdateDatabase.kt
```

Atau secara manual tambahkan kolom singkatan ke tabel perusahaan:
```sql
ALTER TABLE perusahaan ADD COLUMN singkatan TEXT;
```

## Manfaat
- Penomoran invoice/proforma lebih ringkas dan profesional
- Mudah dikenali dan diingat
- Konsisten dengan identitas perusahaan
- Memudahkan pencarian dan pengarsipan dokumen