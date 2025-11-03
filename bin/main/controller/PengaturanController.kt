package controller

import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.TextField
import utils.DatabaseHelper

class PengaturanController {

    @FXML private lateinit var taxRateField: TextField
    @FXML private lateinit var singkatanField: TextField
    @FXML private lateinit var invoiceFormatField: TextField
    @FXML private lateinit var proformaFormatField: TextField
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
            
            // Cek dan tambahkan kolom singkatan jika belum ada
            ensureSingkatanColumnExists(conn)
            
            val stmt = conn.prepareStatement("SELECT default_tax_rate, singkatan, invoice_format, proforma_format FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                taxRateField.text = rs.getDouble("default_tax_rate").toString()
                singkatanField.text = rs.getString("singkatan") ?: ""
                invoiceFormatField.text = rs.getString("invoice_format") ?: ""
                proformaFormatField.text = rs.getString("proforma_format") ?: ""
            }
            conn.close()
        } catch (e: Exception) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat pengaturan: ${e.message}")
        }
    }
    
    private fun ensureSingkatanColumnExists(conn: java.sql.Connection) {
        try {
            // Cek apakah kolom singkatan sudah ada
            val checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('perusahaan') 
                WHERE name = 'singkatan'
            """)
            val rs = checkStmt.executeQuery()
            rs.next()
            val columnExists = rs.getInt("count") > 0
            
            if (!columnExists) {
                // Tambahkan kolom singkatan
                val alterStmt = conn.prepareStatement("ALTER TABLE perusahaan ADD COLUMN singkatan TEXT")
                alterStmt.executeUpdate()
                println("Kolom singkatan berhasil ditambahkan ke tabel perusahaan")
            }
        } catch (e: Exception) {
            println("Error saat menambahkan kolom singkatan: ${e.message}")
        }
    }

    @FXML
    private fun onSimpanClicked() {
        try {
            val conn = DatabaseHelper.getConnection()
            
            // Pastikan kolom singkatan ada sebelum menyimpan
            ensureSingkatanColumnExists(conn)
            
            val stmt = conn.prepareStatement("UPDATE perusahaan SET default_tax_rate = ?, singkatan = ?, invoice_format = ?, proforma_format = ? WHERE id = ?")
            stmt.setDouble(1, taxRateField.text.toDoubleOrNull() ?: 11.0)
            stmt.setString(2, singkatanField.text.uppercase())
            stmt.setString(3, invoiceFormatField.text)
            stmt.setString(4, proformaFormatField.text)
            stmt.setInt(5, idPerusahaan)
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