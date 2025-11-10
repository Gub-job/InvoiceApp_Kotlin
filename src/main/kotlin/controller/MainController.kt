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
import controller.PerusahaanController
import controller.DaftarProformaController
import controller.DaftarInvoiceController
import controller.LaporanPenjualanController

class MainController {
    @FXML
    private lateinit var mainPane: BorderPane

    private var initialCenterNode: Node? = null

    companion object {
        var idPerusahaanAktif: Int = 0
        var namaAdmin: String = "Admin" // Default nama admin
    }

    @FXML
    fun initialize() {
        // Simpan konten awal dari <center>
        initialCenterNode = mainPane.center
    }

    fun onKembaliClicked() {
        // Kembalikan konten ke tampilan awal
        mainPane.center = initialCenterNode
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
            val loader = FXMLLoader(javaClass.getResource("/view/PerusahaanTableView.fxml")) //
            val content = loader.load<ScrollPane>() // Mengubah tipe menjadi ScrollPane
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

    fun bukaInputPenjualan() {
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
                    val controller = loader.getController<ProformaController>()
                    controller.setIdPerusahaan(idPerusahaanAktif)
                    showScreen(view)
                }
                btnInvoice -> {
                    val loader = FXMLLoader(javaClass.getResource("/view/Invoice.fxml"))
                    val view = loader.load<VBox>()
                    val controller = loader.getController<InvoiceController>()
                    controller.setIdPerusahaan(idPerusahaanAktif)
                    showScreen(view)
                }
                btnBatal -> return
            }
        }
    }

    fun bukaDaftarProforma() {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan!"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/DaftarProforma.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<DaftarProformaController>()
            controller.setIdPerusahaan(idPerusahaanAktif)
            controller.setMainController(this)
            showScreen(view)
        } catch (e: Exception) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error!"
            alert.headerText = "Gagal membuka daftar proforma"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaDaftarInvoice() {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan!"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/DaftarInvoiceView.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<DaftarInvoiceController>()
            controller.setIdPerusahaan(idPerusahaanAktif)
            controller.setMainController(this)
            showScreen(view)
        } catch (e: Exception) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error!"
            alert.headerText = "Gagal membuka daftar invoice"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun showScreen(content: Node) {
        mainPane.center = content
    }

    fun bukaLaporan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu.")
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/LaporanPenjualanView.fxml"))
            val content = loader.load<VBox>()
            val controller = loader.getController<LaporanPenjualanController>()
            controller.setPerusahaanId(idPerusahaanAktif, namaAdmin)
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka halaman laporan penjualan: ${e.message}")
        }
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

    fun bukaPengaturan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            println("DEBUG: Memulai bukaPengaturan...")
            val resourceUrl = javaClass.getResource("/view/PengaturanView.fxml")
            println("DEBUG: Mencari resource di '/view/PengaturanView.fxml'. Hasil: $resourceUrl")

            if (resourceUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Resource Error", "File FXML '/view/PengaturanView.fxml' tidak dapat ditemukan di classpath.")
                return
            }

            val loader = FXMLLoader(resourceUrl)
            val content = loader.load<VBox>()
            println("DEBUG: FXML berhasil di-load.")
            val controller = loader.getController<PengaturanController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka halaman pengaturan: ${e.message}")
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
