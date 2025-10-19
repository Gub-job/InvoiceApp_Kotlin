package ui

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.stage.Stage
import utils.DatabaseHelper
import java.io.File

class PerusahaanScreen(private val onPerusahaanSelected: (Int) -> Unit) : Stage() {

    private val listView = ListView<String>()
    private val logoView = ImageView()
    private val btnTambah = Button("+")
    private val btnPilih = Button("Pilih")

    init {
        title = "Pilih Perusahaan"
        width = 600.0
        height = 450.0
        isResizable = true // Mengizinkan resize dan maximize
        minWidth = 500.0
        minHeight = 400.0 // Ukuran minimum untuk mencegah terlalu kecil

        // Styling constants
        val primaryColor = "#2196F3"
        val secondaryColor = "#FFFFFF"
        val fontFamily = "Segoe UI"

        // Label Judul
        val labelJudul = Label("Daftar Perusahaan").apply {
            font = Font.font(fontFamily, 20.0)
            style = "-fx-font-weight: bold; -fx-text-fill: $primaryColor;"
            maxWidth = Double.MAX_VALUE
            alignment = Pos.CENTER
            padding = Insets(10.0, 0.0, 10.0, 0.0) // Kurangi padding bawah
        }

        // ListView
        listView.apply {
            style = """
                -fx-background-color: $secondaryColor;
                -fx-border-color: #E0E0E0;
                -fx-border-radius: 5px;
                -fx-background-radius: 5px;
                -fx-font-family: '$fontFamily';
                -fx-font-size: 14px;
                -fx-padding: 5;
            """
            selectionModel.selectedItemProperty().addListener { _, _, _ ->
                tampilkanLogo()
            }
        }

        // Logo perusahaan
        logoView.apply {
            fitHeight = 120.0
            fitWidth = 120.0
            isPreserveRatio = true
            style = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0.2, 0, 0);"
        }

        val logoBox = VBox(logoView).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0)
            style = "-fx-background-color: #F5F5F5; -fx-background-radius: 8px;"
            maxHeight = 150.0 // Batasi tinggi logo box
        }

        // Tombol styling
        btnTambah.apply {
            prefWidth = 60.0
            prefHeight = 40.0
            font = Font.font(fontFamily, 18.0)
            style = """
                -fx-background-color: $primaryColor;
                -fx-text-fill: $secondaryColor;
                -fx-font-weight: bold;
                -fx-background-radius: 8px;
                -fx-cursor: hand;
            """
            setOnMouseEntered { style += "-fx-background-color: #1976D2;" }
            setOnMouseExited { style = style.replace("-fx-background-color: #1976D2;", "-fx-background-color: $primaryColor;") }
        }

        btnPilih.apply {
            prefWidth = 140.0
            prefHeight = 40.0
            font = Font.font(fontFamily, 14.0)
            style = """
                -fx-background-color: $primaryColor;
                -fx-text-fill: $secondaryColor;
                -fx-font-weight: bold;
                -fx-background-radius: 8px;
                -fx-cursor: hand;
            """
            setOnMouseEntered { style += "-fx-background-color: #1976D2;" }
            setOnMouseExited { style = style.replace("-fx-background-color: #1976D2;", "-fx-background-color: $primaryColor;") }
        }

        val tombolBox = HBox(20.0, btnTambah, btnPilih).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0) // Kurangi padding untuk lebih kompak
        }

        // Center layout dengan Vgrow untuk listView
        val centerVBox = VBox(10.0, listView, logoBox, tombolBox).apply { // Spacing lebih kecil
            alignment = Pos.TOP_CENTER // Ubah ke TOP_CENTER untuk menempel atas, hilangkan ruang kosong atas
            VBox.setVgrow(listView, Priority.ALWAYS) // ListView mengisi ruang vertikal yang tersedia
        }

        // Layout utama
        val root = BorderPane().apply {
            padding = Insets(10.0) // Kurangi padding untuk mengurangi ruang kosong
            top = labelJudul
            center = centerVBox
            style = """
                -fx-background-color: $secondaryColor;
                -fx-border-color: #E0E0E0; // Border lebih tipis dan lembut
                -fx-border-width: 1px;
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
            """
        }

        // Aksi tombol
        btnTambah.setOnAction { tambahPerusahaan() }
        btnPilih.setOnAction { pilihPerusahaan() }

        muatPerusahaan()

        scene = Scene(root).apply {
            stylesheets.add("""
                .list-cell:filled:selected {
                    -fx-background-color: $primaryColor;
                    -fx-text-fill: $secondaryColor;
                }
                .list-cell:filled:hover {
                    -fx-background-color: #E3F2FD;
                    -fx-text-fill: black;
                }
                .list-cell {
                    -fx-padding: 8px;
                    -fx-font-family: '$fontFamily';
                    -fx-font-size: 14px;
                }
            """.trimIndent())
        }

        show()
    }

    private fun muatPerusahaan() {
        val data = FXCollections.observableArrayList<String>()
        DatabaseHelper.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT id, nama FROM perusahaan").use { rs ->
                    while (rs.next()) {
                        data.add("${rs.getInt("id")} - ${rs.getString("nama")}")
                    }
                }
            }
        }
        listView.items = data
        if (data.isEmpty()) {
            // Tambahkan placeholder jika tidak ada data
            listView.placeholder = Label("Tidak ada perusahaan yang tersedia.").apply {
                padding = Insets(10.0)
                style = "-fx-text-fill: #f11717ff;"
            }
        }
    }

    private fun tambahPerusahaan() {
        val dialog = TextInputDialog().apply {
            title = "Tambah Perusahaan"
            headerText = "Masukkan Nama Perusahaan"
            contentText = "Nama:"
            graphic = null
            dialogPane.style = "-fx-font-family: 'Segoe UI';"
        }

        dialog.showAndWait().ifPresent { nama ->
            if (nama.isNotBlank()) {
                DatabaseHelper.getConnection().use { conn ->
                    conn.prepareStatement("INSERT INTO perusahaan (nama) VALUES (?)").use { stmt ->
                        stmt.setString(1, nama)
                        stmt.executeUpdate()
                    }
                }
                muatPerusahaan()
                Alert(Alert.AlertType.INFORMATION, "Perusahaan '$nama' berhasil ditambahkan!").apply {
                    headerText = "Sukses"
                    dialogPane.style = "-fx-font-family: 'Segoe UI';"
                }.showAndWait()
            }
        }
    }

    private fun pilihPerusahaan() {
        listView.selectionModel.selectedItem?.let { selected ->
            val id = selected.substringBefore(" - ").toInt()
            onPerusahaanSelected(id)
            close()
        } ?: Alert(Alert.AlertType.WARNING, "Pilih salah satu perusahaan terlebih dahulu!").apply {
            headerText = "Peringatan"
            dialogPane.style = "-fx-font-family: 'Segoe UI';"
        }.showAndWait()
    }

    private fun tampilkanLogo() {
        listView.selectionModel.selectedItem?.let { selected ->
            val id = selected.substringBefore(" - ").toInt()
            DatabaseHelper.getConnection().use { conn ->
                conn.prepareStatement("SELECT logo_path FROM perusahaan WHERE id = ?").use { stmt ->
                    stmt.setInt(1, id)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val path = rs.getString("logo_path")
                            if (!path.isNullOrBlank() && File(path).exists()) {
                                logoView.image = Image(File(path).toURI().toString())
                                btnPilih.isVisible = false
                                return
                            }
                        }
                    }
                }
            }
        }
        logoView.image = null
        btnPilih.isVisible = true
    }
}