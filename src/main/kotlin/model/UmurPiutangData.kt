package model

import javafx.beans.property.SimpleStringProperty

class UmurPiutangData(
    pelanggan: String,
    nomorInvoice: String,
    tanggalInvoice: String,
    totalInvoice: String,
    dibayar: String,
    sisaPiutang: String,
    umurHari: String,
    kategoriUmur: String
) {
    val pelangganProperty = SimpleStringProperty(pelanggan)
    val nomorInvoiceProperty = SimpleStringProperty(nomorInvoice)
    val tanggalInvoiceProperty = SimpleStringProperty(tanggalInvoice)
    val totalInvoiceProperty = SimpleStringProperty(totalInvoice)
    val dibayarProperty = SimpleStringProperty(dibayar)
    val sisaPiutangProperty = SimpleStringProperty(sisaPiutang)
    val umurHariProperty = SimpleStringProperty(umurHari)
    val kategoriUmurProperty = SimpleStringProperty(kategoriUmur)
}