package utils

import java.sql.Connection
import java.sql.DriverManager

object DatabaseHelper {
    private const val DB_PATH = "src/main/resources/database/invoice_app.db"

    fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:$DB_PATH")
    }
}
