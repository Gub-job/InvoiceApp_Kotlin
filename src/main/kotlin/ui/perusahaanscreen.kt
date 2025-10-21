package ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.stage.FileChooser
import javafx.stage.Modality
import utils.DatabaseHelper
import java.io.File
import javafx.beans.binding.Bindings
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PerusahaanScreen(private val onPerusahaanSelected: (Int) -> Unit) : Stage() {

    private val btnTambah = Button("+")
    private val logoButtons = mutableListOf<Button>()
    private val flowPane = FlowPane()

    init {
        title = "Pilih Perusahaan"
        width = 600.0
        height = 450.0
        isResizable = true
        minWidth = 500.0
        minHeight = 400.0

        // Styling constants
        val primaryColor = "#2196F3"
        val secondaryColor = "#FFFFFF"
        val fontFamily = "Segoe UI"

        // Label Judul
        val labelJudul = Label("Pilih Perusahaan").apply {
            font = Font.font(fontFamily, 20.0)
            style = "-fx-font-weight: bold; -fx-text-fill: $primaryColor;"
            maxWidth = Double.MAX_VALUE
            alignment = Pos.CENTER
            padding = Insets(20.0, 0.0, 20.0, 0.0)
        }

        // Tombol Tambah
        btnTambah.apply {
            prefWidth = 140.0
            prefHeight = 140.0
            font = Font.font(fontFamily, 48.0)
            style = """
                -fx-background-color: $primaryColor;
                -fx-text-fill: $secondaryColor;
                -fx-font-weight: bold;
                -fx-background-radius: 12px;
                -fx-cursor: hand;
            """
            setOnMouseEntered { style += "-fx-background-color: #1976D2;" }
            setOnMouseExited { style = style.replace("-fx-background-color: #1976D2;", "-fx-background-color: $primaryColor;") }
            setOnAction { tambahPerusahaan() }
        }

         // FlowPane untuk menampilkan tombol-tombol
        flowPane.apply {
            hgap = 20.0
            vgap = 20.0
            alignment = Pos.CENTER
            padding = Insets(20.0)
            prefWrapLength = 400.0
            style = "-fx-background-color: $secondaryColor;"
        }

        // ScrollPane untuk menampung flowPane
        val scrollPane = ScrollPane(flowPane).apply {
            isFitToWidth = true
            style = "-fx-background-color: transparent; -fx-background: $secondaryColor;"
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        // Conditional fitToHeight for vertical centering when content is smaller
        scrollPane.fitToHeightProperty().bind(
            Bindings.createBooleanBinding(
                { flowPane.prefHeight <= scrollPane.viewportBounds.height },
                flowPane.prefHeightProperty(),
                scrollPane.viewportBoundsProperty()
            )
        )

        // Layout utama
        val root = BorderPane().apply {
            padding = Insets(10.0)
            top = labelJudul
            center = scrollPane
            style = """
                -fx-background-color: $secondaryColor;
                -fx-border-color: #E0E0E0;
                -fx-border-width: 1px;
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
            """
        }

        muatPerusahaan()

        scene = Scene(root)
        show()
        centerOnScreen()
    }

    private fun muatPerusahaan() {
        flowPane.children.clear()
        logoButtons.clear()

        // Tambahkan tombol tambah
        flowPane.children.add(btnTambah)

        // Load perusahaan dari database
        DatabaseHelper.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT id, nama, logo_path FROM perusahaan").use { rs ->
                    while (rs.next()) {
                        val id = rs.getInt("id")
                        val nama = rs.getString("nama")
                        val logoPath = rs.getString("logo_path")

                        val logoButton = buatTombolLogo(id, nama, logoPath)
                        logoButtons.add(logoButton)
                        flowPane.children.add(logoButton)
                    }
                }
            }
        }
    }

    private fun buatTombolLogo(id: Int, nama: String, logoPath: String?): Button {
        val button = Button().apply {
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
            
            // Coba load logo
            if (!logoPath.isNullOrBlank() && File(logoPath).exists()) {
                try {
                    val imageView = ImageView(Image(File(logoPath).toURI().toString())).apply {
                        fitHeight = 100.0
                        fitWidth = 100.0
                        isPreserveRatio = true
                    }
                    
                    val vbox = VBox(5.0, imageView, Label(nama).apply {
                        style = "-fx-font-size: 10px; -fx-text-fill: #666;"
                        maxWidth = 130.0
                        alignment = Pos.CENTER
                    }).apply {
                        alignment = Pos.CENTER
                    }
                    
                    graphic = vbox
                } catch (e: Exception) {
                    text = nama
                    style += "-fx-font-size: 12px; -fx-text-fill: #333;"
                }
            } else {
                text = nama
                style += "-fx-font-size: 12px; -fx-text-fill: #333; -fx-wrap-text: true;"
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
                onPerusahaanSelected(id)
                close()
            }
        }
        
        return button
    }

    private fun tambahPerusahaan() {
        val dialog = Stage().apply {
            title = "Tambah Perusahaan Baru"
            initModality(Modality.APPLICATION_MODAL)
            initOwner(this@PerusahaanScreen)
            isResizable = false
        }

        // Input fields
        val txtNama = TextField()
        val txtAlamat = TextArea().apply { 
            promptText = "Alamat Lengkap"
            prefRowCount = 2
        }
        val txtTelepon = TextField()
        val txtFax = TextField()
        val txtHP = TextField()
        val txtNoRek = TextField()
        val txtNamaBank = TextField()
        val txtLokasiBank = TextField()
        val txtNamaPemilik = TextField()
        val txtJabatanPemilik = TextField()
        
        var selectedLogoFile: File? = null
        val lblLogoInfo = Label("Belum ada logo dipilih")
        val btnPilihLogo = Button("Pilih Logo").apply {
            style = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;"
            setOnAction {
                val fileChooser = FileChooser().apply {
                    title = "Pilih Logo Perusahaan"
                    extensionFilters.addAll(
                        FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
                    )
                }
                fileChooser.showOpenDialog(dialog)?.let { file ->
                    selectedLogoFile = file
                    lblLogoInfo.text = file.name
                    lblLogoInfo.style = "-fx-text-fill: #4CAF50;"
                }
            }
        }

        // Form layout
        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 12.0
            padding = Insets(20.0)
            style = "-fx-font-family: 'Segoe UI';"
        }

        // Add labels and fields to grid
        var row = 0
        grid.add(Label("Nama Perusahaan:"), 0, row)
        grid.add(txtNama, 1, row++)
        
        grid.add(Label("Alamat:"), 0, row)
        grid.add(txtAlamat, 1, row++)
        
        grid.add(Label("Telepon:"), 0, row)
        grid.add(txtTelepon, 1, row++)
        
        grid.add(Label("Fax:"), 0, row)
        grid.add(txtFax, 1, row++)
        
        grid.add(Label("HP:"), 0, row)
        grid.add(txtHP, 1, row++)
        
        grid.add(Label("No. Rekening:"), 0, row)
        grid.add(txtNoRek, 1, row++)
        
        grid.add(Label("Nama Bank:"), 0, row)
        grid.add(txtNamaBank, 1, row++)
        
        grid.add(Label("Lokasi Kantor Bank:"), 0, row)
        grid.add(txtLokasiBank, 1, row++)
        
        grid.add(Label("Nama PIC:"), 0, row)
        grid.add(txtNamaPemilik, 1, row++)
        
        grid.add(Label("Jabatan PIC:"), 0, row)
        grid.add(txtJabatanPemilik, 1, row++)
        
        grid.add(Label("Logo:"), 0, row)
        val logoBox = VBox(5.0, btnPilihLogo, lblLogoInfo)
        grid.add(logoBox, 1, row++)

        // Set column constraints
        grid.columnConstraints.addAll(
            ColumnConstraints(120.0),
            ColumnConstraints(300.0)
        )

        // Buttons
        val btnSimpan = Button("Simpan").apply {
            prefWidth = 100.0
            style = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;"
            setOnAction {
                if (txtNama.text.isBlank()) {
                    Alert(Alert.AlertType.WARNING, "Nama perusahaan harus diisi!").apply {
                        headerText = "Validasi"
                    }.showAndWait()
                    return@setOnAction
                }

                try {
                    // Copy logo file if selected
                    var logoPath: String? = null
                    selectedLogoFile?.let { file ->
                        val logoDir = File("logos")
                        if (!logoDir.exists()) logoDir.mkdirs()
                        
                        val timestamp = System.currentTimeMillis()
                        val extension = file.extension
                        val newFileName = "logo_${timestamp}.${extension}"
                        val destFile = File(logoDir, newFileName)
                        
                        Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        logoPath = destFile.absolutePath
                    }

                    // Insert to database
                    DatabaseHelper.getConnection().use { conn ->
                        val sql = """
                            INSERT INTO perusahaan 
                            (nama, alamat, telepon, fax, HP, no_rek, nama_bank, 
                             lokasi_kantor_bank, nama_pemilik, jabatan_pemilik, logo_path) 
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """
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
                    
                    Alert(Alert.AlertType.INFORMATION, "Perusahaan '${txtNama.text}' berhasil ditambahkan!").apply {
                        headerText = "Sukses"
                    }.showAndWait()
                    
                } catch (e: Exception) {
                    Alert(Alert.AlertType.ERROR, "Error: ${e.message}").apply {
                        headerText = "Gagal Menyimpan"
                    }.showAndWait()
                    e.printStackTrace()
                }
            }
        }

        val btnBatal = Button("Batal").apply {
            prefWidth = 100.0
            style = "-fx-background-color: #757575; -fx-text-fill: white; -fx-cursor: hand;"
            setOnAction { dialog.close() }
        }

        val buttonBox = HBox(10.0, btnSimpan, btnBatal).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(10.0, 20.0, 10.0, 20.0)
        }

        // Scroll pane for form
        val scrollPane = ScrollPane(grid).apply {
            isFitToWidth = true
            style = "-fx-background-color: white;"
        }

        val root = BorderPane().apply {
            center = scrollPane
            bottom = buttonBox
            style = "-fx-background-color: white;"
        }

        dialog.scene = Scene(root, 500.0, 600.0)
        dialog.showAndWait()
    }
}