package controller

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.layout.BorderPane
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import ui.PelangganScreen
import ui.PerusahaanScreen


class MainController {
    @FXML
    private lateinit var mainPane: BorderPane

    companion object {
        var idPerusahaanAktif: Int = 0
    }

    fun bukaPerusahaan(event: ActionEvent) {
        println("Buka form perusahaan")
    }

    fun bukaPelanggan(event: ActionEvent) {
        val pelangganScreen = PelangganScreen(idPerusahaanAktif)
        val root = pelangganScreen.getView()
        mainPane.center = root   // asumsi mainPane itu BorderPane utama
    }

    fun bukaProduk(event: ActionEvent) {
        println("Buka form produk")
    }

    fun bukaTransaksi(event: ActionEvent) {
        println("Buka form transaksi")
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
