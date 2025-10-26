package model

data class Proforma(
    val id: Int = 0,
    val idPerusahaan: Int,
    val idPelanggan: Int,
    val nomorProforma: String,
    val tanggal: String,
    val contractRef: String?,
    val contractDate: String?,
    val total: Double,
    val tax: Double,
    val totalDenganPpn: Double,
    val keterangan: String?
)
