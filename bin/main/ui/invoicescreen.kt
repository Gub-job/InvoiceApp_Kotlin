package ui

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Bounds
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Popup
import javafx.util.Callback
import model.PelangganData
import utils.DatabaseHelper
import java.text.NumberFormat
import java.sql.*

class InvoiceScreen(private val idPerusahaan: Int) {

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val pelangganField = TextField()
    
    private val popup = Popup()
    private val listView = ListView<PelangganData>()

    // Komponen untuk PPN dan Total
    private val txtSubtotal = TextField("0.00").apply { isEditable = false }
    private val txtPpn = TextField("11") // Default PPN 11%
    private val txtPpnAmount = TextField("0.00").apply { isEditable = false }
    private val txtGrandTotal = TextField("0.00").apply { isEditable = false }
    private val tableItems = FXCollections.observableArrayList<Map<String, Any>>() // Ganti dengan model jika ada
    
    private var selectedPelanggan: PelangganData? = null

    fun getView(): Parent {
        loadPelanggan()

        pelangganField.promptText = "Ketik nama pelanggan..."

        val root = VBox(15.0)
        root.padding = Insets(20.0)

        val title = Label("Form Invoice")
        title.font = Font.font("Arial", FontWeight.BOLD, 18.0)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0

        val lblNo = Label("Nomor Invoice:")
        val txtNo = TextField()

        val lblTanggal = Label("Tanggal:")
        val dpTanggal = DatePicker()

        val lblPelanggan = Label("Pelanggan:")

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
        listView.prefHeight = 200.0

        // Hide popup and select on mouse click
        listView.setOnMouseClicked { event ->
            if (event.clickCount == 1 && listView.selectionModel.selectedItem != null) {
                selectCurrent()
                popup.hide()
            }
        }

        // Handle keyboard di ListView
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

        // Navigasi keyboard dari TextField
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
                        // Biarkan TAB pindah ke field berikutnya
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

        // Listener untuk kalkulasi otomatis
        txtPpn.textProperty().addListener { _, _, _ -> updateTotals() }

        grid.addRow(0, lblNo, txtNo, lblTanggal, dpTanggal)
        grid.addRow(1, lblPelanggan, pelangganField)
        
        // Placeholder untuk TableView
        val itemTable = TableView<Map<String, Any>>() // Ganti dengan TableView<ItemModel>
        itemTable.minHeight = 200.0
        itemTable.isEditable = true
        // TODO: Definisikan kolom-kolom tabel (Nama, Qty, Harga, Total) dan tambahkan listener
        // Saat item di tabel berubah, panggil updateTotals()

        // Layout untuk total
        val totalGrid = GridPane().apply {
            hgap = 10.0
            vgap = 5.0
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            add(Label("Subtotal:"), 0, 0)
            add(txtSubtotal, 1, 0)
            add(Label("PPN (%):"), 0, 1)
            add(txtPpn, 1, 1)
            add(Label("PPN Amount:"), 0, 2)
            add(txtPpnAmount, 1, 2)
            add(Label("Grand Total:"), 0, 3)
            add(txtGrandTotal, 1, 3)
        }
        
        val btnSimpan = Button("Simpan")
        val btnHapus = Button("Hapus")
        val btnKembali = Button("Kembali ke Halaman Utama")

        val tombolBox = HBox(10.0, btnSimpan, btnHapus, btnKembali)
        tombolBox.padding = Insets(10.0, 0.0, 0.0, 0.0)

        root.children.addAll(title, grid, itemTable, totalGrid, tombolBox)
        return root
    }

    private fun updateTotals() {
        // Ganti ini dengan kalkulasi dari item di tabel Anda
        val subtotal = tableItems.sumOf { (it["qty"] as? Double ?: 0.0) * (it["harga"] as? Double ?: 0.0) }
        
        val ppnRate = txtPpn.text.toDoubleOrNull() ?: 0.0
        val ppnAmount = subtotal * (ppnRate / 100.0)
        val grandTotal = subtotal + ppnAmount

        txtSubtotal.text = String.format("%,.2f", subtotal)
        txtPpnAmount.text = String.format("%,.2f", ppnAmount)
        txtGrandTotal.text = String.format("%,.2f", grandTotal)
    }

    private fun selectCurrent() {
        val selected = listView.selectionModel.selectedItem
        if (selected != null) {
            selectedPelanggan = selected
            pelangganField.text = selected.namaProperty.get()
            pelangganField.positionCaret(pelangganField.text.length)
        }
    }

    private fun forwardToList(event: KeyEvent) {
        listView.fireEvent(event.copyFor(listView, listView))
        event.consume()
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
}