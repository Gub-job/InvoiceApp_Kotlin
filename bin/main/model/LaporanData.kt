package model

import javafx.beans.property.SimpleStringProperty

data class LaporanData(
    val tanggalValue: String,
    val nomorValue: String,
    val pelangganValue: String,
    val namaProdukValue: String,
    val qtyValue: String,
    val hargaValue: String,
    val totalValue: String
) {
    val tanggalProperty = SimpleStringProperty(tanggalValue)
    val nomorProperty = SimpleStringProperty(nomorValue)
    val pelangganProperty = SimpleStringProperty(pelangganValue)
    val namaProdukProperty = SimpleStringProperty(namaProdukValue)
    val qtyProperty = SimpleStringProperty(qtyValue)
    val hargaProperty = SimpleStringProperty(hargaValue)
    val totalProperty = SimpleStringProperty(totalValue)
}