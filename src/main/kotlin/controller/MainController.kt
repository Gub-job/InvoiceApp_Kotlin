package controller

import javafx.event.ActionEvent
import javafx.scene.control.Label
import javafx.fxml.FXML
import javafx.scene.layout.BorderPane
import javafx.fxml.FXMLLoader
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.Parent
import javafx.scene.Scene
import ui.PelangganScreen
import ui.PerusahaanScreen
import ui.ProdukScreen
import javafx.scene.control.Button
import ui.PerusahaanTableScreen
import ui.ProformaScreen
import ui.InvoiceScreen

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
            println("Buka detail perusahaan aktif")

            if (idPerusahaanAktif == 0) {
                // kalau belum ada perusahaan dipilih
                val alert = javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
                )
                alert.showAndWait()
                return
            }

            val perusahaanDetail = ui.PerusahaanTableScreen(idPerusahaanAktif)
            val content = perusahaanDetail.getView()
            mainPane.center = content
        }


        fun bukaPelanggan(event: ActionEvent) {
            val pelangganScreen = PelangganScreen(idPerusahaanAktif)
            val root = pelangganScreen.getView()
            mainPane.center = root   // asumsi mainPane itu BorderPane utama
        }

         fun bukaProduk(event: ActionEvent) {
            println(idPerusahaanAktif)
            println("Buka form produk")

            val produkScreen = ProdukScreen(idPerusahaanAktif)
            val node = produkScreen.createContent()

            mainPane.center = node
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
                    btnProforma -> showScreen(ui.ProformaScreen(idPerusahaanAktif).getView())
                    btnInvoice -> showScreen(ui.InvoiceScreen(idPerusahaanAktif).getView())
                    btnBatal -> return
                }
            }
        }

        private fun showScreen(content: javafx.scene.Node) {
            mainPane.center = content
        }


        fun bukaLaporan(event: ActionEvent) {
            println("Buka laporan penjualan")
        }

        fun onGantiPerusahaanClicked() {
        val perusahaanScreen = PerusahaanScreen { id ->
            println("Perusahaan dipilih ulang: $id")
            idPerusahaanAktif = id
        }
        perusahaanScreen.show()
    }
}