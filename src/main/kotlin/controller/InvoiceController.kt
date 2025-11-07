package controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.Popup
import javafx.util.Callback
import model.PelangganData
import model.ProformaData
import model.ProdukData
import utils.DatabaseHelper
import utils.NomorGenerator
import utils.CreateProformaTables
import java.sql.Connection
import java.time.LocalDate

class InvoiceController {

    @FXML private lateinit var pelangganField: TextField
    @FXML private lateinit var contractRefField: TextField
    @FXML private lateinit var contractDatePicker: DatePicker
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
    @FXML private lateinit var ppnField: TextField
    @FXML private lateinit var subtotalLabel: Label
    @FXML private lateinit var ppnAmountLabel: Label
    @FXML private lateinit var dpAmountLabel: Label // Tambahkan DP Amount Label
    @FXML private lateinit var grandTotalLabel: Label

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private val detailList = FXCollections.observableArrayList<ProdukData>()

    private val popup = Popup()
    private val listView = ListView<PelangganData>()
    private val produkPopup = Popup()
    private val produkListView = ListView<ProdukData>()

    private val proformaPopup = Popup() // Untuk autocomplete proforma
    private val proformaListView = ListView<ProformaData>() // Untuk autocomplete proforma
    private val allProformaList = FXCollections.observableArrayList<ProformaData>() // Daftar semua proforma

    private var selectedPelanggan: PelangganData? = null
    private var idPerusahaan: Int = 0
    private var idInvoiceBaru: Int = 0
    private var isEditMode = false // Tambahkan isEditMode
    private var currentEditingCell: TextFieldTableCell<ProdukData, String>? = null

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        // Pastikan tabel proforma dan invoice ada
        CreateProformaTables.createTables()
        loadPelanggan()
        loadProduk()
        loadAllProformas() // Muat semua proforma untuk autocomplete
        nomorField.text = ""
        // Set tanggal hari ini
        tanggalPicker.value = LocalDate.now()
    }

    @FXML
    fun initialize() {
        // Inisialisasi TableView Produk
        table.isEditable = true
        table.items = detailList
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        kolomNama.setCellValueFactory { it.value.namaProperty }
        kolomUom.setCellValueFactory { it.value.uomProperty }
        kolomQty.setCellValueFactory { it.value.qtyProperty }
        kolomHarga.setCellValueFactory { it.value.hargaProperty }
        kolomTotal.setCellValueFactory { it.value.totalProperty }

        kolomNama.cellFactory = Callback {
            object : TextFieldTableCell<ProdukData, String>() {
                private var textField: TextField? = null

                override fun startEdit() {
                    super.startEdit()
                    currentEditingCell = this

                    textField = graphic as? TextField
                    textField?.let { tf ->
                        tf.textProperty().addListener { _, _, newValue ->
                            filterAndShowProduk(newValue)
                        }

                        tf.setOnKeyPressed { event ->
                            when (event.code) {
                                javafx.scene.input.KeyCode.TAB, javafx.scene.input.KeyCode.RIGHT -> {
                                    if (produkPopup.isShowing && produkListView.items.isNotEmpty()) {
                                        val selected = produkListView.selectionModel.selectedItem
                                            ?: produkListView.items[0]
                                        applyProdukAndMoveNext(selected)
                                    } else {
                                        commitEdit(tf.text)
                                        javafx.application.Platform.runLater { table.edit(table.selectionModel.selectedIndex, kolomQty) }
                                    }
                                    event.consume()
                                }
                                javafx.scene.input.KeyCode.ENTER -> {
                                    if (produkPopup.isShowing && produkListView.items.isNotEmpty()) {
                                        val selected = produkListView.selectionModel.selectedItem
                                            ?: produkListView.items[0]
                                        applyProdukAndMoveNext(selected)
                                    } else if (tf.text.isNotBlank()) {
                                        commitEdit(tf.text)
                                        // Pindah ke kolom Qty
                                        javafx.application.Platform.runLater { table.edit(table.selectionModel.selectedIndex, kolomQty) }
                                    }
                                    event.consume()
                                }
                                javafx.scene.input.KeyCode.DOWN -> {
                                    if (produkPopup.isShowing) {
                                        produkListView.requestFocus()
                                        if (produkListView.selectionModel.isEmpty) {
                                            produkListView.selectionModel.selectFirst()
                                        }
                                        event.consume()
                                    }
                                }
                                javafx.scene.input.KeyCode.ESCAPE -> {
                                    produkPopup.hide()
                                    cancelEdit()
                                    event.consume()
                                }
                                javafx.scene.input.KeyCode.LEFT -> {
                                    commitEdit(tf.text)
                                    event.consume()
                                    // Tidak ada aksi, sudah di kolom paling kiri
                                }
                                else -> {}
                            }
                        }
                        javafx.application.Platform.runLater { tf.requestFocus() }
                    }
                }

                override fun cancelEdit() {
                    super.cancelEdit()
                    produkPopup.hide()
                    currentEditingCell = null
                }

                override fun commitEdit(newValue: String?) {
                    super.commitEdit(newValue)
                    produkPopup.hide()
                    currentEditingCell = null
                }
            }
        }

        // Custom cell factory untuk Qty dengan Tab support (mirip ProformaController)
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

        // Custom cell factory untuk Harga dengan Tab support (mirip ProformaController)
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

        // Inisialisasi kolom No
        kolomNo.setCellFactory {
            object : TableCell<ProdukData, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else (index + 1).toString()
                }
            }
        }

        kolomQty.setOnEditCommit {
            it.rowValue.qtyProperty.set(it.newValue)
            hitungTotalBaris(it.rowValue)
            updateTotals()
        }
        kolomHarga.setOnEditCommit {
            it.rowValue.hargaProperty.set(it.newValue)
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
        setupProformaAutocomplete() // Setup autocomplete untuk proforma
        setupProdukAutocomplete()

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
            simpanInvoiceDanDetail() // Akan memanggil simpanBaru atau update
        }

        cetakBtn.setOnAction {
            showAlert("Informasi", "Fitur cetak belum diimplementasikan.")
        }

        ppnField.textProperty().addListener { _, _, _ ->
            updateTotals()
        }
        
        // Listener untuk tanggal - update nomor jika sudah ada produk
        // Juga set contract date mengikuti tanggal invoice
        tanggalPicker.valueProperty().addListener { _, _, newDate ->
            updateNomorIfReady()
            // contractDatePicker.value = newDate // Dihapus agar tidak otomatis mengikuti tanggal invoice
        }

        // Listener untuk DP field - hanya angka dan update total
        dpField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches("\\d*\\.?\\d*".toRegex())) {
                dpField.text = oldValue
            } else {
                updateDPDisplay()
                updateTotals()
            }
        }

        // Contract ref mengikuti nomor invoice
        // nomorField.textProperty().addListener { _, _, newValue ->
        //     contractRefField.text = newValue // Dihapus agar tidak otomatis mengikuti nomor invoice
        // }
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
                applyProdukAndMoveNext(selectedProduk)
            }
        }

        produkListView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                    val selectedProduk = produkListView.selectionModel.selectedItem
                    if (selectedProduk != null) {
                        applyProdukAndMoveNext(selectedProduk)
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

    // ===========================================================
    // === AUTOCOMPLETE PROFORMA (untuk Contract Ref)
    // ===========================================================
    private fun setupProformaAutocomplete() {
        proformaListView.cellFactory = Callback {
            object : ListCell<ProformaData>() {
                override fun updateItem(item: ProformaData?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.nomorProperty.get() + " - " + item.pelangganProperty.get()
                }
            }
        }
        proformaPopup.content.add(proformaListView)
        proformaListView.prefHeight = 200.0
        proformaListView.prefWidthProperty().bind(contractRefField.widthProperty())

        // Mouse click - langsung apply dan load data
        proformaListView.setOnMouseClicked {
            if (it.clickCount == 1 && proformaListView.selectionModel.selectedItem != null) {
                val selectedProforma = proformaListView.selectionModel.selectedItem
                if (selectedProforma != null) {
                    applyProformaAndLoadData(selectedProforma)
                }
            }
        }

        // Keyboard handler untuk ListView
        proformaListView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                    val selectedProforma = proformaListView.selectionModel.selectedItem
                    if (selectedProforma != null) {
                        applyProformaAndLoadData(selectedProforma)
                    }
                    event.consume()
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    proformaPopup.hide()
                    contractRefField.requestFocus()
                    event.consume()
                }
                else -> {}
            }
        }

        // Listener untuk contractRefField
        contractRefField.textProperty().addListener { _, _, newValue ->
            filterAndShowProforma(newValue)
        }

        // Tambahkan listener untuk klik mouse
        contractRefField.setOnMouseClicked {
            filterAndShowProforma(contractRefField.text)
        }

        contractRefField.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.DOWN -> {
                    if (proformaPopup.isShowing) {
                        proformaListView.requestFocus()
                        if (proformaListView.selectionModel.isEmpty) {
                            proformaListView.selectionModel.selectFirst()
                        }
                        event.consume()
                    }
                }
                javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                    if (proformaPopup.isShowing && proformaListView.items.isNotEmpty()) {
                        val selected = proformaListView.selectionModel.selectedItem
                            ?: proformaListView.items[0]
                        applyProformaAndLoadData(selected)
                        event.consume()
                    }
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    proformaPopup.hide()
                    event.consume()
                }
                else -> {}
            }
        }
    }

    private fun loadAllProformas() {
        allProformaList.clear()
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("""
                SELECT p.id_proforma, p.no_proforma, p.tanggal_proforma, pel.nama as pelanggan_nama, 
                       p.total_dengan_ppn as total
                FROM proforma p 
                LEFT JOIN pelanggan pel ON p.id_pelanggan = pel.id 
                WHERE p.id_perusahaan = ? 
                ORDER BY p.tanggal_proforma DESC, p.id_proforma DESC
            """)
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                allProformaList.add(ProformaData(
                    rs.getInt("id_proforma"),
                    rs.getString("no_proforma") ?: "",
                    rs.getString("tanggal_proforma") ?: "",
                    rs.getString("pelanggan_nama") ?: "Tidak ada",
                    rs.getDouble("total")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat daftar proforma untuk autocomplete: ${e.message}")
        }
    }

    private fun filterAndShowProforma(keyword: String) {
        val filteredList = if (keyword.isEmpty()) {
            allProformaList // Tampilkan semua jika keyword kosong
        } else {
            allProformaList.filter {
                it.nomorProperty.get().contains(keyword, ignoreCase = true) ||
                it.pelangganProperty.get().contains(keyword, ignoreCase = true)
            }
        }

        if (filteredList.isNotEmpty()) {
            proformaListView.items.setAll(filteredList)
            proformaListView.selectionModel.selectFirst()

            if (!proformaPopup.isShowing) {
                val screenBounds: Bounds = contractRefField.localToScreen(contractRefField.boundsInLocal)
                proformaPopup.show(
                    contractRefField,
                    screenBounds.minX,
                    screenBounds.minY + screenBounds.height
                )
            }
        } else {
            proformaPopup.hide()
        }
    }

    private fun applyProformaAndLoadData(proforma: ProformaData) {
        contractRefField.text = proforma.nomorProperty.get()
        proformaPopup.hide()
        loadProformaIntoInvoice(proforma.idProperty.get())
    }

    private fun updateDPDisplay() {
        val dpAmount = calculateDPValue()
        dpAmountLabel.text = String.format("%,.2f", dpAmount)
    }

    private fun filterAndShowProduk(keyword: String) {
        if (keyword.isEmpty()) {
            produkPopup.hide()
            return
        }

        val filtered = produkList.filter {
            it.namaProperty.get().contains(keyword, ignoreCase = true)
        }

        if (filtered.isNotEmpty()) {
            produkListView.items.setAll(filtered)
            produkListView.selectionModel.selectFirst()

            if (!produkPopup.isShowing) {
                val cell = currentEditingCell
                if (cell != null) {
                    val screenBounds: Bounds = cell.localToScreen(cell.boundsInLocal)
                    produkPopup.show(
                        cell,
                        screenBounds.minX,
                        screenBounds.minY + screenBounds.height
                    )
                }
            }
        } else {
            produkPopup.hide()
        }
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
        produkPopup.hide()
        updateNomorIfReady() // Panggil di sini agar nomor selalu ter-update setelah produk dipilih
        moveToNextColumn()
    }
    
    private fun updateNomorIfReady() {
        // Generate nomor hanya jika ada produk yang valid dan tanggal sudah dipilih
        val firstProduct = detailList.firstOrNull { it.namaProperty.get().isNotBlank() } // Ambil produk pertama yang valid
        if (firstProduct != null && tanggalPicker.value != null) { // Pastikan ada produk dan tanggal sudah dipilih
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
                table.edit(selectedRow, kolomQty) // Pindah ke kolom Qty
            }
        }
    }

    private fun simpanInvoiceDanDetail() {
        if (isEditMode) {
            updateInvoiceDanDetail()
        } else {
            simpanInvoiceBaru()
        }
    }

    private fun simpanInvoiceBaru() {
        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", ".").toDoubleOrNull() ?: 0.0 }
            val ppnRate = ppnField.text.toDoubleOrNull() ?: 11.0
            val ppnAmount = subtotal * (ppnRate / 100.0)
            val totalDenganPpn = subtotal + ppnAmount

            val sql = """
                INSERT INTO invoice (id_perusahaan, id_pelanggan, contract_ref, contract_date, 
                                    total, tax, total_dengan_ppn, no_invoice, tanggal_invoice, dp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            val stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            stmt.setInt(1, idPerusahaan)
            stmt.setInt(2, selectedPelanggan?.idProperty?.get() ?: 0)
            stmt.setString(3, contractRefField.text)
            stmt.setString(4, contractDatePicker.value?.toString())
            stmt.setDouble(5, subtotal)
            stmt.setDouble(6, ppnAmount)
            stmt.setDouble(7, totalDenganPpn)
            stmt.setString(8, nomorField.text)
            stmt.setString(9, tanggalPicker.value?.toString() ?: LocalDate.now().toString())
            stmt.setDouble(10, calculateDPValue())
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            if (rs.next()) idInvoiceBaru = rs.getInt(1)

            simpanDetailInvoice(conn, idInvoiceBaru)

            conn.commit()
            showAlert("Sukses", "Invoice berhasil disimpan.")
            isEditMode = true // Setelah simpan, masuk mode edit
            simpanBtn.text = "Simpan"
        } catch (e: Exception) {
            conn.rollback()
            showAlert("Error", "Gagal menyimpan invoice: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun updateInvoiceDanDetail() {
        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", ".").toDoubleOrNull() ?: 0.0 }
            val ppnRate = ppnField.text.toDoubleOrNull() ?: 11.0
            val ppnAmount = subtotal * (ppnRate / 100.0)
            val totalDenganPpn = subtotal + ppnAmount

            val sql = """
                UPDATE invoice SET 
                    id_pelanggan = ?, contract_ref = ?, contract_date = ?, total = ?, tax = ?, 
                    total_dengan_ppn = ?, no_invoice = ?, tanggal_invoice = ?, dp = ?
                WHERE id_invoice = ?
            """
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, selectedPelanggan?.idProperty?.get() ?: 0)
            stmt.setString(2, contractRefField.text)
            stmt.setString(3, contractDatePicker.value?.toString())
            stmt.setDouble(4, subtotal)
            stmt.setDouble(5, ppnAmount)
            stmt.setDouble(6, totalDenganPpn)
            stmt.setString(7, nomorField.text)
            stmt.setString(8, tanggalPicker.value?.toString() ?: LocalDate.now().toString())
            stmt.setDouble(9, calculateDPValue())
            stmt.setInt(10, idInvoiceBaru) // id_invoice
            stmt.executeUpdate()

            // Hapus detail lama dan masukkan yang baru
            val deleteStmt = conn.prepareStatement("DELETE FROM detail_invoice WHERE id_invoice = ?")
            deleteStmt.setInt(1, idInvoiceBaru)
            deleteStmt.executeUpdate()

            simpanDetailInvoice(conn, idInvoiceBaru)

            conn.commit()
            showAlert("Sukses", "Invoice berhasil diupdate.")
        } catch (e: Exception) {
            conn.rollback()
            showAlert("Error", "Gagal mengupdate invoice: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun simpanDetailInvoice(conn: Connection, idInvoice: Int) {
        for (produk in detailList) {
            val detailStmt = conn.prepareStatement(
                """
                INSERT INTO detail_invoice (id_invoice, id_produk, qty, harga, total)
                VALUES (?, ?, ?, ?, ?) 
                """
            )
            detailStmt.setInt(1, idInvoice)
            detailStmt.setInt(2, produk.idProperty.get())
            detailStmt.setDouble(3, produk.qtyProperty.get().toDoubleOrNull() ?: 0.0)
            detailStmt.setDouble(4, produk.hargaProperty.get().toDoubleOrNull() ?: 0.0)
            val total = (produk.qtyProperty.get().toDoubleOrNull() ?: 0.0) *
                        (produk.hargaProperty.get().toDoubleOrNull() ?: 0.0)
            detailStmt.setDouble(5, total)
            detailStmt.executeUpdate()
        }
    }

    // Method untuk memuat data proforma ke dalam form invoice
    private fun loadProformaIntoInvoice(idProforma: Int) {
        val conn = DatabaseHelper.getConnection()
        try {
            // 1. Load data master proforma (FIX: ganti 'id' menjadi 'id_proforma')
            val stmt = conn.prepareStatement("SELECT * FROM proforma WHERE id_proforma = ?")
            stmt.setInt(1, idProforma)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                // Set invoice ID for potential update
                // this.idInvoiceBaru = idProforma // Sebaiknya jangan di-set di sini, biarkan auto-increment
                this.isEditMode = true
                simpanBtn.text = "Simpan"

                nomorField.text = NomorGenerator.generateNomor(
                    idPerusahaan,
                    "invoice",
                    "", // Divisi tidak langsung dari proforma header, akan diisi dari produk
                    "", // Nama produk tidak langsung dari proforma header, akan diisi dari produk
                    "", // Singkatan tidak langsung dari proforma header, akan diisi dari produk
                    LocalDate.now() // Tanggal invoice adalah tanggal hari ini
                ) // Nomor invoice akan digenerate ulang
                tanggalPicker.value = LocalDate.now()
                contractRefField.text = rs.getString("no_proforma")
                rs.getString("tanggal_proforma")?.let { contractDatePicker.value = LocalDate.parse(it) }

                val dpValue = rs.getDouble("dp")
                val subtotal = rs.getDouble("total")
                if (subtotal > 0) {
                    val dpPercentage = (dpValue / subtotal) * 100
                    dpField.text = String.format("%.2f", dpPercentage).replace(",", ".")
                } else {
                    dpField.text = "0.00"
                }

                val ppnAmount = rs.getDouble("tax")
                if (subtotal > 0) {
                    val ppnPercentage = (ppnAmount / subtotal) * 100
                    ppnField.text = String.format("%.2f", ppnPercentage).replace(",", ".")
                } else {
                    ppnField.text = "0.00"
                }

                // Load pelanggan
                val idPelanggan = rs.getInt("id_pelanggan")
                pelangganList.find { it.idProperty.get() == idPelanggan }?.let {
                    selectedPelanggan = it
                    pelangganField.text = it.namaProperty.get()
                    alamatField.text = it.alamatProperty.get()
                    teleponField.text = it.teleponProperty.get()
                }
            }

            // 2. Load detail produk dari detail_proforma
            detailList.clear()
            val detailStmt = conn.prepareStatement("""
                SELECT dp.*, p.nama_produk, p.uom, p.divisi, p.singkatan 
                FROM detail_proforma dp 
                JOIN produk p ON dp.id_produk = p.id_produk
                WHERE dp.id_proforma = ?
            """) // FIX: ganti 'dp.id' menjadi 'dp.id_proforma'
            detailStmt.setInt(1, idProforma)
            val detailRs = detailStmt.executeQuery()
            while(detailRs.next()) {
                val produk = ProdukData(
                    id = detailRs.getInt("id_produk"),
                    nama = detailRs.getString("nama_produk"),
                    uom = detailRs.getString("uom"),
                    qty = detailRs.getDouble("qty").toString(),
                    harga = detailRs.getDouble("harga").toString()
                )
                detailList.add(produk)
                hitungTotalBaris(produk)
            }
            updateTotals() // Panggil updateTotals setelah semua detail dimuat
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat data proforma ke invoice: ${e.message}")
        } finally {
            conn.close()
        }
    }

    // Method untuk memuat data invoice yang sudah ada (jika ada mode edit invoice)
    fun loadInvoice(idInvoice: Int) {
        this.idInvoiceBaru = idInvoice
        this.isEditMode = true
        simpanBtn.text = "Simpan"

        val conn = DatabaseHelper.getConnection()
        try {
            // 1. Load data master invoice
            val stmt = conn.prepareStatement("SELECT * FROM invoice WHERE id = ?")
            stmt.setInt(1, idInvoice)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                nomorField.text = rs.getString("nomor")
                tanggalPicker.value = LocalDate.parse(rs.getString("tanggal"))
                contractRefField.text = rs.getString("contract_ref")
                rs.getString("contract_date")?.let { contractDatePicker.value = LocalDate.parse(it) }

                val dpValue = rs.getDouble("dp")
                val subtotal = rs.getDouble("total")
                if (subtotal > 0) {
                    val dpPercentage = (dpValue / subtotal) * 100
                    dpField.text = String.format("%.2f", dpPercentage).replace(",", ".")
                } else {
                    dpField.text = "0.00"
                }

                val ppnAmount = rs.getDouble("tax")
                if (subtotal > 0) {
                    val ppnPercentage = (ppnAmount / subtotal) * 100
                    ppnField.text = String.format("%.2f", ppnPercentage).replace(",", ".")
                } else {
                    ppnField.text = "0.00"
                }

                // Load pelanggan
                val idPelanggan = rs.getInt("id_pelanggan")
                pelangganList.find { it.idProperty.get() == idPelanggan }?.let {
                    selectedPelanggan = it
                    pelangganField.text = it.namaProperty.get()
                    alamatField.text = it.alamatProperty.get()
                    teleponField.text = it.teleponProperty.get()
                }
            }

            // 2. Load detail produk
            detailList.clear()
            val detailStmt = conn.prepareStatement("""
                SELECT di.*, p.nama_produk, p.uom, p.divisi, p.singkatan 
                FROM detail_invoice di 
                JOIN produk p ON di.id_produk = p.id_produk
                WHERE di.id = ?
            """)
            detailStmt.setInt(1, idInvoice)
            val detailRs = detailStmt.executeQuery()
            while(detailRs.next()) {
                val produk = ProdukData(
                    id = detailRs.getInt("id_produk"),
                    nama = detailRs.getString("nama_produk"),
                    uom = detailRs.getString("uom"),
                    qty = detailRs.getDouble("qty").toString(),
                    harga = detailRs.getDouble("harga").toString()
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
                val screenBounds: Bounds = pelangganField.localToScreen(pelangganField.boundsInLocal)
                popup.show(pelangganField, screenBounds.minX, screenBounds.minY + screenBounds.height)
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
            val qty = item.qtyProperty.get().toDoubleOrNull() ?: 0.0
            val harga = item.hargaProperty.get().toDoubleOrNull() ?: 0.0
            val total = qty * harga
            item.totalProperty.set(total.toString())
        } catch (e: Exception) {
            item.totalProperty.set("0")
        }
        table.refresh()
    }

    private fun updateTotals() {
        // 1. Hitung nilai dasar
        val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", ".").toDoubleOrNull() ?: 0.0 }
        val ppnRate = ppnField.text.toDoubleOrNull() ?: 0.0
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
        val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", ".").toDoubleOrNull() ?: 0.0 }
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
}