package model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class PelangganData(id: Int, nama: String, alamat: String, telepon: String) {
    val idProperty = SimpleIntegerProperty(id)
    val namaProperty = SimpleStringProperty(nama)
    val alamatProperty = SimpleStringProperty(alamat)
    val teleponProperty = SimpleStringProperty(telepon)
}
