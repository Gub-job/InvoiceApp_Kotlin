package model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class ProdukData(id: Int, nama: String, uom: String) {
    val idProperty = SimpleIntegerProperty(id)
    val namaProperty = SimpleStringProperty(nama)
    val uomProperty = SimpleStringProperty(uom)
}
