package utils

import java.sql.SQLException

object DatabaseUpdater {
    
    fun addSingkatanColumn() {
        try {
            val conn = DatabaseHelper.getConnection()
            
            // Cek apakah kolom singkatan sudah ada
            val checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('perusahaan') 
                WHERE name = 'singkatan'
            """)
            val rs = checkStmt.executeQuery()
            rs.next()
            val columnExists = rs.getInt("count") > 0
            
            if (!columnExists) {
                // Tambahkan kolom singkatan
                val alterStmt = conn.prepareStatement("ALTER TABLE perusahaan ADD COLUMN singkatan TEXT")
                alterStmt.executeUpdate()
                println("Kolom singkatan berhasil ditambahkan ke tabel perusahaan")
            } else {
                println("Kolom singkatan sudah ada di tabel perusahaan")
            }
            
            conn.close()
        } catch (e: SQLException) {
            println("Error saat menambahkan kolom singkatan: ${e.message}")
        }
    }
}