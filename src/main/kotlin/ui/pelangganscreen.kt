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

        fun getView(): BorderPane {
        val root = BorderPane()
        val table = TableView<PelangganData>()

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

        val layoutBawah = HBox(10.0, btnTambah)
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
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(20.0)

        val txtNama = TextField()
        val txtAlamat = TextField()
        val txtTelepon = TextField()

        grid.add(Label("Nama:"), 0, 0)
        grid.add(txtNama, 1, 0)
        grid.add(Label("Alamat:"), 0, 1)
        grid.add(txtAlamat, 1, 1)
        grid.add(Label("Telepon:"), 0, 2)
        grid.add(txtTelepon, 1, 2)

        val btnSimpan = Button("Simpan")
        btnSimpan.setOnAction {
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

        grid.add(btnSimpan, 1, 3)

        dialog.scene = Scene(grid)
        dialog.title = "Tambah Pelanggan"
        dialog.show()
    }
}
