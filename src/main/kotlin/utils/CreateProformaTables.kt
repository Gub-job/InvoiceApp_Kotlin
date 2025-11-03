package utils

import java.sql.SQLException

object CreateProformaTables {
    
    fun createTables() {
        try {
            val conn = DatabaseHelper.getConnection()
            
            // Cek dan tambahkan kolom yang hilang di tabel proforma
            addMissingColumns(conn)
            
            // Buat tabel proforma jika belum ada
            val createProformaTable = """
                CREATE TABLE IF NOT EXISTS proforma (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_perusahaan INTEGER NOT NULL,
                    id_pelanggan INTEGER NOT NULL,
                    nomor TEXT NOT NULL,
                    tanggal TEXT NOT NULL,
                    dp REAL DEFAULT 0.0,
                    tax REAL DEFAULT 0.0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """
            
            val createDetailProformaTable = """
                CREATE TABLE IF NOT EXISTS detail_proforma (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_proforma INTEGER NOT NULL,
                    id_produk INTEGER NOT NULL,
                    qty REAL NOT NULL,
                    harga REAL NOT NULL,
                    total REAL NOT NULL
                )
            """
            
            // Buat tabel invoice jika belum ada
            val createInvoiceTable = """
                CREATE TABLE IF NOT EXISTS invoice (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_perusahaan INTEGER NOT NULL,
                    id_pelanggan INTEGER NOT NULL,
                    nomor TEXT NOT NULL,
                    tanggal TEXT NOT NULL,
                    dp REAL DEFAULT 0.0,
                    tax REAL DEFAULT 0.0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """
            
            val createDetailInvoiceTable = """
                CREATE TABLE IF NOT EXISTS detail_invoice (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_invoice INTEGER NOT NULL,
                    id_produk INTEGER NOT NULL,
                    qty REAL NOT NULL,
                    harga REAL NOT NULL,
                    total REAL NOT NULL
                )
            """
            
            // Eksekusi semua query
            conn.createStatement().execute(createProformaTable)
            conn.createStatement().execute(createDetailProformaTable)
            conn.createStatement().execute(createInvoiceTable)
            conn.createStatement().execute(createDetailInvoiceTable)
            
            println("Tabel proforma, detail_proforma, invoice, dan detail_invoice berhasil dibuat/diupdate")
            conn.close()
            
        } catch (e: SQLException) {
            println("Error saat membuat tabel: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun addMissingColumns(conn: java.sql.Connection) {
        try {
            // Cek kolom tanggal di tabel proforma
            val checkTanggal = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('proforma') 
                WHERE name = 'tanggal'
            """)
            val rs1 = checkTanggal.executeQuery()
            rs1.next()
            if (rs1.getInt("count") == 0) {
                conn.createStatement().execute("ALTER TABLE proforma ADD COLUMN tanggal TEXT")
                println("Kolom tanggal ditambahkan ke tabel proforma")
            }
            
            // Cek kolom dp di tabel proforma
            val checkDp = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('proforma') 
                WHERE name = 'dp'
            """)
            val rs2 = checkDp.executeQuery()
            rs2.next()
            if (rs2.getInt("count") == 0) {
                conn.createStatement().execute("ALTER TABLE proforma ADD COLUMN dp REAL DEFAULT 0.0")
                println("Kolom dp ditambahkan ke tabel proforma")
            }
            
            // Cek kolom tax di tabel proforma
            val checkTax = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('proforma') 
                WHERE name = 'tax'
            """)
            val rs3 = checkTax.executeQuery()
            rs3.next()
            if (rs3.getInt("count") == 0) {
                conn.createStatement().execute("ALTER TABLE proforma ADD COLUMN tax REAL DEFAULT 0.0")
                println("Kolom tax ditambahkan ke tabel proforma")
            }
            
        } catch (e: Exception) {
            println("Error saat menambahkan kolom: ${e.message}")
        }
    }
}