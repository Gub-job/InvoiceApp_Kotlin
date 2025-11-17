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
import java.time.LocalDate

class DaftarInvoiceController {

    @FXML private lateinit var invoiceTable: TableView<InvoiceData>
    @FXML private lateinit var kolomId: TableColumn<InvoiceData, Int>
    @FXML private lateinit var kolomNomor: TableColumn<InvoiceData, String>
    @FXML private lateinit var kolomTanggal: TableColumn<InvoiceData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<InvoiceData, String>
    @FXML private lateinit var kolomTotal: TableColumn<InvoiceData, Double>
    @FXML private lateinit var refreshBtn: Button
    @FXML private lateinit var buatBaruBtn: Button
    @FXML private lateinit var startDatePicker: DatePicker
    @FXML private lateinit var endDatePicker: DatePicker

    private val invoiceList = FXCollections.observableArrayList<InvoiceData>()
    private var idPerusahaan: Int = 0
    private var mainController: MainController? = null

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        startDatePicker.value = LocalDate.now().withDayOfMonth(1)
        endDatePicker.value = LocalDate.now()
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
        startDatePicker.valueProperty().addListener { _, _, _ -> loadInvoiceList() }
        endDatePicker.valueProperty().addListener { _, _, _ -> loadInvoiceList() }
        
        // Double-click untuk membuka invoice dalam jendela terpisah
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
            val stmt = conn.prepareStatement("""
                SELECT i.id_invoice, i.nomor_invoice, i.tanggal,
                       pel.nama as pelanggan_nama,
                       i.total_dengan_ppn as total
                FROM invoice i
                LEFT JOIN pelanggan pel ON i.id_pelanggan = pel.id
                WHERE i.id_perusahaan = ? AND i.tanggal BETWEEN ? AND ?
                ORDER BY i.tanggal DESC, i.id_invoice DESC
            """)
            stmt.setInt(1, idPerusahaan)
            stmt.setString(2, startDatePicker.value?.toString() ?: LocalDate.now().withDayOfMonth(1).toString())
            stmt.setString(3, endDatePicker.value?.toString() ?: LocalDate.now().toString())
            val rs = stmt.executeQuery()

            while (rs.next()) {
                invoiceList.add(InvoiceData(
                    rs.getInt("id_invoice"),
                    rs.getString("nomor_invoice") ?: "",
                    rs.getString("tanggal") ?: LocalDate.now().toString(),
                    rs.getString("pelanggan_nama") ?: "Tidak ada",
                    rs.getDouble("total")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat daftar invoice: ${e.message}")
        }
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

            val editStage = Stage()
            editStage.title = "Edit Invoice - ${invoice.nomorProperty.get()}"
            editStage.initModality(Modality.APPLICATION_MODAL)
            editStage.initOwner(invoiceTable.scene.window)
            
            val scene = Scene(view)
            editStage.scene = scene
            editStage.isResizable = false

            editStage.showAndWait()
            loadInvoiceList()
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