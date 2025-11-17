package model

import javafx.beans.property.SimpleStringProperty

class PiutangData(
    tanggal: String,
    nomor: String,
    pelanggan: String,
    total: String,
    dibayar: String,
    sisa: String,
    status: String
) {
    val tanggalProperty = SimpleStringProperty(tanggal)
    val nomorProperty = SimpleStringProperty(nomor)
    val pelangganProperty = SimpleStringProperty(pelanggan)
    val totalProperty = SimpleStringProperty(total)
    val dibayarProperty = SimpleStringProperty(dibayar)
    val sisaProperty = SimpleStringProperty(sisa)
    val statusProperty = SimpleStringProperty(status)
}
