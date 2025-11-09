package model

import java.time.LocalDate

data class LaporanData(
    val nomor: String,
    val tanggal: LocalDate,
    val namaPelanggan: String,
    val subtotal: Double,
    val ppn: Double,
    val grandTotal: Double
)