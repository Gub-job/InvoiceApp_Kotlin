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
import java.text.NumberFormat
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
    @FXML lateinit var itemsTable: TableView<ProdukData>
    @FXML lateinit var subtotalLabel: Label
    @FXML lateinit var dpLabel: Label
    @FXML lateinit var ppnLabel: Label
    @FXML lateinit var grandTotalLabel: Label
    @FXML lateinit var terbilangLabel: Label
    @FXML lateinit var companyNameLabel: Label
    @FXML lateinit var ownerNameLabel: Label
    @FXML lateinit var ownerPositionLabel: Label
    @FXML lateinit var namaPerusahaanHeader: Label
    @FXML lateinit var alamatPerusahaanHeader: Label
    @FXML lateinit var teleponPerusahaanHeader: Label
    @FXML lateinit var hpPerusahaanHeader: Label
    @FXML lateinit var noRekLabel: Label
    @FXML lateinit var bankNameLabel: Label
    @FXML lateinit var bankLocationLabel: Label

    // Deklarasikan semua kolom tabel dengan @FXML
    @FXML private lateinit var noCol: TableColumn<ProdukData, String>
    @FXML private lateinit var namaProdukCol: TableColumn<ProdukData, String>
    @FXML private lateinit var qtyCol: TableColumn<ProdukData, String>
    @FXML private lateinit var hargaCol: TableColumn<ProdukData, String>
    @FXML private lateinit var totalCol: TableColumn<ProdukData, String>

    @FXML
    fun initialize() {
        // Hilangkan warna selang-seling baris
        itemsTable.style = "-fx-background-color: white; -fx-control-inner-background: white;"
        
        // Inisialisasi kolom tabel
        noCol.setCellFactory {
            object : javafx.scene.control.TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else (index + 1).toString()
                }
            }
        }
        namaProdukCol.setCellValueFactory { it.value.namaProperty }
        namaProdukCol.setCellFactory {
            object : javafx.scene.control.TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = item
                    isWrapText = true
                }
            }
        }
        
        // Format untuk angka harga (dengan desimal)
        val priceNumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }

        // Format untuk angka Qty (tanpa desimal)
        val qtyNumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        qtyCol.setCellValueFactory { it.value.qtyProperty } // Tetap ambil data dari qtyProperty
        qtyCol.setCellFactory {
            object : javafx.scene.control.TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                    } else {
                        val formattedQty = qtyNumberFormat.format(item.toDoubleOrNull() ?: 0.0)
                        val produkData = tableView.items.getOrNull(index)
                        val uom = produkData?.uomProperty?.get() ?: ""
                        text = if (uom.isNotBlank()) "$formattedQty $uom" else formattedQty
                    }
                }
            }
        }

        hargaCol.setCellValueFactory { it.value.hargaProperty }
        hargaCol.setCellFactory {
            object : javafx.scene.control.TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else {
                        val value = item.replace(",", "").toDoubleOrNull() ?: 0.0
                        priceNumberFormat.format(value)
                    }
                }
            }
        }

        // Format kolom total sebagai mata uang
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        totalCol.setCellValueFactory { it.value.totalProperty }
        totalCol.setCellFactory {
            object : javafx.scene.control.TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else {
                        val value = item.replace(",", "").toDoubleOrNull() ?: 0.0
                        currencyFormat.format(value)
                    }
                }
            }
        }
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

        // Populate tabel dengan data items
        itemsTable.items.clear()
        itemsTable.items.addAll(data.items)

        // Kembalikan header kolom Qty ke teks statis
        qtyCol.text = "Qty."

        subtotalLabel.text = data.subtotal
        dpLabel.text = data.dp
        ppnLabel.text = data.ppn
        grandTotalLabel.text = data.grandTotal
        terbilangLabel.text = "Terbilang: ${data.terbilang}"
        
        loadOwnerData(idPerusahaan)
    }
    
    private fun loadOwnerData(idPerusahaan: Int = 1) {
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
            ownerNameLabel.text = "Nama Pemilik"
            ownerPositionLabel.text = "Jabatan"
        }
    }
}