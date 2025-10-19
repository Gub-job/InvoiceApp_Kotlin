plugins {
    kotlin("jvm") version "2.2.20"
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("main.MainKt")

    applicationDefaultJvmArgs = listOf(
        "--module-path", """D:\DOWN_APK\javafx-sdk-17.0.16\lib""",
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}