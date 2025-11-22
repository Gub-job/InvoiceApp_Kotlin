package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.FileChooser
import model.UmurPiutangData
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import utils.DatabaseHelper
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LaporanUmurPiutangController {

    @FXML private lateinit var tanggalLaporanPicker: DatePicker
    @FXML private lateinit var tableView: TableView<UmurPiutangData>
    @FXML private lateinit var kolomPelanggan: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomNomor: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomTanggal: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomTotal: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomDibayar: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomSisa: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomUmur: TableColumn<UmurPiutangData, String>
    @FXML private lateinit var kolomKategori: TableColumn<UmurPiutangData, String>
    
    @FXML private lateinit var totalPiutangLabel: Label
    @FXML private lateinit var current30Label: Label
    @FXML private lateinit var hari3160Label: Label
    @FXML private lateinit var hari6190Label: Label
    @FXML private lateinit var lebih90Label: Label
    @FXML private lateinit var hari91120Label: Label
    @FXML private lateinit var lebih120Label: Label

    private val piutangList = FXCollections.observableArrayList<UmurPiutangData>()
    private var idPerusahaan: Int = 0

    fun setPerusahaanId(id: Int) {
        this.idPerusahaan = id
        tanggalLaporanPicker.value = LocalDate.now()
        loadLaporan()
    }

    @FXML
    fun initialize() {
        kolomPelanggan.setCellValueFactory { it.value.pelangganProperty }
        kolomNomor.setCellValueFactory { it.value.nomorInvoiceProperty }
        kolomTanggal.setCellValueFactory { it.value.tanggalInvoiceProperty }
        kolomTotal.setCellValueFactory { it.value.totalInvoiceProperty }
        kolomDibayar.setCellValueFactory { it.value.dibayarProperty }
        kolomSisa.setCellValueFactory { it.value.sisaPiutangProperty }
        kolomUmur.setCellValueFactory { it.value.umurHariProperty }
        kolomKategori.setCellValueFactory { it.value.kategoriUmurProperty }

        tableView.items = piutangList
        tanggalLaporanPicker.valueProperty().addListener { _, _, _ -> loadLaporan() }
    }

    @FXML
    private fun onRefreshClicked() {
        loadLaporan()
    }

    private fun loadLaporan() {
        piutangList.clear()
        val tanggalLaporan = tanggalLaporanPicker.value ?: LocalDate.now()
        
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("""
                SELECT 
                    i.id_invoice, i.nomor_invoice, i.tanggal, pel.nama as pelanggan,
                    i.total_dengan_ppn as total,
                    COALESCE(pembayaran_total.total_dibayar, 0) as dibayar,
                    (i.total_dengan_ppn - COALESCE(pembayaran_total.total_dibayar, 0)) as sisa
                FROM invoice i
                JOIN pelanggan pel ON i.id_pelanggan = pel.id
                LEFT JOIN (
                    SELECT id_invoice, SUM(jumlah) as total_dibayar
                    FROM pembayaran
                    GROUP BY id_invoice
                ) pembayaran_total ON i.id_invoice = pembayaran_total.id_invoice
                WHERE i.id_perusahaan = ? 
                AND (i.total_dengan_ppn - COALESCE(pembayaran_total.total_dibayar, 0)) > 0
                AND i.tanggal <= ?
                ORDER BY pel.nama, i.tanggal
            """)
            stmt.setInt(1, idPerusahaan)
            stmt.setString(2, tanggalLaporan.toString())
            val rs = stmt.executeQuery()
            
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            var totalPiutang = 0.0
            var current30 = 0.0
            var hari3160 = 0.0
            var hari6190 = 0.0
            var lebih90 = 0.0
            var hari91120 = 0.0
            var lebih120 = 0.0
            
            while (rs.next()) {
                val tanggalInvoice = LocalDate.parse(rs.getString("tanggal"))
                val umurHari = ChronoUnit.DAYS.between(tanggalInvoice, tanggalLaporan)
                val sisa = rs.getDouble("sisa")
                
                val kategoriUmur = when {
                    umurHari <= 30 -> "Current (0-30 hari)"
                    umurHari <= 60 -> "31-60 hari"
                    umurHari <= 90 -> "61-90 hari"
                    umurHari <= 120 -> "91-120 hari"
                    else -> "> 120 hari"
                }
                
                // Akumulasi berdasarkan kategori
                when {
                    umurHari <= 30 -> current30 += sisa
                    umurHari <= 60 -> hari3160 += sisa
                    umurHari <= 90 -> hari6190 += sisa
                    umurHari <= 120 -> hari91120 += sisa
                    else -> lebih120 += sisa
                }
                
                // Akumulasi untuk > 90 hari (gabungan 91-120 dan > 120)
                if (umurHari > 90) {
                    lebih90 += sisa
                }
                
                totalPiutang += sisa
                
                piutangList.add(UmurPiutangData(
                    rs.getString("pelanggan"),
                    rs.getString("nomor_invoice"),
                    tanggalInvoice.format(formatter),
                    String.format("%,.2f", rs.getDouble("total")),
                    String.format("%,.2f", rs.getDouble("dibayar")),
                    String.format("%,.2f", sisa),
                    "$umurHari hari",
                    kategoriUmur
                ))
            }
            
            // Update summary labels
            totalPiutangLabel.text = String.format("%,.2f", totalPiutang)
            current30Label.text = String.format("%,.2f", current30)
            hari3160Label.text = String.format("%,.2f", hari3160)
            hari6190Label.text = String.format("%,.2f", hari6190)
            lebih90Label.text = String.format("%,.2f", lebih90)
            hari91120Label.text = String.format("%,.2f", hari91120)
            lebih120Label.text = String.format("%,.2f", lebih120)
            
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat laporan: ${e.message}")
        } finally {
            conn.close()
        }
    }

    @FXML
    private fun onExportExcelClicked() {
        if (piutangList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tidak ada data untuk diekspor")
            return
        }

        val fileChooser = FileChooser()
        fileChooser.title = "Simpan Laporan Umur Piutang"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Excel Files", "*.xlsx"))
        fileChooser.initialFileName = "Laporan_Umur_Piutang_${LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))}.xlsx"
        
        val file = fileChooser.showSaveDialog(tableView.scene.window)
        if (file != null) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Umur Piutang")
                
                // Header laporan
                var rowIndex = 0
                val titleRow = sheet.createRow(rowIndex++)
                titleRow.createCell(0).setCellValue("LAPORAN UMUR PIUTANG")
                
                val dateRow = sheet.createRow(rowIndex++)
                dateRow.createCell(0).setCellValue("Per Tanggal: ${tanggalLaporanPicker.value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                
                rowIndex++ // Empty row
                
                // Summary
                val summaryHeaders = arrayOf("Kategori", "Jumlah")
                val summaryHeaderRow = sheet.createRow(rowIndex++)
                summaryHeaders.forEachIndexed { index, header ->
                    summaryHeaderRow.createCell(index).setCellValue(header)
                }
                
                val summaryData = arrayOf(
                    arrayOf("Current (0-30 hari)", current30Label.text),
                    arrayOf("31-60 hari", hari3160Label.text),
                    arrayOf("61-90 hari", hari6190Label.text),
                    arrayOf("> 90 hari", lebih90Label.text),
                    arrayOf("91-120 hari", hari91120Label.text),
                    arrayOf("> 120 hari", lebih120Label.text),
                    arrayOf("TOTAL", totalPiutangLabel.text)
                )
                
                summaryData.forEach { data ->
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(data[0])
                    row.createCell(1).setCellValue(data[1])
                }
                
                rowIndex += 2 // Empty rows
                
                // Detail header
                val detailHeaders = arrayOf("Pelanggan", "No. Invoice", "Tanggal", "Total", "Dibayar", "Sisa", "Umur", "Kategori")
                val detailHeaderRow = sheet.createRow(rowIndex++)
                detailHeaders.forEachIndexed { index, header ->
                    detailHeaderRow.createCell(index).setCellValue(header)
                }
                
                // Detail data
                piutangList.forEach { data ->
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(data.pelangganProperty.get())
                    row.createCell(1).setCellValue(data.nomorInvoiceProperty.get())
                    row.createCell(2).setCellValue(data.tanggalInvoiceProperty.get())
                    row.createCell(3).setCellValue(data.totalInvoiceProperty.get())
                    row.createCell(4).setCellValue(data.dibayarProperty.get())
                    row.createCell(5).setCellValue(data.sisaPiutangProperty.get())
                    row.createCell(6).setCellValue(data.umurHariProperty.get())
                    row.createCell(7).setCellValue(data.kategoriUmurProperty.get())
                }
                
                // Auto size columns
                for (i in 0 until detailHeaders.size) {
                    sheet.autoSizeColumn(i)
                }
                
                val fileOut = FileOutputStream(file)
                workbook.write(fileOut)
                fileOut.close()
                workbook.close()
                
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Laporan berhasil diekspor ke ${file.name}")
            } catch (e: Exception) {
                e.printStackTrace()
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal mengekspor laporan: ${e.message}")
            }
        }
    }

    private fun showAlert(alertType: Alert.AlertType, title: String, message: String) {
        val alert = Alert(alertType)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}