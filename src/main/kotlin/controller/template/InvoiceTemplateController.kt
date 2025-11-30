package controller.template

import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import model.DocumentData
import model.ProdukData
import utils.DatabaseHelper
import java.text.NumberFormat
import javafx.scene.layout.GridPane
import java.util.Locale

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
    @FXML lateinit var itemsGrid: GridPane
    @FXML lateinit var subtotalLabel: Label
    @FXML lateinit var dpLabel: Label
    @FXML lateinit var ppnLabel: Label
    @FXML lateinit var grandTotalLabel: Label
    @FXML lateinit var terbilangLabel: Label
    @FXML lateinit var companyNameLabel: Label
    @FXML lateinit var signatureCompanyNameLabel: Label
    @FXML lateinit var ownerNameLabel: Label
    @FXML lateinit var ownerPositionLabel: Label
    @FXML lateinit var namaPerusahaanHeader: Label
    @FXML lateinit var alamatPerusahaanHeader: Label
    @FXML lateinit var teleponPerusahaanHeader: Label
    @FXML lateinit var hpPerusahaanHeader: Label
    @FXML lateinit var noRekLabel: Label
    @FXML lateinit var bankNameLabel: Label
    @FXML lateinit var bankLocationLabel: Label

    @FXML
    fun initialize() {
        // Tidak ada inisialisasi yang diperlukan di sini untuk GridPane
    }

    fun populateData(data: DocumentData, idPerusahaan: Int = 1) {
        documentTypeLabel.text = data.documentType
        nomorDokumenLabel.text = data.nomorDokumen
        tanggalDokumenLabel.text = data.tanggalDokumen
        contractRefLabel.text = data.contractRef ?: "-"
        contractDateLabel.text = data.contractDate ?: "-"

        namaPelangganLabel.text = data.namaPelanggan
        alamatPelangganLabel.text = data.alamatPelanggan
        teleponPelangganLabel.text = "Telp: ${data.teleponPelanggan}"

        populateItems(data.items)

        subtotalLabel.text = data.subtotal
        dpLabel.text = data.dp
        ppnLabel.text = data.ppn
        grandTotalLabel.text = data.grandTotal
        terbilangLabel.text = "Terbilang: ${data.terbilang}"
        
        loadOwnerData(idPerusahaan)
    }

    private fun populateItems(items: List<ProdukData>) {
        itemsGrid.children.clear()

        val priceNumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
        val qtyNumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }

        // Header
        itemsGrid.add(Label("NO.").apply { style = "-fx-font-weight: bold; -fx-padding: 5;" }, 0, 0)
        itemsGrid.add(Label("QTY").apply { style = "-fx-font-weight: bold; -fx-padding: 5;" }, 1, 0)
        itemsGrid.add(Label("DESCRIPTION").apply { style = "-fx-font-weight: bold; -fx-padding: 5;" }, 2, 0)
        itemsGrid.add(Label("PRICE").apply { style = "-fx-font-weight: bold; -fx-padding: 5; -fx-alignment: center-right;"; maxWidth = Double.MAX_VALUE }, 3, 0)
        itemsGrid.add(Label("TOTAL").apply { style = "-fx-font-weight: bold; -fx-padding: 5; -fx-alignment: center-right;"; maxWidth = Double.MAX_VALUE }, 4, 0)

        // Data
        items.forEachIndexed { index, produk ->
            val rowIndex = index + 1

            // Nomor
            val noLabel = Label((index + 1).toString()).apply { style = "-fx-padding: 5;" }
            itemsGrid.add(noLabel, 0, rowIndex)

            // Qty
            val qtyText = qtyNumberFormat.format(produk.qtyProperty.get().toDoubleOrNull() ?: 0.0) + " " + produk.uomProperty.get().trim()
            val qtyLabel = Label(qtyText).apply { style = "-fx-padding: 5;" }
            itemsGrid.add(qtyLabel, 1, rowIndex)

            // Deskripsi
            val descLabel = Label(produk.namaProperty.get()).apply { style = "-fx-padding: 5;"; isWrapText = true }
            itemsGrid.add(descLabel, 2, rowIndex)

            // Harga
            val hargaValue = produk.hargaProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0
            val hargaLabel = Label(priceNumberFormat.format(hargaValue)).apply {
                style = "-fx-padding: 5; -fx-alignment: center-right;"
                maxWidth = Double.MAX_VALUE
            }
            itemsGrid.add(hargaLabel, 3, rowIndex)

            // Total
            val totalValue = produk.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0
            val totalLabel = Label(priceNumberFormat.format(totalValue)).apply {
                style = "-fx-padding: 5; -fx-alignment: center-right;"
                maxWidth = Double.MAX_VALUE
            }
            itemsGrid.add(totalLabel, 4, rowIndex)
        }
    }
    private fun loadOwnerData(idPerusahaan: Int = 1) { // This function should be inside the class
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("SELECT nama, alamat, telepon, hp, nama_pemilik, no_rek, nama_bank, lokasi_kantor_bank, jabatan_pemilik, logo_path FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val namaPerusahaan = rs.getString("nama")
                val alamat = rs.getString("alamat")
                val telepon = rs.getString("telepon")
                val hp = rs.getString("hp")
                val namaPemilik = rs.getString("nama_pemilik")
                val jabatanPemilik = rs.getString("jabatan_pemilik")
                val logoPath = rs.getString("logo_path")
                val noRek = rs.getString("no_rek")
                val namaBank = rs.getString("nama_bank")
                val lokasiKantorBank = rs.getString("lokasi_kantor_bank")

                // Header perusahaan
                namaPerusahaanHeader.text = if (!namaPerusahaan.isNullOrBlank()) namaPerusahaan else "Nama Perusahaan"
                alamatPerusahaanHeader.text = if (!alamat.isNullOrBlank()) alamat else "Alamat Perusahaan"
                teleponPerusahaanHeader.text = if (!telepon.isNullOrBlank()) "Telp: $telepon" else "Telp: -"
                hpPerusahaanHeader.text = if (!hp.isNullOrBlank()) "HP: $hp" else "HP: -"
                
                // Footer perusahaan
                companyNameLabel.text = if (!namaPerusahaan.isNullOrBlank()) namaPerusahaan else "Nama Perusahaan"
                signatureCompanyNameLabel.text = if (!namaPerusahaan.isNullOrBlank()) namaPerusahaan else "Nama Perusahaan"
                ownerNameLabel.text = if (!namaPemilik.isNullOrBlank()) namaPemilik else "Nama Pemilik"
                ownerPositionLabel.text = if (!jabatanPemilik.isNullOrBlank()) jabatanPemilik else "Jabatan"
                noRekLabel.text = if (!noRek.isNullOrBlank()) "IDR ACC NO. $noRek" else "No. Rekening: -"
                bankNameLabel.text = if (!namaBank.isNullOrBlank()) "$namaBank" else "Bank: -"
                bankLocationLabel.text = if (!lokasiKantorBank.isNullOrBlank()) "$lokasiKantorBank" else "Cabang: -"
                
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
                namaPerusahaanHeader.text = "Nama Perusahaan"
                alamatPerusahaanHeader.text = "Alamat Perusahaan"
                teleponPerusahaanHeader.text = "Telp: -"
                hpPerusahaanHeader.text = "HP: -"
                companyNameLabel.text = "Nama Perusahaan"
                signatureCompanyNameLabel.text = "Nama Perusahaan"
                ownerNameLabel.text = "Nama Pemilik"
                ownerPositionLabel.text = "Jabatan"
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
            namaPerusahaanHeader.text = "Nama Perusahaan"
            alamatPerusahaanHeader.text = "Alamat Perusahaan"
            teleponPerusahaanHeader.text = "Telp: -"
            hpPerusahaanHeader.text = "HP: -"
            companyNameLabel.text = "Nama Perusahaan"
            signatureCompanyNameLabel.text = "Nama Perusahaan"
            ownerNameLabel.text = "Nama Pemilik"
            ownerPositionLabel.text = "Jabatan"
        }
    }
}