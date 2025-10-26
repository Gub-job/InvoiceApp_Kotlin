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
import java.sql.*

class InvoiceScreen(private val idPerusahaan: Int) {

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val pelangganField = TextField()
    
    private val popup = Popup()
    private val listView = ListView<PelangganData>()
    
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

        val lblTotal = Label("Total:")
        val txtTotal = TextField()
        txtTotal.isEditable = false

        grid.addRow(0, lblNo, txtNo, lblTanggal, dpTanggal)
        grid.addRow(1, lblPelanggan, pelangganField)
        grid.addRow(2, lblTotal, txtTotal)

        val btnSimpan = Button("Simpan")
        val btnHapus = Button("Hapus")
        val btnKembali = Button("Kembali ke Halaman Utama")

        val tombolBox = HBox(10.0, btnSimpan, btnHapus, btnKembali)
        tombolBox.padding = Insets(10.0, 0.0, 0.0, 0.0)

        root.children.addAll(title, grid, tombolBox)
        return root
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