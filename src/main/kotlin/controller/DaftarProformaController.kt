package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.*
import javafx.scene.layout.VBox
import model.ProformaData
import utils.DatabaseHelper
import java.sql.Connection

class DaftarProformaController {
    
    @FXML private lateinit var proformaTable: TableView<ProformaData>
    @FXML private lateinit var kolomId: TableColumn<ProformaData, Int>
    @FXML private lateinit var kolomNomor: TableColumn<ProformaData, String>
    @FXML private lateinit var kolomTanggal: TableColumn<ProformaData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<ProformaData, String>
    @FXML private lateinit var kolomTotal: TableColumn<ProformaData, Double>
    @FXML private lateinit var refreshBtn: Button
    @FXML private lateinit var buatBaruBtn: Button
    
    private val proformaList = FXCollections.observableArrayList<ProformaData>()
    private var idPerusahaan: Int = 0
    private var mainController: MainController? = null
    
    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        loadProformaList()
    }
    
    fun setMainController(controller: MainController) {
        mainController = controller
    }
    
    @FXML
    fun initialize() {
        proformaTable.items = proformaList
        
        kolomId.setCellValueFactory { it.value.idProperty.asObject() }
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
    }
    
    private fun loadProformaList() {
        proformaList.clear()
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("""
                SELECT p.id_proforma, p.no_proforma, p.tanggal_proforma, pel.nama as pelanggan_nama, 
                       p.total_dengan_ppn as total
                FROM proforma p 
                LEFT JOIN pelanggan pel ON p.id_pelanggan = pel.id 
                WHERE p.id_perusahaan = ? 
                ORDER BY p.tanggal_proforma DESC, p.id_proforma DESC
            """)
            stmt.setInt(1, idPerusahaan)
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
            val loader = FXMLLoader(javaClass.getResource("/view/Proforma.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<ProformaController>()
            controller.setIdPerusahaan(idPerusahaan)
            
            // Tampilkan di main pane
            mainController?.showScreen(view)
        } catch (e: Exception) {
            showAlert("Error", "Gagal membuka form proforma: ${e.message}")
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