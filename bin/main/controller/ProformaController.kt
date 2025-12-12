package controller

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.*
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.util.Callback
import model.PelangganData
import model.ProdukData

import utils.PrintPreview
import utils.DatabaseHelper
import utils.NomorGenerator
import utils.CreateProformaTables
import java.sql.*
import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale

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
    @FXML private lateinit var tableContainer: VBox // Ganti TableView dengan VBox di FXML
    @FXML private lateinit var simpanBtn: Button
    @FXML private lateinit var hapusBtn: Button
    @FXML private lateinit var hapusItemBtn: Button
    @FXML private lateinit var tambahBtn: Button
    @FXML private lateinit var cloneBtn: Button
    @FXML private lateinit var konversiBtn: Button
    @FXML private lateinit var cetakBtn: Button
    // Tambahkan @FXML untuk komponen PPN dari FXML
    @FXML private lateinit var ppnCheckBox: CheckBox
    @FXML private lateinit var subtotalLabel: Label
    @FXML private lateinit var dpAmountLabel: Label
    @FXML private lateinit var ppnAmountLabel: Label
    @FXML private lateinit var grandTotalLabel: Label

    // Komponen baru untuk menggantikan TableView
    private lateinit var tableGrid: GridPane

    private val pelangganList = FXCollections.observableArrayList<PelangganData>()
    private val produkList = FXCollections.observableArrayList<ProdukData>()
    private val detailList = FXCollections.observableArrayList<ProdukData>()

    private val popup = Popup()
    private val listView = ListView<PelangganData>()
    private val produkPopup = Popup()
    private val produkListView = ListView<ProdukData>()

    // TextField terpusat untuk autocomplete produk yang lebih stabil
    private val produkSearchField = TextField()

    private var selectedPelanggan: PelangganData? = null
    private var idPerusahaan: Int = 0
    private var idProformaBaru: Int = 0
    private var isEditMode = false
    private var currentProdukField: TextField? = null
    private var currentProdukData: ProdukData? = null    

    fun setIdPerusahaan(id: Int) {
        idPerusahaan = id
        // Pastikan tabel proforma dan invoice ada
        CreateProformaTables.createTables()
        loadPelanggan()
        loadProduk()
        loadDefaultTaxRate()
        ensureStatusColumnExist() // Tambahkan ini untuk memastikan kolom status ada
        // Set tanggal hari ini
        tanggalPicker.value = LocalDate.now()
    }

    fun loadProforma(idProforma: Int) {
        this.idProformaBaru = idProforma
        this.isEditMode = true
        simpanBtn.text = "Simpan"
        hapusBtn.isDisable = false
        cloneBtn.isDisable = false
        konversiBtn.isDisable = false

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
                    ppnCheckBox.isSelected = ppnPercentage > 0
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
                    qty = detailRs.getDouble("qty").toString(), // Load sebagai angka murni
                    harga = detailRs.getDouble("harga").toString(), // Load sebagai angka murni
                    divisi = detailRs.getString("divisi") ?: "",
                    singkatan = detailRs.getString("singkatan") ?: ""
                )
                detailList.add(produk)
                hitungTotalBaris(produk)
            }
            updateTotals()
            redrawTable() // Gambar ulang grid setelah memuat data
        } catch (e: Exception) {
            showAlert("Error", "Gagal memuat data proforma: ${e.message}")
        } finally {
            conn.close()
        }
    }

    private fun ensureStatusColumnExist(conn: Connection? = null) {
        val connection = conn ?: DatabaseHelper.getConnection()
        try {
            val metaData = connection.metaData
            val rs = metaData.getColumns(null, null, "proforma", "status")
            if (!rs.next()) {
                connection.createStatement().execute("ALTER TABLE proforma ADD COLUMN status TEXT")
                println("Kolom 'status' ditambahkan ke tabel proforma.")
            }
        } catch (e: Exception) {
            println("Gagal memastikan kolom status ada di proforma: ${e.message}")
        } finally {
            if (conn == null) {
                connection.close()
            }
        }
    }

    @FXML
    fun initialize() {
        setupTableGrid()

        setupPelangganAutocomplete()
        setupProdukAutocomplete()

        tambahBtn.setOnAction {
            addNewRow()
        }

        hapusItemBtn.setOnAction {
            if (detailList.isNotEmpty()) {
                detailList.removeAt(detailList.size - 1)
                redrawTable()
                updateTotals()
            }
        }

        simpanBtn.setOnAction {
            simpanProformaDanDetail()
        }

        cetakBtn.setOnAction {
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
        
        // Listener untuk PPN checkbox
        ppnCheckBox.selectedProperty().addListener { _, _, _ ->
            updateTotals()
        }




    }

    private fun setupTableGrid() {
        tableGrid = GridPane().apply {
            hgap = 5.0
            vgap = 5.0
            padding = javafx.geometry.Insets(10.0)
        }

        // Atur constraint kolom agar lebarnya sesuai
        val colNo = ColumnConstraints().apply { percentWidth = 5.0 }
        val colNama = ColumnConstraints().apply { percentWidth = 40.0 }
        val colQty = ColumnConstraints().apply { percentWidth = 10.0 }
        val colUom = ColumnConstraints().apply { percentWidth = 10.0 }
        val colHarga = ColumnConstraints().apply { percentWidth = 15.0 }
        val colTotal = ColumnConstraints().apply { percentWidth = 20.0 }
        tableGrid.columnConstraints.addAll(colNo, colNama, colQty, colUom, colHarga, colTotal)

        val scrollPane = ScrollPane(tableGrid).apply {
            isFitToWidth = true
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        }
        tableContainer.children.clear()
        tableContainer.children.add(scrollPane)
        VBox.setVgrow(scrollPane, Priority.ALWAYS)

        redrawTable()
    }

    private fun redrawTable() {
        tableGrid.children.clear()

        // Header
        val headers = listOf("No", "Nama Barang", "Qty", "UOM", "Harga", "Total")
        headers.forEachIndexed { index, text ->
            val headerLabel = Label(text).apply { style = "-fx-font-weight: bold;" }
            tableGrid.add(headerLabel, index, 0)
        }

        // Data Rows
        detailList.forEachIndexed { rowIndex, produkData ->
            val actualRow = rowIndex + 1

            // No
            tableGrid.add(Label(actualRow.toString()), 0, actualRow)

            // Nama Barang (TextField dengan Autocomplete)
            val namaField = TextField(produkData.namaProperty.get()).apply {
                textProperty().bindBidirectional(produkData.namaProperty)
                promptText = "Ketik nama produk"
                maxWidth = Double.MAX_VALUE
                
                // Set focus listener untuk menyimpan referensi
                focusedProperty().addListener { _, _, focused ->
                    if (focused) {
                        currentProdukField = this
                        currentProdukData = produkData
                    }
                }
                
                // Autocomplete saat mengetik
                textProperty().addListener { _, _, newValue ->
                    currentProdukField = this
                    currentProdukData = produkData
                    if (newValue.isNotEmpty()) {
                        val filtered = produkList.filter { 
                            it.namaProperty.get().contains(newValue, ignoreCase = true) 
                        }
                        if (filtered.isNotEmpty()) {
                            produkListView.items.setAll(filtered)
                            produkListView.selectionModel.selectFirst()
                            if (!produkPopup.isShowing) {
                                val bounds = localToScreen(boundsInLocal)
                                if (bounds != null) {
                                    produkPopup.show(this, bounds.minX, bounds.minY + bounds.height)
                                }
                            }
                        } else {
                            produkPopup.hide()
                        }
                    } else {
                        produkPopup.hide()
                    }
                }
                
                // Keyboard navigation
                setOnKeyPressed { event ->
                    when (event.code) {
                        javafx.scene.input.KeyCode.DOWN -> {
                            if (produkPopup.isShowing) {
                                produkListView.requestFocus()
                                if (produkListView.selectionModel.isEmpty) {
                                    produkListView.selectionModel.selectFirst()
                                }
                            }
                            event.consume()
                        }
                        javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                            if (produkPopup.isShowing && produkListView.items.isNotEmpty()) {
                                val selected = produkListView.selectionModel.selectedItem ?: produkListView.items[0]
                                produkData.idProperty.set(selected.idProperty.get())
                                produkData.namaProperty.set(selected.namaProperty.get())
                                produkData.uomProperty.set(selected.uomProperty.get())
                                produkData.divisiProperty.set(selected.divisiProperty.get())
                                produkData.singkatanProperty.set(selected.singkatanProperty.get())
                                produkPopup.hide()
                                updateNomorIfReady()
                                redrawTable()
                                
                                // Pindah fokus ke field Qty di baris yang sama
                                javafx.application.Platform.runLater {
                                    val rowIndex = detailList.indexOf(produkData) + 1
                                    val qtyField = tableGrid.children.find { 
                                        GridPane.getRowIndex(it) == rowIndex && GridPane.getColumnIndex(it) == 2 
                                    } as? TextField
                                    qtyField?.requestFocus()
                                }
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
            tableGrid.add(namaField, 1, actualRow)

            // Qty
            val qtyField = TextField(produkData.qtyProperty.get()).apply {
                textProperty().bindBidirectional(produkData.qtyProperty)
                textProperty().addListener { _, _, _ ->
                    hitungTotalBaris(produkData)
                    updateTotals()
                }
            }
            tableGrid.add(qtyField, 2, actualRow)

            // UOM
            val uomLabel = Label().apply { textProperty().bind(produkData.uomProperty) }
            tableGrid.add(uomLabel, 3, actualRow)

            // Harga
            val hargaField = TextField(produkData.hargaProperty.get()).apply {
                textProperty().bindBidirectional(produkData.hargaProperty)
                textProperty().addListener { _, _, _ ->
                    hitungTotalBaris(produkData)
                    updateTotals()
                }
                // LOGIKA TAB TERAKHIR
                setOnKeyPressed { event ->
                    if (event.code == javafx.scene.input.KeyCode.TAB) {
                        // Jika ini baris terakhir, buat baris baru
                        if (rowIndex == detailList.size - 1) {
                            addNewRow()
                        }
                    }
                }
            }
            tableGrid.add(hargaField, 4, actualRow)

            // Total
            val totalLabel = Label().apply { textProperty().bind(produkData.totalProperty) }
            tableGrid.add(totalLabel, 5, actualRow)
        }
    }

    private fun addNewRow() {
        val newProduk = ProdukData(0, "", "", "")
        detailList.add(newProduk)
        redrawTable()
        // Fokus ke field nama di baris baru
        javafx.application.Platform.runLater {
            (tableGrid.children.lastOrNull { GridPane.getRowIndex(it) == detailList.size && GridPane.getColumnIndex(it) == 1 } as? ComboBox<*>)?.requestFocus()
        }
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

        produkListView.setOnMouseClicked {
            if (it.clickCount == 1 && produkListView.selectionModel.selectedItem != null) {
                val selected = produkListView.selectionModel.selectedItem
                if (currentProdukData != null && currentProdukField != null) {
                    currentProdukData!!.idProperty.set(selected.idProperty.get())
                    currentProdukData!!.namaProperty.set(selected.namaProperty.get())
                    currentProdukData!!.uomProperty.set(selected.uomProperty.get())
                    currentProdukData!!.divisiProperty.set(selected.divisiProperty.get())
                    currentProdukData!!.singkatanProperty.set(selected.singkatanProperty.get())
                    updateNomorIfReady()
                    redrawTable()
                }
                produkPopup.hide()
            }
        }

        produkListView.setOnKeyPressed { event ->
            when (event.code) {
                javafx.scene.input.KeyCode.ENTER, javafx.scene.input.KeyCode.TAB -> {
                    val selected = produkListView.selectionModel.selectedItem
                    if (selected != null && currentProdukData != null) {
                        currentProdukData!!.idProperty.set(selected.idProperty.get())
                        currentProdukData!!.namaProperty.set(selected.namaProperty.get())
                        currentProdukData!!.uomProperty.set(selected.uomProperty.get())
                        currentProdukData!!.divisiProperty.set(selected.divisiProperty.get())
                        currentProdukData!!.singkatanProperty.set(selected.singkatanProperty.get())
                        updateNomorIfReady()
                        redrawTable()
                        
                        // Pindah fokus ke field Qty di baris yang sama
                        javafx.application.Platform.runLater {
                            val rowIndex = detailList.indexOf(currentProdukData) + 1
                            val qtyField = tableGrid.children.find { 
                                GridPane.getRowIndex(it) == rowIndex && GridPane.getColumnIndex(it) == 2 
                            } as? TextField
                            qtyField?.requestFocus()
                        }
                    }
                    produkPopup.hide()
                    event.consume()
                }
                javafx.scene.input.KeyCode.ESCAPE -> {
                    produkPopup.hide()
                    currentProdukField?.requestFocus()
                    event.consume()
                }
                else -> {}
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

    private fun updateNomorIfReady() {
        // Generate nomor hanya jika ada produk yang valid dan tanggal sudah dipilih
        val firstProduct = detailList.firstOrNull { it.namaProperty.get().isNotBlank() }
        if (firstProduct != null && tanggalPicker.value != null) {
                nomorField.text = NomorGenerator.generateNomor(
                    idPerusahaan,
                    "proforma",
                    firstProduct.divisiProperty.get(),
                    firstProduct.namaProperty.get(),
                    firstProduct.singkatanProperty.get(),
                    tanggalPicker.value
                )
        }
    }

    private fun simpanProformaDanDetail() {
        if (isEditMode) {
            updateProformaDanDetail()
        } else {
            simpanProformaBaru()
        }
    }

    private fun simpanProformaBaru() {
        // Cek duplikat nomor
        if (isNomorProformaDuplikat(nomorField.text)) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Nomor Duplikat"
            alert.headerText = "Nomor ini sudah ada"
            alert.contentText = "Nomor proforma '${nomorField.text}' sudah digunakan. Apakah masih mau pakai nomor ini?"
            val result = alert.showAndWait()
            if (result.isEmpty || result.get() != ButtonType.OK) {
                return
            }
        }

        val conn = DatabaseHelper.getConnection()
        conn.autoCommit = false
        try {
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
            val ppnRate = if (ppnCheckBox.isSelected) 11.0 else 0.0
            val ppnAmount = subtotal * (ppnRate / 100.0)
            val totalDenganPpn = subtotal + ppnAmount

            // Ambil divisi dari produk pertama untuk disimpan
            val divisi = detailList.firstOrNull()?.divisiProperty?.get() ?: ""
            
            val sql = """
                INSERT INTO proforma (id_perusahaan, id_pelanggan, contract_ref, contract_date, 
                                    total, tax, total_dengan_ppn, no_proforma, tanggal_proforma, dp, divisi, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
            stmt.setInt(1, idPerusahaan)
            stmt.setInt(2, selectedPelanggan?.idProperty?.get() ?: 0)
            stmt.setString(3, contractRefField.text) // Gunakan nilai dari field contract ref
            stmt.setString(4, contractDatePicker.value?.toString()) // Gunakan nilai dari field contract date
            stmt.setDouble(5, subtotal)
            stmt.setDouble(6, ppnAmount)
            stmt.setDouble(7, totalDenganPpn)
            stmt.setString(8, nomorField.text)
            stmt.setString(9, tanggalPicker.value?.toString() ?: LocalDate.now().toString())
            stmt.setDouble(10, calculateDPValue())
            stmt.setString(11, divisi)
            stmt.setString(11, "Aktif") // Menambahkan status default
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            if (rs.next()) idProformaBaru = rs.getInt(1)

            simpanDetailProforma(conn, idProformaBaru)

            conn.commit()
            showAlert("Sukses", "Proforma berhasil disimpan.")
            isEditMode = true
            simpanBtn.text = "Simpan"
            hapusBtn.isDisable = false
            cloneBtn.isDisable = false
            konversiBtn.isDisable = false
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
            val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
            val ppnRate = if (ppnCheckBox.isSelected) 11.0 else 0.0
            val ppnAmount = subtotal * (ppnRate / 100.0)
            val totalDenganPpn = subtotal + ppnAmount
            
            // Ambil divisi dari produk pertama untuk diupdate
            val divisi = detailList.firstOrNull()?.divisiProperty?.get() ?: ""

            val sql = """
                UPDATE proforma SET 
                    id_pelanggan = ?, contract_ref = ?, contract_date = ?, total = ?, tax = ?, 
                    total_dengan_ppn = ?, no_proforma = ?, tanggal_proforma = ?, dp = ?, divisi = ?
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
            stmt.setString(10, divisi)
            stmt.setInt(11, idProformaBaru)
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
            val qty = produk.qtyProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0
            val harga = produk.hargaProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0
            detailStmt.setDouble(3, qty)
            detailStmt.setDouble(4, harga)
            detailStmt.setDouble(5, qty * harga)
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
        if (idProformaBaru == 0) {
            showAlert("Peringatan", "Silakan simpan proforma terlebih dahulu sebelum mencetak.")
            return
        }

        try {
            val grandTotalValue = grandTotalLabel.text.replace(",", "").toDoubleOrNull() ?: 0.0
            val terbilang = utils.Terbilang.convert(grandTotalValue)
            
            val data = model.DocumentData(
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
                terbilang = terbilang,
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
            
            if (filtered.isNotEmpty() && pelangganField.scene?.window?.isShowing == true) {
                val screenBounds = pelangganField.localToScreen(pelangganField.boundsInLocal)
                if (screenBounds != null) {
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
            val qtyText = item.qtyProperty.get().replace(",", "")
            val hargaText = item.hargaProperty.get().replace(",", "")
            val qty = qtyText.toDoubleOrNull() ?: 0.0
            val harga = hargaText.toDoubleOrNull() ?: 0.0
            val total = qty * harga
            item.totalProperty.set(total.toString()) // Simpan total sebagai angka murni
        } catch (e: Exception) {
            item.totalProperty.set("0.0")
        }
        // table.refresh() // Tidak perlu dengan binding
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
                ppnCheckBox.isSelected = rs.getDouble("default_tax_rate") > 0
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

    private fun isNomorProformaDuplikat(nomor: String): Boolean {
        val conn = DatabaseHelper.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT COUNT(*) FROM proforma WHERE no_proforma = ? AND id_perusahaan = ?")
            stmt.setString(1, nomor)
            stmt.setInt(2, idPerusahaan)
            val rs = stmt.executeQuery()
            return rs.next() && rs.getInt(1) > 0
        } finally {
            conn.close()
        }
    }

        @FXML
    private fun onCloneProformaClicked() {
        if (idProformaBaru == 0) {
            showAlert("Peringatan", "Silakan simpan proforma terlebih dahulu.")
            return
        }

        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Konfirmasi Clone"
        alert.headerText = "Clone Proforma"
        alert.contentText = "Apakah Anda yakin ingin membuat salinan proforma ini?"
        
        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            val conn = DatabaseHelper.getConnection()
            conn.autoCommit = false
            try {
                val subtotal = detailList.sumOf { it.totalProperty.get().replace(",", "").toDoubleOrNull() ?: 0.0 }
                val ppnRate = if (ppnCheckBox.isSelected) 11.0 else 0.0
                val ppnAmount = subtotal * (ppnRate / 100.0)
                val totalDenganPpn = subtotal + ppnAmount
                
                // Generate nomor proforma baru
                val firstProduct = detailList.firstOrNull()
                val tanggalClone = tanggalPicker.value ?: LocalDate.now()
                val nomorBaru = if (firstProduct != null) {
                    NomorGenerator.generateNomor(
                        idPerusahaan,
                        "proforma",
                        firstProduct.divisiProperty.get(),
                        firstProduct.namaProperty.get(),
                        firstProduct.singkatanProperty.get(),
                        tanggalClone
                    )
                } else {
                    "PRO-" + System.currentTimeMillis()
                }

                // Ambil divisi dari produk pertama untuk disimpan
                val divisi = detailList.firstOrNull()?.divisiProperty?.get() ?: ""
                
                val sql = """
                    INSERT INTO proforma (id_perusahaan, id_pelanggan, contract_ref, contract_date, 
                                        total, tax, total_dengan_ppn, no_proforma, tanggal_proforma, dp, divisi, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
                val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                stmt.setInt(1, idPerusahaan)
                stmt.setInt(2, selectedPelanggan?.idProperty?.get() ?: 0)
                stmt.setString(3, nomorBaru) // contract_ref menggunakan nomor BARU
                stmt.setString(4, tanggalClone.toString()) // contract_date menggunakan tanggal BARU
                stmt.setDouble(5, subtotal)
                stmt.setDouble(6, ppnAmount)
                stmt.setDouble(7, totalDenganPpn)
                stmt.setString(8, nomorBaru)
                stmt.setString(9, tanggalClone.toString())
                stmt.setDouble(10, calculateDPValue())
                stmt.setString(11, divisi)
                stmt.setString(12, "Aktif") // PERBAIKAN: Index 12 untuk status
                stmt.executeUpdate()

                val rs = stmt.generatedKeys
                val idProformaClone = if (rs.next()) rs.getInt(1) else 0

                simpanDetailProforma(conn, idProformaClone)

                conn.commit()
                showAlert("Sukses", "Proforma berhasil di-clone dengan nomor: $nomorBaru")
                (cloneBtn.scene.window as? Stage)?.close()
            } catch (e: Exception) {
                conn.rollback()
                showAlert("Error", "Gagal clone proforma: ${e.message}")
                e.printStackTrace() // Tambahkan ini untuk debug
            } finally {
                conn.close()
            }
        }
    }

    @FXML
    private fun onKonversiKeInvoiceClicked() {
        if (idProformaBaru == 0) {
            showAlert("Peringatan", "Silakan simpan proforma terlebih dahulu.")
            return
        }

        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Konfirmasi Konversi"
        alert.headerText = "Konversi ke Invoice"
        alert.contentText = "Apakah Anda yakin ingin mengkonversi proforma ini menjadi invoice?"
        
        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            val conn = DatabaseHelper.getConnection()
            conn.autoCommit = false
            try {
                // Ambil data proforma
                val proformaStmt = conn.prepareStatement("SELECT * FROM proforma WHERE id_proforma = ?")
                proformaStmt.setInt(1, idProformaBaru)
                val proformaRs = proformaStmt.executeQuery()
                
                if (proformaRs.next()) {
                    // Insert ke invoice
                    val invoiceStmt = conn.prepareStatement(
                        """INSERT INTO invoice (id_perusahaan, id_pelanggan, nomor_invoice, tanggal, dp, tax, total, total_dengan_ppn, contract_ref, contract_date)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                        Statement.RETURN_GENERATED_KEYS
                    )
                    invoiceStmt.setInt(1, proformaRs.getInt("id_perusahaan"))
                    invoiceStmt.setInt(2, proformaRs.getInt("id_pelanggan"))
                    
                    // Generate nomor invoice baru menggunakan data produk pertama
                    val firstProduct = detailList.firstOrNull()
                    val nomorInvoice = if (firstProduct != null) {
                        NomorGenerator.generateNomor(
                            idPerusahaan,
                            "invoice",
                            firstProduct.divisiProperty.get(),
                            firstProduct.namaProperty.get(),
                            firstProduct.singkatanProperty.get(),
                            tanggalPicker.value ?: LocalDate.now()
                        )
                    } else {
                        "INV-" + System.currentTimeMillis()
                    }
                    
                    invoiceStmt.setString(3, nomorInvoice)
                    invoiceStmt.setString(4, LocalDate.now().toString())
                    invoiceStmt.setDouble(5, proformaRs.getDouble("dp"))
                    invoiceStmt.setDouble(6, proformaRs.getDouble("tax"))
                    invoiceStmt.setDouble(7, proformaRs.getDouble("total"))
                    invoiceStmt.setDouble(8, proformaRs.getDouble("total_dengan_ppn"))
                    invoiceStmt.setString(9, proformaRs.getString("contract_ref"))
                    invoiceStmt.setString(10, proformaRs.getString("contract_date"))
                    invoiceStmt.executeUpdate()
                    
                    val invoiceRs = invoiceStmt.generatedKeys
                    val idInvoice = if (invoiceRs.next()) invoiceRs.getInt(1) else 0
                    
                    // Copy detail proforma ke detail invoice
                    val detailStmt = conn.prepareStatement(
                        "SELECT * FROM detail_proforma WHERE id_proforma = ?"
                    )
                    detailStmt.setInt(1, idProformaBaru)
                    val detailRs = detailStmt.executeQuery()
                    
                    while (detailRs.next()) {
                        val insertDetailStmt = conn.prepareStatement(
                            "INSERT INTO detail_invoice (id_invoice, id_produk, qty, harga, total) VALUES (?, ?, ?, ?, ?)"
                        )
                        insertDetailStmt.setInt(1, idInvoice)
                        insertDetailStmt.setInt(2, detailRs.getInt("id_produk"))
                        insertDetailStmt.setDouble(3, detailRs.getDouble("qty"))
                        insertDetailStmt.setDouble(4, detailRs.getDouble("harga"))
                        insertDetailStmt.setDouble(5, detailRs.getDouble("total"))
                        insertDetailStmt.executeUpdate()
                    }
                    
                    conn.commit()
                    showAlert("Sukses", "Proforma berhasil dikonversi menjadi invoice dengan nomor: $nomorInvoice")
                    (konversiBtn.scene.window as? Stage)?.close()
                }
            } catch (e: Exception) {
                conn.rollback()
                showAlert("Error", "Gagal mengkonversi proforma: ${e.message}")
            } finally {
                conn.close()
            }
        }
    }

    @FXML
    private fun onHapusProformaClicked() {
        if (idProformaBaru == 0) {
            showAlert("Peringatan", "Tidak ada proforma yang dapat dihapus.")
            return
        }

        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Konfirmasi Hapus"
        alert.headerText = "Hapus Proforma"
        alert.contentText = "Apakah Anda yakin ingin menghapus proforma ini?"
        
        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            val conn = DatabaseHelper.getConnection()
            try {
                conn.prepareStatement("DELETE FROM detail_proforma WHERE id_proforma = ?").apply {
                    setInt(1, idProformaBaru)
                    executeUpdate()
                }
                conn.prepareStatement("DELETE FROM proforma WHERE id_proforma = ?").apply {
                    setInt(1, idProformaBaru)
                    executeUpdate()
                }
                showAlert("Sukses", "Proforma berhasil dihapus.")
                (hapusBtn.scene.window as? Stage)?.close()
            } catch (e: Exception) {
                showAlert("Error", "Gagal menghapus proforma: ${e.message}")
            } finally {
                conn.close()
            }
        }
    }
}