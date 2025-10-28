package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.KeyEvent
import javafx.stage.Popup
import javafx.util.Callback
import model.PelangganData
import model.ProdukData
import utils.DatabaseHelper
import java.sql.*
import java.time.LocalDate

class ProformaController {

    @FXML private lateinit var pelangganField: TextField
    @FXML private lateinit var nomorField: TextField
    @FXML private lateinit var contractRefField: TextField
    @FXML private lateinit var contractDatePicker: DatePicker
    @FXML private lateinit var tanggalPicker: DatePicker
    @FXML private lateinit var dpField: TextField
    @FXML private lateinit var alamatField: TextField
    @FXML private lateinit var teleponField: TextField
    @FXML private lateinit var kolomNo: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomQty: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomHarga: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomTotal: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomNama: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomUom: TableColumn<ProdukData, String>
    @FXML private lateinit var table: TableView<ProdukData>
    @FXML private lateinit var simpanBtn: Button
    @FXML private lateinit var konversiBtn: Button
    @FXML private lateinit var hapusBtn: Button
    @FXML private lateinit var tambahBtn: Button

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private val detailList = FXCollections.observableArrayList<ProdukData>()

    private val popup = Popup()
    private val listView = ListView<PelangganData>()
    private val produkPopup = Popup()
    private val produkListView = ListView<ProdukData>()

    private var selectedPelanggan: PelangganData? = null
    private var idPerusahaan: Int = 0
    private var idProformaBaru: Int = 0

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        loadPelanggan()
        loadProduk()
    }

    @FXML
    fun initialize() {
        table.isEditable = true
        table.items = detailList
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        kolomNama.setCellValueFactory { it.value.namaProperty }
        kolomUom.setCellValueFactory { it.value.uomProperty }
        kolomQty.setCellValueFactory { it.value.qtyProperty }
        kolomHarga.setCellValueFactory { it.value.hargaProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty }

        kolomQty.cellFactory = TextFieldTableCell.forTableColumn()
        kolomHarga.cellFactory = TextFieldTableCell.forTableColumn()
        kolomNama.cellFactory = TextFieldTableCell.forTableColumn()

        kolomQty.setOnEditCommit {
            it.rowValue.qtyProperty.set(it.newValue)
            hitungTotal(it.rowValue)
        }
        kolomHarga.setOnEditCommit {
            it.rowValue.hargaProperty.set(it.newValue)
            hitungTotal(it.rowValue)
        }

        kolomNo.setCellFactory {
            object : TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else (index + 1).toString()
                }
            }
        }

        setupPelangganAutocomplete()
        setupProdukAutocomplete()

        tambahBtn.setOnAction {
            val newProduk = ProdukData(0, "", "", "0", "0", "0")
            detailList.add(newProduk)
        }

        hapusBtn.setOnAction {
            val selected = table.selectionModel.selectedItem
            if (selected != null) {
                detailList.remove(selected)
                hapusDetailDariDB(selected)
            }
        }

        simpanBtn.setOnAction {
            simpanProformaDanDetail()
        }

        contractRefField.textProperty().addListener { _, _, newValue ->
            nomorField.text = newValue
        }
        contractDatePicker.valueProperty().addListener { _, _, newDate ->
            tanggalPicker.value = newDate
        }
    }

    // ===========================================================
    // === SIMPAN PROFORMA & DETAILNYA
    // ===========================================================
    private fun simpanProformaDanDetail() {
        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            val stmt = conn.prepareStatement(
                """
                INSERT INTO proforma (id_perusahaan, id_pelanggan, nomor, tanggal, dp)
                VALUES (?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            )
            stmt.setInt(1, idPerusahaan)
            stmt.setInt(2, selectedPelanggan?.idProperty?.get() ?: 0)
            stmt.setString(3, nomorField.text)
            stmt.setString(4, tanggalPicker.value?.toString() ?: LocalDate.now().toString())
            stmt.setDouble(5, dpField.text.toDoubleOrNull() ?: 0.0)
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            if (rs.next()) idProformaBaru = rs.getInt(1)

            for (produk in detailList) {
                val detailStmt = conn.prepareStatement(
                    """
                    INSERT INTO detail_proforma (id_proforma, id_produk, qty, harga, total)
                    VALUES (?, ?, ?, ?, ?)
                    """
                )
                detailStmt.setInt(1, idProformaBaru)
                detailStmt.setInt(2, produk.idProperty.get())
                detailStmt.setDouble(3, produk.qtyProperty.get().toDoubleOrNull() ?: 0.0)
                detailStmt.setDouble(4, produk.hargaProperty.get().toDoubleOrNull() ?: 0.0)
                val total = (produk.qtyProperty.get().toDoubleOrNull() ?: 0.0) *
                            (produk.hargaProperty.get().toDoubleOrNull() ?: 0.0)
                detailStmt.setDouble(5, total)
                detailStmt.executeUpdate()
            }

            conn.commit()
            showAlert("Sukses", "Proforma berhasil disimpan.")
        } catch (e: Exception) {
            conn.rollback()
            showAlert("Error", "Gagal menyimpan proforma: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun hapusDetailDariDB(produk: ProdukData) {
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("DELETE FROM detail_proforma WHERE id_produk = ? AND id_proforma = ?")
            stmt.setInt(1, produk.idProperty.get())
            stmt.setInt(2, idProformaBaru)
            stmt.executeUpdate()
            conn.close()
        } catch (e: Exception) {
            println("Gagal hapus detail: ${e.message}")
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    // ===========================================================
    // === AUTOCOMPLETE PELANGGAN
    // ===========================================================
    private fun setupPelangganAutocomplete() {
        listView.cellFactory = Callback {
            object : ListCell<PelangganData>() {
                override fun updateItem(item: PelangganData?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.namaProperty.get()
                }
            }
        }
        popup.content.add(listView)
        listView.prefWidthProperty().bind(pelangganField.widthProperty())
        listView.prefHeight = 200.0

        // Klik mouse untuk pilih
        listView.setOnMouseClicked { event ->
            if (event.clickCount == 1 && listView.selectionModel.selectedItem != null) {
                selectCurrent()
                popup.hide()
            }
        }

        // ✅ Navigasi keyboard di ListView
        listView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER -> {
                    selectCurrent()
                    popup.hide()
                    event.consume()
                }
                javafx.scene.input.KeyCode.TAB -> {
                    selectCurrent()
                    popup.hide()
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    popup.hide()
                    event.consume()
                }
                else -> {} // biar navigasi panah tetap jalan
            }
        }

        // Filter saat mengetik
        pelangganField.textProperty().addListener { _, _, newValue ->
            if (newValue.isNullOrBlank()) {
                popup.hide()
                selectedPelanggan = null
                alamatField.clear()
                teleponField.clear()
                return@addListener
            }

            val filtered = pelangganList.filter {
                it.namaProperty.get().contains(newValue, ignoreCase = true)
            }

            listView.items.setAll(filtered)

            if (filtered.isEmpty()) {
                popup.hide()
            } else {
                listView.selectionModel.select(0) // ✅ Auto select item pertama
                if (!popup.isShowing) {
                    val screenBounds: Bounds = pelangganField.localToScreen(pelangganField.boundsInLocal)
                    popup.show(pelangganField, screenBounds.minX, screenBounds.minY + screenBounds.height)
                }
            }
        }

        // ✅ Navigasi keyboard di TextField - forward ke ListView
        pelangganField.setOnKeyPressed { event ->
            if (popup.isShowing) {
                when (event.code) {
                    javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCode.UP,
                    javafx.scene.input.KeyCode.PAGE_DOWN, javafx.scene.input.KeyCode.PAGE_UP,
                    javafx.scene.input.KeyCode.HOME, javafx.scene.input.KeyCode.END -> {
                        // Forward event ke ListView
                        listView.fireEvent(event.copyFor(listView, listView))
                        event.consume()
                    }
                    javafx.scene.input.KeyCode.ENTER -> {
                        selectCurrent()
                        popup.hide()
                        event.consume()
                    }
                    javafx.scene.input.KeyCode.TAB -> {
                        selectCurrent()
                        popup.hide()
                        // Tidak consume biar TAB bisa pindah ke field berikutnya
                    }
                    javafx.scene.input.KeyCode.ESCAPE -> {
                        popup.hide()
                        event.consume()
                    }
                    else -> {}
                }
            }
        }

        // ✅ Saat focus hilang, pilih yang highlighted
        pelangganField.focusedProperty().addListener { _, _, isFocused ->
            if (!isFocused && popup.isShowing) {
                selectCurrent()
                popup.hide()
            }
        }
    }

    private fun selectCurrent() {
        val selected = listView.selectionModel.selectedItem
        if (selected != null) {
            selectedPelanggan = selected
            pelangganField.text = selected.namaProperty.get()
            pelangganField.positionCaret(pelangganField.text.length) // ✅ Pindah cursor ke akhir
            alamatField.text = selected.alamatProperty.get()
            teleponField.text = selected.teleponProperty.get()
        }
    }

    // ===========================================================
    // === AUTOCOMPLETE PRODUK
    // ===========================================================
    private fun setupProdukAutocomplete() {
        produkListView.cellFactory = Callback {
            object : ListCell<ProdukData>() {
                override fun updateItem(item: ProdukData?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.namaProperty.get()
                }
            }
        }
        produkPopup.content.add(produkListView)
        produkListView.prefHeight = 180.0
        produkListView.prefWidth = 300.0

        kolomNama.setOnEditStart { event ->
        table.selectionModel.select(event.tablePosition.row)        
        }

        kolomNama.setOnEditCommit { event ->
            val keyword = event.newValue
            val filtered = produkList.filter { it.namaProperty.get().contains(keyword, true) }
            if (filtered.isNotEmpty()) {
                val screenBounds: Bounds = table.localToScreen(table.boundsInLocal)
                produkListView.items.setAll(filtered)
                produkPopup.show(table, screenBounds.minX + 200, screenBounds.minY + 200)
            } else produkPopup.hide()
        }

        produkListView.setOnMouseClicked {
            if (it.clickCount == 1 && produkListView.selectionModel.selectedItem != null) {
                val selectedProduk = produkListView.selectionModel.selectedItem
                applyProdukToRow(selectedProduk)
                produkPopup.hide()
            }
        }
    }

    private fun applyProdukToRow(produk: ProdukData?) {
        val row = table.selectionModel.selectedItem ?: return
        if (produk != null) {
            row.idProperty.set(produk.idProperty.get())
            row.namaProperty.set(produk.namaProperty.get())
            row.uomProperty.set(produk.uomProperty.get())
        }
        table.refresh()
    }

    private fun hitungTotal(item: ProdukData) {
        try {
            val qty = item.qtyProperty.get().toDoubleOrNull() ?: 0.0
            val harga = item.hargaProperty.get().toDoubleOrNull() ?: 0.0
            val total = qty * harga
            item.totalProperty.set(String.format("%,.2f", total))
        } catch (e: Exception) {
            item.totalProperty.set("0")
        }
        table.refresh()
    }

    private fun loadPelanggan() {
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM pelanggan WHERE id_perusahaan = ?")
        stmt.setInt(1, idPerusahaan)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            pelangganList.add(
                PelangganData(
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("alamat"),
                    rs.getString("telepon")
                )
            )
        }
        conn.close()
    }

    private fun loadProduk() {
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM produk WHERE id_perusahaan = ?")
        stmt.setInt(1, idPerusahaan)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            produkList.add(
                ProdukData(
                    rs.getInt("id_produk"),
                    rs.getString("nama_produk"),
                    rs.getString("uom")
                )
            )
        }
        conn.close()
    }
}
