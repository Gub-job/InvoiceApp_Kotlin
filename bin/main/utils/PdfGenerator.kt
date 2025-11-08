package utils

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.kernel.colors.ColorConstants
import model.ProdukData
import java.io.File

object PdfGenerator {
    
    data class DocumentData(
        val documentType: String,
        val nomorDokumen: String,
        val tanggalDokumen: String,
        val namaPelanggan: String,
        val alamatPelanggan: String,
        val teleponPelanggan: String,
        val items: List<ProdukData>,
        val subtotal: String,
        val dp: String,
        val ppn: String,
        val grandTotal: String,
        val contractRef: String? = null,
        val contractDate: String? = null
    )
    
    fun generatePdf(data: DocumentData, file: File) {
        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)
        
        // Header dengan border
        document.add(Paragraph(data.documentType)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(24f)
            .setBold()
            .setMarginBottom(20f))
        
        // Info dokumen dalam tabel 2 kolom
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        infoTable.addCell(Cell().add(Paragraph("No: ${data.nomorDokumen}").setFontSize(12f)).setBorder(null))
        infoTable.addCell(Cell().add(Paragraph("Tanggal: ${data.tanggalDokumen}").setFontSize(12f)).setBorder(null))
        
        if (!data.contractRef.isNullOrBlank()) {
            infoTable.addCell(Cell().add(Paragraph("Contract Ref: ${data.contractRef}").setFontSize(12f)).setBorder(null))
            infoTable.addCell(Cell().add(Paragraph("Contract Date: ${data.contractDate ?: ""}").setFontSize(12f)).setBorder(null))
        }
        document.add(infoTable.setMarginBottom(20f))
        
        // Customer Info dengan border
        document.add(Paragraph("Kepada:").setFontSize(14f).setBold().setMarginBottom(5f))
        val customerTable = Table(1).useAllAvailableWidth()
        customerTable.addCell(Cell().add(Paragraph(data.namaPelanggan).setFontSize(12f)).setBorder(SolidBorder(1f)))
        customerTable.addCell(Cell().add(Paragraph(data.alamatPelanggan).setFontSize(12f)).setBorder(SolidBorder(1f)))
        customerTable.addCell(Cell().add(Paragraph("Telp: ${data.teleponPelanggan}").setFontSize(12f)).setBorder(SolidBorder(1f)))
        document.add(customerTable.setMarginBottom(20f))
        
        // Items Table dengan styling
        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(0.8f, 3.5f, 1f, 1f, 1.5f, 1.5f))).useAllAvailableWidth()
        
        // Header dengan background
        itemsTable.addHeaderCell(Cell().add(Paragraph("No").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY))
        itemsTable.addHeaderCell(Cell().add(Paragraph("Nama Produk").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY))
        itemsTable.addHeaderCell(Cell().add(Paragraph("UOM").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY))
        itemsTable.addHeaderCell(Cell().add(Paragraph("Qty").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT))
        itemsTable.addHeaderCell(Cell().add(Paragraph("Harga").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT))
        itemsTable.addHeaderCell(Cell().add(Paragraph("Total").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT))
        
        // Data rows
        data.items.forEachIndexed { index, item ->
            itemsTable.addCell(Cell().add(Paragraph((index + 1).toString())).setTextAlignment(TextAlignment.CENTER))
            itemsTable.addCell(Cell().add(Paragraph(item.namaProperty.get())))
            itemsTable.addCell(Cell().add(Paragraph(item.uomProperty.get())).setTextAlignment(TextAlignment.CENTER))
            itemsTable.addCell(Cell().add(Paragraph(item.qtyProperty.get())).setTextAlignment(TextAlignment.RIGHT))
            itemsTable.addCell(Cell().add(Paragraph(item.hargaProperty.get())).setTextAlignment(TextAlignment.RIGHT))
            itemsTable.addCell(Cell().add(Paragraph(item.totalProperty.get())).setTextAlignment(TextAlignment.RIGHT))
        }
        
        document.add(itemsTable.setMarginBottom(20f))
        
        // Summary dalam tabel
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        summaryTable.addCell(Cell().add(Paragraph("Subtotal:").setBold()).setBorder(null).setTextAlignment(TextAlignment.RIGHT))
        summaryTable.addCell(Cell().add(Paragraph(data.subtotal)).setBorder(null).setTextAlignment(TextAlignment.RIGHT))
        
        if (data.dp != "0.00") {
            summaryTable.addCell(Cell().add(Paragraph("DP Amount:").setBold()).setBorder(null).setTextAlignment(TextAlignment.RIGHT))
            summaryTable.addCell(Cell().add(Paragraph(data.dp)).setBorder(null).setTextAlignment(TextAlignment.RIGHT))
        }
        
        summaryTable.addCell(Cell().add(Paragraph("PPN:").setBold()).setBorder(null).setTextAlignment(TextAlignment.RIGHT))
        summaryTable.addCell(Cell().add(Paragraph(data.ppn)).setBorder(null).setTextAlignment(TextAlignment.RIGHT))
        
        summaryTable.addCell(Cell().add(Paragraph("Grand Total:").setBold().setFontSize(14f)).setBorder(SolidBorder(1f)).setTextAlignment(TextAlignment.RIGHT))
        summaryTable.addCell(Cell().add(Paragraph(data.grandTotal).setBold().setFontSize(14f)).setBorder(SolidBorder(1f)).setTextAlignment(TextAlignment.RIGHT))
        
        document.add(summaryTable)
        
        document.close()
    }
}