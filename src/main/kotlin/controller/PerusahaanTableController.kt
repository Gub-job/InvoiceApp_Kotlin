package controller

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.FileChooser
import model.Perusahaan
import utils.DatabaseHelper
import java.io.File

class PerusahaanTabelController {

    private var idPerusahaan: Int = 0
    private var perusahaan: Perusahaan? = null
    private var logoPath: String? = null

    @FXML private lateinit var namaField: TextField
    @FXML private lateinit var alamatField: TextArea
    @FXML private lateinit var teleponField: TextField
    @FXML private lateinit var faxField: TextField
    @FXML private lateinit var hpField: TextField
    @FXML private lateinit var noRekField: TextField
    @FXML private lateinit var namaBankField: TextField
    @FXML private lateinit var lokasiBankField: TextField
    @FXML private lateinit var namaPemilikField: TextField
    @FXML private lateinit var jabatanPemilikField: TextField
    @FXML private lateinit var adminField: TextField
    @FXML private lateinit var logoView: ImageView
    @FXML private lateinit var gantiLogoBtn: Button

    fun setPerusahaanId(id: Int) {
        this.idPerusahaan = id
        // Panggil loadData di sini, setelah idPerusahaan di-set.
        // Ini aman karena setPerusahaanId dipanggil setelah FXML selesai di-load.
        loadData()
    }

    @FXML
    fun initialize() {
        // Inisialisasi sekarang hanya untuk setup awal, seperti event handler.
        // setupGantiLogoButton() tidak perlu dipanggil di sini karena sudah diatur di FXML onAction
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
            adminField.text = rs.getString("nama_admin")
            logoPath = rs.getString("logo_path")

            if (!logoPath.isNullOrEmpty() && File(logoPath!!).exists()) {
                logoView.image = Image("file:$logoPath")
            }
        }
        conn.close()
    }

    @FXML // Ini adalah event handler untuk tombol, jadi @FXML tetap diperlukan
    private fun setupGantiLogoButton() {
        val fileChooser = FileChooser()
        fileChooser.title = "Pilih Logo Perusahaan"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg")
        )

        // Menggunakan showOpenDialog dari window yang ada untuk konsistensi
        val file = fileChooser.showOpenDialog(gantiLogoBtn.scene.window)
        if (file != null) {
            logoPath = file.absolutePath
            logoView.image = Image("file:${file.absolutePath}")
        }
    }

    @FXML
    private fun simpanData() {
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.prepareStatement(
            """UPDATE perusahaan SET 
                nama=?, alamat=?, telepon=?, fax=?, HP=?, no_rek=?,
                nama_bank=?, lokasi_kantor_bank=?, nama_pemilik=?,
                jabatan_pemilik=?, logo_path=?, nama_admin=? WHERE id=?"""
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
        stmt.setString(12, adminField.text)
        stmt.setInt(13, idPerusahaan)

        stmt.executeUpdate()
        conn.close()

        Alert(Alert.AlertType.INFORMATION, "Data perusahaan berhasil disimpan.").showAndWait()
    }

    @FXML
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
}
