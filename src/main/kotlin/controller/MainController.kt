package controller

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.Modality
import javafx.stage.Stage
import controller.PelangganController
import controller.ProdukController
import controller.PerusahaanTabelController
import controller.ProformaController
import ui.InvoiceScreen
import controller.PerusahaanController

class MainController {
    @FXML
    private lateinit var mainPane: BorderPane

    companion object {
        var idPerusahaanAktif: Int = 0
    }

    fun onKembaliClicked() {
        mainPane.center = Label("Selamat datang di Invoqr").apply {
            style = "-fx-font-size: 16px; -fx-padding: 20;"
        }
    }

    fun bukaPerusahaan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/PerusahaanTableView.fxml"))
            val content = loader.load<VBox>()
            val controller = loader.getController<controller.PerusahaanTabelController>()
            controller.setPerusahaanId(idPerusahaanAktif) // kirim ID perusahaan aktif
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Kesalahan"
            alert.headerText = "Gagal membuka detail perusahaan"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaPelanggan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            // Load FXML
            val loader = FXMLLoader(javaClass.getResource("/view/PelangganView.fxml"))
            val root = loader.load<Parent>()

            // Ambil controllernya
            val controller = loader.getController<controller.PelangganController>()
            controller.setPerusahaanId(idPerusahaanAktif)

            // Tampilkan di mainPane
            mainPane.center = root

        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "Gagal membuka halaman pelanggan"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaProduk(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            // Load FXML
            val loader = FXMLLoader(javaClass.getResource("/view/ProdukView.fxml"))
            val root = loader.load<Parent>()

            // Ambil controller dari FXML
            val controller = loader.getController<controller.ProdukController>()
            controller.setPerusahaanId(idPerusahaanAktif)

            // Tampilkan ke dalam mainPane
            mainPane.center = root

        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "Gagal membuka halaman produk"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaTransaksi() {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Pilih Jenis Transaksi"
        alert.headerText = "Silakan pilih jenis transaksi yang ingin dibuat:"
        alert.contentText = "Proforma atau Invoice?"

        val btnProforma = ButtonType("Proforma")
        val btnInvoice = ButtonType("Invoice")
        val btnBatal = ButtonType.CANCEL

        alert.buttonTypes.setAll(btnProforma, btnInvoice, btnBatal)

        val result = alert.showAndWait()
        if (result.isPresent) {
            when (result.get()) {
                btnProforma -> {
                    val loader = FXMLLoader(javaClass.getResource("/view/Proforma.fxml"))
                    val view = loader.load<VBox>()
                    val controller = loader.getController<controller.ProformaController>()
                    controller.setIdPerusahaan(idPerusahaanAktif)
                    showScreen(view)
                }
                btnInvoice -> {
                    // sementara tetap pakai yang lama, nanti bisa kamu ubah juga kalau Invoice dipisah ke FXML
                    showScreen(InvoiceScreen(idPerusahaanAktif).getView())
                }
                btnBatal -> return
            }
        }
    }

    private fun showScreen(content: Node) {
        mainPane.center = content
    }

    fun bukaLaporan(event: ActionEvent) {
        println("Buka laporan penjualan")
    }

    fun onGantiPerusahaanClicked() {
        try {
            val loader = FXMLLoader(javaClass.getResource("/view/PerusahaanView.fxml"))
            val root = loader.load<Parent>()

            // ambil controller dari FXML
            val controller = loader.getController<PerusahaanController>()

            // set callback biar idPerusahaanAktif berubah
            controller.setOnPerusahaanSelected { idTerpilih ->
                idPerusahaanAktif = idTerpilih
                println("Perusahaan aktif sekarang: $idPerusahaanAktif")
            }

            val perusahaanStage = Stage()
            perusahaanStage.initModality(Modality.APPLICATION_MODAL)
            perusahaanStage.title = "Invoqr"
            perusahaanStage.scene = Scene(root)
            perusahaanStage.showAndWait()

            // tampilkan hasil pilihan
            if (idPerusahaanAktif != 0) {
                println("Perusahaan dipilih ulang: $idPerusahaanAktif")
            } else {
                println("Tidak ada perusahaan dipilih ulang.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "Gagal membuka jendela perusahaan"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }
}
