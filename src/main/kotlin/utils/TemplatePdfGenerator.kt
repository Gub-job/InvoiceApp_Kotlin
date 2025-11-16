package utils

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import model.DocumentData
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO

object TemplatePdfGenerator {

    fun generatePdf(data: DocumentData, stream: OutputStream, idPerusahaan: Int = 1) {
        try {
            // 1. Muat FXML dan Controller
            val loader = FXMLLoader(javaClass.getResource("/view/InvoiceTemplate.fxml"))
            val root = loader.load<AnchorPane>()
            val controller = loader.getController<controller.template.InvoiceTemplateController>()

            // 2. Isi data ke template
            controller.populateData(data, idPerusahaan)

            // 3. Render scene dan tunggu selesai
            val scene = Scene(root)
            javafx.application.Platform.runLater {
                // Force layout update
                root.applyCss()
                root.layout()
            }
            Thread.sleep(100) // Tunggu rendering selesai
            
            val snapshot = root.snapshot(null, null)
            val bufferedImage = SwingFXUtils.fromFXImage(snapshot, null)

            // 4. Buat PDF dan masukkan gambar snapshot
            val writer = PdfWriter(stream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)
            document.setMargins(0f, 0f, 0f, 0f) // Hapus margin agar gambar pas

            val imageStream = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "png", imageStream)
            val imageData = ImageDataFactory.create(imageStream.toByteArray())
            document.add(Image(imageData).setAutoScale(true))

            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}