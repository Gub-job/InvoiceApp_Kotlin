package test

import utils.DatabaseUpdater

fun main() {
    println("Memulai update database...")
    DatabaseUpdater.addSingkatanColumn()
    println("Update database selesai.")
}