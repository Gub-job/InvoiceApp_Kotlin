package model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class ProdukData(
    id: Int,
    nama: String,
    uom: String,
    qty: String = "",
    harga: String = "",
    total: String = "",
    divisi: String = ""
) {
    val idProperty = SimpleIntegerProperty(id)
    val namaProperty = SimpleStringProperty(nama)
    val uomProperty = SimpleStringProperty(uom)
    val qtyProperty = SimpleStringProperty(qty)
    val hargaProperty = SimpleStringProperty(harga)
    val totalProperty = SimpleStringProperty(total)
    val divisiProperty = SimpleStringProperty(divisi)
}
