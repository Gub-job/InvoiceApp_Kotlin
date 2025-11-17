package main

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Modality
import javafx.stage.Stage
import controller.PerusahaanController

class MainApp : Application() {
    override fun start(stage: Stage) {
        // 1️⃣ Load dan tampilkan modal pemilihan perusahaan
        val loader = FXMLLoader(javaClass.getResource("/view/PerusahaanView.fxml"))
        val perusahaanRoot = loader.load<javafx.scene.Parent>()
        val perusahaanController = loader.getController<PerusahaanController>()  // Asumsikan nama kelas controller Anda adalah PerusahaanController
        perusahaanController.setOnPerusahaanSelected { idTerpilih ->
            controller.MainController.idPerusahaanAktif = idTerpilih
            println("Perusahaan aktif pertama kali: $idTerpilih")
        }
        val perusahaanStage = Stage()

        perusahaanStage.initModality(Modality.APPLICATION_MODAL)
        perusahaanStage.title = "Pilih Perusahaan"
        perusahaanStage.scene = Scene(perusahaanRoot)
        perusahaanStage.showAndWait()

        // 2️⃣ Ambil ID terpilih langsung dari controller modal
        val idPerusahaanDipilih = perusahaanController.selectedId  // Ini harus null jika tidak ada pemilihan

        if (idPerusahaanDipilih != null) {
            println("Perusahaan dipilih: $idPerusahaanDipilih")

            // Opsional: Simpan ke MainController atau tempat lain jika dibutuhkan untuk tampilan utama
            // misalnya, MainController.idPerusahaanAktif = idPerusahaanDipilih

            // 3️⃣ Tampilkan tampilan utama
            val mainLoader = FXMLLoader(javaClass.getResource("/view/MainView.fxml"))
            val mainScene = Scene(mainLoader.load())

            stage.title = "Invoqr"
            stage.scene = mainScene
            stage.minWidth = 800.0
            stage.minHeight = 600.0
            stage.isMaximized = true
            
            // Konfirmasi keluar saat tombol X ditekan
            stage.setOnCloseRequest { event ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Konfirmasi Keluar"
                alert.headerText = "Yakin ingin keluar dari aplikasi?"
                alert.contentText = null
                
                val result = alert.showAndWait()
                if (result.get() != ButtonType.OK) {
                    event.consume() // Batalkan penutupan
                }
            }
            
            stage.show()
        } else {
            println("Tidak ada perusahaan dipilih, aplikasi ditutup.")
            Platform.exit()  // Keluar dari app
        }
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}