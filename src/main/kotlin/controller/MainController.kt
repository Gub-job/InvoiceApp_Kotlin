package controller

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.chart.NumberAxis
import javafx.collections.FXCollections
import javafx.application.Platform

class MainController {
    @FXML private lateinit var mainPane: BorderPane
    @FXML private lateinit var welcomeLabel: Label
    @FXML private lateinit var totalInvoiceLabel: Label
    @FXML private lateinit var totalInvoiceAmountLabel: Label
    @FXML private lateinit var totalProformaLabel: Label
    @FXML private lateinit var totalProformaAmountLabel: Label
    @FXML private lateinit var totalPiutangLabel: Label
    @FXML private lateinit var piutangBelumLunasLabel: Label
    @FXML private lateinit var totalPelangganLabel: Label
    @FXML private lateinit var totalProdukLabel: Label
    @FXML private lateinit var invoiceBulanIniLabel: Label
    @FXML private lateinit var proformaBulanIniLabel: Label
    @FXML private lateinit var pembayaranBulanIniLabel: Label
    @FXML private lateinit var salesChart: LineChart<String, Number>

    private var initialCenterNode: Node? = null

    companion object {
        var idPerusahaanAktif: Int = 0
        var namaAdmin: String = "Admin"
    }

    @FXML
    fun initialize() {
        initialCenterNode = mainPane.center
        salesChart.isLegendVisible = false
        salesChart.animated = false
    }
    
    fun setPerusahaanId(id: Int) {
        idPerusahaanAktif = id
        Platform.runLater {
            loadDashboardData()
        }
    }
    
    fun loadDashboardData() {
        if (idPerusahaanAktif == 0) return
        
        val conn = utils.DatabaseHelper.getConnection()
        try {
            // Ambil tanggal awal dan akhir tahun ini
            val today = java.time.LocalDate.now()
            val firstDayOfYear = today.withDayOfYear(1).toString()
            val lastDayOfYear = today.withDayOfYear(today.lengthOfYear()).toString()

            // Total Invoice
            val invoiceStmt = conn.prepareStatement("SELECT COUNT(*), COALESCE(SUM(total_dengan_ppn), 0) FROM invoice WHERE id_perusahaan = ? AND tanggal BETWEEN ? AND ?")
            invoiceStmt.setInt(1, idPerusahaanAktif)
            invoiceStmt.setString(2, firstDayOfYear)
            invoiceStmt.setString(3, lastDayOfYear)
            val invoiceRs = invoiceStmt.executeQuery()
            if (invoiceRs.next()) {
                totalInvoiceLabel.text = invoiceRs.getInt(1).toString()
                totalInvoiceAmountLabel.text = "Rp " + String.format("%,.0f", invoiceRs.getDouble(2))
            }
            
            // Total Proforma
            val proformaStmt = conn.prepareStatement("SELECT COUNT(*), COALESCE(SUM(total_dengan_ppn), 0) FROM proforma WHERE id_perusahaan = ? AND tanggal_proforma BETWEEN ? AND ?")
            proformaStmt.setInt(1, idPerusahaanAktif)
            proformaStmt.setString(2, firstDayOfYear)
            proformaStmt.setString(3, lastDayOfYear)
            val proformaRs = proformaStmt.executeQuery()
            if (proformaRs.next()) {
                totalProformaLabel.text = proformaRs.getInt(1).toString()
                totalProformaAmountLabel.text = "Rp " + String.format("%,.0f", proformaRs.getDouble(2))
            }
            
            // Total Piutang
            val piutangStmt = conn.prepareStatement("""
                SELECT COUNT(*), COALESCE(SUM(i.total_dengan_ppn - COALESCE(p.total_dibayar, 0)), 0)
                FROM invoice i
                LEFT JOIN (SELECT id_invoice, SUM(jumlah) as total_dibayar FROM pembayaran GROUP BY id_invoice) p ON i.id_invoice = p.id_invoice
                WHERE i.id_perusahaan = ? AND (i.total_dengan_ppn - COALESCE(p.total_dibayar, 0)) > 0
            """)
            piutangStmt.setInt(1, idPerusahaanAktif)
            val piutangRs = piutangStmt.executeQuery()
            if (piutangRs.next()) {
                val count = piutangRs.getInt(1)
                piutangBelumLunasLabel.text = "$count belum lunas"
                totalPiutangLabel.text = "Rp " + String.format("%,.0f", piutangRs.getDouble(2))
            }
            
            // Total Pembayaran Diterima
            val pembayaranStmt = conn.prepareStatement("""
                SELECT COALESCE(SUM(p.jumlah), 0), COUNT(DISTINCT p.id_invoice)
                FROM pembayaran p
                JOIN invoice i ON p.id_invoice = i.id_invoice
                WHERE i.id_perusahaan = ?
            """)
            pembayaranStmt.setInt(1, idPerusahaanAktif)
            val pembayaranRs = pembayaranStmt.executeQuery()
            if (pembayaranRs.next()) {
                totalPelangganLabel.text = "Rp " + String.format("%,.0f", pembayaranRs.getDouble(1))
                totalProdukLabel.text = pembayaranRs.getInt(2).toString() + " invoice"
            }
            
            // Data Bulan Ini
            val firstDay = today.withDayOfMonth(1).toString()
            val lastDay = today.withDayOfMonth(today.lengthOfMonth()).toString()
            
            val invBulanStmt = conn.prepareStatement("SELECT COUNT(*) FROM invoice WHERE id_perusahaan = ? AND tanggal BETWEEN ? AND ?")
            invBulanStmt.setInt(1, idPerusahaanAktif)
            invBulanStmt.setString(2, firstDay)
            invBulanStmt.setString(3, lastDay)
            val invBulanRs = invBulanStmt.executeQuery()
            if (invBulanRs.next()) {
                invoiceBulanIniLabel.text = invBulanRs.getInt(1).toString() + " invoice"
            }
            
            val profBulanStmt = conn.prepareStatement("SELECT COUNT(*) FROM proforma WHERE id_perusahaan = ? AND tanggal_proforma BETWEEN ? AND ?")
            profBulanStmt.setInt(1, idPerusahaanAktif)
            profBulanStmt.setString(2, firstDay)
            profBulanStmt.setString(3, lastDay)
            val profBulanRs = profBulanStmt.executeQuery()
            if (profBulanRs.next()) {
                proformaBulanIniLabel.text = profBulanRs.getInt(1).toString() + " proforma"
            }
            
            val bayarBulanStmt = conn.prepareStatement("""
                SELECT COALESCE(SUM(p.jumlah), 0)
                FROM pembayaran p
                JOIN invoice i ON p.id_invoice = i.id_invoice
                WHERE i.id_perusahaan = ? AND p.tanggal BETWEEN ? AND ?
            """)
            bayarBulanStmt.setInt(1, idPerusahaanAktif)
            bayarBulanStmt.setString(2, firstDay)
            bayarBulanStmt.setString(3, lastDay)
            val bayarBulanRs = bayarBulanStmt.executeQuery()
            if (bayarBulanRs.next()) {
                pembayaranBulanIniLabel.text = "Rp " + String.format("%,.0f", bayarBulanRs.getDouble(1))
            }
            
            loadSalesChart()
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }
    
    private fun loadSalesChart() {
        val conn = utils.DatabaseHelper.getConnection()
        try {
            val seriesInvoice = XYChart.Series<String, Number>()
            seriesInvoice.name = "Penjualan Invoice"
            
            val today = java.time.LocalDate.now()
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            
            for (i in 11 downTo 0) {
                val targetMonth = today.minusMonths(i.toLong())
                val firstDay = targetMonth.withDayOfMonth(1).toString()
                val lastDay = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth()).toString()
                val monthLabel = monthNames[targetMonth.monthValue - 1]
                
                val invStmt = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total_dengan_ppn), 0) FROM invoice WHERE id_perusahaan = ? AND tanggal BETWEEN ? AND ?"
                )
                invStmt.setInt(1, idPerusahaanAktif)
                invStmt.setString(2, firstDay)
                invStmt.setString(3, lastDay)
                val invRs = invStmt.executeQuery()
                val invTotal = if (invRs.next()) invRs.getDouble(1) else 0.0
                
                seriesInvoice.data.add(XYChart.Data(monthLabel, invTotal))
            }
            
            Platform.runLater {
                salesChart.data.clear()
                salesChart.data.add(seriesInvoice)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn.close()
        }
    }

    fun onKembaliClicked() {
        mainPane.center = initialCenterNode
        loadDashboardData()
    }

    fun bukaPerusahaan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/PerusahaanTableView.fxml"))
            val content = loader.load<ScrollPane>()
            val controller = loader.getController<PerusahaanTabelController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Kesalahan"
            alert.headerText = "Gagal membuka detail perusahaan"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaPelanggan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/PelangganView.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<PelangganController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            mainPane.center = root
        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "Gagal membuka halaman pelanggan"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaProduk(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/ProdukView.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<ProdukController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            mainPane.center = root
        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "Gagal membuka halaman produk"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaInputPenjualan() {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Pilih Jenis Transaksi"
        alert.headerText = "Silakan pilih jenis transaksi yang ingin dibuat:"
        alert.contentText = "Proforma atau Invoice?"

        val btnProforma = ButtonType("Proforma")
        val btnInvoice = ButtonType("Invoice")
        val btnBatal = ButtonType.CANCEL

        alert.buttonTypes.setAll(btnProforma, btnInvoice, btnBatal)

        val result = alert.showAndWait()
        if (result.isPresent) {
            when (result.get()) {
                btnProforma -> {
                    val loader = FXMLLoader(javaClass.getResource("/view/Proforma.fxml"))
                    val view = loader.load<VBox>()
                    val controller = loader.getController<ProformaController>()
                    controller.setIdPerusahaan(idPerusahaanAktif)
                    showScreen(view)
                }
                btnInvoice -> {
                    val loader = FXMLLoader(javaClass.getResource("/view/Invoice.fxml"))
                    val view = loader.load<VBox>()
                    val controller = loader.getController<InvoiceController>()
                    controller.setIdPerusahaan(idPerusahaanAktif)
                    showScreen(view)
                }
                btnBatal -> return
            }
        }
    }

    fun bukaDaftarProforma() {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan!"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/DaftarProforma.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<DaftarProformaController>()
            controller.setIdPerusahaan(idPerusahaanAktif)
            controller.setMainController(this)
            showScreen(view)
        } catch (e: Exception) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error!"
            alert.headerText = "Gagal membuka daftar proforma"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaDaftarInvoice() {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan!"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/DaftarInvoiceView.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<DaftarInvoiceController>()
            controller.setIdPerusahaan(idPerusahaanAktif)
            controller.setMainController(this)
            showScreen(view)
        } catch (e: Exception) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error!"
            alert.headerText = "Gagal membuka daftar invoice"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaDaftarPiutang() {
        if (idPerusahaanAktif == 0) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu.")
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/DaftarPiutangView.fxml"))
            val view = loader.load<VBox>()
            val controller = loader.getController<DaftarPiutangController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            showScreen(view)
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka daftar piutang: ${e.message}")
        }
    }

    fun showScreen(content: Node) {
        mainPane.center = content
    }

    fun bukaLaporanPenjualan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu.")
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/LaporanPenjualanView.fxml"))
            val content = loader.load<VBox>()
            val controller = loader.getController<LaporanPenjualanController>()
            controller.setPerusahaanId(idPerusahaanAktif, namaAdmin)
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka laporan penjualan: ${e.message}")
        }
    }

    fun bukaLaporanUmurPiutang(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu.")
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/view/LaporanUmurPiutangView.fxml"))
            val content = loader.load<VBox>()
            val controller = loader.getController<LaporanUmurPiutangController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka laporan umur piutang: ${e.message}")
        }
    }

    fun onGantiPerusahaanClicked() {
        try {
            val loader = FXMLLoader(javaClass.getResource("/view/PerusahaanView.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<PerusahaanController>()

            controller.setOnPerusahaanSelected { idTerpilih ->
                idPerusahaanAktif = idTerpilih
                println("Perusahaan aktif sekarang: $idPerusahaanAktif")
            }

            val perusahaanStage = Stage()
            perusahaanStage.initModality(Modality.APPLICATION_MODAL)
            perusahaanStage.title = "Invoqr"
            perusahaanStage.scene = Scene(root)
            perusahaanStage.showAndWait()

            if (idPerusahaanAktif != 0) {
                println("Perusahaan dipilih ulang: $idPerusahaanAktif")
            } else {
                println("Tidak ada perusahaan dipilih ulang.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "Gagal membuka jendela perusahaan"
            alert.contentText = e.message
            alert.showAndWait()
        }
    }

    fun bukaPengaturan(event: ActionEvent) {
        if (idPerusahaanAktif == 0) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = null
            alert.contentText = "Belum ada perusahaan dipilih. Silakan pilih perusahaan terlebih dahulu."
            alert.showAndWait()
            return
        }

        try {
            println("DEBUG: Memulai bukaPengaturan...")
            val resourceUrl = javaClass.getResource("/view/PengaturanView.fxml")
            println("DEBUG: Mencari resource di '/view/PengaturanView.fxml'. Hasil: $resourceUrl")

            if (resourceUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Resource Error", "File FXML '/view/PengaturanView.fxml' tidak dapat ditemukan di classpath.")
                return
            }

            val loader = FXMLLoader(resourceUrl)
            val content = loader.load<VBox>()
            println("DEBUG: FXML berhasil di-load.")
            val controller = loader.getController<PengaturanController>()
            controller.setPerusahaanId(idPerusahaanAktif)
            mainPane.center = content
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka halaman pengaturan: ${e.message}")
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}