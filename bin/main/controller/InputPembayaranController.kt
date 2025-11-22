package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import utils.DatabaseHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PembayaranHistory(
    val tanggal: String,
    val jumlah: String,
    val keterangan: String
)

class InputPembayaranController {

    @FXML private lateinit var nomorInvoiceLabel: Label
    @FXML private lateinit var pelangganLabel: Label
    @FXML private lateinit var totalInvoiceLabel: Label
    @FXML private lateinit var sudahDibayarLabel: Label
    @FXML private lateinit var sisaPiutangLabel: Label
    @FXML private lateinit var tanggalBayarPicker: DatePicker
    @FXML private lateinit var jumlahBayarField: TextField
    @FXML private lateinit var keteranganArea: TextArea
    @FXML private lateinit var validasiLabel: Label
    @FXML private lateinit var simpanBtn: Button
    @FXML private lateinit var batalBtn: Button
    @FXML private lateinit var historyTable: TableView<PembayaranHistory>
    @FXML private lateinit var kolomTanggal: TableColumn<PembayaranHistory, String>
    @FXML private lateinit var kolomJumlah: TableColumn<PembayaranHistory, String>
    @FXML private lateinit var kolomKeterangan: TableColumn<PembayaranHistory, String>

    private val historyList = FXCollections.observableArrayList<PembayaranHistory>()
    private var idInvoice: Int = 0
    private var nomorInvoice: String = ""
    private var totalInvoice: Double = 0.0
    private var sudahDibayar: Double = 0.0
    private var sisaPiutang: Double = 0.0
    private var onSaveCallback: (() -> Unit)? = null

    @FXML
    fun initialize() {
        tanggalBayarPicker.value = LocalDate.now()
        
        kolomTanggal.setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.tanggal) }
        kolomJumlah.setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.jumlah) }
        kolomKeterangan.setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.keterangan) }
        
        historyTable.items = historyList
        
        // Format jumlah bayar dengan pemisah ribuan
        jumlahBayarField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches("\\d*[,.]?\\d*".toRegex())) {
                jumlahBayarField.text = oldValue
            }
        }
    }

    fun setInvoiceData(idInvoice: Int, nomor: String, pelanggan: String, total: Double, dibayar: Double, sisa: Double) {
        this.idInvoice = idInvoice
        this.nomorInvoice = nomor
        this.totalInvoice = total
        this.sudahDibayar = dibayar
        this.sisaPiutang = sisa
        
        nomorInvoiceLabel.text = nomor
        pelangganLabel.text = pelanggan
        totalInvoiceLabel.text = String.format("Rp %,.2f", total)
        sudahDibayarLabel.text = String.format("Rp %,.2f", dibayar)
        sisaPiutangLabel.text = String.format("Rp %,.2f", sisa)
        
        loadHistory()
    }

    fun setOnSaveCallback(callback: () -> Unit) {
        onSaveCallback = callback
    }

    @FXML
    private fun onSimpanClicked() {
        validasiLabel.isVisible = false
        
        // Validasi
        if (tanggalBayarPicker.value == null) {
            showValidation("Tanggal bayar harus diisi!")
            return
        }
        
        val jumlahText = jumlahBayarField.text.replace(",", "").replace(".", "")
        if (jumlahText.isBlank()) {
            showValidation("Jumlah bayar harus diisi!")
            return
        }
        
        val jumlahBayar = jumlahText.toDoubleOrNull()
        if (jumlahBayar == null || jumlahBayar <= 0) {
            showValidation("Jumlah bayar tidak valid!")
            return
        }
        
        if (jumlahBayar > sisaPiutang) {
            showValidation("Jumlah bayar tidak boleh lebih dari sisa piutang!")
            return
        }
        
        // Simpan ke database
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("""
                INSERT INTO pembayaran (id_invoice, tanggal, jumlah, keterangan)
                VALUES (?, ?, ?, ?)
            """)
            stmt.setInt(1, idInvoice)
            stmt.setString(2, tanggalBayarPicker.value.toString())
            stmt.setDouble(3, jumlahBayar)
            stmt.setString(4, keteranganArea.text)
            stmt.executeUpdate()
            
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Pembayaran berhasil disimpan!")
            
            // Update tampilan dengan data terbaru dari database
            updateDataFromDatabase()
            
            // Reset form
            jumlahBayarField.clear()
            keteranganArea.clear()
            tanggalBayarPicker.value = LocalDate.now()
            
            // Reload history
            loadHistory()
            
            // Callback untuk refresh daftar piutang
            onSaveCallback?.invoke()
            
        } catch (e: Exception) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan pembayaran: ${e.message}")
        } finally {
            conn.close()
        }
    }

    @FXML
    private fun onBatalClicked() {
        (batalBtn.scene.window as? Stage)?.close()
    }

    private fun loadHistory() {
        historyList.clear()
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("""
                SELECT tanggal, jumlah, keterangan
                FROM pembayaran
                WHERE id_invoice = ?
                ORDER BY tanggal DESC
            """)
            stmt.setInt(1, idInvoice)
            val rs = stmt.executeQuery()
            
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            while (rs.next()) {
                val tanggal = LocalDate.parse(rs.getString("tanggal")).format(formatter)
                val jumlah = String.format("Rp %,.2f", rs.getDouble("jumlah"))
                val keterangan = rs.getString("keterangan") ?: "-"
                
                historyList.add(PembayaranHistory(tanggal, jumlah, keterangan))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }

    private fun showValidation(message: String) {
        validasiLabel.text = message
        validasiLabel.isVisible = true
    }

    private fun updateDataFromDatabase() {
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("""
                SELECT 
                    i.total_dengan_ppn as total,
                    COALESCE(SUM(p.jumlah), 0) as dibayar,
                    (i.total_dengan_ppn - COALESCE(SUM(p.jumlah), 0)) as sisa
                FROM invoice i
                LEFT JOIN pembayaran p ON i.id_invoice = p.id_invoice
                WHERE i.id_invoice = ?
                GROUP BY i.id_invoice, i.total_dengan_ppn
            """)
            stmt.setInt(1, idInvoice)
            val rs = stmt.executeQuery()
            
            if (rs.next()) {
                totalInvoice = rs.getDouble("total")
                sudahDibayar = rs.getDouble("dibayar")
                sisaPiutang = rs.getDouble("sisa")
                
                sudahDibayarLabel.text = String.format("Rp %,.2f", sudahDibayar)
                sisaPiutangLabel.text = String.format("Rp %,.2f", sisaPiutang)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
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
