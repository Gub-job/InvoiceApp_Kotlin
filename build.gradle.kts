plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

javafx {
    version = "17"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("MainAppKt")

    applicationDefaultJvmArgs = listOf(
        "--module-path", "D:/JavaFX/javafx-sdk-17/lib",
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}