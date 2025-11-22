package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import model.ProformaData
import utils.DatabaseHelper
import java.sql.Connection
import java.time.LocalDate

class DaftarProformaController {
    
    @FXML private lateinit var proformaTable: TableView<ProformaData>
    @FXML private lateinit var kolomId: TableColumn<ProformaData, Int>
    @FXML private lateinit var kolomNomor: TableColumn<ProformaData, String>
    @FXML private lateinit var kolomTanggal: TableColumn<ProformaData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<ProformaData, String>
    @FXML private lateinit var kolomTotal: TableColumn<ProformaData, Double>
    @FXML private lateinit var refreshBtn: Button
    @FXML private lateinit var buatBaruBtn: Button
    @FXML private lateinit var startDatePicker: DatePicker
    @FXML private lateinit var endDatePicker: DatePicker
    
    private val proformaList = FXCollections.observableArrayList<ProformaData>()
    private var idPerusahaan: Int = 0
    private var mainController: MainController? = null
    
    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        startDatePicker.value = LocalDate.now().withDayOfMonth(1)
        endDatePicker.value = LocalDate.now()
        loadProformaList()
    }
    
    fun setMainController(controller: MainController) {
        mainController = controller
    }
    
    @FXML
    fun initialize() {
        proformaTable.items = proformaList
        
        // Ubah kolom ID menjadi nomor urut
        kolomId.setCellFactory {
            object : TableCell<ProformaData, Int>() {
                override fun updateItem(item: Int?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else (index + 1).toString()
                }
            }
        }
        kolomNomor.setCellValueFactory { it.value.nomorProperty }
        kolomTanggal.setCellValueFactory { it.value.tanggalProperty }
        kolomPelanggan.setCellValueFactory { it.value.pelangganProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty.asObject() }
        
        // Format kolom total sebagai currency
        kolomTotal.setCellFactory { 
            object : TableCell<ProformaData, Double>() {
                override fun updateItem(item: Double?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else String.format("%,.2f", item)
                }
            }
        }
        
        refreshBtn.setOnAction { loadProformaList() }
        buatBaruBtn.setOnAction { buatProformaBaru() }
        startDatePicker.valueProperty().addListener { _, _, _ -> loadProformaList() }
        endDatePicker.valueProperty().addListener { _, _, _ -> loadProformaList() }

        // Tambahkan listener untuk double-click pada baris tabel
        proformaTable.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                proformaTable.selectionModel.selectedItem?.let { selectedProforma ->
                    editProforma(selectedProforma)
                }
            }
        }
    }
    
    private fun loadProformaList() {
        proformaList.clear()
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement(
                """
                SELECT p.id_proforma, p.no_proforma, p.tanggal_proforma, pel.nama as pelanggan_nama,
                       p.total_dengan_ppn as total
                FROM proforma p 
                LEFT JOIN pelanggan pel ON p.id_pelanggan = pel.id 
                WHERE p.id_perusahaan = ? AND p.tanggal_proforma BETWEEN ? AND ?
                ORDER BY p.tanggal_proforma DESC, p.id_proforma DESC
            """
            )
            stmt.setInt(1, idPerusahaan)
            stmt.setString(2, startDatePicker.value?.toString() ?: LocalDate.now().withDayOfMonth(1).toString())
            stmt.setString(3, endDatePicker.value?.toString() ?: LocalDate.now().toString())
            val rs = stmt.executeQuery()
            
            while (rs.next()) {
                proformaList.add(ProformaData(
                    rs.getInt("id_proforma"),
                    rs.getString("no_proforma") ?: "",
                    rs.getString("tanggal_proforma") ?: "",
                    rs.getString("pelanggan_nama") ?: "Tidak ada",
                    rs.getDouble("total")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat daftar proforma: ${e.message}")
        }
    }
    
    private fun buatProformaBaru() {
        try {
            val resourceUrl = javaClass.getResource("/view/Proforma.fxml")
            if (resourceUrl == null) {
                showAlert("Error", "File FXML tidak ditemukan: /view/Proforma.fxml")
                return
            }
            
            val loader = FXMLLoader(resourceUrl)
            val view = loader.load<VBox>()
            val controller = loader.getController<ProformaController>()
            controller.setIdPerusahaan(idPerusahaan)
            
            val editStage = Stage()
            editStage.title = "Buat Proforma Baru"
            editStage.initModality(Modality.APPLICATION_MODAL)
            editStage.initOwner(proformaTable.scene.window)
            
            val scene = Scene(view)
            editStage.scene = scene
            editStage.isResizable = false

            editStage.showAndWait()
            loadProformaList()
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert("Error", "Gagal membuka form proforma: ${e.message}")
        }
    }

    private fun editProforma(proforma: ProformaData) {
        try {
            val resourceUrl = javaClass.getResource("/view/Proforma.fxml")
            if (resourceUrl == null) {
                showAlert("Error", "File FXML tidak ditemukan: /view/Proforma.fxml")
                return
            }
            
            val loader = FXMLLoader(resourceUrl)
            val view = loader.load<VBox>()
            val controller = loader.getController<ProformaController>()
            controller.setIdPerusahaan(idPerusahaan)
            controller.loadProforma(proforma.idProperty.get())

            val editStage = Stage()
            editStage.title = "Edit Proforma - ${proforma.nomorProperty.get()}"
            editStage.initModality(Modality.APPLICATION_MODAL)
            editStage.initOwner(proformaTable.scene.window)
            
            val scene = Scene(view)
            editStage.scene = scene
            editStage.isResizable = false

            editStage.showAndWait()
            loadProformaList()
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert("Error", "Gagal membuka proforma untuk diedit: ${e.message}")
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