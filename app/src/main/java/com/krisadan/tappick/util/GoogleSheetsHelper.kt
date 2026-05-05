package com.krisadan.tappick.util

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.krisadan.tappick.data.model.HistoryEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleSheetsHelper {
    private const val TAG = "GoogleSheetsHelper"
    private const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbwpvpPBKV6DunfQKGfNwiF6yNvZXjsdKQ5jJ1aaIeo2tHXY9VEzLIlSaxHP0iNZrf9V/exec"
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val thaiLocale = Locale("th", "TH")
    private val sdfDate = SimpleDateFormat("dd/MM/yy", thaiLocale)
    private val sdfTime = SimpleDateFormat("HH:mm", thaiLocale)

    fun uploadEntry(entry: HistoryEntry, roleName: String) {
        val dateStr = sdfDate.format(Date(entry.timestamp))
        val timeStr = sdfTime.format(Date(entry.timestamp))

        val rows = entry.items.map { item ->
            listOf(
                dateStr,
                timeStr,
                entry.memberName,
                roleName,
                item.productName,
                item.quantity.toString()
            )
        }

        val jsonBody = JsonObject().apply {
            val rowsArray = JsonArray()
            rows.forEach { row ->
                val rowArray = JsonArray()
                row.forEach { rowArray.add(it) }
                rowsArray.add(rowArray)
            }
            add("rows", rowsArray)
        }

        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(WEB_APP_URL)
            .post(body)
            .build()

        Log.d(TAG, "Starting upload to Google Sheets...")

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        Log.d(TAG, "Upload Successful: $responseBody")
                    } else {
                        Log.e(TAG, "Upload Failed. Code: ${response.code}, Body: $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Error: ${e.message}", e)
            }
        }.start()
    }
}
