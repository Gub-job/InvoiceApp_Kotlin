package model

import javafx.beans.property.SimpleStringProperty

class LaporanData(
    tanggal: String,
    nomor: String,
    pelanggan: String,
    namaProduk: String,
    qty: String,
    harga: String,
    total: String,
    ppn: String = "0.00",
    totalDenganPpn: String = "0.00"
) {
    val tanggalProperty = SimpleStringProperty(tanggal)
    val nomorProperty = SimpleStringProperty(nomor)
    val pelangganProperty = SimpleStringProperty(pelanggan)
    val namaProdukProperty = SimpleStringProperty(namaProduk)
    val qtyProperty = SimpleStringProperty(qty)
    val hargaProperty = SimpleStringProperty(harga)
    val totalProperty = SimpleStringProperty(total)
    val ppnProperty = SimpleStringProperty(ppn)
    val totalDenganPpnProperty = SimpleStringProperty(totalDenganPpn)
}