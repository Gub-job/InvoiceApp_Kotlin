package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.control.*
import javafx.stage.Popup
import javafx.util.Callback
import model.PiutangData
import utils.DatabaseHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PelangganFilter(val id: Int, val nama: String) {
    override fun toString() = nama
}

class DaftarPiutangController {

    @FXML private lateinit var pelangganSearchField: TextField
    @FXML private lateinit var statusComboBox: ComboBox<String>
    @FXML private lateinit var tableView: TableView<PiutangData>
    @FXML private lateinit var kolomTanggal: TableColumn<PiutangData, String>
    @FXML private lateinit var kolomNomor: TableColumn<PiutangData, String>
    @FXML private lateinit var kolomPelanggan: TableColumn<PiutangData, String>
    @FXML private lateinit var kolomTotal: TableColumn<PiutangData, String>
    @FXML private lateinit var kolomDibayar: TableColumn<PiutangData, String>
    @FXML private lateinit var kolomSisa: TableColumn<PiutangData, String>
    @FXML private lateinit var kolomStatus: TableColumn<PiutangData, String>
    @FXML private lateinit var totalPiutangLabel: Label
    @FXML private lateinit var startDatePicker: DatePicker
    @FXML private lateinit var endDatePicker: DatePicker

    private val piutangList = FXCollections.observableArrayList<PiutangData>()
    private val pelangganList = FXCollections.observableArrayList<PelangganFilter>()
    private val pelangganPopup = Popup()
    private val pelangganListView = ListView<PelangganFilter>()
    private var selectedPelanggan: PelangganFilter? = null
    private var idPerusahaan: Int = 0

    fun setPerusahaanId(id: Int) {
        this.idPerusahaan = id
        utils.CreatePembayaranTable.createTable()
        startDatePicker.value = LocalDate.now().withDayOfMonth(1)
        endDatePicker.value = LocalDate.now()
        loadPelanggan()
        loadPiutang()
    }

    @FXML
    fun initialize() {
        kolomTanggal.setCellValueFactory { it.value.tanggalProperty }
        kolomNomor.setCellValueFactory { it.value.nomorProperty }
        kolomPelanggan.setCellValueFactory { it.value.pelangganProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty }
        kolomDibayar.setCellValueFactory { it.value.dibayarProperty }
        kolomSisa.setCellValueFactory { it.value.sisaProperty }
        kolomStatus.setCellValueFactory { it.value.statusProperty }

        tableView.items = piutangList

        statusComboBox.items.setAll("Semua", "Belum Lunas", "Lunas")
        statusComboBox.selectionModel.selectFirst()
        statusComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ -> loadPiutang() }
        startDatePicker.valueProperty().addListener { _, _, _ -> loadPiutang() }
        endDatePicker.valueProperty().addListener { _, _, _ -> loadPiutang() }
        setupPelangganAutocomplete()
        
        // Double-click untuk input pembayaran
        tableView.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                tableView.selectionModel.selectedItem?.let { selectedPiutang ->
                    bukaFormPembayaran(selectedPiutang)
                }
            }
        }
    }

    @FXML
    private fun onRefreshClicked() {
        loadPiutang()
    }

    private fun loadPiutang() {
        piutangList.clear()
        val conn = DatabaseHelper.getConnection()
        try {
            val selectedStatus = statusComboBox.selectionModel.selectedItem ?: "Semua"
            
            val queryBuilder = StringBuilder("""
                SELECT 
                    i.id_invoice, i.tanggal, i.nomor_invoice, pel.nama as pelanggan, 
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
                WHERE i.id_perusahaan = ? AND i.tanggal BETWEEN ? AND ?
            """)
            
            if (selectedPelanggan != null && selectedPelanggan!!.id != -1) {
                queryBuilder.append(" AND i.id_pelanggan = ?")
            }
            
            // Tidak perlu GROUP BY lagi karena sudah menggunakan subquery
            
            when (selectedStatus) {
                "Belum Lunas" -> queryBuilder.append(" HAVING sisa > 0")
                "Lunas" -> queryBuilder.append(" HAVING sisa = 0")
            }
            
            queryBuilder.append(" ORDER BY i.tanggal DESC")

            val stmt = conn.prepareStatement(queryBuilder.toString())
            var paramIndex = 1
            stmt.setInt(paramIndex++, idPerusahaan)
            stmt.setString(paramIndex++, startDatePicker.value?.toString() ?: LocalDate.now().withDayOfMonth(1).toString())
            stmt.setString(paramIndex++, endDatePicker.value?.toString() ?: LocalDate.now().toString())
            if (selectedPelanggan != null && selectedPelanggan!!.id != -1) {
                stmt.setInt(paramIndex++, selectedPelanggan!!.id)
            }
            val rs = stmt.executeQuery()
            
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            var totalPiutang = 0.0
            
            while (rs.next()) {
                val total = rs.getDouble("total")
                val dibayar = rs.getDouble("dibayar")
                val sisa = rs.getDouble("sisa")
                val status = if (sisa == 0.0) "Lunas" else "Belum Lunas"
                
                piutangList.add(PiutangData(
                    rs.getInt("id_invoice"),
                    LocalDate.parse(rs.getString("tanggal")).format(formatter),
                    rs.getString("nomor_invoice"),
                    rs.getString("pelanggan"),
                    String.format("%,.2f", total),
                    String.format("%,.2f", dibayar),
                    String.format("%,.2f", sisa),
                    status,
                    total,
                    dibayar,
                    sisa
                ))
                
                if (sisa > 0) totalPiutang += sisa
            }
            
            totalPiutangLabel.text = String.format("%,.2f", totalPiutang)
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat data piutang: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun loadPelanggan() {
        pelangganList.clear()
        val conn = DatabaseHelper.getConnection()
        try {
            pelangganList.add(PelangganFilter(-1, "Semua Pelanggan"))
            
            val stmt = conn.prepareStatement("SELECT id, nama FROM pelanggan WHERE id_perusahaan = ? ORDER BY nama")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            
            while (rs.next()) {
                pelangganList.add(PelangganFilter(rs.getInt("id"), rs.getString("nama")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }

    private fun setupPelangganAutocomplete() {
        pelangganListView.cellFactory = Callback {
            object : ListCell<PelangganFilter>() {
                override fun updateItem(item: PelangganFilter?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.nama
                }
            }
        }
        pelangganPopup.content.add(pelangganListView)
        pelangganListView.prefWidthProperty().bind(pelangganSearchField.widthProperty())
        pelangganListView.prefHeight = 200.0

        pelangganSearchField.textProperty().addListener { _, _, newValue ->
            val filtered = pelangganList.filter {
                it.nama.contains(newValue ?: "", ignoreCase = true)
            }
            pelangganListView.items.setAll(filtered)
            pelangganListView.selectionModel.selectFirst()

            if (filtered.isNotEmpty() && pelangganSearchField.scene?.window?.isShowing == true) {
                val screenBounds: Bounds = pelangganSearchField.localToScreen(pelangganSearchField.boundsInLocal)
                pelangganPopup.show(pelangganSearchField, screenBounds.minX, screenBounds.minY + screenBounds.height)
            } else {
                pelangganPopup.hide()
            }
        }

        pelangganListView.setOnMouseClicked { event ->
            if (event.clickCount == 1 && pelangganListView.selectionModel.selectedItem != null) {
                selectPelanggan(pelangganListView.selectionModel.selectedItem)
            }
        }

        pelangganSearchField.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.DOWN -> {
                    if (pelangganPopup.isShowing) {
                        pelangganListView.requestFocus()
                        if (pelangganListView.selectionModel.isEmpty) {
                            pelangganListView.selectionModel.selectFirst()
                        }
                    }
                }
                javafx.scene.input.KeyCode.ENTER -> {
                    if (pelangganPopup.isShowing && pelangganListView.items.isNotEmpty()) {
                        selectPelanggan(pelangganListView.selectionModel.selectedItem ?: pelangganListView.items[0])
                        event.consume()
                    }
                }
                javafx.scene.input.KeyCode.ESCAPE -> pelangganPopup.hide()
                else -> {}
            }
        }

        pelangganListView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER -> {
                    selectPelanggan(pelangganListView.selectionModel.selectedItem)
                    pelangganSearchField.requestFocus()
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    pelangganPopup.hide()
                    pelangganSearchField.requestFocus()
                }
                else -> {}
            }
        }
    }

    private fun selectPelanggan(pelanggan: PelangganFilter?) {
        if (pelanggan != null) {
            selectedPelanggan = pelanggan
            pelangganSearchField.text = pelanggan.nama
            pelangganPopup.hide()
            loadPiutang()
        }
    }

    private fun bukaFormPembayaran(piutang: PiutangData) {
        try {
            val loader = javafx.fxml.FXMLLoader(javaClass.getResource("/view/InputPembayaranView.fxml"))
            val root = loader.load<javafx.scene.Parent>()
            val controller = loader.getController<InputPembayaranController>()
            
            // Gunakan data asli dari database yang sudah disimpan di PiutangData
            controller.setInvoiceData(
                piutang.idInvoice,
                piutang.nomorProperty.get(),
                piutang.pelangganProperty.get(),
                piutang.totalAsli,
                piutang.dibayarAsli,
                piutang.sisaAsli
            )
            controller.setOnSaveCallback { loadPiutang() }
            
            val stage = javafx.stage.Stage()
            stage.title = "Input Pembayaran - ${piutang.nomorProperty.get()}"
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL)
            stage.initOwner(tableView.scene.window)
            stage.scene = javafx.scene.Scene(root)
            stage.isResizable = true
            stage.minWidth = 650.0
            stage.minHeight = 600.0
            stage.width = 650.0
            stage.height = 600.0
            stage.showAndWait()
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka form pembayaran: ${e.message}")
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
