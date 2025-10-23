package ui

import javafx.application.Application
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import model.PelangganData
import utils.DatabaseHelper

class PelangganScreen(private val idPerusahaan: Int) {
    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private lateinit var table: TableView<PelangganData>

    fun getView(): BorderPane {
        val root = BorderPane()
        table = TableView<PelangganData>()

        val colNama = TableColumn<PelangganData, String>("Nama")
        colNama.setCellValueFactory { it.value.namaProperty }

        val colAlamat = TableColumn<PelangganData, String>("Alamat")
        colAlamat.setCellValueFactory { it.value.alamatProperty }

        val colTelepon = TableColumn<PelangganData, String>("Telepon")
        colTelepon.setCellValueFactory { it.value.teleponProperty }

        table.columns.addAll(colNama, colAlamat, colTelepon)
        table.items = pelangganList

        val btnTambah = Button("Tambah Pelanggan")
        btnTambah.setOnAction { showFormTambah() }

        val btnEdit = Button("Edit Pelanggan")
        btnEdit.setOnAction { showFormEdit() }

        val btnHapus = Button("Hapus Pelanggan")
        btnHapus.setOnAction { hapusPelanggan() }

        val btnRefresh = Button("Refresh")
        btnRefresh.setOnAction { muatPelanggan() }

        val layoutBawah = HBox(10.0, btnTambah, btnEdit, btnHapus, btnRefresh)
        layoutBawah.alignment = Pos.CENTER_RIGHT
        layoutBawah.padding = Insets(10.0)

        root.center = table
        root.bottom = layoutBawah

        muatPelanggan()
        return root
    }

    private fun muatPelanggan() {
        pelangganList.clear()
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery("SELECT * FROM pelanggan WHERE id_perusahaan = $idPerusahaan")
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
        rs.close()
        stmt.close()
        conn.close()
    }

    private fun showFormTambah() {
        val dialog = Stage()
        val grid = GridPane()
        grid.hgap = 15.0
        grid.vgap = 15.0
        grid.padding = Insets(30.0)
        grid.alignment = Pos.CENTER

        val txtNama = TextField()
        val txtAlamat = TextArea()
        val txtTelepon = TextField()

        txtNama.prefWidth = 300.0
        txtAlamat.prefWidth = 300.0
        txtAlamat.prefRowCount = 3
        txtTelepon.prefWidth = 300.0

        grid.add(Label("Nama:"), 0, 0)
        grid.add(txtNama, 1, 0)
        grid.add(Label("Alamat:"), 0, 1)
        grid.add(txtAlamat, 1, 1)
        grid.add(Label("Telepon:"), 0, 2)
        grid.add(txtTelepon, 1, 2)

        val btnSimpan = Button("Simpan")
        btnSimpan.prefWidth = 120.0

        val btnBatal = Button("Batal")
        btnBatal.prefWidth = 120.0
        btnBatal.setOnAction { dialog.close() }

        val tombolBox = HBox(15.0, btnBatal, btnSimpan)
        tombolBox.alignment = Pos.CENTER_RIGHT
        grid.add(tombolBox, 1, 3)

        btnSimpan.setOnAction {
            if (txtNama.text.isBlank()) {
                showAlert("Error", "Nama tidak boleh kosong!")
                return@setOnAction
            }
            
            val conn = DatabaseHelper.getConnection()
            val sql = "INSERT INTO pelanggan (nama, alamat, telepon, id_perusahaan) VALUES (?, ?, ?, ?)"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, txtNama.text)
            ps.setString(2, txtAlamat.text)
            ps.setString(3, txtTelepon.text)
            ps.setInt(4, idPerusahaan)
            ps.executeUpdate()
            ps.close()
            conn.close()
            dialog.close()
            muatPelanggan()
        }

        val scene = Scene(grid, 500.0, 350.0)
        dialog.scene = scene
        dialog.title = "Tambah Pelanggan"
        dialog.isResizable = false
        dialog.show()
    }

    private fun showFormEdit() {
        val selected = table.selectionModel.selectedItem
        if (selected == null) {
            showAlert("Peringatan", "Pilih pelanggan yang akan diedit!")
            return
        }

        val dialog = Stage()
        val grid = GridPane()
        grid.hgap = 15.0
        grid.vgap = 15.0
        grid.padding = Insets(30.0)
        grid.alignment = Pos.CENTER

        val txtNama = TextField(selected.namaProperty.value)
        val txtAlamat = TextArea(selected.alamatProperty.value)
        val txtTelepon = TextField(selected.teleponProperty.value)

        txtNama.prefWidth = 300.0
        txtAlamat.prefWidth = 300.0
        txtAlamat.prefRowCount = 3
        txtTelepon.prefWidth = 300.0

        grid.add(Label("Nama:"), 0, 0)
        grid.add(txtNama, 1, 0)
        grid.add(Label("Alamat:"), 0, 1)
        grid.add(txtAlamat, 1, 1)
        grid.add(Label("Telepon:"), 0, 2)
        grid.add(txtTelepon, 1, 2)

        val btnSimpan = Button("Simpan")
        btnSimpan.prefWidth = 120.0

        val btnBatal = Button("Batal")
        btnBatal.prefWidth = 120.0
        btnBatal.setOnAction { dialog.close() }

        val tombolBox = HBox(15.0, btnBatal, btnSimpan)
        tombolBox.alignment = Pos.CENTER_RIGHT
        grid.add(tombolBox, 1, 3)

        btnSimpan.setOnAction {
            if (txtNama.text.isBlank()) {
                showAlert("Error", "Nama tidak boleh kosong!")
                return@setOnAction
            }

            val conn = DatabaseHelper.getConnection()
            val sql = "UPDATE pelanggan SET nama = ?, alamat = ?, telepon = ? WHERE id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setString(1, txtNama.text)
            ps.setString(2, txtAlamat.text)
            ps.setString(3, txtTelepon.text)
            ps.setInt(4, selected.idProperty.value)
            ps.executeUpdate()
            ps.close()
            conn.close()
            dialog.close()
            muatPelanggan()
        }

        val scene = Scene(grid, 500.0, 350.0)
        dialog.scene = scene
        dialog.title = "Edit Pelanggan"
        dialog.isResizable = false
        dialog.show()
    }

    private fun hapusPelanggan() {
        val selected = table.selectionModel.selectedItem
        if (selected == null) {
            showAlert("Peringatan", "Pilih pelanggan yang akan dihapus!")
            return
        }

        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Konfirmasi Hapus"
        alert.headerText = "Hapus pelanggan: ${selected.namaProperty.value}?"
        alert.contentText = "Data yang dihapus tidak dapat dikembalikan."

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            val conn = DatabaseHelper.getConnection()
            val sql = "DELETE FROM pelanggan WHERE id = ?"
            val ps = conn.prepareStatement(sql)
            ps.setInt(1, selected.idProperty.value)
            ps.executeUpdate()
            ps.close()
            conn.close()
            muatPelanggan()
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}