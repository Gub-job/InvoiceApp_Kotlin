package model

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class InvoiceData(
    id: Int,
    nomor: String,
    tanggal: String,
    pelanggan: String,
    total: Double
) {
    val idProperty = SimpleIntegerProperty(id)
    val nomorProperty = SimpleStringProperty(nomor)
    val tanggalProperty = SimpleStringProperty(tanggal)
    val pelangganProperty = SimpleStringProperty(pelanggan)
    val totalProperty = SimpleDoubleProperty(total)
}