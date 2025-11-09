package controller

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import model.LaporanData
import utils.DatabaseHelper
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class LaporanPenjualanController {

    @FXML
    private lateinit var tanggalMulaiPicker: DatePicker

    @FXML
    private lateinit var tanggalAkhirPicker: DatePicker

    @FXML
    private lateinit var tampilkanButton: Button

    @FXML
    private lateinit var laporanTable: TableView<LaporanData>

    @FXML
    private lateinit var noInvoiceCol: TableColumn<LaporanData, String>

    @FXML
    private lateinit var tanggalCol: TableColumn<LaporanData, String>

    @FXML
    private lateinit var pelangganCol: TableColumn<LaporanData, String>

    @FXML
    private lateinit var subtotalCol: TableColumn<LaporanData, String>

    @FXML
    private lateinit var ppnCol: TableColumn<LaporanData, String>

    @FXML
    private lateinit var grandTotalCol: TableColumn<LaporanData, String>

    @FXML
    private lateinit var totalSubtotalLabel: Label

    private var idPerusahaan: Int = 0
    private val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    fun setPerusahaanId(id: Int) {
        this.idPerusahaan = id
    }

    @FXML
    fun initialize() {
        tanggalMulaiPicker.value = LocalDate.now().withDayOfMonth(1)
        tanggalAkhirPicker.value = LocalDate.now()

        setupTableColumns()
    }

    private fun setupTableColumns() {
        val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        noInvoiceCol.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.nomor) }
        tanggalCol.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.tanggal.format(dateFormat)) }
        pelangganCol.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.namaPelanggan) }
        subtotalCol.setCellValueFactory { cellData -> SimpleStringProperty(numberFormat.format(cellData.value.subtotal)) }
        ppnCol.setCellValueFactory { cellData -> SimpleStringProperty(numberFormat.format(cellData.value.ppn)) }
        grandTotalCol.setCellValueFactory { cellData -> SimpleStringProperty(numberFormat.format(cellData.value.grandTotal)) }

        // Atur alignment kolom angka ke kanan
        subtotalCol.style = "-fx-alignment: CENTER-RIGHT;"
        ppnCol.style = "-fx-alignment: CENTER-RIGHT;"
        grandTotalCol.style = "-fx-alignment: CENTER-RIGHT;"
    }

    @FXML
    fun tampilkanLaporan() {
        val tanggalMulai = tanggalMulaiPicker.value
        val tanggalAkhir = tanggalAkhirPicker.value

        if (tanggalMulai == null || tanggalAkhir == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tanggal mulai dan tanggal akhir harus diisi.")
            return
        }

        if (tanggalMulai.isAfter(tanggalAkhir)) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tanggal mulai tidak boleh setelah tanggal akhir.")
            return
        }

        try {
            val daftarInvoice = getInvoicesByDateRange(idPerusahaan, tanggalMulai, tanggalAkhir)
            laporanTable.items = FXCollections.observableArrayList(daftarInvoice)

            val totalSubtotal = daftarInvoice.sumOf { it.subtotal }
            totalSubtotalLabel.text = numberFormat.format(totalSubtotal)

        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat laporan: ${e.message}")
        }
    }

    private fun getInvoicesByDateRange(idPerusahaan: Int, startDate: LocalDate, endDate: LocalDate): List<LaporanData> {
        val result = mutableListOf<LaporanData>()
        val sql = """
            SELECT i.nomor_invoice, i.tanggal, p.nama as nama_pelanggan, i.total, i.tax, i.total_dengan_ppn
            FROM invoice i
            JOIN pelanggan p ON i.id_pelanggan = p.id
            WHERE i.id_perusahaan = ? AND i.tanggal BETWEEN ? AND ?
            ORDER BY i.tanggal
        """
        try {
            DatabaseHelper.getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setInt(1, idPerusahaan)
                    stmt.setString(2, startDate.toString())
                    stmt.setString(3, endDate.toString())
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        result.add(LaporanData(
                            nomor = rs.getString("nomor_invoice"),
                            tanggal = LocalDate.parse(rs.getString("tanggal")),
                            namaPelanggan = rs.getString("nama_pelanggan"),
                            subtotal = rs.getDouble("total"),
                            ppn = rs.getDouble("tax"),
                            grandTotal = rs.getDouble("total_dengan_ppn")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Gagal mengambil data invoice dari database.")
        }
        return result
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}