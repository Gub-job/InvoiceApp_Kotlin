package controller

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.FileChooser
import model.LaporanData
import model.ProdukData
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import utils.DatabaseHelper
import java.io.FileOutputStream
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class LaporanPenjualanController {

    @FXML private lateinit var startDatePicker: DatePicker
    @FXML private lateinit var endDatePicker: DatePicker

    @FXML private lateinit var produkFilterComboBox: ComboBox<ProdukData>
    @FXML private lateinit var tableView: TableView<LaporanData>
    @FXML private lateinit var kolomTanggal: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomNomor: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomNamaProduk: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomQty: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomHarga: TableColumn<LaporanData, String>
    @FXML private lateinit var kolomTotal: TableColumn<LaporanData, String>


    private val laporanList = FXCollections.observableArrayList<LaporanData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private var idPerusahaan: Int = 0
    private var namaAdmin: String = ""

    fun setPerusahaanId(id: Int, namaAdmin: String = "") {
        this.idPerusahaan = id
        this.namaAdmin = namaAdmin
        // Set tanggal default
        startDatePicker.value = LocalDate.now().withDayOfMonth(1)
        endDatePicker.value = LocalDate.now()
        loadProdukUntukFilter()
        loadLaporan()
    }

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

        tableView.items = laporanList



        // Setup ComboBox Produk
        produkFilterComboBox.items = produkList
        produkFilterComboBox.converter = ProdukConverter()
        produkFilterComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ -> loadLaporan() }

        // Listener untuk trigger pencarian
        startDatePicker.valueProperty().addListener { _, _, _ -> loadLaporan() }
        endDatePicker.valueProperty().addListener { _, _, _ -> loadLaporan() }

    }

    @FXML
    private fun onCariClicked() {
        loadLaporan()
    }

    private fun loadProdukUntukFilter() {
        produkList.clear()
        // Tambahkan opsi "Semua Produk" di awal
        produkList.add(ProdukData(id = -1, nama = "Semua Produk", uom = "", qty = "", harga = ""))

        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT id_produk, nama_produk FROM produk WHERE id_perusahaan = ? ORDER BY nama_produk")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                produkList.add(ProdukData(id = rs.getInt("id_produk"), nama = rs.getString("nama_produk"), uom = "", qty = "", harga = ""))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
        produkFilterComboBox.selectionModel.selectFirst()
    }

    private fun loadLaporan() {
        laporanList.clear()
        val startDate = startDatePicker.value
        val endDate = endDatePicker.value
        if (startDate == null || endDate == null) return

        val selectedProduk = produkFilterComboBox.selectionModel.selectedItem
        val conn = DatabaseHelper.getConnection()
        try {
            val queryBuilder = StringBuilder()
            queryBuilder.append("""
                SELECT 
                    i.tanggal, i.nomor_invoice as nomor, pel.nama as pelanggan, pr.nama_produk, di.qty, di.harga, di.total,
                    i.tax as ppn_invoice, i.total as subtotal_invoice, i.dp
                FROM invoice i
                JOIN pelanggan pel ON i.id_pelanggan = pel.id
                JOIN detail_invoice di ON i.id_invoice = di.id_invoice
                JOIN produk pr ON di.id_produk = pr.id_produk
                WHERE i.id_perusahaan = ? AND i.tanggal BETWEEN ? AND ?
            """)
            
            if (selectedProduk != null && selectedProduk.idProperty.get() != -1) {
                queryBuilder.append(" AND di.id_produk = ?")
            }
            queryBuilder.append(" ORDER BY i.tanggal DESC")

            val stmt = conn.prepareStatement(queryBuilder.toString())
            stmt.setInt(1, idPerusahaan)
            stmt.setString(2, startDate.toString())
            stmt.setString(3, endDate.toString())
            if (selectedProduk != null && selectedProduk.idProperty.get() != -1) {
                stmt.setInt(4, selectedProduk.idProperty.get())
            }

            val rs = stmt.executeQuery()
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            while (rs.next()) {
                val itemTotal = rs.getDouble("total")
                val subtotalInvoice = rs.getDouble("subtotal_invoice")
                val dpInvoice = rs.getDouble("dp")
                val ppnInvoice = rs.getDouble("ppn_invoice")

                val ppnItem: Double
                // Logika perhitungan PPN per item disesuaikan dengan cara PPN dihitung di Invoice
                // Jika ada DP, PPN dihitung dari DP. Jika tidak, dari subtotal.
                // PPN per item didistribusikan secara proporsional.
                if (dpInvoice > 0) {
                    // Jika PPN dihitung dari DP, distribusikan PPN berdasarkan rasio item terhadap subtotal
                    ppnItem = if (subtotalInvoice > 0) (itemTotal / subtotalInvoice) * ppnInvoice else 0.0
                } else {
                    // Jika PPN dihitung dari subtotal, distribusikan PPN berdasarkan rasio item terhadap subtotal
                    ppnItem = if (subtotalInvoice > 0) (itemTotal / subtotalInvoice) * ppnInvoice else 0.0
                }

                val totalDenganPpnItem = itemTotal + ppnItem

                laporanList.add(LaporanData(
                    LocalDate.parse(rs.getString("tanggal")).format(formatter),
                    rs.getString("nomor"),
                    rs.getString("pelanggan"),
                    rs.getString("nama_produk"),
                    String.format(Locale.GERMAN, "%.2f", rs.getDouble("qty")),
                    String.format(Locale.GERMAN, "%,.2f", rs.getDouble("harga")),
                    String.format(Locale.GERMAN, "%,.2f", itemTotal),
                    String.format(Locale.GERMAN, "%,.2f", ppnItem),
                    String.format(Locale.GERMAN, "%,.2f", totalDenganPpnItem)
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }

    @FXML
    private fun onCetakClicked() {
        try {
            val loader = javafx.fxml.FXMLLoader(javaClass.getResource("/view/LaporanCetakTemplate.fxml"))
            val root = loader.load<javafx.scene.Parent>()
            val controller = loader.getController<LaporanCetakController>()
            
            val periode = "${startDatePicker.value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - ${endDatePicker.value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
            
            controller.setData(laporanList.toList(), idPerusahaan, periode, namaAdmin)
            
            val stage = javafx.stage.Stage()
            stage.title = "Preview Cetak Laporan"
            stage.scene = javafx.scene.Scene(root) // Hapus ukuran tetap
            stage.minWidth = 800.0
            stage.minHeight = 600.0
            stage.isResizable = true
            stage.isMaximized = true // Buat jendela langsung maximized
            stage.show() // Tampilkan setelah di-set maximized
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka preview cetak: ${e.message}")
        }
    }

    @FXML
    private fun onExportExcelClicked() {
        if (laporanList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tidak ada data untuk diekspor")
            return
        }

        val fileChooser = FileChooser()
        fileChooser.title = "Simpan Laporan Excel"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Excel Files", "*.xlsx"))
        fileChooser.initialFileName = "Laporan_Penjualan_${LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))}.xlsx"
        
        val file = fileChooser.showSaveDialog(tableView.scene.window)
        if (file != null) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Laporan Penjualan")
                
                // Header
                val headerRow = sheet.createRow(0)
                val headers = arrayOf("Tanggal", "Nomor", "Pelanggan", "Produk", "Qty", "Harga", "Total")
                headers.forEachIndexed { index, header ->
                    headerRow.createCell(index).setCellValue(header)
                }
                
                // Data
                laporanList.forEachIndexed { rowIndex, data ->
                    val row = sheet.createRow(rowIndex + 1)
                    row.createCell(0).setCellValue(data.tanggalProperty.get())
                    row.createCell(1).setCellValue(data.nomorProperty.get())
                    row.createCell(2).setCellValue(data.pelangganProperty.get())
                    row.createCell(3).setCellValue(data.namaProdukProperty.get())
                    row.createCell(4).setCellValue(data.qtyProperty.get())
                    row.createCell(5).setCellValue(data.hargaProperty.get())
                    row.createCell(6).setCellValue(data.totalProperty.get())
                }
                
                // Auto size columns
                for (i in 0 until headers.size) {
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
class ProdukConverter : javafx.util.StringConverter<ProdukData>() {
    override fun toString(produk: ProdukData?): String {
        return produk?.namaProperty?.get() ?: ""
    }

    override fun fromString(string: String?): ProdukData? {
        return null
    }
}