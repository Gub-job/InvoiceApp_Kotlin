package utils

import com.itextpdf.io.source.ByteArrayOutputStream
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.Alert
import javafx.scene.control.Tooltip
import javafx.scene.control.ToolBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import model.DocumentData
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.awt.print.PrinterJob
import java.io.FileInputStream

class PrintPreview(private val data: DocumentData, private val owner: Window) {

    private val stage = Stage()
    private val pdfBytes: ByteArray

    init {
        // 1. Generate PDF ke memory
        val baos = ByteArrayOutputStream()
        TemplatePdfGenerator.generatePdf(data, baos) // UBAH DI SINI
        pdfBytes = baos.toByteArray()
    }

    fun show() {
        stage.initOwner(owner)
        stage.initModality(Modality.WINDOW_MODAL)
        stage.title = "Pratinjau Cetak - ${data.nomorDokumen}"
        stage.isMaximized = true // Buka dalam mode maximized

        val root = BorderPane()

        // 2. Render PDF menjadi gambar dengan ukuran yang pas
        val previewContainer = VBox(10.0).apply {
            style = "-fx-background-color: #E0E0E0;"
            padding = Insets(10.0)
        }
        try {
            val document = PDDocument.load(pdfBytes)
            val renderer = PDFRenderer(document)
            for (i in 0 until document.numberOfPages) {
                val image = renderer.renderImageWithDPI(i, 100f) // DPI lebih rendah untuk ukuran lebih kecil
                val fxImage = SwingFXUtils.toFXImage(image, null) 
                val imageView = ImageView(fxImage).apply {
                    style = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"
                    isPreserveRatio = true
                    fitWidth = 600.0 // Batasi lebar gambar
                }
                previewContainer.children.add(imageView)
            }
            document.close()
        } catch (e: Exception) {
            previewContainer.children.add(Label("Gagal memuat pratinjau: ${e.message}"))
            e.printStackTrace()
        }

        val scrollPane = ScrollPane(previewContainer).apply {
            isFitToWidth = true
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }
        root.center = scrollPane

        // 3. Buat ToolBar dengan tombol aksi dan zoom
        val btnZoomFit = Button("Pas Layar").apply {
            setOnAction { 
                previewContainer.children.filterIsInstance<ImageView>().forEach {
                    it.fitWidth = scrollPane.width - 50
                }
            }
        }

        val btnZoom100 = Button("100%").apply {
            setOnAction { 
                previewContainer.children.filterIsInstance<ImageView>().forEach {
                    it.fitWidth = 595.0 // A4 width
                }
            }
        }

        val btnCetak = Button("Cetak").apply {
            setOnAction { printPdf() }
        }

        val btnSimpan = Button("Simpan PDF").apply {
            setOnAction { savePdf() }
        }

        val btnTutup = Button("Tutup").apply {
            setOnAction { stage.close() }
        }

        val toolBar = ToolBar(btnZoomFit, btnZoom100, btnCetak, btnSimpan, btnTutup)
        root.top = toolBar

        stage.scene = Scene(root)
        stage.show()
    }

    private fun createIcon(fileName: String): ImageView {
        // Fungsi ini membuat placeholder. Anda harus meletakkan file gambar
        // (print.png, save.png, close.png) di folder resources/icons/
        return try {
            val url = javaClass.getResource("/icons/$fileName")?.toExternalForm()
            ImageView(Image(url, 16.0, 16.0, true, true))
        } catch (e: Exception) {
            // Fallback jika ikon tidak ditemukan
            ImageView()
        }
    }

    private fun printPdf() {
        try {
            val document = PDDocument.load(pdfBytes)
            val printerJob = PrinterJob.getPrinterJob()

            if (printerJob.printDialog()) { // Tampilkan dialog printer
                printerJob.setPageable(PDFPageable(document))
                printerJob.print()
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Dokumen telah dikirim ke printer.")
            }
            document.close()
        } catch (e: Exception) {
            showAlert(Alert.AlertType.ERROR, "Gagal Mencetak", "Terjadi kesalahan saat mencetak: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun savePdf() {
        val fileChooser = FileChooser().apply {
            title = "Simpan sebagai PDF"
            initialFileName = "${data.nomorDokumen.replace("/", "_")}.pdf"
            extensionFilters.add(FileChooser.ExtensionFilter("PDF Files", "*.pdf"))
        }
        val file = fileChooser.showSaveDialog(stage)

        if (file != null) {
            try {
                file.writeBytes(pdfBytes)
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "File PDF berhasil disimpan di:\n${file.absolutePath}")
                stage.close() // Tutup setelah berhasil menyimpan
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Gagal Menyimpan", "Gagal menyimpan file PDF: ${e.message}")
            }
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        Alert(type).apply {
            this.title = title
            this.headerText = null
            this.contentText = message
            initOwner(stage)
            showAndWait()
        }
    }
}
