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
                    total REAL DEFAULT 0.0, -- Tambahkan kolom total (subtotal)
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
                    no_invoice TEXT NOT NULL,
                    tanggal_invoice TEXT NOT NULL,
                    dp REAL NOT NULL DEFAULT 0.0,
                    tax REAL NOT NULL DEFAULT 0.0, 
                    total REAL NOT NULL DEFAULT 0.0,
                    total_dengan_ppn REAL NOT NULL DEFAULT 0.0,
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
            
            // Panggil fungsi untuk memastikan kolom-kolom baru ada di tabel invoice
            addMissingInvoiceColumns(conn)
            
            // Tambahkan kolom nama_admin ke tabel perusahaan
            addAdminNameToPerusahaan(conn)
            
            println("Tabel proforma, detail_proforma, invoice, dan detail_invoice berhasil dibuat/diupdate")
            conn.close()
            
        } catch (e: SQLException) {
            println("Error saat membuat tabel: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun addMissingColumns(conn: java.sql.Connection) {
        try {
            // Tambahkan kolom yang hilang di tabel proforma
            addColumnIfNotExists(conn, "proforma", "tanggal", "TEXT")
            addColumnIfNotExists(conn, "proforma", "dp", "REAL DEFAULT 0.0")
            addColumnIfNotExists(conn, "proforma", "tax", "REAL DEFAULT 0.0")
            addColumnIfNotExists(conn, "proforma", "divisi", "TEXT")
            
            // Tambahkan kolom yang hilang di tabel invoice
            addColumnIfNotExists(conn, "invoice", "id_perusahaan", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfNotExists(conn, "invoice", "id_pelanggan", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfNotExists(conn, "invoice", "no_invoice", "TEXT")
            addColumnIfNotExists(conn, "invoice", "tanggal_invoice", "TEXT")
            addColumnIfNotExists(conn, "invoice", "total", "REAL DEFAULT 0.0")
            addColumnIfNotExists(conn, "invoice", "tax", "REAL DEFAULT 0.0")
            addColumnIfNotExists(conn, "invoice", "total_dengan_ppn", "REAL DEFAULT 0.0")
            addColumnIfNotExists(conn, "invoice", "dp", "REAL DEFAULT 0.0")
            addColumnIfNotExists(conn, "invoice", "divisi", "TEXT")
            
        } catch (e: Exception) {
            println("Error saat menambahkan kolom: ${e.message}")
        }
    }
    
    private fun addColumnIfNotExists(conn: java.sql.Connection, tableName: String, columnName: String, columnType: String) {
        try {
            val checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('$tableName') 
                WHERE name = ?
            """)
            checkStmt.setString(1, columnName)
            val rs = checkStmt.executeQuery()
            rs.next()
            if (rs.getInt("count") == 0) {
                conn.createStatement().execute("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
                println("Kolom $columnName ditambahkan ke tabel $tableName")
            }
        } catch (e: Exception) {
            println("Error menambahkan kolom $columnName ke $tableName: ${e.message}")
        }
    }

    private fun addMissingInvoiceColumns(conn: java.sql.Connection) {
        try {
            // Cek kolom no_invoice
            if (!columnExists(conn, "invoice", "no_invoice")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN no_invoice TEXT")
                println("Kolom no_invoice ditambahkan ke tabel invoice")
            }
            // Cek kolom tanggal_invoice
            if (!columnExists(conn, "invoice", "tanggal_invoice")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN tanggal_invoice TEXT")
                println("Kolom tanggal_invoice ditambahkan ke tabel invoice")
            }
            // Cek kolom total_dengan_ppn
            if (!columnExists(conn, "invoice", "total_dengan_ppn")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN total_dengan_ppn REAL DEFAULT 0.0")
                println("Kolom total_dengan_ppn ditambahkan ke tabel invoice")
            }
            // Cek kolom total (subtotal)
            if (!columnExists(conn, "invoice", "total")) {
                conn.createStatement().execute("ALTER TABLE invoice ADD COLUMN total REAL DEFAULT 0.0")
                println("Kolom total ditambahkan ke tabel invoice")
            }
        } catch (e: Exception) {
            println("Error saat menambahkan kolom ke tabel invoice: ${e.message}")
        }
    }

    private fun columnExists(conn: java.sql.Connection, tableName: String, columnName: String): Boolean {
        val checkStmt = conn.prepareStatement("""
            SELECT COUNT(*) as count FROM pragma_table_info('$tableName') 
            WHERE name = '$columnName'
        """)
        val rs = checkStmt.executeQuery()
        rs.next()
        val count = rs.getInt("count")
        rs.close()
        checkStmt.close()
        return count > 0
    }

    private fun addAdminNameToPerusahaan(conn: java.sql.Connection) {
        addColumnIfNotExists(conn, "perusahaan", "nama_admin", "TEXT")
        addColumnIfNotExists(conn, "perusahaan", "nama_pemilik", "TEXT")
        addColumnIfNotExists(conn, "perusahaan", "jabatan_pemilik", "TEXT")
    }
}