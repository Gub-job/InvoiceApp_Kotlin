plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // JavaFX
    implementation("org.openjfx:javafx-controls:21.0.1")
    implementation("org.openjfx:javafx-fxml:21.0.1")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // PDF Export (OpenPDF)
    implementation("com.github.librepdf:openpdf:1.3.30")

    // Excel Export (Apache POI)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}

application {
    mainClass.set("main.MainKt")

    // Jalankan JavaFX
    applicationDefaultJvmArgs = listOf(
        "--module-path", "PATH_JAVAFX_LIB",
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}