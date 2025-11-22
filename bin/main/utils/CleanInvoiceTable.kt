package utils

import java.sql.SQLException

object CleanInvoiceTable {
    
    fun cleanTable() {
        val conn = DatabaseHelper.getConnection()
        try {
            println("Memulai pembersihan tabel invoice...")
            
            // 1. Disable foreign keys
            conn.createStatement().execute("PRAGMA foreign_keys=off")
            
            // 2. Buat tabel baru dengan struktur yang bersih
            conn.createStatement().execute("""
                CREATE TABLE invoice_new (
                    id_invoice INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_perusahaan INTEGER,
                    id_pelanggan INTEGER,
                    nomor_invoice TEXT,
                    tanggal TEXT,
                    dp REAL,
                    tax REAL,
                    total REAL,
                    total_dengan_ppn REAL,
                    contract_ref TEXT,
                    contract_date TEXT,
                    divisi TEXT,
                    status TEXT,
                    FOREIGN KEY (id_perusahaan) REFERENCES perusahaan(id),
                    FOREIGN KEY (id_pelanggan) REFERENCES pelanggan(id_pelanggan)
                )
            """)
            
            // 3. Copy data dari tabel lama
            conn.createStatement().execute("""
                INSERT INTO invoice_new (
                    id_invoice, id_perusahaan, id_pelanggan, nomor_invoice, tanggal,
                    dp, tax, total, total_dengan_ppn, contract_ref, contract_date, divisi, status
                )
                SELECT 
                    id_invoice, id_perusahaan, id_pelanggan, nomor_invoice, tanggal,
                    dp, tax, total, total_dengan_ppn, contract_ref, contract_date, divisi, status
                FROM invoice
            """)
            
            // 4. Hapus tabel lama dan rename
            conn.createStatement().execute("DROP TABLE invoice")
            conn.createStatement().execute("ALTER TABLE invoice_new RENAME TO invoice")
            
            // 5. Enable foreign keys kembali
            conn.createStatement().execute("PRAGMA foreign_keys=on")
            
            println("✅ Pembersihan tabel invoice berhasil!")
            println("✅ Kolom id_proforma_asal telah dihapus")
            println("✅ Foreign key constraint telah diperbaiki")
            
        } catch (e: SQLException) {
            println("❌ Error saat membersihkan tabel: ${e.message}")
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }
}

fun main() {
    CleanInvoiceTable.cleanTable()
}