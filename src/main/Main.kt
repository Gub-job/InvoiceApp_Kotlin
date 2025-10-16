package main

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class MainApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/view/MainView.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "Aplikasi Invoice Penjualan"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
