package controller

import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.util.Callback
import model.PelangganData
import model.ProdukData
import utils.DatabaseHelper
import utils.NomorGenerator
import utils.PdfGenerator
import utils.CreateProformaTables
import java.sql.*
import java.time.LocalDate

class ProformaController {

    @FXML private lateinit var pelangganField: TextField
    @FXML private lateinit var nomorField: TextField
    @FXML private lateinit var contractRefField: TextField
    @FXML private lateinit var contractDatePicker: DatePicker
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
    @FXML private lateinit var konversiBtn: Button
    @FXML private lateinit var hapusBtn: Button
    @FXML private lateinit var tambahBtn: Button
    // Tambahkan @FXML untuk komponen PPN dari FXML
    @FXML private lateinit var ppnField: Label
    @FXML private lateinit var subtotalLabel: Label
    @FXML private lateinit var dpAmountLabel: Label
    @FXML private lateinit var ppnAmountLabel: Label
    @FXML private lateinit var grandTotalLabel: Label

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private val detailList = FXCollections.observableArrayList<ProdukData>()

    private val popup = Popup()
    private val listView = ListView<PelangganData>()
    private val produkPopup = Popup()
    private val produkListView = ListView<ProdukData>()

    private var selectedPelanggan: PelangganData? = null
    private var idPerusahaan: Int = 0
    private var idProformaBaru: Int = 0
    private var isEditMode = false
    private var currentEditingCell: TextFieldTableCell<ProdukData, String>? = null

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        // Pastikan tabel proforma dan invoice ada
        CreateProformaTables.createTables()
        loadPelanggan()
        loadProduk()
        loadDefaultTaxRate()
        // Set tanggal hari ini
        tanggalPicker.value = LocalDate.now()
    }

    fun loadProforma(idProforma: Int) {
        this.idProformaBaru = idProforma
        this.isEditMode = true
        simpanBtn.text = "Simpan" // Ganti teks tombol

        val conn = DatabaseHelper.getConnection()
        try {
            // 1. Load data master proforma
            val stmt = conn.prepareStatement("SELECT * FROM proforma WHERE id_proforma = ?")
            stmt.setInt(1, idProforma)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                nomorField.text = rs.getString("no_proforma")
                tanggalPicker.value = LocalDate.parse(rs.getString("tanggal_proforma"))
                contractRefField.text = rs.getString("contract_ref")
                rs.getString("contract_date")?.let { contractDatePicker.value = LocalDate.parse(it) }
                
                val dpValue = rs.getDouble("dp")
                val subtotal = rs.getDouble("total")
                if (subtotal > 0) {
                    val dpPercentage = (dpValue / subtotal) * 100
                    dpField.text = String.format("%.2f", dpPercentage).replace(",", ".")
                }

                val taxAmount = rs.getDouble("tax")
                if (subtotal > 0) {
                    val ppnPercentage = (taxAmount / subtotal) * 100
                    ppnField.text = String.format("%.2f", ppnPercentage).replace(",", ".")
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
            val detailStmt = conn.prepareStatement("""
                SELECT dp.*, p.nama_produk, p.uom, p.divisi, p.singkatan 
                FROM detail_proforma dp 
                JOIN produk p ON dp.id_produk = p.id_produk
                WHERE dp.id_proforma = ?
            """)
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
            updateTotals()
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat data proforma: ${e.message}")
        } finally {
            conn.close()
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
        
        // Custom cell factory untuk kolomNama dengan autocomplete smooth
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
                                    event.consume()
                                    if (produkPopup.isShowing && produkListView.items.isNotEmpty()) {
                                        val selected = produkListView.selectionModel.selectedItem 
                                            ?: produkListView.items[0]
                                        applyProdukAndMoveNext(selected)
                                    } else {
                                        commitEdit(tf.text)
                                        moveToNextColumn()
                                    }
                                }
                                javafx.scene.input.KeyCode.ENTER -> {
                                    if (produkPopup.isShowing && produkListView.items.isNotEmpty()) {
                                        val selected = produkListView.selectionModel.selectedItem 
                                            ?: produkListView.items[0]
                                        applyProdukAndMoveNext(selected)
                                    } else {
                                        commitEdit(tf.text)
                                        moveToNextColumn()
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
                                else -> {}
                            }
                        }
                        
                        // Auto focus
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
        setupProdukAutocomplete()

        tambahBtn.setOnAction {
            // Tambah baris kosong baru
            val newProduk = ProdukData(0, "", "", "")
            detailList.add(newProduk)
            // Auto edit baris baru
            javafx.application.Platform.runLater {
                table.selectionModel.select(detailList.size - 1)
                table.edit(detailList.size - 1, kolomNama)
            }
        }

        hapusBtn.setOnAction {
            val selected = table.selectionModel.selectedItem
            if (selected != null) {
                detailList.remove(selected)
                hapusDetailDariDB(selected)
                updateTotals()
            }
        }

        simpanBtn.setOnAction {
            simpanProformaDanDetail()
        }

        konversiBtn.setOnAction {
            cetakProformaKePdf()
        }

        tanggalPicker.valueProperty().addListener { _, _, newDate ->
            updateNomorIfReady()
            // Menyamakan tanggal kontrak dengan tanggal proforma
            contractDatePicker.value = newDate
        }

        // Listener untuk DP - hitung otomatis jika input persen
        dpField.textProperty().addListener { _, _, _ ->
            // Panggil updateTotals agar Grand Total ikut dihitung ulang sesuai aturan.
            updateTotals()
        }
        // Filter untuk DP field - hanya angka
        dpField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches("\\d*\\.?\\d*".toRegex())) {
                dpField.text = oldValue
            } else {
                updateDPDisplay()
            }
        }
        
        // Contract ref mengikuti nomor proforma
        nomorField.textProperty().addListener { _, _, newValue ->
            contractRefField.text = newValue
        }
        
        updateDPDisplay() // Panggil saat inisialisasi
    }

    // ===========================================================
    // === AUTOCOMPLETE PRODUK (Smooth & Responsive)
    // ===========================================================
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

        // Mouse click - langsung apply dan pindah
        produkListView.setOnMouseClicked {
            if (it.clickCount == 1 && produkListView.selectionModel.selectedItem != null) {
                val selectedProduk = produkListView.selectionModel.selectedItem
                applyProdukAndMoveNext(selectedProduk)
            }
        }

        // Keyboard handler untuk ListView
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
        val firstProduct = detailList.firstOrNull { it.namaProperty.get().isNotBlank() }
        if (firstProduct != null && tanggalPicker.value != null) {
            val generatedNomor = NomorGenerator.generateNomor(
                idPerusahaan,
                "proforma",
                firstProduct.divisiProperty.get(),
                firstProduct.namaProperty.get(),
                firstProduct.singkatanProperty.get(),
                tanggalPicker.value
            )
            nomorField.text = generatedNomor
            contractRefField.text = generatedNomor
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

    // ===========================================================
    // === SIMPAN PROFORMA & DETAILNYA
    // ===========================================================
    private fun simpanProformaDanDetail() {
        if (isEditMode) {
            updateProformaDanDetail()
        } else {
            simpanProformaBaru()
        }
    }

    private fun simpanProformaBaru() {
        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", ".").toDoubleOrNull() ?: 0.0 }
            val ppnRate = ppnField.text.toDoubleOrNull() ?: 0.0
            val ppnAmount = subtotal * (ppnRate / 100.0)
            val totalDenganPpn = subtotal + ppnAmount

            val sql = """
                INSERT INTO proforma (id_perusahaan, id_pelanggan, contract_ref, contract_date, 
                                    total, tax, total_dengan_ppn, no_proforma, tanggal_proforma, dp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
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
            if (rs.next()) idProformaBaru = rs.getInt(1)

            simpanDetailProforma(conn, idProformaBaru)

            conn.commit()
            showAlert("Sukses", "Proforma berhasil disimpan.")
            isEditMode = true // Setelah simpan, masuk mode edit
            simpanBtn.text = "Simpan"
        } catch (e: Exception) {
            conn.rollback()
            showAlert("Error", "Gagal menyimpan proforma: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun updateProformaDanDetail() {
        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", ".").toDoubleOrNull() ?: 0.0 }
            val ppnRate = ppnField.text.toDoubleOrNull() ?: 0.0
            val ppnAmount = subtotal * (ppnRate / 100.0)
            val totalDenganPpn = subtotal + ppnAmount

            val sql = """
                UPDATE proforma SET 
                    id_pelanggan = ?, contract_ref = ?, contract_date = ?, total = ?, tax = ?, 
                    total_dengan_ppn = ?, no_proforma = ?, tanggal_proforma = ?, dp = ?
                WHERE id_proforma = ?
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
            stmt.setInt(10, idProformaBaru)
            stmt.executeUpdate()

            // Hapus detail lama dan masukkan yang baru
            val deleteStmt = conn.prepareStatement("DELETE FROM detail_proforma WHERE id_proforma = ?")
            deleteStmt.setInt(1, idProformaBaru)
            deleteStmt.executeUpdate()

            simpanDetailProforma(conn, idProformaBaru)

            conn.commit()
            showAlert("Sukses", "Proforma berhasil diupdate.")
            
            // Tutup jendela edit setelah berhasil update
            (simpanBtn.scene.window as? Stage)?.close()
        } catch (e: Exception) {
            conn.rollback()
            showAlert("Error", "Gagal mengupdate proforma: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun simpanDetailProforma(conn: Connection, idProforma: Int) {
        for (produk in detailList) {
            val detailStmt = conn.prepareStatement(
                """
                INSERT INTO detail_proforma (id_proforma, id_produk, qty, harga, total)
                VALUES (?, ?, ?, ?, ?)
                """
            )
            detailStmt.setInt(1, idProforma)
            detailStmt.setInt(2, produk.idProperty.get())
            detailStmt.setDouble(3, produk.qtyProperty.get().toDoubleOrNull() ?: 0.0)
            detailStmt.setDouble(4, produk.hargaProperty.get().toDoubleOrNull() ?: 0.0)
            val total = (produk.qtyProperty.get().toDoubleOrNull() ?: 0.0) *
                        (produk.hargaProperty.get().toDoubleOrNull() ?: 0.0)
            detailStmt.setDouble(5, total)
            detailStmt.executeUpdate()
        }
    }

    private fun hapusDetailDariDB(produk: ProdukData) {
        try {
            val conn = DatabaseHelper.getConnection()
            val stmt = conn.prepareStatement("DELETE FROM detail_proforma WHERE id_produk = ? AND id_proforma = ?")
            stmt.setInt(1, produk.idProperty.get())
            stmt.setInt(2, idProformaBaru)
            stmt.executeUpdate()
            conn.close()
        } catch (e: Exception) {
            println("Gagal hapus detail: ${e.message}")
        }
    }

    private fun cetakProformaKePdf() {
        val fileChooser = javafx.stage.FileChooser().apply {
            title = "Simpan Proforma sebagai PDF"
            initialFileName = "${nomorField.text.replace("/", "_")}.pdf"
            extensionFilters.add(javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"))
        }
        val file = fileChooser.showSaveDialog(konversiBtn.scene.window)

        if (file != null) {
            try {
                val data = PdfGenerator.DocumentData(
                    documentType = "PROFORMA INVOICE",
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
                PdfGenerator.generatePdf(data, file)
                showAlert("Sukses", "File PDF berhasil disimpan di:\n${file.absolutePath}")
            } catch (e: Exception) {
                showAlert("Error", "Gagal membuat file PDF: ${e.message}")
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    // ===========================================================
    // === AUTOCOMPLETE PELANGGAN
    // ===========================================================
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
            val qty = item.qtyProperty.get().toDoubleOrNull() ?: 0.0
            val harga = item.hargaProperty.get().toDoubleOrNull() ?: 0.0
            val total = qty * harga
            item.totalProperty.set(total.toString()) // Simpan sebagai angka, format nanti
        } catch (e: Exception) {
            item.totalProperty.set("0")
        }
        table.refresh()
    }

    private fun updateTotals() {
        // 1. Hitung nilai dasar
        val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
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
    }

    private fun calculateDPValue(): Double {
        val dpPercentage = dpField.text.toDoubleOrNull() ?: 0.0
        val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
        return subtotal * (dpPercentage / 100.0)
    }

    private fun updateDPDisplay() {
        val dpAmount = calculateDPValue()
        dpAmountLabel.text = String.format("%,.2f", dpAmount)
    }

    private fun loadDefaultTaxRate() {
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT default_tax_rate FROM perusahaan WHERE id = ?")
            stmt.setInt(1, idPerusahaan)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                ppnField.text = rs.getDouble("default_tax_rate").toString()
            }
        } catch (e: Exception) {
            println("Gagal memuat default tax rate: ${e.message}")
        } finally {
            conn.close()
        }
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