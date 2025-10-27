// package controller

// import model.Proforma
// import utils.DatabaseHelper
// import java.sql.Connection
// import java.sql.PreparedStatement

// class ProformaController {

//     fun insertProforma(proforma: Proforma): Boolean {
//         val conn: Connection? = DatabaseHelper.getConnection()
//         val sql = """
//             INSERT INTO proforma (
//                 id_perusahaan, id_pelanggan, nomor_proforma, tanggal, 
//                 contract_ref, contract_date, total, tax, total_dengan_ppn, keterangan
//             ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
//         """.trimIndent()

//         return try {
//             val stmt: PreparedStatement = conn!!.prepareStatement(sql)
//             stmt.setInt(1, proforma.idPerusahaan)
//             stmt.setInt(2, proforma.idPelanggan)
//             stmt.setString(3, proforma.nomorProforma)
//             stmt.setString(4, proforma.tanggal)
//             stmt.setString(5, proforma.contractRef)
//             stmt.setString(6, proforma.contractDate)
//             stmt.setDouble(7, proforma.total)
//             stmt.setDouble(8, proforma.tax)
//             stmt.setDouble(9, proforma.totalDenganPpn)
//             stmt.setString(10, proforma.keterangan)

//             stmt.executeUpdate() > 0
//         } catch (e: Exception) {
//             e.printStackTrace()
//             false
//         } finally {
//             conn?.close()
//         }
//     }
// }
