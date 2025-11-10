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
    implementation("org.openjfx:javafx-controls:17.0.16")
    implementation("org.openjfx:javafx-fxml:17.0.16")

    // Dependensi untuk membuat PDF
    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")

    // Dependensi untuk Print Preview (PDFBox)
    implementation("org.apache.pdfbox:pdfbox:2.0.30")
    implementation("org.openjfx:javafx-swing:17.0.10") // Sesuaikan versi dengan javafx Anda

    // Dependensi untuk ekspor ke Excel (Apache POI)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

application {
    mainClass.set("main.MainKt")
}