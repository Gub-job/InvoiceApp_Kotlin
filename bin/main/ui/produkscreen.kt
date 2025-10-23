package ui

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import model.ProdukData
import utils.DatabaseHelper

class ProdukScreen(private val idPerusahaan: Int) {

    private val dataList = FXCollections.observableArrayList<ProdukData>()
    private val table = TableView<ProdukData>()

    fun createContent(): BorderPane {
        val root = BorderPane()

        // Setup kolom tabel
        val kolomNama = TableColumn<ProdukData, String>("Nama Produk").apply {
            setCellValueFactory { it.value.namaProperty }
            prefWidth = 250.0
        }

        val kolomUom = TableColumn<ProdukData, String>("Satuan (UOM)").apply {
            setCellValueFactory { it.value.uomProperty }
            prefWidth = 150.0
        }

        table.columns.addAll(kolomNama, kolomUom)
        table.items = dataList
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        // Tombol aksi
        val btnTambah = Button("Tambah Produk")
        val btnEdit = Button("Edit Produk")
        val btnHapus = Button("Hapus Produk")
        val btnRefresh = Button("Refresh")

        btnTambah.setOnAction { showFormTambah() }
        btnEdit.setOnAction { showFormEdit() }
        btnHapus.setOnAction { hapusProduk() }
        btnRefresh.setOnAction { loadData() }

        val layoutBawah = HBox(10.0, btnTambah, btnEdit, btnHapus, btnRefresh).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(10.0)
        }

        root.center = table
        root.bottom = layoutBawah

        loadData()
        return root
    }

    private fun loadData() {
        dataList.clear()
        
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "SELECT id_produk, nama_produk, uom FROM produk WHERE id_perusahaan = ? ORDER BY nama_produk"
            )
            stmt.setInt(1, idPerusahaan)
            
            val rs = stmt.executeQuery()
            while (rs.next()) {
                dataList.add(
                    ProdukData(
                        rs.getInt("id_produk"),
                        rs.getString("nama_produk"),
                        rs.getString("uom")
                    )
                )
            }
            rs.close()
            stmt.close()
        } finally {
            conn.close()
        }
        
        table.refresh()
    }

    private fun showFormTambah() {
        val dialog = createFormDialog("Tambah Produk", null)
        dialog.show()
    }

    private fun showFormEdit() {
        val selected = table.selectionModel.selectedItem
        if (selected == null) {
            showAlert("Peringatan", "Pilih produk yang akan diedit!")
            return
        }

        val dialog = createFormDialog("Edit Produk", selected)
        dialog.show()
    }

    private fun createFormDialog(title: String, data: ProdukData?): Stage {
        val dialog = Stage()
        val grid = GridPane().apply {
            hgap = 15.0
            vgap = 15.0
            padding = Insets(30.0)
            alignment = Pos.CENTER
        }

        val txtNamaProduk = TextField(data?.namaProperty?.value ?: "").apply {
            prefWidth = 300.0
            promptText = "Masukkan nama produk"
        }

        val txtUOM = TextField(data?.uomProperty?.value ?: "").apply {
            prefWidth = 300.0
            promptText = "Contoh: pcs, kg, liter"
        }

        grid.add(Label("Nama Produk:"), 0, 0)
        grid.add(txtNamaProduk, 1, 0)
        grid.add(Label("Satuan (UOM):"), 0, 1)
        grid.add(txtUOM, 1, 1)

        val btnSimpan = Button("Simpan").apply { prefWidth = 120.0 }
        val btnBatal = Button("Batal").apply { prefWidth = 120.0 }

        btnBatal.setOnAction { dialog.close() }
        btnSimpan.setOnAction {
            if (txtNamaProduk.text.isBlank()) {
                showAlert("Error", "Nama produk tidak boleh kosong!")
                txtNamaProduk.requestFocus()
                return@setOnAction
            }

            if (txtUOM.text.isBlank()) {
                showAlert("Error", "Satuan (UOM) tidak boleh kosong!")
                txtUOM.requestFocus()
                return@setOnAction
            }

            if (data == null) {
                simpanProdukBaru(txtNamaProduk.text, txtUOM.text)
            } else {
                updateProduk(data.idProperty.value, txtNamaProduk.text, txtUOM.text)
            }

            dialog.close()
            loadData()
        }

        val tombolBox = HBox(15.0, btnBatal, btnSimpan).apply {
            alignment = Pos.CENTER_RIGHT
        }
        grid.add(tombolBox, 1, 2)

        dialog.scene = Scene(grid, 500.0, 300.0)
        dialog.title = title
        dialog.isResizable = false
        
        return dialog
    }

    private fun simpanProdukBaru(nama: String, uom: String) {
        val conn = DatabaseHelper.getConnection()
        try {
            val sql = "INSERT INTO produk (nama_produk, uom, id_perusahaan) VALUES (?, ?, ?)"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, nama.trim())
            ps.setString(2, uom.trim())
            ps.setInt(3, idPerusahaan)
            ps.executeUpdate()
            ps.close()
        } finally {
            conn.close()
        }
    }

    private fun updateProduk(id: Int, nama: String, uom: String) {
        val conn = DatabaseHelper.getConnection()
        try {
            // PERBAIKAN: Gunakan id_produk bukan id
            val sql = "UPDATE produk SET nama_produk = ?, uom = ? WHERE id_produk = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, nama.trim())
            ps.setString(2, uom.trim())
            ps.setInt(3, id)
            
            val rowsAffected = ps.executeUpdate()
            println("Update produk: $rowsAffected rows affected") // Debug
            
            ps.close()
        } finally {
            conn.close()
        }
    }

    private fun hapusProduk() {
        val selected = table.selectionModel.selectedItem
        if (selected == null) {
            showAlert("Peringatan", "Pilih produk yang akan dihapus!")
            return
        }

        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Konfirmasi Hapus"
            headerText = "Hapus produk: ${selected.namaProperty.value}?"
            contentText = "Data yang dihapus tidak dapat dikembalikan."
        }

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            val conn = DatabaseHelper.getConnection()
            try {
                // PERBAIKAN: Gunakan id_produk bukan produk_id
                val sql = "DELETE FROM produk WHERE id_produk = ?"
                val ps = conn.prepareStatement(sql)
                ps.setInt(1, selected.idProperty.value)
                
                val rowsAffected = ps.executeUpdate()
                println("Delete produk: $rowsAffected rows affected") // Debug
                
                ps.close()
            } finally {
                conn.close()
            }
            loadData()
        }
    }

    private fun showAlert(title: String, message: String) {
        Alert(Alert.AlertType.WARNING).apply {
            this.title = title
            headerText = null
            contentText = message
            showAndWait()
        }
    }
}