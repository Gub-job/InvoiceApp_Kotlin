package ui

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Bounds
import javafx.scene.control.*
import javafx.scene.layout.*
import model.PelangganData
import model.ProdukData
import utils.DatabaseHelper
import java.sql.*
import java.time.LocalDate
import javafx.stage.Popup
import javafx.scene.input.KeyEvent
import javafx.util.Callback

class ProformaScreen(private val idPerusahaan: Int) {

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private val detailList = FXCollections.observableArrayList<ProdukData>()

    private val pelangganField = TextField()
    private val nomorField = TextField()
    private val contractRefField = TextField()
    private val contractDatePicker = DatePicker()
    private val tanggalPicker = DatePicker(LocalDate.now())
    private val dpField = TextField()
    private val alamatField = TextField()
    private val teleponField = TextField()
    private val simpanBtn = Button("Simpan")
    private val konversiBtn = Button("Konversi ke Invoice")
    private val hapusBtn = Button("Hapus")

    private val table = TableView<ProdukData>()

    private val popup = Popup()
    private val listView = ListView<PelangganData>()

    private var selectedPelanggan: PelangganData? = null

    fun getView(): VBox {
        loadPelanggan()
        loadProduk()

        pelangganField.promptText = "Ketik nama pelanggan..."
        alamatField.isEditable = false
        teleponField.isEditable = false

        // Setup ListView for popup
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
        listView.prefHeight = 200.0 // Adjust as needed

        // Hide popup and select on mouse click
        listView.setOnMouseClicked { event ->
            if (event.clickCount == 1 && listView.selectionModel.selectedItem != null) {
                selectCurrent()
                popup.hide()
            }
        }

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


        // Filter saat mengetik, tampilkan popup list jika ada match
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
                listView.selectionModel.select(0)
                if (!popup.isShowing) {
                    val screenBounds: Bounds = pelangganField.localToScreen(pelangganField.boundsInLocal)
                    popup.show(pelangganField, screenBounds.minX, screenBounds.minY + screenBounds.height)
                }
            }
        }

        // Navigasi keyboard
        pelangganField.setOnKeyPressed { event ->
            if (popup.isShowing) {
                when (event.code) {
                    javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCode.UP,
                    javafx.scene.input.KeyCode.PAGE_DOWN, javafx.scene.input.KeyCode.PAGE_UP,
                    javafx.scene.input.KeyCode.HOME, javafx.scene.input.KeyCode.END -> {
                        forwardToList(event)
                    }
                    javafx.scene.input.KeyCode.ENTER -> {
                        selectCurrent()
                        popup.hide()
                        event.consume()
                    }
                    javafx.scene.input.KeyCode.TAB -> {
                        selectCurrent()
                        popup.hide()
                        // Do not consume, let TAB move focus to next field
                    }
                    javafx.scene.input.KeyCode.ESCAPE -> {
                        popup.hide()
                        event.consume()
                    }
                    else -> {}
                }
            }
        }

        // Saat focus hilang, pilih yang highlighted kalau popup muncul
        pelangganField.focusedProperty().addListener { _, _, isFocused ->
            if (!isFocused && popup.isShowing) {
                selectCurrent()
                popup.hide()
            }
        }

        // Layout form header
        val formHeader = GridPane().apply {
            hgap = 10.0; vgap = 10.0; padding = Insets(10.0)
            add(Label("Nomor Proforma:"), 0, 0); add(nomorField, 1, 0)
            add(Label("Tanggal:"), 2, 0); add(tanggalPicker, 3, 0)
            add(Label("Pelanggan:"), 0, 1); add(pelangganField, 1, 1)
            add(Label("Alamat:"), 2, 1); add(alamatField, 3, 1)
            add(Label("Telepon:"), 0, 2); add(teleponField, 1, 2)
            add(Label("Contract Ref:"), 0, 3); add(contractRefField, 1, 3)
            add(Label("Contract Date:"), 2, 3); add(contractDatePicker, 3, 3)
            add(Label("DP:"), 0, 4); add(dpField, 1, 4)
        }

        val kolomNama = TableColumn<ProdukData, String>("Nama Barang")
        kolomNama.setCellValueFactory { it.value.namaProperty }

        val kolomUom = TableColumn<ProdukData, String>("UOM")
        kolomUom.setCellValueFactory { it.value.uomProperty }

        table.columns.addAll(kolomNama, kolomUom)
        table.items = detailList
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        val tombolBox = HBox(10.0, simpanBtn, konversiBtn, hapusBtn).apply {
            padding = Insets(10.0)
        }

        return VBox(15.0, formHeader, table, tombolBox).apply {
            padding = Insets(15.0)
        }
    }

    private fun selectCurrent() {
        val selected = listView.selectionModel.selectedItem
        if (selected != null) {
            selectedPelanggan = selected
            pelangganField.text = selected.namaProperty.get()
            pelangganField.positionCaret(pelangganField.text.length)
            isiDataPelanggan(selected)
        }
    }

    private fun forwardToList(event: KeyEvent) {
        listView.fireEvent(event.copyFor(listView, listView))
        event.consume()
    }

    private fun isiDataPelanggan(p: PelangganData) {
        alamatField.text = p.alamatProperty.get()
        teleponField.text = p.teleponProperty.get()
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