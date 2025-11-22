package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.print.PrinterJob
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.FileChooser
import javafx.stage.Stage
import model.LaporanData
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.util.IOUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import utils.DatabaseHelper
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class LaporanCetakController {
    
    @FXML private lateinit var logoImageView: ImageView
    @FXML private lateinit var labelNamaPerusahaan: Label
    @FXML private lateinit var labelAlamatPerusahaan: Label
    @FXML private lateinit var labelKontakPerusahaan: Label
    @FXML private lateinit var labelPeriode: Label

    @FXML private lateinit var labelTanggalCetak: Label
    @FXML private lateinit var labelTotalKeseluruhan: Label
    @FXML private lateinit var tableView: TableView<LaporanData>
    @FXML private lateinit var kolomTanggal: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomNomor: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomNamaProduk: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomQty: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomHarga: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomTotal: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomPpn: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomTotalPpn: TableColumn<LaporanData, String>

    
    private val laporanList = FXCollections.observableArrayList<LaporanData>()
    private var idPerusahaan: Int = 0
    private var logoPath: String? = null
    
    @FXML
    fun initialize() {
        // Setup kolom tabel
        kolomTanggal.setCellValueFactory { it.value.tanggalProperty }
        kolomNomor.setCellValueFactory { it.value.nomorProperty }
        kolomPelanggan.setCellValueFactory { it.value.pelangganProperty }
        kolomNamaProduk.setCellValueFactory { it.value.namaProdukProperty }
        kolomQty.setCellValueFactory { it.value.qtyProperty }
        kolomHarga.setCellValueFactory { it.value.hargaProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty }
        kolomPpn.setCellValueFactory { it.value.ppnProperty }
        kolomTotalPpn.setCellValueFactory { it.value.totalDenganPpnProperty }
        tableView.items = laporanList
    }
    
    private var namaAdmin: String = ""
    private var namaAdminPerusahaan: String = ""
    
    fun setData(data: List<LaporanData>, idPerusahaan: Int, periode: String, namaAdmin: String = "") {
        this.idPerusahaan = idPerusahaan
        this.namaAdmin = namaAdmin
        laporanList.clear()
        laporanList.addAll(data)
        
        // Set info perusahaan
        loadInfoPerusahaan()
        
        // Set info laporan
        labelPeriode.text = ": $periode"
        labelTanggalCetak.text = ": ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))}"
        
        // Hitung total
        val total = data.sumOf {
            it.totalProperty.get().replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        }
        val totalPpn = data.sumOf {
            it.ppnProperty.get().replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        }
        labelTotalKeseluruhan.text = "Total: Rp ${String.format("%,.2f", total)} | Total PPN: Rp ${String.format("%,.2f", totalPpn)}"
    }
    
    private fun loadInfoPerusahaan() {
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT nama, alamat, telepon, nama_admin, logo_path FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                labelNamaPerusahaan.text = rs.getString("nama") ?: "NAMA PERUSAHAAN"
                labelAlamatPerusahaan.text = rs.getString("alamat") ?: "Alamat Perusahaan"
                val telepon = rs.getString("telepon") ?: "-"
                labelKontakPerusahaan.text = "Telp: $telepon"
                namaAdminPerusahaan = rs.getString("nama_admin") ?: "Admin"
                logoPath = rs.getString("logo_path")
                
                // Load logo jika ada
                if (!logoPath.isNullOrBlank()) {
                    try {
                        val logoFile = java.io.File(logoPath)
                        if (logoFile.exists()) {
                            val image = Image(logoFile.toURI().toString())
                            logoImageView.image = image
                            logoImageView.isVisible = true
                        }
                    } catch (e: Exception) {
                        println("Gagal load logo: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }
    
    @FXML
    private fun onCetakClicked() {
        val printerJob = PrinterJob.createPrinterJob()
        if (printerJob != null && printerJob.showPrintDialog(tableView.scene.window)) {
            val success = printerJob.printPage(tableView.scene.root as Node)
            if (success) {
                printerJob.endJob()
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Laporan berhasil dicetak.")
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal mencetak laporan.")
            }
        }
    }
    
    @FXML
    private fun onEksporExcelClicked() {
        val fileChooser = FileChooser()
        fileChooser.title = "Simpan Laporan Excel"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Excel Files", "*.xlsx"))
        val file = fileChooser.showSaveDialog(tableView.scene.window)

        if (file != null) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Laporan Penjualan")
                
                // Create styles
                val boldFont = workbook.createFont().apply { 
                    bold = true 
                    fontHeightInPoints = 11
                }
                val titleFont = workbook.createFont().apply { 
                    bold = true
                    fontHeightInPoints = 14
                }
                val headerFont = workbook.createFont().apply { 
                    bold = true
                    fontHeightInPoints = 11
                }
                
                val companyNameStyle = workbook.createCellStyle().apply {
                    setFont(boldFont)
                    verticalAlignment = VerticalAlignment.TOP
                }
                
                val companyInfoStyle = workbook.createCellStyle().apply {
                    verticalAlignment = VerticalAlignment.TOP
                }
                
                val titleStyle = workbook.createCellStyle().apply {
                    setFont(titleFont)
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                }
                
                val headerStyle = workbook.createCellStyle().apply {
                    setFont(headerFont)
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                    fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }
                
                val dataStyle = workbook.createCellStyle().apply {
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                    verticalAlignment = VerticalAlignment.CENTER
                }
                
                val dataCenterStyle = workbook.createCellStyle().apply {
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                }
                
                val dataRightStyle = workbook.createCellStyle().apply {
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                    alignment = HorizontalAlignment.RIGHT
                    verticalAlignment = VerticalAlignment.CENTER
                }
                
                val boldStyle = workbook.createCellStyle().apply {
                    setFont(boldFont)
                }
                
                val infoLabelStyle = workbook.createCellStyle().apply {
                    setFont(boldFont)
                    verticalAlignment = VerticalAlignment.TOP
                }
                
                val totalLabelStyle = workbook.createCellStyle().apply {
                    setFont(boldFont)
                    alignment = HorizontalAlignment.RIGHT
                }
                
                val totalValueStyle = workbook.createCellStyle().apply {
                    setFont(boldFont)
                    alignment = HorizontalAlignment.RIGHT
                }
                
                var currentRow = 0
                
                // Sisipkan logo jika ada
                val currentLogoPath = logoPath
                if (!currentLogoPath.isNullOrBlank()) {
                    val logoFile = java.io.File(currentLogoPath)
                    if (logoFile.exists()) {
                        try {
                            val inputStream: java.io.InputStream = FileInputStream(currentLogoPath)
                            val bytes = IOUtils.toByteArray(inputStream)
                            
                            val ext = currentLogoPath.substringAfterLast('.', "").lowercase()
                            val pictureType = when (ext) {
                                "png" -> Workbook.PICTURE_TYPE_PNG
                                "jpg", "jpeg" -> Workbook.PICTURE_TYPE_JPEG
                                else -> Workbook.PICTURE_TYPE_PNG
                            }
                            
                            val pictureIdx = workbook.addPicture(bytes, pictureType)
                            inputStream.close()

                            val drawing = sheet.createDrawingPatriarch()
                            val anchor = workbook.creationHelper.createClientAnchor()
                            anchor.setCol1(0)
                            anchor.setRow1(0)
                            anchor.setCol2(1)
                            anchor.setRow2(3)
                            drawing.createPicture(anchor, pictureIdx)
                        } catch (e: Exception) {
                            println("ERROR: Gagal memuat logo ke Excel: ${e.message}")
                        }
                    }
                }
                
                // Header perusahaan (di samping logo)
                sheet.createRow(0).apply {
                    createCell(1).apply {
                        setCellValue(labelNamaPerusahaan.text)
                        cellStyle = companyNameStyle
                    }
                }
                sheet.createRow(1).apply {
                    createCell(1).apply {
                        setCellValue(labelAlamatPerusahaan.text)
                        cellStyle = companyInfoStyle
                    }
                }
                sheet.createRow(2).apply {
                    createCell(1).apply {
                        setCellValue(labelKontakPerusahaan.text)
                        cellStyle = companyInfoStyle
                    }
                }
                
                currentRow = 3
                
                // Separator (garis horizontal)
                val separatorRow = sheet.createRow(currentRow++)
                for (i in 0..8) {
                    separatorRow.createCell(i).cellStyle = workbook.createCellStyle().apply {
                        setBorderBottom(BorderStyle.THIN)
                    }
                }
                
                // Judul Laporan
                sheet.createRow(currentRow++).apply {
                    createCell(0).apply {
                        setCellValue("LAPORAN PENJUALAN")
                        cellStyle = titleStyle
                    }
                }
                sheet.addMergedRegion(CellRangeAddress(currentRow-1, currentRow-1, 0, 8))
                
                currentRow++ // Empty row
                
                // Info laporan
                sheet.createRow(currentRow++).apply {
                    createCell(0).apply { 
                        setCellValue("Periode")
                        cellStyle = infoLabelStyle
                    }
                    createCell(1).setCellValue(labelPeriode.text)
                }
                sheet.createRow(currentRow++).apply {
                    createCell(0).apply { 
                        setCellValue("Tanggal Cetak")
                        cellStyle = infoLabelStyle
                    }
                    createCell(1).setCellValue(labelTanggalCetak.text)
                }
                
                currentRow++ // Empty row (separator)
                
                // Table headers
                val headerRow = sheet.createRow(currentRow++)
                val headers = listOf("Tanggal", "Nomor", "Pelanggan", "Produk", "Qty", "Harga", "Total", "PPN", "Total+PPN")
                headers.forEachIndexed { index, headerText ->
                    headerRow.createCell(index).apply {
                        setCellValue(headerText)
                        cellStyle = headerStyle
                    }
                }

                // Data
                laporanList.forEachIndexed { rowIndex, data ->
                    val row = sheet.createRow(currentRow + rowIndex)
                    row.createCell(0).apply { setCellValue(data.tanggalProperty.get()); cellStyle = dataCenterStyle }
                    row.createCell(1).apply { setCellValue(data.nomorProperty.get()); cellStyle = dataStyle }
                    row.createCell(2).apply { setCellValue(data.pelangganProperty.get()); cellStyle = dataStyle }
                    row.createCell(3).apply { setCellValue(data.namaProdukProperty.get()); cellStyle = dataStyle }
                    row.createCell(4).apply { setCellValue(data.qtyProperty.get()); cellStyle = dataRightStyle }
                    row.createCell(5).apply { setCellValue(data.hargaProperty.get()); cellStyle = dataRightStyle }
                    row.createCell(6).apply { setCellValue(data.totalProperty.get()); cellStyle = dataRightStyle }
                    row.createCell(7).apply { setCellValue(data.ppnProperty.get()); cellStyle = dataRightStyle }
                    row.createCell(8).apply { setCellValue(data.totalDenganPpnProperty.get()); cellStyle = dataRightStyle }
                }
                
                // Total
                currentRow += laporanList.size
                currentRow++ // Empty row sebelum total
                
                val totalSub = laporanList.sumOf { it.totalProperty.get().replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0 }
                val totalPpn = laporanList.sumOf { it.ppnProperty.get().replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0 }
                val grandTotal = laporanList.sumOf { it.totalDenganPpnProperty.get().replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0 }

                // Format angka sesuai dengan format Indonesia (titik sebagai pemisah ribuan)
                val formatAngka = { nilai: Double ->
                    String.format(Locale.GERMAN, "%,.2f", nilai).replace(",", ".")
                }

                sheet.createRow(currentRow++).apply {
                    createCell(6).apply { 
                        setCellValue("Total: Rp ${formatAngka(totalSub)} | Total PPN: Rp ${formatAngka(totalPpn)}")
                        cellStyle = totalLabelStyle
                    }
                }
                sheet.addMergedRegion(CellRangeAddress(currentRow-1, currentRow-1, 6, 8))
                
                // Auto-size columns
                for (i in 0..8) {
                    sheet.autoSizeColumn(i)
                    // Tambahkan sedikit padding
                    val currentWidth = sheet.getColumnWidth(i)
                    sheet.setColumnWidth(i, (currentWidth * 1.1).toInt())
                }
                
                // Set minimum width untuk kolom logo
                sheet.setColumnWidth(0, 3000)

                FileOutputStream(file).use {
                    workbook.write(it)
                }
                workbook.close()

                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Laporan berhasil diekspor ke Excel.")
            } catch (e: Exception) {
                e.printStackTrace()
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal mengekspor laporan: ${e.message}")
            }
        }
    }
    
    @FXML
    private fun onTutupClicked() {
        val stage = tableView.scene.window as Stage
        stage.close()
    }
    
    private fun showAlert(alertType: Alert.AlertType, title: String, message: String) {
        val alert = Alert(alertType)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}