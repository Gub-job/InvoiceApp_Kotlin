package model

data class DocumentData(
    val documentType: String,
    val nomorDokumen: String,
    val tanggalDokumen: String,
    val namaPelanggan: String,
    val alamatPelanggan: String,
    val teleponPelanggan: String,
    val items: List<ProdukData>,
    val subtotal: String,
    val dp: String,
    val ppn: String,
    val grandTotal: String,
    val terbilang: String,
    val contractRef: String?,
    val contractDate: String?
)