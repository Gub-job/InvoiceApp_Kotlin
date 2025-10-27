package controller

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.stage.*
import utils.DatabaseHelper
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PerusahaanController {

    @FXML
    private lateinit var flowPane: FlowPane

    @FXML
    private lateinit var scrollPane: ScrollPane

    private val btnTambah = Button("+")
    private val logoButtons = mutableListOf<Button>()

    private val primaryColor = "#2196F3"
    private val secondaryColor = "#FFFFFF"

    private var onPerusahaanSelected: ((Int) -> Unit)? = null

    // Penambahan: Properti untuk melacak ID terpilih, mulai null
    var selectedId: Int? = null

    fun setOnPerusahaanSelected(callback: (Int) -> Unit) {
        onPerusahaanSelected = callback
    }

    @FXML
    fun initialize() {
        setupTambahButton()
        muatPerusahaan()
    }

    private fun setupTambahButton() {
        btnTambah.apply {
            prefWidth = 140.0
            prefHeight = 140.0
            text = "+"
            style = """
                -fx-background-color: $primaryColor;
                -fx-text-fill: $secondaryColor;
                -fx-font-size: 48px;
                -fx-font-weight: bold;
                -fx-background-radius: 12px;
                -fx-cursor: hand;
            """
            setOnMouseEntered { style += "-fx-background-color: #1976D2;" }
            setOnMouseExited { style = style.replace("-fx-background-color: #1976D2;", "-fx-background-color: $primaryColor;") }
            setOnAction { tambahPerusahaan() }
        }
    }

    private fun muatPerusahaan() {
        flowPane.children.clear()
        logoButtons.clear()
        flowPane.children.add(btnTambah)

        DatabaseHelper.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT id, nama, logo_path FROM perusahaan").use { rs ->
                    while (rs.next()) {
                        val id = rs.getInt("id")
                        val nama = rs.getString("nama")
                        val logoPath = rs.getString("logo_path")
                        val button = buatTombolLogo(id, nama, logoPath)
                        logoButtons.add(button)
                        flowPane.children.add(button)
                    }
                }
            }
        }
    }

    private fun buatTombolLogo(id: Int, nama: String, logoPath: String?): Button {
        val btn = Button().apply {
            prefWidth = 140.0
            prefHeight = 140.0
            style = """
                -fx-background-color: #F5F5F5;
                -fx-border-color: #E0E0E0;
                -fx-border-width: 2px;
                -fx-background-radius: 12px;
                -fx-border-radius: 12px;
                -fx-cursor: hand;
            """
            if (!logoPath.isNullOrBlank() && File(logoPath).exists()) {
                val image = ImageView(Image(File(logoPath).toURI().toString())).apply {
                    fitHeight = 100.0
                    fitWidth = 100.0
                    isPreserveRatio = true
                }
                val label = Label(nama).apply {
                    style = "-fx-font-size: 10px; -fx-text-fill: #666;"
                    maxWidth = 130.0
                    alignment = javafx.geometry.Pos.CENTER
                }
                val vbox = VBox(5.0, image, label).apply {
                    alignment = javafx.geometry.Pos.CENTER
                }
                graphic = vbox
            } else {
                text = nama
                style += "-fx-font-size: 12px; -fx-text-fill: #333;"
            }

            setOnMouseEntered {
                style = style.replace("-fx-background-color: #F5F5F5;", "-fx-background-color: #E3F2FD;")
                style = style.replace("-fx-border-color: #E0E0E0;", "-fx-border-color: #2196F3;")
            }
            setOnMouseExited {
                style = style.replace("-fx-background-color: #E3F2FD;", "-fx-background-color: #F5F5F5;")
                style = style.replace("-fx-border-color: #2196F3;", "-fx-border-color: #E0E0E0;")
            }
            setOnAction {
                // Penambahan: Set selectedId sebelum tutup
                selectedId = id
                onPerusahaanSelected?.invoke(id)
                println("Perusahaan dipilih: $id")
                flowPane.scene.window.hide()
            }
        }
        return btn
    }

    private fun tambahPerusahaan() {
        val dialog = Stage().apply {
            title = "Tambah Perusahaan Baru"
            initModality(Modality.APPLICATION_MODAL)
            initOwner(flowPane.scene.window)
            isResizable = true  // Biar bisa di-resize pakai kursor
        }

        val txtNama = TextField()
        val txtAlamat = TextArea().apply { 
            promptText = "Alamat Lengkap"
            prefRowCount = 3
            prefWidth = 350.0
            isWrapText = true
        }
        val txtTelepon = TextField()
        val txtFax = TextField()
        val txtHP = TextField()
        val txtNoRek = TextField()
        val txtNamaBank = TextField()
        val txtLokasiBank = TextField()
        val txtNamaPemilik = TextField()
        val txtJabatanPemilik = TextField()
        val lblLogo = Label("Belum ada logo dipilih")
        var selectedLogoFile: File? = null

        val btnPilihLogo = Button("Pilih Logo").apply {
            style = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;"
            setOnAction {
                val fc = FileChooser()
                fc.title = "Pilih Logo Perusahaan"
                fc.extensionFilters.add(FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"))
                fc.showOpenDialog(dialog)?.let {
                    selectedLogoFile = it
                    lblLogo.text = it.name
                    lblLogo.style = "-fx-text-fill: #4CAF50;"
                }
            }
        }

        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = javafx.geometry.Insets(20.0)
            add(Label("Nama Perusahaan:"), 0, 0)
            add(txtNama, 1, 0)
            add(Label("Alamat:"), 0, 1)
            add(txtAlamat, 1, 1)
            add(Label("Telepon:"), 0, 2)
            add(txtTelepon, 1, 2)
            add(Label("Fax:"), 0, 3)
            add(txtFax, 1, 3)
            add(Label("HP:"), 0, 4)
            add(txtHP, 1, 4)
            add(Label("No. Rekening:"), 0, 5)
            add(txtNoRek, 1, 5)
            add(Label("Nama Bank:"), 0, 6)
            add(txtNamaBank, 1, 6)
            add(Label("Lokasi Bank:"), 0, 7)
            add(txtLokasiBank, 1, 7)
            add(Label("Nama Pemilik:"), 0, 8)
            add(txtNamaPemilik, 1, 8)
            add(Label("Jabatan Pemilik:"), 0, 9)
            add(txtJabatanPemilik, 1, 9)
            add(Label("Logo:"), 0, 10)
            add(VBox(5.0, btnPilihLogo, lblLogo), 1, 10)
        }

        val btnSimpan = Button("Simpan").apply {
            style = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;"
            setOnAction {
                if (txtNama.text.isBlank()) {
                    Alert(Alert.AlertType.WARNING, "Nama perusahaan wajib diisi!").show()
                    return@setOnAction
                }

                var logoPath: String? = null
                selectedLogoFile?.let { file ->
                    val dir = File("logos")
                    if (!dir.exists()) dir.mkdirs()
                    val dest = File(dir, "logo_${System.currentTimeMillis()}.${file.extension}")
                    Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    logoPath = dest.absolutePath
                }

                DatabaseHelper.getConnection().use { conn ->
                    val sql = """
                        INSERT INTO perusahaan 
                        (nama, alamat, telepon, fax, HP, no_rek, nama_bank, lokasi_kantor_bank, nama_pemilik, jabatan_pemilik, logo_path) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, txtNama.text)
                        stmt.setString(2, txtAlamat.text)
                        stmt.setString(3, txtTelepon.text)
                        stmt.setString(4, txtFax.text)
                        stmt.setString(5, txtHP.text)
                        stmt.setString(6, txtNoRek.text)
                        stmt.setString(7, txtNamaBank.text)
                        stmt.setString(8, txtLokasiBank.text)
                        stmt.setString(9, txtNamaPemilik.text)
                        stmt.setString(10, txtJabatanPemilik.text)
                        stmt.setString(11, logoPath)
                        stmt.executeUpdate()
                    }
                }
                muatPerusahaan()
                dialog.close()
                Alert(Alert.AlertType.INFORMATION, "Perusahaan '${txtNama.text}' berhasil ditambahkan!").show()
            }
        }

        val root = VBox(15.0, grid, btnSimpan).apply {
            padding = javafx.geometry.Insets(20.0)
            alignment = javafx.geometry.Pos.CENTER
        }

        dialog.scene = Scene(root, 600.0, 650.0)
        dialog.showAndWait()
    }

    companion object {
        fun tampilkan(onPerusahaanSelected: (Int) -> Unit) {
            val loader = FXMLLoader(PerusahaanController::class.java.getResource("PerusahaanView.fxml"))
            val root = loader.load<BorderPane>()
            val controller = loader.getController<PerusahaanController>()
            controller.setOnPerusahaanSelected(onPerusahaanSelected)
            val stage = Stage()
            stage.title = "Pilih Perusahaan"
            stage.scene = Scene(root)
            // Penambahan opsional: Handle close request di sini jika menggunakan companion
            stage.setOnCloseRequest {
                controller.selectedId = null  // Pastikan null jika ditutup manual
            }
            stage.show()
        }
    }
}