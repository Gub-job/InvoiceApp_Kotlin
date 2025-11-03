package test

import utils.DatabaseHelper

fun main() {
    val conn = DatabaseHelper.getConnection()
    try {
        val stmt = conn.prepareStatement("SELECT id, nama, invoice_format, proforma_format, singkatan FROM perusahaan")
        val rs = stmt.executeQuery()
        
        while (rs.next()) {
            println("=== PERUSAHAAN ID: ${rs.getInt("id")} ===")
            println("Nama: ${rs.getString("nama")}")
            println("Singkatan: '${rs.getString("singkatan") ?: "NULL"}'")
            println("Invoice Format: '${rs.getString("invoice_format") ?: "NULL"}'")
            println("Proforma Format: '${rs.getString("proforma_format") ?: "NULL"}'")
            println()
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        conn.close()
    }
}