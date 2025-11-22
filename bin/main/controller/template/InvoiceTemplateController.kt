package controller.template

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.AnchorPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import model.DocumentData
import model.ProdukData
import utils.DatabaseHelper

class InvoiceTemplateController {

    @FXML lateinit var templateRoot: AnchorPane
    @FXML lateinit var logoImageView: ImageView
    @FXML lateinit var documentTypeLabel: Label
    @FXML lateinit var nomorDokumenLabel: Label
    @FXML lateinit var tanggalDokumenLabel: Label
    @FXML lateinit var contractRefLabel: Label
    @FXML lateinit var contractDateLabel: Label
    @FXML lateinit var namaPelangganLabel: Label
    @FXML lateinit var alamatPelangganLabel: Label
    @FXML lateinit var teleponPelangganLabel: Label
    @FXML lateinit var itemsTable: TableView<ProdukData>
    @FXML lateinit var subtotalLabel: Label
    @FXML lateinit var dpLabel: Label
    @FXML lateinit var ppnLabel: Label
    @FXML lateinit var grandTotalLabel: Label
    @FXML lateinit var companyNameLabel: Label
    @FXML lateinit var ownerNameLabel: Label
    @FXML lateinit var ownerPositionLabel: Label

    @FXML
    fun initialize() {
        // Definisikan kolom di sini karena beberapa FXML mungkin tidak punya fx:id
        val noCol = itemsTable.columns.find { it.text == "No" } as? TableColumn<ProdukData, String>
        val namaProdukCol = itemsTable.columns.find { it.text == "Nama Produk" } as? TableColumn<ProdukData, String>
        val uomCol = itemsTable.columns.find { it.text == "UOM" } as? TableColumn<ProdukData, String>
        val qtyCol = itemsTable.columns.find { it.text == "Qty" } as? TableColumn<ProdukData, String>
        val hargaCol = itemsTable.columns.find { it.text == "Harga" } as? TableColumn<ProdukData, String>
        val totalCol = itemsTable.columns.find { it.text == "Total" } as? TableColumn<ProdukData, String>
        // Inisialisasi kolom tabel
        noCol?.setCellFactory {
            object : javafx.scene.control.TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else (index + 1).toString()
                }
            }
        }
        namaProdukCol?.setCellValueFactory { it.value.namaProperty }
        uomCol?.setCellValueFactory { it.value.uomProperty }
        qtyCol?.setCellValueFactory { it.value.qtyProperty }
        hargaCol?.setCellValueFactory { it.value.hargaProperty }
        totalCol?.setCellValueFactory { it.value.totalProperty }
    }

    fun populateData(data: DocumentData, idPerusahaan: Int = 1) {
        documentTypeLabel.text = data.documentType
        nomorDokumenLabel.text = "No: ${data.nomorDokumen}"
        tanggalDokumenLabel.text = "Tanggal: ${data.tanggalDokumen}"
        contractRefLabel.text = "Contract Ref: ${data.contractRef ?: "-"}"
        contractDateLabel.text = "Contract Date: ${data.contractDate ?: "-"}"

        namaPelangganLabel.text = data.namaPelanggan
        alamatPelangganLabel.text = data.alamatPelanggan
        teleponPelangganLabel.text = "Telp: ${data.teleponPelanggan}"

        itemsTable.items.setAll(data.items)

        subtotalLabel.text = data.subtotal
        dpLabel.text = data.dp
        ppnLabel.text = data.ppn
        grandTotalLabel.text = data.grandTotal
        
        loadOwnerData(idPerusahaan)
    }
    
    private fun loadOwnerData(idPerusahaan: Int = 1) {
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("SELECT nama, nama_pemilik, jabatan_pemilik, logo_path FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val namaPerusahaan = rs.getString("nama")
                val namaPemilik = rs.getString("nama_pemilik")
                val jabatanPemilik = rs.getString("jabatan_pemilik")
                val logoPath = rs.getString("logo_path")
                companyNameLabel.text = if (!namaPerusahaan.isNullOrBlank()) namaPerusahaan else "Nama Perusahaan"
                ownerNameLabel.text = if (!namaPemilik.isNullOrBlank()) namaPemilik else "Nama Pemilik"
                ownerPositionLabel.text = if (!jabatanPemilik.isNullOrBlank()) jabatanPemilik else "Jabatan"
                
                // Load logo
                if (!logoPath.isNullOrBlank()) {
                    try {
                        val logoFile = java.io.File(logoPath)
                        if (logoFile.exists()) {
                            val image = Image(logoFile.toURI().toString())
                            logoImageView.image = image
                            logoImageView.isVisible = true
                        }
                    } catch (e: Exception) {
                        println("Gagal load logo: ${e.message}")
                    }
                }
            } else {
                companyNameLabel.text = "Nama Perusahaan"
                ownerNameLabel.text = "Nama Pemilik"
                ownerPositionLabel.text = "Jabatan"
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
            companyNameLabel.text = "Nama Perusahaan"
            ownerNameLabel.text = "Nama Pemilik"
            ownerPositionLabel.text = "Jabatan"
        }
    }
}