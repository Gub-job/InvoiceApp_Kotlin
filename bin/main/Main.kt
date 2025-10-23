package main

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import ui.PerusahaanScreen

class MainApp : Application() {
    override fun start(stage: Stage) {
        // Tampilkan dialog pilih perusahaan terlebih dahulu
        val perusahaanScreen = PerusahaanScreen { idPerusahaan ->
            println("Perusahaan dipilih: $idPerusahaan")

            // Setelah perusahaan dipilih, baru tampilkan MainView
            val fxmlLoader = FXMLLoader(javaClass.getResource("/view/MainView.fxml"))
            val scene = Scene(fxmlLoader.load())
            stage.title = "Invoqr"
            stage.scene = scene
            stage.minWidth = 800.0
            stage.minHeight = 600.0
            stage.isMaximized = true
            stage.show()
        }

        perusahaanScreen.show() // tampilkan dialog pilih perusahaan
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
