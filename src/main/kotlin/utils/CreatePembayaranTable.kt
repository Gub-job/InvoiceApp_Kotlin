package utils

object CreatePembayaranTable {
    fun createTable() {
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.createStatement()
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pembayaran (
                    id_pembayaran INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_invoice INTEGER NOT NULL,
                    tanggal TEXT NOT NULL,
                    jumlah REAL NOT NULL,
                    keterangan TEXT,
                    FOREIGN KEY (id_invoice) REFERENCES invoice(id_invoice)
                )
            """)
            println("Tabel pembayaran berhasil dibuat atau sudah ada.")
        } catch (e: Exception) {
            println("Error membuat tabel pembayaran: ${e.message}")
        } finally {
            conn.close()
        }
    }
}
