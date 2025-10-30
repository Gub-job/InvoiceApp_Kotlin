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
    @FXML private lateinit var kolomDivisi: TableColumn<ProdukData, String> // Tambahkan kolom divisi
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
        kolomDivisi.setCellValueFactory { it.value.divisiProperty } // Hubungkan data ke kolom
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
    val query = "SELECT id_produk, nama_produk, uom, divisi FROM produk WHERE id_perusahaan = ?"
    val ps = conn.prepareStatement(query)
    ps.setInt(1, idPerusahaan)
    val rs = ps.executeQuery()

    while (rs.next()) {
        dataList.add(
            ProdukData(
                id = rs.getInt("id_produk"),
                nama = rs.getString("nama_produk"),
                uom = rs.getString("uom"),
                divisi = rs.getString("divisi")
            )
        )
    }

    ps.close()
    conn.close()
    table.items = dataList
    table.refresh()
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
        val txtUom = TextField(data?.uomProperty?.value ?: "")
        val txtDivisi = TextField(data?.divisiProperty?.value ?: "") // Field untuk divisi

        grid.add(Label("Nama Produk:"), 0, 0)
        grid.add(txtNama, 1, 0)
        grid.add(Label("Divisi:"), 0, 1)
        grid.add(txtDivisi, 1, 1)
        grid.add(Label("Satuan (UOM):"), 0, 2)
        grid.add(txtUom, 1, 2)

        val btnSimpan = Button("Simpan")
        val btnBatal = Button("Batal")

        val tombolBox = HBox(10.0, btnBatal, btnSimpan).apply { alignment = javafx.geometry.Pos.CENTER_RIGHT }
        grid.add(tombolBox, 1, 3) // Pindahkan ke baris 3

        btnBatal.setOnAction { dialog.close() }
        btnSimpan.setOnAction {
            if (txtNama.text.isBlank() || txtUom.text.isBlank()) {
                showAlert("Error", "Semua field harus diisi!")
                return@setOnAction
            }

            if (data == null) // Tambah baru
                simpanProdukBaru(txtNama.text, txtUom.text, txtDivisi.text)
            else // Edit
                updateProduk(data.idProperty.value, txtNama.text, txtUom.text, txtDivisi.text)

            dialog.close()
            loadData() // SELALU panggil loadData() setelah perubahan
        }

        dialog.scene = Scene(grid, 500.0, 250.0)
        dialog.title = title
        dialog.isResizable = false
        dialog.show()
    }

    private fun simpanProdukBaru(nama: String, uom: String, divisi: String) {
        val conn = DatabaseHelper.getConnection()
        try {
            val ps = conn.prepareStatement("INSERT INTO produk (nama_produk, uom, divisi, id_perusahaan) VALUES (?, ?, ?, ?)")
            ps.setString(1, nama.trim())
            ps.setString(2, uom.trim())
            ps.setString(3, divisi.trim())
            ps.setInt(4, idPerusahaan)
            ps.executeUpdate()
            ps.close()
        } finally {
            conn.close()
        }
    }

    private fun updateProduk(id: Int, nama: String, uom: String, divisi: String) {
        val conn = DatabaseHelper.getConnection()
        try {
            val ps = conn.prepareStatement("UPDATE produk SET nama_produk = ?, uom = ?, divisi = ? WHERE id_produk = ?")
            ps.setString(1, nama.trim())
            ps.setString(2, uom.trim())
            ps.setString(3, divisi.trim())
            ps.setInt(4, id)
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
