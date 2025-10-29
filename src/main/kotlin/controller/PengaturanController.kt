package controller

import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.TextField
import utils.DatabaseHelper

class PengaturanController {

    @FXML private lateinit var taxRateField: TextField
    @FXML private lateinit var simpanButton: Button

    private var idPerusahaan: Int = 0

    fun setPerusahaanId(id: Int) {
        idPerusahaan = id
        if (idPerusahaan != 0) {
            loadPengaturan()
        }
    }

    private fun loadPengaturan() {
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("SELECT default_tax_rate FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                taxRateField.text = rs.getDouble("default_tax_rate").toString()
            }
            conn.close()
        } catch (e: Exception) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat pengaturan: ${e.message}")
        }
    }

    @FXML
    private fun onSimpanClicked() {
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("UPDATE perusahaan SET default_tax_rate = ? WHERE id = ?")
            stmt.setDouble(1, taxRateField.text.toDoubleOrNull() ?: 11.0)
            stmt.setInt(2, idPerusahaan)
            stmt.executeUpdate()
            conn.close()
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Pengaturan berhasil disimpan.")
        } catch (e: Exception) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan pengaturan: ${e.message}")
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}