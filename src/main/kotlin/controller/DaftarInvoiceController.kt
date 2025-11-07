package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import model.InvoiceData
import utils.DatabaseHelper
import java.sql.Connection

class DaftarInvoiceController {

    @FXML private lateinit var invoiceTable: TableView<InvoiceData>
    @FXML private lateinit var kolomId: TableColumn<InvoiceData, Int>
    @FXML private lateinit var kolomNomor: TableColumn<InvoiceData, String>
    @FXML private lateinit var kolomTanggal: TableColumn<InvoiceData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<InvoiceData, String>
    @FXML private lateinit var kolomTotal: TableColumn<InvoiceData, Double>
    @FXML private lateinit var refreshBtn: Button
    @FXML private lateinit var buatBaruBtn: Button

    private val invoiceList = FXCollections.observableArrayList<InvoiceData>()
    private var idPerusahaan: Int = 0
    private var mainController: MainController? = null

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        loadInvoiceList()
    }

    fun setMainController(controller: MainController) {
        mainController = controller
    }

    @FXML
    fun initialize() {
        invoiceTable.items = invoiceList

        kolomId.setCellValueFactory { it.value.idProperty.asObject() }
        kolomNomor.setCellValueFactory { it.value.nomorProperty }
        kolomTanggal.setCellValueFactory { it.value.tanggalProperty }
        kolomPelanggan.setCellValueFactory { it.value.pelangganProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty.asObject() }

        // Format kolom total sebagai currency
        kolomTotal.setCellFactory {
            object : TableCell<InvoiceData, Double>() {
                override fun updateItem(item: Double?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else String.format("%,.2f", item)
                }
            }
        }

        refreshBtn.setOnAction { loadInvoiceList() }
        buatBaruBtn.setOnAction { buatInvoiceBaru() }

        // Tambahkan listener untuk double-click pada baris tabel
        invoiceTable.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                invoiceTable.selectionModel.selectedItem?.let { selectedInvoice ->
                    editInvoice(selectedInvoice)
                }
            }
        }
    }

    private fun loadInvoiceList() {
        invoiceList.clear()
        try {
            val conn = DatabaseHelper.getConnection()
            // PENTING: Pastikan kolom yang dibutuhkan ada sebelum query
            ensureRequiredColumnsExist(conn)

            val stmt = conn.prepareStatement("""
                SELECT i.id_invoice, i.no_invoice, i.tanggal_invoice, pel.nama as pelanggan_nama,
                       CASE
                           WHEN i.dp > 0 THEN i.dp + (i.dp * (i.tax / 100.0)) -- Asumsi tax adalah persentase
                           ELSE i.total_dengan_ppn
                       END as total
                FROM invoice i
                LEFT JOIN pelanggan pel ON i.id_pelanggan = pel.id
                WHERE i.id_perusahaan = ?
                ORDER BY i.tanggal_invoice DESC, i.id_invoice DESC
            """)
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                invoiceList.add(InvoiceData(
                    rs.getInt("id_invoice"),
                    rs.getString("no_invoice") ?: "",
                    rs.getString("tanggal_invoice") ?: "",
                    rs.getString("pelanggan_nama") ?: "Tidak ada",
                    rs.getDouble("total")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat daftar invoice: ${e.message}")
        }
    }

    private fun ensureRequiredColumnsExist(conn: Connection) {
        try {
            if (!columnExists(conn, "invoice", "no_invoice")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN no_invoice TEXT")
                println("Kolom no_invoice ditambahkan ke tabel invoice")
            }
            if (!columnExists(conn, "invoice", "tanggal_invoice")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN tanggal_invoice TEXT")
                println("Kolom tanggal_invoice ditambahkan ke tabel invoice")
            }
            if (!columnExists(conn, "invoice", "total_dengan_ppn")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN total_dengan_ppn REAL DEFAULT 0.0")
                println("Kolom total_dengan_ppn ditambahkan ke tabel invoice")
            }
        } catch (e: Exception) {
            println("Gagal memastikan kolom invoice ada: ${e.message}")
            // Tampilkan alert jika gagal, karena ini kritis
            showAlert("Database Error", "Gagal memverifikasi struktur tabel invoice: ${e.message}")
        }
    }

    private fun columnExists(conn: Connection, tableName: String, columnName: String): Boolean {
        val checkStmt = conn.prepareStatement("""
            SELECT COUNT(*) as count FROM pragma_table_info('$tableName') 
            WHERE name = '$columnName'
        """)
        val rs = checkStmt.executeQuery()
        var count = 0
        if (rs.next()) {
            count = rs.getInt("count")
        }
        rs.close()
        checkStmt.close()
        return count > 0
    }

    private fun buatInvoiceBaru() {
        try {
            val loader = FXMLLoader(javaClass.getResource("/view/Invoice.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<InvoiceController>()
            controller.setIdPerusahaan(idPerusahaan)

            // Tampilkan di main pane
            mainController?.showScreen(view)
        } catch (e: Exception) {
            showAlert("Error", "Gagal membuka form invoice: ${e.message}")
        }
    }

    private fun editInvoice(invoice: InvoiceData) {
        try {
            val loader = FXMLLoader(javaClass.getResource("/view/Invoice.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<InvoiceController>()
            controller.setIdPerusahaan(idPerusahaan)
            controller.loadInvoice(invoice.idProperty.get())

            mainController?.showScreen(view)
        } catch (e: Exception) {
            showAlert("Error", "Gagal membuka invoice untuk diedit: ${e.message}")
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}