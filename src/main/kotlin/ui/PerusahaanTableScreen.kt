package ui

import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import model.Perusahaan
import utils.DatabaseHelper
import java.io.File

class PerusahaanTableScreen(private val idPerusahaan: Int) {

    private var perusahaan: Perusahaan? = null
    private var logoPath: String? = null

    // Input fields
    private val namaField = TextField()
    private val alamatField = TextArea()
    private val teleponField = TextField()
    private val faxField = TextField()
    private val hpField = TextField()
    private val noRekField = TextField()
    private val namaBankField = TextField()
    private val lokasiBankField = TextField()
    private val namaPemilikField = TextField()
    private val jabatanPemilikField = TextField()
    private val logoView = ImageView()
    private val gantiLogoBtn = Button("Ganti Logo")

    init {
        alamatField.prefRowCount = 3
        logoView.fitHeight = 100.0
        logoView.isPreserveRatio = true
        loadData()
        setupGantiLogoButton()
    }

    private fun loadData() {
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM perusahaan WHERE id = ?")
        stmt.setInt(1, idPerusahaan)
        val rs = stmt.executeQuery()
        if (rs.next()) {
            perusahaan = Perusahaan(
                rs.getInt("id"),
                rs.getString("nama"),
                rs.getString("logo_path")
            )
            namaField.text = rs.getString("nama")
            alamatField.text = rs.getString("alamat")
            teleponField.text = rs.getString("telepon")
            faxField.text = rs.getString("fax")
            hpField.text = rs.getString("HP")
            noRekField.text = rs.getString("no_rek") ?: ""
            namaBankField.text = rs.getString("nama_bank")
            lokasiBankField.text = rs.getString("lokasi_kantor_bank")
            namaPemilikField.text = rs.getString("nama_pemilik")
            jabatanPemilikField.text = rs.getString("jabatan_pemilik")
            logoPath = rs.getString("logo_path")

            if (!logoPath.isNullOrEmpty() && File(logoPath!!).exists()) {
                logoView.image = Image("file:$logoPath")
            }
        }
        conn.close()
    }

    private fun setupGantiLogoButton() {
        gantiLogoBtn.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.title = "Pilih Logo Perusahaan"
            fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg")
            )

            val file = fileChooser.showOpenDialog(null)
            if (file != null) {
                logoPath = file.absolutePath
                logoView.image = Image("file:${file.absolutePath}")
            }
        }
    }

    private fun simpanData() {
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.prepareStatement(
            """UPDATE perusahaan SET 
                nama=?, alamat=?, telepon=?, fax=?, HP=?, no_rek=?, 
                nama_bank=?, lokasi_kantor_bank=?, nama_pemilik=?, 
                jabatan_pemilik=?, logo_path=? WHERE id=?"""
        )

        stmt.setString(1, namaField.text)
        stmt.setString(2, alamatField.text)
        stmt.setString(3, teleponField.text)
        stmt.setString(4, faxField.text)
        stmt.setString(5, hpField.text)
        stmt.setString(6, noRekField.text)
        stmt.setString(7, namaBankField.text)
        stmt.setString(8, lokasiBankField.text)
        stmt.setString(9, namaPemilikField.text)
        stmt.setString(10, jabatanPemilikField.text)
        stmt.setString(11, logoPath)
        stmt.setInt(12, idPerusahaan)

        stmt.executeUpdate()
        conn.close()

        Alert(Alert.AlertType.INFORMATION, "Data perusahaan berhasil disimpan.").showAndWait()
    }

    private fun hapusData() {
        val confirm = Alert(
            Alert.AlertType.CONFIRMATION,
            "Hapus perusahaan ini?",
            ButtonType.YES,
            ButtonType.NO
        )
        val result = confirm.showAndWait()
        if (result.isPresent && result.get() == ButtonType.YES) {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("DELETE FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            stmt.executeUpdate()
            conn.close()
            Alert(Alert.AlertType.INFORMATION, "Perusahaan berhasil dihapus.").showAndWait()
        }
    }

    fun getView(): VBox {
        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(10.0)

        grid.add(Label("Nama"), 0, 0)
        grid.add(namaField, 1, 0)
        grid.add(Label("Alamat"), 0, 1)
        grid.add(alamatField, 1, 1)
        grid.add(Label("Telepon"), 0, 2)
        grid.add(teleponField, 1, 2)
        grid.add(Label("Fax"), 0, 3)
        grid.add(faxField, 1, 3)
        grid.add(Label("HP"), 0, 4)
        grid.add(hpField, 1, 4)
        grid.add(Label("No. Rekening"), 0, 5)
        grid.add(noRekField, 1, 5)
        grid.add(Label("Nama Bank"), 0, 6)
        grid.add(namaBankField, 1, 6)
        grid.add(Label("Lokasi Kantor Bank"), 0, 7)
        grid.add(lokasiBankField, 1, 7)
        grid.add(Label("Nama Pemilik"), 0, 8)
        grid.add(namaPemilikField, 1, 8)
        grid.add(Label("Jabatan Pemilik"), 0, 9)
        grid.add(jabatanPemilikField, 1, 9)
        grid.add(gantiLogoBtn, 1, 10)
        grid.add(logoView, 1, 11)

        val simpanBtn = Button("Simpan")
        val hapusBtn = Button("Hapus")
        simpanBtn.setOnAction { simpanData() }
        hapusBtn.setOnAction { hapusData() }

        val buttonBox = HBox(10.0, simpanBtn, hapusBtn)
        val layout = VBox(15.0, grid, buttonBox)
        layout.padding = Insets(15.0)

        return layout
    }
}
