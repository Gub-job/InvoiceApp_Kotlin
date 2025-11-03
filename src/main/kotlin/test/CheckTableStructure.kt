package test

import utils.DatabaseHelper

fun main() {
    val conn = DatabaseHelper.getConnection()
    try {
        println("=== STRUKTUR TABEL PROFORMA ===")
        val stmt = conn.prepareStatement("PRAGMA table_info(proforma)")
        val rs = stmt.executeQuery()
        
        while (rs.next()) {
            val cid = rs.getInt("cid")
            val name = rs.getString("name")
            val type = rs.getString("type")
            val notnull = rs.getInt("notnull")
            val defaultValue = rs.getString("dflt_value")
            val pk = rs.getInt("pk")
            
            println("$cid: $name ($type) - NotNull: $notnull, Default: $defaultValue, PK: $pk")
        }
        
        println("\n=== STRUKTUR TABEL DETAIL_PROFORMA ===")
        val stmt2 = conn.prepareStatement("PRAGMA table_info(detail_proforma)")
        val rs2 = stmt2.executeQuery()
        
        while (rs2.next()) {
            val cid = rs2.getInt("cid")
            val name = rs2.getString("name")
            val type = rs2.getString("type")
            val notnull = rs2.getInt("notnull")
            val defaultValue = rs2.getString("dflt_value")
            val pk = rs2.getInt("pk")
            
            println("$cid: $name ($type) - NotNull: $notnull, Default: $defaultValue, PK: $pk")
        }
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        conn.close()
    }
}