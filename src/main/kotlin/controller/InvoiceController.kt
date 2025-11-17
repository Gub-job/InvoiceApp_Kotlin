package controller

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.layout.Pane
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.util.Callback
import model.PelangganData
import model.ProdukData
import utils.DatabaseHelper

import utils.NomorGenerator
import utils.PrintPreview
import utils.CreateProformaTables
import java.sql.Connection
import java.sql.Statement
import java.time.LocalDate

// Data class untuk referensi proforma
data class ProformaRefData(
    val id: Int,
    val nomor: String,
    val tanggal: String,
    val namaPelanggan: String,
    val subtotal: Double // Tambahkan subtotal
) {
    override fun toString(): String = "$nomor - $namaPelanggan"
}

class InvoiceController {

    @FXML private lateinit var pelangganField: TextField
    @FXML private lateinit var nomorField: TextField
    @FXML private lateinit var tanggalPicker: DatePicker
    @FXML private lateinit var dpField: TextField
    @FXML private lateinit var alamatField: TextField
    @FXML private lateinit var teleponField: TextField
    @FXML private lateinit var kolomNo: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomQty: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomHarga: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomTotal: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomNama: TableColumn<ProdukData, String>
    @FXML private lateinit var kolomUom: TableColumn<ProdukData, String>
    @FXML private lateinit var table: TableView<ProdukData>
    @FXML private lateinit var simpanBtn: Button
    @FXML private lateinit var cetakBtn: Button
    @FXML private lateinit var hapusBtn: Button
    @FXML private lateinit var tambahBtn: Button
    @FXML private lateinit var ppnCheckBox: CheckBox
    @FXML private lateinit var subtotalLabel: Label 
    @FXML private lateinit var ppnAmountLabel: Label
    @FXML private lateinit var dpAmountLabel: Label
    @FXML private lateinit var grandTotalLabel: Label
    @FXML private lateinit var contractRefField: TextField
    @FXML private lateinit var contractDatePicker: DatePicker

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private val detailList = FXCollections.observableArrayList<ProdukData>()

    private val popup = Popup()
    private val listView = ListView<PelangganData>()
    private val produkPopup = Popup()
    private val produkListView = ListView<ProdukData>()
    private val proformaRefPopup = Popup()
    private val proformaRefListView = ListView<ProformaRefData>()
    private val proformaRefList = FXCollections.observableArrayList<ProformaRefData>()

    // TextField terpusat untuk autocomplete produk yang lebih stabil
    private val produkSearchField = TextField()

    private var selectedPelanggan: PelangganData? = null
    private var idPerusahaan: Int = 0
    private var idInvoiceBaru: Int = 0
    private var isEditMode = false
    private var currentEditingCell: TextFieldTableCell<ProdukData, String>? = null

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        // Pastikan tabel proforma dan invoice ada
        CreateProformaTables.createTables()
        loadPelanggan()
        loadProduk()
        loadDefaultTaxRate()
        loadProformaRefs()
        ensureContractColumnsExist() // Pastikan kolom referensi ada
        // Kosongkan nomor field, akan diisi setelah produk ditambahkan
        nomorField.text = ""
        // Set tanggal hari ini
        tanggalPicker.value = LocalDate.now()
    }

    fun loadInvoice(idInvoice: Int) {
        this.idInvoiceBaru = idInvoice
        this.isEditMode = true // PENTING: Aktifkan mode edit
        simpanBtn.text = "Simpan" // Ganti teks tombol jadi "Simpan" untuk mode edit

        val conn = DatabaseHelper.getConnection()
        try {
            // Pastikan kolom yang dibutuhkan ada sebelum query
            ensureContractColumnsExist(conn)

            // 1. Load data master invoice
            val stmt = conn.prepareStatement("SELECT * FROM invoice WHERE id_invoice = ?") // Menggunakan nama kolom yang konsisten
            stmt.setInt(1, idInvoice)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                nomorField.text = rs.getString("nomor_invoice") // FIX: ganti ke nomor_invoice
                try {
                    tanggalPicker.value = LocalDate.parse(rs.getString("tanggal")) // FIX: ganti ke tanggal
                } catch (e: Exception) {
                    tanggalPicker.value = LocalDate.now() // Fallback ke tanggal hari ini jika parse gagal
                }
                contractRefField.text = rs.getString("contract_ref")
                rs.getString("contract_date")?.let { contractDatePicker.value = LocalDate.parse(it) }
                
                val dpAmount = rs.getDouble("dp")
                val subtotal = rs.getDouble("total") // Ambil subtotal dari database

                // Hitung persentase DP untuk dpField
                val dpPercentage = if (subtotal > 0) (dpAmount / subtotal) * 100 else 0.0
                dpField.text = String.format("%.2f", dpPercentage).replace(",", ".")

                // ppnField adalah Label, nilainya sudah diatur oleh loadDefaultTaxRate()

                // Load pelanggan terkait
                val idPelanggan = rs.getInt("id_pelanggan")
                pelangganList.find { it.idProperty.get() == idPelanggan }?.let {
                    selectedPelanggan = it
                    pelangganField.text = it.namaProperty.get()
                    alamatField.text = it.alamatProperty.get()
                    teleponField.text = it.teleponProperty.get()
                }
            }

            // 2. Load detail produk dari invoice
            val detailStmt = conn.prepareStatement("""
                SELECT di.*, p.nama_produk, p.uom, p.divisi, p.singkatan 
                FROM detail_invoice di 
                JOIN produk p ON di.id_produk = p.id_produk
                WHERE di.id_invoice = ?
            """)
            detailStmt.setInt(1, idInvoice)
            val detailRs = detailStmt.executeQuery()
            while(detailRs.next()) {
                val produk = ProdukData(
                    id = detailRs.getInt("id_produk"),
                    nama = detailRs.getString("nama_produk"),
                    uom = detailRs.getString("uom"),
                    qty = String.format("%,.2f", detailRs.getDouble("qty")),
                    harga = String.format("%,.2f", detailRs.getDouble("harga"))
                )
                detailList.add(produk)
                hitungTotalBaris(produk)
            }
            updateTotals()
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat data invoice: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun ensureContractColumnsExist(conn: Connection? = null) {
        val connection = conn ?: DatabaseHelper.getConnection()
        try {
            val metaData = connection.metaData
            var rs = metaData.getColumns(null, null, "invoice", "contract_ref")
            if (!rs.next()) {
                connection.createStatement().execute("ALTER TABLE invoice ADD COLUMN contract_ref TEXT")
                println("Kolom 'contract_ref' ditambahkan ke tabel invoice.")
            }
            rs = metaData.getColumns(null, null, "invoice", "contract_date")
            if (!rs.next()) {
                connection.createStatement().execute("ALTER TABLE invoice ADD COLUMN contract_date TEXT")
                println("Kolom 'contract_date' ditambahkan ke tabel invoice.")
            }
        } catch (e: Exception) {
            println("Gagal memastikan kolom referensi kontrak ada: ${e.message}")
        } finally {
            // Hanya tutup koneksi jika kita yang membuatnya
            if (conn == null) {
                connection.close()
            }
        }
    }

    @FXML
    fun initialize() {
        table.isEditable = true
        table.items = detailList
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        kolomNama.setCellValueFactory { it.value.namaProperty }
        kolomUom.setCellValueFactory { it.value.uomProperty }
        kolomQty.setCellValueFactory { it.value.qtyProperty }
        kolomHarga.setCellValueFactory { it.value.hargaProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty }

        // Custom cell factory untuk Qty dengan Tab support
        kolomQty.cellFactory = Callback {
            object : TextFieldTableCell<ProdukData, String>() {
                override fun startEdit() {
                    super.startEdit()
                    val tf = graphic as? TextField
                    tf?.setOnKeyPressed { event ->
                        val selectedRow = table.selectionModel.selectedIndex
                        when (event.code) {
                            javafx.scene.input.KeyCode.TAB, javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.RIGHT -> {
                                commitEdit(tf.text)
                                event.consume()
                                javafx.application.Platform.runLater {
                                    table.edit(selectedRow, kolomHarga)
                                }
                            }
                            javafx.scene.input.KeyCode.LEFT -> {
                                commitEdit(tf.text)
                                event.consume()
                                javafx.application.Platform.runLater {
                                    table.edit(selectedRow, kolomNama)
                                }
                            }
                            else -> {}
                        }
                    }
                    javafx.application.Platform.runLater { tf?.requestFocus() }
                }
            }
        }
        
        // Custom cell factory untuk Harga dengan Tab support
        kolomHarga.cellFactory = Callback {
            object : TextFieldTableCell<ProdukData, String>() {
                override fun startEdit() {
                    super.startEdit()
                    val tf = graphic as? TextField
                    tf?.setOnKeyPressed { event ->
                        val selectedRow = table.selectionModel.selectedIndex
                        when (event.code) {
                            javafx.scene.input.KeyCode.TAB, javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.RIGHT -> {
                                commitEdit(tf.text)
                                event.consume()
                                // Pindah ke baris berikutnya
                                if (selectedRow < detailList.size - 1) {
                                    javafx.application.Platform.runLater {
                                        table.selectionModel.select(selectedRow + 1)
                                        table.edit(selectedRow + 1, kolomNama)
                                    }
                                }
                            }
                            javafx.scene.input.KeyCode.LEFT -> {
                                commitEdit(tf.text)
                                event.consume()
                                javafx.application.Platform.runLater {
                                    table.edit(selectedRow, kolomQty)
                                }
                            }
                            else -> {}
                        }
                    }
                    javafx.application.Platform.runLater { tf?.requestFocus() }
                }
            }
        }

        kolomNama.cellFactory = Callback {
            object : TextFieldTableCell<ProdukData, String>() {
                override fun startEdit() {
                    super.startEdit()
                    val tf = graphic as? TextField
                    tf?.textProperty()?.addListener { _, _, newValue ->
                        filterAndShowProdukForCell(newValue, tf)
                    }
                    tf?.setOnKeyPressed { event ->
                        when (event.code) {
                            javafx.scene.input.KeyCode.DOWN -> {
                                if (produkPopup.isShowing) {
                                    produkListView.requestFocus()
                                    produkListView.selectionModel.selectFirst()
                                }
                                event.consume()
                            }
                            javafx.scene.input.KeyCode.TAB -> {
                                if (produkPopup.isShowing && produkListView.items.isNotEmpty()) {
                                    val selected = produkListView.selectionModel.selectedItem ?: produkListView.items[0]
                                    applyProdukToCell(selected, tf)
                                    event.consume()
                                }
                            }
                            javafx.scene.input.KeyCode.ENTER -> {
                                if (produkPopup.isShowing && produkListView.selectionModel.selectedItem != null) {
                                    val selected = produkListView.selectionModel.selectedItem
                                    applyProdukToCell(selected, tf)
                                    event.consume()
                                }
                            }
                            javafx.scene.input.KeyCode.ESCAPE -> {
                                produkPopup.hide()
                                event.consume()
                            }
                            else -> {}
                        }
                    }
                }

                override fun cancelEdit() {
                    super.cancelEdit()
                    produkPopup.hide()
                }

                override fun commitEdit(newValue: String?) {
                    super.commitEdit(newValue)
                    produkPopup.hide()
                }
            }
        }

        kolomQty.setOnEditCommit {
            val value = it.newValue.replace(",", "").toDoubleOrNull() ?: 0.0
            it.rowValue.qtyProperty.set(String.format("%,.2f", value))
            hitungTotalBaris(it.rowValue)
            updateTotals()
        }
        kolomHarga.setOnEditCommit {
            val value = it.newValue.replace(",", "").toDoubleOrNull() ?: 0.0
            it.rowValue.hargaProperty.set(String.format("%,.2f", value))
            hitungTotalBaris(it.rowValue)
            updateTotals()
        }

        kolomNo.setCellFactory {
            object : TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else (index + 1).toString()
                }
            }
        }

        setupPelangganAutocomplete()
        setupProdukAutocomplete()
        setupProformaRefAutocomplete()

        tambahBtn.setOnAction {
            val newProduk = ProdukData(0, "", "", "")
            detailList.add(newProduk)
            javafx.application.Platform.runLater {
                table.selectionModel.select(detailList.size - 1)
                table.edit(detailList.size - 1, kolomNama)
            }
        }

        hapusBtn.setOnAction {
            val selected = table.selectionModel.selectedItem
            if (selected != null) {
                detailList.remove(selected)
                // hapusDetailDariDB(selected) // Implement if needed
                updateTotals()
            }
        }

        simpanBtn.setOnAction {
            if (isEditMode) {
                updateInvoiceDanDetail()
            } else {
                simpanInvoiceDanDetail()
            }
        }

        cetakBtn.setOnAction {
            cetakInvoiceKePdf()
        }

        // Listener untuk tanggal - update nomor jika sudah ada produk
        tanggalPicker.valueProperty().addListener { _, _, newDate ->
            updateNomorIfReady()
        }
        
        // Listener untuk PPN checkbox
        ppnCheckBox.selectedProperty().addListener { _, _, _ ->
            updateTotals()
        }


    }

    private fun setupProformaRefAutocomplete() {
        proformaRefListView.cellFactory = Callback {
            object : ListCell<ProformaRefData>() {
                override fun updateItem(item: ProformaRefData?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.toString()
                }
            }
        }
        proformaRefPopup.content.add(proformaRefListView)
        proformaRefListView.prefWidthProperty().bind(contractRefField.widthProperty())
        proformaRefListView.prefHeight = 200.0

        contractRefField.textProperty().addListener { _, _, newValue ->
            val filtered = proformaRefList.filter {
                it.nomor.contains(newValue, ignoreCase = true) ||
                it.namaPelanggan.contains(newValue, ignoreCase = true)
            }
            proformaRefListView.items.setAll(filtered)
            if (filtered.isNotEmpty()) {
                // Hanya tampilkan popup jika field sudah terlihat di layar
                if (contractRefField.scene?.window?.isShowing == true) {
                    val screenBounds: Bounds = contractRefField.localToScreen(contractRefField.boundsInLocal)
                    proformaRefPopup.show(contractRefField, screenBounds.minX, screenBounds.minY + screenBounds.height)
                }
            } else {
                proformaRefPopup.hide()
            }
        }

        proformaRefListView.setOnMouseClicked { event ->
            if (event.clickCount == 1 && proformaRefListView.selectionModel.selectedItem != null) {
                applyProformaRef(proformaRefListView.selectionModel.selectedItem)
                proformaRefPopup.hide()
            }
        }
    }

    private fun applyProformaRef(proformaRef: ProformaRefData?) {
        if (proformaRef == null) return

        // 1. Isi field referensi
        contractRefField.text = proformaRef.nomor
        contractDatePicker.value = LocalDate.parse(proformaRef.tanggal)

        // 2. Load data lengkap dari proforma yang dipilih
        val conn = DatabaseHelper.getConnection()
        try {
            // Ambil data master proforma (pelanggan, dp, tax)
            val proformaStmt = conn.prepareStatement("SELECT * FROM proforma WHERE id_proforma = ?")
            proformaStmt.setInt(1, proformaRef.id)
            val proformaRs = proformaStmt.executeQuery()

            if (proformaRs.next()) {
                // Set Pelanggan
                val idPelanggan = proformaRs.getInt("id_pelanggan")
                pelangganList.find { it.idProperty.get() == idPelanggan }?.let {
                    selectedPelanggan = it
                    pelangganField.text = it.namaProperty.get()
                    alamatField.text = it.alamatProperty.get()
                    teleponField.text = it.teleponProperty.get()
                }
                // Set DP & PPN
                val dpAmount = proformaRs.getDouble("dp")
                val subtotalFromProforma = proformaRef.subtotal // Gunakan subtotal dari ProformaRefData
                
                // Hitung persentase DP untuk dpField
                val dpPercentage = if (subtotalFromProforma > 0) (dpAmount / subtotalFromProforma) * 100 else 0.0
                dpField.text = String.format("%.2f", dpPercentage).replace(",", ".")
                
                // ppnField adalah Label, nilainya sudah diatur oleh loadDefaultTaxRate()
            }
            // Ambil detail produk dari proforma
            detailList.clear() // Kosongkan list detail yang sekarang
            val detailStmt = conn.prepareStatement("""
                SELECT dp.*, p.nama_produk, p.uom, p.divisi, p.singkatan 
                FROM detail_proforma dp 
                JOIN produk p ON dp.id_produk = p.id_produk
                WHERE dp.id_proforma = ?
            """)
            detailStmt.setInt(1, proformaRef.id)
            val detailRs = detailStmt.executeQuery()
            while(detailRs.next()) {
                val produk = ProdukData(
                    id = detailRs.getInt("id_produk"),
                    nama = detailRs.getString("nama_produk"),
                    uom = detailRs.getString("uom"),
                    qty = String.format("%,.2f", detailRs.getDouble("qty")),
                    harga = String.format("%,.2f", detailRs.getDouble("harga")),
                    divisi = detailRs.getString("divisi"),
                    singkatan = detailRs.getString("singkatan")
                )
                detailList.add(produk)
                hitungTotalBaris(produk)
            }
            updateTotals()
            updateNomorIfReady() // Generate nomor invoice baru setelah semua data ter-load
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat detail dari proforma: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun setupProdukAutocomplete() {
        produkListView.cellFactory = Callback {
            object : ListCell<ProdukData>() {
                override fun updateItem(item: ProdukData?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.namaProperty.get()
                }
            }
        }
        produkPopup.content.add(produkListView)
        produkListView.prefHeight = 200.0
        produkListView.prefWidth = 300.0

        produkListView.setOnMouseClicked {
            if (it.clickCount == 1 && produkListView.selectionModel.selectedItem != null) {
                val selectedProduk = produkListView.selectionModel.selectedItem
                val selectedRow = table.selectionModel.selectedIndex
                if (selectedRow >= 0 && selectedRow < detailList.size) {
                    val row = detailList[selectedRow]
                    row.idProperty.set(selectedProduk.idProperty.get())
                    row.namaProperty.set(selectedProduk.namaProperty.get())
                    row.uomProperty.set(selectedProduk.uomProperty.get())
                    row.divisiProperty.set(selectedProduk.divisiProperty.get())
                    row.singkatanProperty.set(selectedProduk.singkatanProperty.get())
                }
                produkPopup.hide()
                table.edit(-1, null)
                table.refresh()
                updateNomorIfReady()
                moveToNextColumn()
            }
        }

        produkListView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                    val selectedProduk = produkListView.selectionModel.selectedItem
                    if (selectedProduk != null) {
                        val selectedRow = table.selectionModel.selectedIndex
                        if (selectedRow >= 0 && selectedRow < detailList.size) {
                            val row = detailList[selectedRow]
                            row.idProperty.set(selectedProduk.idProperty.get())
                            row.namaProperty.set(selectedProduk.namaProperty.get())
                            row.uomProperty.set(selectedProduk.uomProperty.get())
                            row.divisiProperty.set(selectedProduk.divisiProperty.get())
                            row.singkatanProperty.set(selectedProduk.singkatanProperty.get())
                        }
                        produkPopup.hide()
                        table.edit(-1, null)
                        table.refresh()
                        updateNomorIfReady()
                        moveToNextColumn()
                    }
                    event.consume()
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    produkPopup.hide()
                    table.requestFocus()
                    event.consume()
                }
                else -> {}
            }
        }
    }

    private fun showAllProduk() {
        if (produkList.isNotEmpty()) {
            produkListView.items.setAll(produkList)
            produkListView.selectionModel.selectFirst()
            
            if (!produkPopup.isShowing) {
                val bounds = produkSearchField.localToScreen(produkSearchField.boundsInLocal)
                if (bounds != null) {
                    produkPopup.show(produkSearchField, bounds.minX, bounds.minY + bounds.height)
                }
            }
        }
    }

    private fun filterAndShowProdukForCell(keyword: String, textField: TextField) {
        if (keyword.isEmpty()) {
            produkPopup.hide()
            return
        }

        val filtered = produkList.filter {
            it.namaProperty.get().contains(keyword.trim(), ignoreCase = true)
        }

        if (filtered.isNotEmpty()) {
            produkListView.items.setAll(filtered)
            produkListView.selectionModel.selectFirst()

            if (!produkPopup.isShowing) {
                val bounds = textField.localToScreen(textField.boundsInLocal)
                if (bounds != null) {
                    produkPopup.show(textField, bounds.minX, bounds.minY + bounds.height)
                }
            }
        } else {
            produkPopup.hide()
        }
    }

    private fun applyProdukToCell(produk: ProdukData, textField: TextField) {
        val selectedRow = table.selectionModel.selectedIndex
        if (selectedRow >= 0 && selectedRow < detailList.size) {
            val row = detailList[selectedRow]
            row.idProperty.set(produk.idProperty.get())
            row.namaProperty.set(produk.namaProperty.get())
            row.uomProperty.set(produk.uomProperty.get())
            row.divisiProperty.set(produk.divisiProperty.get())
            row.singkatanProperty.set(produk.singkatanProperty.get())
        }
        produkPopup.hide()
        table.edit(-1, null)
        table.refresh()
        updateNomorIfReady()
        moveToNextColumn()
    }

    private fun hideProdukSearch() {
        try {
            (produkSearchField.scene?.root as? Pane)?.children?.remove(produkSearchField)
        } catch (e: Exception) {
            // Ignore error jika sudah tidak ada
        }
        produkPopup.hide()
        table.requestFocus()
    }


    private fun applyProdukAndMoveNext(produk: ProdukData?) {
        if (produk != null && currentEditingCell != null) {
            val row = currentEditingCell?.tableRow?.item
            if (row != null) {
                row.idProperty.set(produk.idProperty.get())
                row.namaProperty.set(produk.namaProperty.get())
                row.uomProperty.set(produk.uomProperty.get())
                row.divisiProperty.set(produk.divisiProperty.get())
                row.singkatanProperty.set(produk.singkatanProperty.get())
                currentEditingCell?.commitEdit(produk.namaProperty.get())
            }
        }
        hideProdukSearch()
        updateNomorIfReady() // Panggil di sini agar nomor selalu ter-update setelah produk dipilih
        moveToNextColumn()
    }
    
    private fun updateNomorIfReady() {
        // Generate nomor hanya jika ada produk yang valid dan tanggal sudah dipilih
        val firstProduct = detailList.firstOrNull { it.namaProperty.get().isNotBlank() }
        if (firstProduct != null && tanggalPicker.value != null) {
                nomorField.text = NomorGenerator.generateNomor(
                    idPerusahaan,
                    "invoice",
                    firstProduct.divisiProperty.get(),
                    firstProduct.namaProperty.get(),
                    firstProduct.singkatanProperty.get(),
                    tanggalPicker.value
                )
        }
    }

    private fun moveToNextColumn() {
        val selectedRow = table.selectionModel.selectedIndex
        if (selectedRow >= 0) {
            javafx.application.Platform.runLater {
                table.edit(selectedRow, kolomQty)
            }
        }
    }

    private fun simpanInvoiceDanDetail() {
        // 1. Validasi input penting
        if (selectedPelanggan == null) {
            showAlert("Peringatan", "Pelanggan harus dipilih.")
            return
        }
        if (detailList.isEmpty()) {
            showAlert("Peringatan", "Invoice harus memiliki setidaknya satu item produk.")
            return
        }
        if (nomorField.text.isBlank()) {
            showAlert("Peringatan", "Nomor Invoice tidak boleh kosong. Pastikan produk dan tanggal sudah terisi.")
            return
        }

        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            // 2. Hitung semua nilai yang akan disimpan
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
            val ppnRate = if (ppnCheckBox.isSelected) 11.0 else 0.0
            val dpPercentage = dpField.text.toDoubleOrNull() ?: 0.0
            val dpAmount = subtotal * (dpPercentage / 100.0)

            val ppnAmount = if (dpAmount > 0) {
                dpAmount * (ppnRate / 100.0)
            } else {
                subtotal * (ppnRate / 100.0)
            }
            val grandTotal = if (dpAmount > 0) dpAmount + ppnAmount else subtotal + ppnAmount

            // 3. Debug: Print nilai yang akan disimpan
            println("=== DEBUG SIMPAN INVOICE ===")
            println("ID Perusahaan: $idPerusahaan")
            println("ID Pelanggan: ${selectedPelanggan!!.idProperty.get()}")
            println("No Invoice: ${nomorField.text}")
            println("Tanggal: ${tanggalPicker.value?.toString()}")
            println("Subtotal: $subtotal")
            println("PPN Amount: $ppnAmount")
            println("Grand Total: $grandTotal")
            println("DP Amount: $dpAmount")
            println("=== END DEBUG ===")
            
            // 3. Siapkan statement INSERT dengan semua kolom yang dibutuhkan
            val stmt = conn.prepareStatement(
                """
                INSERT INTO invoice (id_perusahaan, id_pelanggan, nomor_invoice, tanggal, dp, tax, total, total_dengan_ppn, contract_ref, contract_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            )
            stmt.setInt(1, idPerusahaan)
            stmt.setInt(2, selectedPelanggan!!.idProperty.get()) // Pakai '!!' karena sudah divalidasi
            stmt.setString(3, nomorField.text)
            stmt.setString(4, tanggalPicker.value?.toString() ?: LocalDate.now().toString())
            stmt.setDouble(5, dpAmount)       // Simpan DP amount
            stmt.setDouble(6, ppnAmount)      // Simpan PPN amount
            stmt.setDouble(7, subtotal)       // Simpan subtotal (kolom 'total')
            stmt.setDouble(8, grandTotal)     // Simpan grand total (kolom 'total_dengan_ppn')
            stmt.setString(9, contractRefField.text) // Simpan contract ref
            stmt.setString(10, contractDatePicker.value?.toString()) // Simpan contract date
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            if (rs.next()) idInvoiceBaru = rs.getInt(1)

            // 4. Simpan detail invoice
            for (produk in detailList) {
                val detailStmt = conn.prepareStatement(
                    "INSERT INTO detail_invoice (id_invoice, id_produk, qty, harga, total) VALUES (?, ?, ?, ?, ?)"
                )
                detailStmt.setInt(1, idInvoiceBaru)
                detailStmt.setInt(2, produk.idProperty.get())
                detailStmt.setDouble(3, produk.qtyProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0)
                detailStmt.setDouble(4, produk.hargaProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0)
                detailStmt.setDouble(5, produk.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0)
                detailStmt.executeUpdate()
            }

            conn.commit()
            isEditMode = true // Setelah simpan, otomatis masuk mode edit
            showAlert("Sukses", "Invoice berhasil disimpan.")
        } catch (e: Exception) {
            conn.rollback()
            println("=== DEBUG ERROR SIMPAN INVOICE ===")
            println("Error: ${e.message}")
            println("Stack trace:")
            e.printStackTrace()
            println("=== END DEBUG ===")
            showAlert("Error", "Gagal menyimpan invoice: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun updateInvoiceDanDetail() {
        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            // 1. Hitung semua nilai
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
            val ppnRate = if (ppnCheckBox.isSelected) 11.0 else 0.0
            val dpPercentage = dpField.text.toDoubleOrNull() ?: 0.0
            val dpAmount = subtotal * (dpPercentage / 100.0)
            val ppnAmount = if (dpAmount > 0) dpAmount * (ppnRate / 100.0) else subtotal * (ppnRate / 100.0)
            val grandTotal = if (dpAmount > 0) dpAmount + ppnAmount else subtotal + ppnAmount

            // 2. Siapkan statement UPDATE
            val stmt = conn.prepareStatement(
                """
                UPDATE invoice SET
                    id_pelanggan = ?, nomor_invoice = ?, tanggal = ?, dp = ?, tax = ?,
                    total = ?, total_dengan_ppn = ?, contract_ref = ?, contract_date = ?
                WHERE id_invoice = ?
                """
            )
            stmt.setInt(1, selectedPelanggan?.idProperty?.get() ?: 0)
            stmt.setString(2, nomorField.text)
            stmt.setString(3, tanggalPicker.value?.toString())
            stmt.setDouble(4, dpAmount)
            stmt.setDouble(5, ppnAmount)
            stmt.setDouble(6, subtotal)
            stmt.setDouble(7, grandTotal)
            stmt.setString(8, contractRefField.text)
            stmt.setString(9, contractDatePicker.value?.toString())
            stmt.setInt(10, idInvoiceBaru) // WHERE clause
            stmt.executeUpdate()

            // 3. Hapus detail lama dan masukkan yang baru
            val deleteStmt = conn.prepareStatement("DELETE FROM detail_invoice WHERE id_invoice = ?")
            deleteStmt.setInt(1, idInvoiceBaru)
            deleteStmt.executeUpdate()

            for (produk in detailList) {
                val detailStmt = conn.prepareStatement(
                    "INSERT INTO detail_invoice (id_invoice, id_produk, qty, harga, total) VALUES (?, ?, ?, ?, ?)"
                )
                detailStmt.setInt(1, idInvoiceBaru)
                detailStmt.setInt(2, produk.idProperty.get())
                detailStmt.setDouble(3, produk.qtyProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0)
                detailStmt.setDouble(4, produk.hargaProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0)
                detailStmt.setDouble(5, produk.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0)
                detailStmt.executeUpdate()
            }

            conn.commit()
            showAlert("Sukses", "Invoice berhasil diupdate.")

            // Tutup jendela setelah update berhasil
            (simpanBtn.scene.window as? Stage)?.close()

        } catch (e: Exception) {
            conn.rollback()
            showAlert("Error", "Gagal mengupdate invoice: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun cetakInvoiceKePdf() {
        if (idInvoiceBaru == 0 && !isEditMode) {
            showAlert("Peringatan", "Silakan simpan invoice terlebih dahulu sebelum mencetak.")
            return
        }

        try {
            val data = model.DocumentData(
                documentType = "INVOICE",
                nomorDokumen = nomorField.text,
                tanggalDokumen = tanggalPicker.value.toString(),
                namaPelanggan = pelangganField.text,
                alamatPelanggan = alamatField.text,
                teleponPelanggan = teleponField.text,
                items = detailList.toList(),
                subtotal = subtotalLabel.text,
                dp = dpAmountLabel.text,
                ppn = ppnAmountLabel.text,
                grandTotal = grandTotalLabel.text,
                contractRef = contractRefField.text,
                contractDate = contractDatePicker.value?.toString()
            )

            val preview = PrintPreview(data, cetakBtn.scene.window, idPerusahaan)
            preview.show()

        } catch (e: Exception) {
            showAlert("Error", "Gagal membuat pratinjau cetak: ${e.message}")
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    private fun setupPelangganAutocomplete() {
        listView.cellFactory = Callback {
            object : ListCell<PelangganData>() {
                override fun updateItem(item: PelangganData?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.namaProperty.get()
                }
            }
        }
        popup.content.add(listView)
        listView.prefWidthProperty().bind(pelangganField.widthProperty())
        listView.prefHeight = 200.0

        listView.setOnMouseClicked { event ->
            if (event.clickCount == 1 && listView.selectionModel.selectedItem != null) {
                selectCurrent()
                popup.hide()
            }
        }

        pelangganField.textProperty().addListener { _, _, newValue ->
            val filtered = pelangganList.filter {
                it.namaProperty.get().contains(newValue ?: "", ignoreCase = true)
            }
            listView.items.setAll(filtered)
            listView.selectionModel.selectFirst()

            if (filtered.isNotEmpty()) {
                // Hanya tampilkan popup jika field sudah terlihat di layar
                if (pelangganField.scene?.window?.isShowing == true) {
                    val screenBounds: Bounds = pelangganField.localToScreen(pelangganField.boundsInLocal)
                    popup.show(pelangganField, screenBounds.minX, screenBounds.minY + screenBounds.height)
                }
            } else {
                popup.hide()
            }
        }

        pelangganField.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.DOWN -> {
                    if (popup.isShowing) {
                        listView.requestFocus()
                        if (listView.selectionModel.isEmpty) {
                            listView.selectionModel.selectFirst()
                        }
                    }
                }
                javafx.scene.input.KeyCode.TAB, javafx.scene.input.KeyCode.ENTER -> {
                    if (popup.isShowing && listView.items.isNotEmpty()) {
                        selectCurrent()
                        popup.hide()
                        event.consume()
                    }
                }
                javafx.scene.input.KeyCode.ESCAPE -> popup.hide()
                else -> {}
            }
        }

        listView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                    selectCurrent()
                    popup.hide()
                    pelangganField.parent.requestFocus()
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    popup.hide()
                    pelangganField.requestFocus()
                }
                else -> {}
            }
        }
    }

    private fun selectCurrent() {
        val selected = listView.selectionModel.selectedItem
        if (selected != null) {
            selectedPelanggan = selected
            pelangganField.text = selected.namaProperty.get()
            alamatField.text = selected.alamatProperty.get()
            teleponField.text = selected.teleponProperty.get()
        }
    }

    private fun hitungTotalBaris(item: ProdukData) {
        try {
            val qty = item.qtyProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0
            val harga = item.hargaProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0
            val total = qty * harga
            item.totalProperty.set(String.format("%,.2f", total))
        } catch (e: Exception) {
            item.totalProperty.set("0")
        }
        table.refresh()
    }

    private fun updateTotals() {
        // 1. Hitung nilai dasar
        val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
        val ppnRate = if (ppnCheckBox.isSelected) 11.0 else 0.0
        val dpAmountValue = calculateDPValue()

        // 2. Hitung PPN Amount sesuai aturan: dari DP jika ada, jika tidak dari Subtotal
        val ppnAmount = if (dpAmountValue > 0) {
            dpAmountValue * (ppnRate / 100.0)
        } else {
            subtotal * (ppnRate / 100.0)
        }
        
        // 3. Terapkan logika Grand Total sesuai permintaan
        val grandTotal = if (dpAmountValue > 0) dpAmountValue + ppnAmount else subtotal + ppnAmount

        // 4. Update semua label
        dpAmountLabel.text = String.format("%,.2f", dpAmountValue)
        subtotalLabel.text = String.format("%,.2f", subtotal)
        ppnAmountLabel.text = String.format("%,.2f", ppnAmount)
        grandTotalLabel.text = String.format("%,.2f", grandTotal)

        table.columns.firstOrNull()?.let {
            it.isVisible = false
            it.isVisible = true
        }
    }

    private fun calculateDPValue(): Double {
        val dpPercentage = dpField.text.toDoubleOrNull() ?: 0.0
        val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
        return subtotal * (dpPercentage / 100.0)
    }

    private fun loadPelanggan() {
        val conn = DatabaseHelper.getConnection()
        val stmt = conn.prepareStatement("SELECT * FROM pelanggan WHERE id_perusahaan = ?")
        stmt.setInt(1, idPerusahaan)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            pelangganList.add(
                PelangganData(
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("alamat"),
                    rs.getString("telepon")
                )
            )
        }
        conn.close()
    }

    private fun loadProduk() {
        val conn = DatabaseHelper.getConnection()
        
        // Cek dan tambahkan kolom singkatan jika belum ada
        try {
            val checkStmt = conn.prepareStatement("""
                SELECT COUNT(*) as count FROM pragma_table_info('produk') 
                WHERE name = 'singkatan'
            """)
            val rs = checkStmt.executeQuery()
            rs.next()
            val columnExists = rs.getInt("count") > 0
            
            if (!columnExists) {
                val alterStmt = conn.prepareStatement("ALTER TABLE produk ADD COLUMN singkatan TEXT")
                alterStmt.executeUpdate()
            }
        } catch (e: Exception) {
            // Ignore error, kolom mungkin sudah ada
        }
        
        val stmt = conn.prepareStatement("SELECT id_produk, nama_produk, uom, divisi, singkatan FROM produk WHERE id_perusahaan = ?")
        stmt.setInt(1, idPerusahaan)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            produkList.add(
                ProdukData(
                    rs.getInt("id_produk"),
                    rs.getString("nama_produk"),
                    rs.getString("uom"),
                    divisi = rs.getString("divisi"),
                    singkatan = rs.getString("singkatan") ?: ""
                )
            )
        }
        conn.close()
    }

    private fun loadDefaultTaxRate() {
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT default_tax_rate FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                ppnCheckBox.isSelected = rs.getDouble("default_tax_rate") > 0
            }
        } catch (e: Exception) {
            println("Gagal memuat default tax rate: ${e.message}")
        } finally {
            conn.close()
        }
    }
    private fun loadProformaRefs() {
        proformaRefList.clear()
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("""
                SELECT p.id_proforma, p.no_proforma, p.tanggal_proforma, pel.nama as pelanggan_nama, p.total
                FROM proforma p
                LEFT JOIN pelanggan pel ON p.id_pelanggan = pel.id
                WHERE p.id_perusahaan = ?
                ORDER BY p.id_proforma DESC
            """)
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            while(rs.next()) {
                proformaRefList.add(ProformaRefData(
                    rs.getInt("id_proforma"),
                    rs.getString("no_proforma"),
                    rs.getString("tanggal_proforma"),
                    rs.getString("pelanggan_nama") ?: "N/A",
                    rs.getDouble("total") // Muat subtotal
                ))
            }
        } catch (e: Exception) {
            println("Gagal memuat referensi proforma: ${e.message}")
        } finally {
            conn.close()
        }
    }
}