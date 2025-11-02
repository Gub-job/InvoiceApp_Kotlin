package controller

import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import model.ProdukData
import utils.DatabaseHelper

class ProdukController {

    @FXML private lateinit var table: TableView<ProdukData>
    @FXML private lateinit var kolomNama: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomSingkatan: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomDivisi: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomUom: TableColumn<ProdukData, String>
    @FXML private lateinit var btnTambah: Button
    @FXML private lateinit var btnEdit: Button
    @FXML private lateinit var btnHapus: Button
    @FXML private lateinit var btnRefresh: Button

    private val dataList = FXCollections.observableArrayList<ProdukData>()

    // ❗ Tambahkan variabel idPerusahaan
    private var idPerusahaan: Int = 0

    // ✅ Dipanggil setelah controller dimuat
    fun setPerusahaanId(id: Int) {
        idPerusahaan = id
        loadData()
    }

    @FXML
    fun initialize() {
        kolomNama.setCellValueFactory { it.value.namaProperty }
        kolomSingkatan.setCellValueFactory { it.value.singkatanProperty }
        kolomDivisi.setCellValueFactory { it.value.divisiProperty }
        kolomUom.setCellValueFactory { it.value.uomProperty }

        table.items = dataList
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
    }

    @FXML
    fun tambahProduk(event: ActionEvent) {
        showFormDialog("Tambah Produk", null)
    }

    @FXML
    fun editProduk(event: ActionEvent) {
        val selected = table.selectionModel.selectedItem
        if (selected == null) {
            showAlert("Peringatan", "Pilih produk yang akan diedit!")
            return
        }
        showFormDialog("Edit Produk", selected)
    }

    @FXML
    fun hapusProduk(event: ActionEvent) {
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
                val ps = conn.prepareStatement("DELETE FROM produk WHERE id_produk = ?")
                ps.setInt(1, selected.idProperty.value)
                ps.executeUpdate()
                ps.close()
            } finally {
                conn.close()
            }
            loadData()
        }
    }

    @FXML
    fun refreshData(event: ActionEvent) {
        loadData()
    }

    private fun loadData() {
        dataList.clear()
        val conn = DatabaseHelper.getConnection()
        
        // Cek dan tambahkan kolom singkatan jika belum ada
        ensureSingkatanProdukColumnExists(conn)
        
        val query = "SELECT id_produk, nama_produk, uom, divisi, singkatan FROM produk WHERE id_perusahaan = ?"
        val ps = conn.prepareStatement(query)
        ps.setInt(1, idPerusahaan)
        val rs = ps.executeQuery()

        while (rs.next()) {
            dataList.add(
                ProdukData(
                    id = rs.getInt("id_produk"),
                    nama = rs.getString("nama_produk"),
                    uom = rs.getString("uom"),
                    divisi = rs.getString("divisi"),
                    singkatan = rs.getString("singkatan") ?: ""
                )
            )
        }

        ps.close()
        conn.close()
        table.items = dataList
        table.refresh()
    }
    
    private fun ensureSingkatanProdukColumnExists(conn: java.sql.Connection) {
        try {
            val checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('produk') 
                WHERE name = 'singkatan'
            """)
            val rs = checkStmt.executeQuery()
            rs.next()
            val columnExists = rs.getInt("count") > 0
            
            if (!columnExists) {
                val alterStmt = conn.prepareStatement("ALTER TABLE produk ADD COLUMN singkatan TEXT")
                alterStmt.executeUpdate()
                println("Kolom singkatan berhasil ditambahkan ke tabel produk")
            }
        } catch (e: Exception) {
            println("Error saat menambahkan kolom singkatan produk: ${e.message}")
        }
    }


    private fun showFormDialog(title: String, data: ProdukData?) {
        val dialog = Stage()
        val grid = GridPane().apply {
            hgap = 15.0
            vgap = 15.0
            alignment = javafx.geometry.Pos.CENTER
            padding = javafx.geometry.Insets(30.0)
        }

        val txtNama = TextField(data?.namaProperty?.value ?: "")
        val txtSingkatan = TextField(data?.singkatanProperty?.value ?: "")
        val txtUom = TextField(data?.uomProperty?.value ?: "")
        val txtDivisi = TextField(data?.divisiProperty?.value ?: "")

        grid.add(Label("Nama Produk:"), 0, 0)
        grid.add(txtNama, 1, 0)
        grid.add(Label("Singkatan:"), 0, 1)
        grid.add(txtSingkatan, 1, 1)
        grid.add(Label("Divisi:"), 0, 2)
        grid.add(txtDivisi, 1, 2)
        grid.add(Label("Satuan (UOM):"), 0, 3)
        grid.add(txtUom, 1, 3)

        val btnSimpan = Button("Simpan")
        val btnBatal = Button("Batal")

        val tombolBox = HBox(10.0, btnBatal, btnSimpan).apply { alignment = javafx.geometry.Pos.CENTER_RIGHT }
        grid.add(tombolBox, 1, 4)

        btnBatal.setOnAction { dialog.close() }
        btnSimpan.setOnAction {
            if (txtNama.text.isBlank() || txtUom.text.isBlank()) {
                showAlert("Error", "Semua field harus diisi!")
                return@setOnAction
            }

            if (data == null) // Tambah baru
                simpanProdukBaru(txtNama.text, txtSingkatan.text, txtUom.text, txtDivisi.text)
            else // Edit
                updateProduk(data.idProperty.value, txtNama.text, txtSingkatan.text, txtUom.text, txtDivisi.text)

            dialog.close()
            loadData() // SELALU panggil loadData() setelah perubahan
        }

        dialog.scene = Scene(grid, 500.0, 250.0)
        dialog.title = title
        dialog.isResizable = false
        dialog.show()
    }

    private fun simpanProdukBaru(nama: String, singkatan: String, uom: String, divisi: String) {
        val conn = DatabaseHelper.getConnection()
        try {
            ensureSingkatanProdukColumnExists(conn)
            val ps = conn.prepareStatement("INSERT INTO produk (nama_produk, singkatan, uom, divisi, id_perusahaan) VALUES (?, ?, ?, ?, ?)")
            ps.setString(1, nama.trim())
            ps.setString(2, singkatan.trim().uppercase())
            ps.setString(3, uom.trim())
            ps.setString(4, divisi.trim())
            ps.setInt(5, idPerusahaan)
            ps.executeUpdate()
            ps.close()
        } finally {
            conn.close()
        }
    }

    private fun updateProduk(id: Int, nama: String, singkatan: String, uom: String, divisi: String) {
        val conn = DatabaseHelper.getConnection()
        try {
            ensureSingkatanProdukColumnExists(conn)
            val ps = conn.prepareStatement("UPDATE produk SET nama_produk = ?, singkatan = ?, uom = ?, divisi = ? WHERE id_produk = ?")
            ps.setString(1, nama.trim())
            ps.setString(2, singkatan.trim().uppercase())
            ps.setString(3, uom.trim())
            ps.setString(4, divisi.trim())
            ps.setInt(5, id)
            ps.executeUpdate()
            ps.close()
        } finally {
            conn.close()
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
