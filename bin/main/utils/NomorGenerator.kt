package utils

import java.time.LocalDate
import java.time.YearMonth

object NomorGenerator {

    private val romanNumerals = mapOf(
        1 to "I", 2 to "II", 3 to "III", 4 to "IV", 5 to "V", 6 to "VI",
        7 to "VII", 8 to "VIII", 9 to "IX", 10 to "X", 11 to "XI", 12 to "XII"
    )

    fun generateNomor(
        idPerusahaan: Int,
        tipe: String, // "invoice" atau "proforma"
        divisi: String?,
        produk: String?,
        singkatanProduk: String? = null,
        tanggal: LocalDate? = null
    ): String {
        val conn = DatabaseHelper.getConnection()
        try {
            // 1. Ambil format dari database (coba dengan singkatan dulu, jika gagal tanpa singkatan)
            var formatString = ""
            var singkatan = ""
            
            try {
                // Coba ambil dengan singkatan
                val formatStmt = conn.prepareStatement("SELECT ${tipe}_format, singkatan FROM perusahaan WHERE id = ?")
                formatStmt.setInt(1, idPerusahaan)
                val formatRs = formatStmt.executeQuery()
                if (formatRs.next()) {
                    formatString = formatRs.getString(1) ?: ""
                    singkatan = formatRs.getString(2) ?: ""
                }
                formatStmt.close()
            } catch (e: Exception) {
                // Jika gagal (kolom singkatan belum ada), coba tanpa singkatan
                try {
                    val formatStmt = conn.prepareStatement("SELECT ${tipe}_format FROM perusahaan WHERE id = ?")
                    formatStmt.setInt(1, idPerusahaan)
                    val formatRs = formatStmt.executeQuery()
                    if (formatRs.next()) {
                        formatString = formatRs.getString(1) ?: ""
                        singkatan = "" // Default kosong
                    }
                    formatStmt.close()
                } catch (e2: Exception) {
                    return "SQL_ERROR: ${e2.message?.take(30) ?: "Unknown"}"
                }
            }
            
            if (formatString.isEmpty()) {
                return "FORMAT_NOT_FOUND"
            }

            val targetDate = tanggal ?: LocalDate.now()
            val year = targetDate.year
            val month = targetDate.monthValue

            // 2. Dapatkan nomor urut berikutnya
            var nextNumber = 1
            try {
                val firstDayOfMonth = LocalDate.of(year, month, 1).toString()
                val lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth().toString()

                val dateColumn = if (tipe == "invoice") "tanggal" else "tanggal_proforma"
                val countStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM $tipe WHERE id_perusahaan = ? AND $dateColumn BETWEEN ? AND ? AND divisi = ?"
                )
                countStmt.setInt(1, idPerusahaan)
                countStmt.setString(2, firstDayOfMonth)
                countStmt.setString(3, lastDayOfMonth)
                countStmt.setString(4, divisi ?: "")
                val countRs = countStmt.executeQuery()
                nextNumber = if (countRs.next()) countRs.getInt(1) + 1 else 1
                countStmt.close()
            } catch (e: Exception) {
                // Jika tabel belum ada atau error lain, gunakan nomor 1
                println("Warning: Tidak bisa menghitung nomor urut, menggunakan 001. Error: ${e.message}")
                nextNumber = 1
            }

            // Debug: Print format yang diambil dari database
            println("=== DEBUG NOMOR GENERATOR ===")
            println("Tipe: $tipe")
            println("Format dari DB: '$formatString'")
            println("Singkatan perusahaan: '$singkatan'")
            println("Divisi: '$divisi'")
            println("Produk: '$produk'")
            println("Singkatan produk: '$singkatanProduk'")
            println("Tanggal: $targetDate (Bulan: $month, Tahun: $year)")
            println("Next Number: $nextNumber")
            
            // 3. Ganti placeholder di format string
            var result = formatString

            // Cari placeholder {nomor:X}
            val nomorRegex = "\\{nomor:(\\d+)}".toRegex()
            nomorRegex.find(formatString)?.let { matchResult ->
                val digits = matchResult.groupValues[1].toIntOrNull() ?: 3
                val formattedNumber = nextNumber.toString().padStart(digits, '0')
                result = result.replace(matchResult.value, formattedNumber)
                println("Setelah replace nomor: '$result'")
            }

            // Ganti placeholder satu per satu
            result = result.replace("{divisi}", divisi?.trim()?.uppercase() ?: "NODIV")
            println("Setelah replace divisi: '$result'")
            
            // Gunakan singkatan produk jika ada, jika tidak gunakan nama produk
            val produkText = if (!singkatanProduk.isNullOrBlank()) {
                singkatanProduk.trim().uppercase()
            } else {
                produk?.trim()?.uppercase() ?: "NOPROD"
            }
            result = result.replace("{produk}", produkText)
            println("Setelah replace produk: '$result'")
            
            result = result.replace("{bulan_romawi}", romanNumerals[month] ?: "X")
            println("Setelah replace bulan: '$result'")
            
            result = result.replace("{tahun}", year.toString())
            println("Setelah replace tahun: '$result'")
            
            result = result.replace("{singkatan}", singkatan.trim().uppercase())
            println("Hasil akhir: '$result'")
            println("=== END DEBUG ===")

            return result

        } catch (e: Exception) {
            e.printStackTrace()
            return "ERROR: ${e.message?.take(30) ?: "Unknown"}"
        } finally {
            try {
                conn.close()
            } catch (e: Exception) {
                // Ignore close error
            }
        }
    }
}