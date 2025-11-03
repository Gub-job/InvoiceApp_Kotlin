import java.sql.DriverManager

fun main() {
    val conn = DriverManager.getConnection("jdbc:sqlite:src/main/resources/database/invoice_app.db")
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
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        conn.close()
    }
}