package test

import utils.DatabaseHelper  // tambahkan ini

fun main() {
    // Tes koneksi ke database
    try {
        val conn = DatabaseHelper.getConnection()  // gunakan langsung object-nya
        println("Koneksi berhasil: ${conn != null}")
        conn.close()
    } catch (e: Exception) {
        e.printStackTrace()
        println("Koneksi gagal: ${e.message}")
    }
}
