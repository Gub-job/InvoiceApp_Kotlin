package utils

object Terbilang {
    
    private val satuan = arrayOf("", "Satu", "Dua", "Tiga", "Empat", "Lima", "Enam", "Tujuh", "Delapan", "Sembilan")
    private val belasan = arrayOf("Sepuluh", "Sebelas", "Dua Belas", "Tiga Belas", "Empat Belas", "Lima Belas", "Enam Belas", "Tujuh Belas", "Delapan Belas", "Sembilan Belas")
    
    fun convert(angka: Double): String {
        if (angka == 0.0) return "Nol Rupiah"
        
        val angkaInt = angka.toLong()
        val desimal = ((angka - angkaInt) * 100).toInt()
        
        var hasil = convertAngka(angkaInt) + " Rupiah"
        
        if (desimal > 0) {
            hasil += " " + convertAngka(desimal.toLong()) + " Sen"
        }
        
        return hasil.trim()
    }
    
    private fun convertAngka(angka: Long): String {
        if (angka == 0L) return ""
        
        return when {
            angka < 10 -> satuan[angka.toInt()]
            angka < 20 -> belasan[(angka - 10).toInt()]
            angka < 100 -> {
                val puluhan = angka / 10
                val sisa = angka % 10
                satuan[puluhan.toInt()] + " Puluh" + (if (sisa > 0) " " + satuan[sisa.toInt()] else "")
            }
            angka < 200 -> "Seratus" + (if (angka > 100) " " + convertAngka(angka - 100) else "")
            angka < 1000 -> {
                val ratusan = angka / 100
                val sisa = angka % 100
                satuan[ratusan.toInt()] + " Ratus" + (if (sisa > 0) " " + convertAngka(sisa) else "")
            }
            angka < 2000 -> "Seribu" + (if (angka > 1000) " " + convertAngka(angka - 1000) else "")
            angka < 1000000 -> {
                val ribuan = angka / 1000
                val sisa = angka % 1000
                convertAngka(ribuan) + " Ribu" + (if (sisa > 0) " " + convertAngka(sisa) else "")
            }
            angka < 1000000000 -> {
                val jutaan = angka / 1000000
                val sisa = angka % 1000000
                convertAngka(jutaan) + " Juta" + (if (sisa > 0) " " + convertAngka(sisa) else "")
            }
            angka < 1000000000000 -> {
                val miliaran = angka / 1000000000
                val sisa = angka % 1000000000
                convertAngka(miliaran) + " Miliar" + (if (sisa > 0) " " + convertAngka(sisa) else "")
            }
            else -> {
                val triliunan = angka / 1000000000000
                val sisa = angka % 1000000000000
                convertAngka(triliunan) + " Triliun" + (if (sisa > 0) " " + convertAngka(sisa) else "")
            }
        }
    }
}
