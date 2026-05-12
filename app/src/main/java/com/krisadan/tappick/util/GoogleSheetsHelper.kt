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
    private const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbwykYzMilpfe70y1zNvn3NfShqZZmEkRdTrUCH-nReOMnifmrexyTqwppTWsPQkmKE7/exec"
    
    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val thaiLocale = Locale("th", "TH")
    private val sdfDate = SimpleDateFormat("dd/MM/yy", thaiLocale)
    private val sdfTime = SimpleDateFormat("HH:mm", thaiLocale)

    fun uploadEntry(entry: HistoryEntry, roleName: String) {
        Thread {
            try {
                val dateStr = sdfDate.format(Date(entry.timestamp))
                val timeStr = sdfTime.format(Date(entry.timestamp))

                val rowsArray = JsonArray()
                entry.items.forEach { item ->
                    val rowArray = JsonArray().apply {
                        add(dateStr)
                        add(timeStr)
                        add(entry.memberName)
                        add(roleName)
                        add(item.productName)
                        add(item.quantity.toString())
                    }
                    rowsArray.add(rowArray)
                }

                val jsonBody = JsonObject().apply { add("rows", rowsArray) }
                val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(WEB_APP_URL).post(body).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) Log.e(TAG, "Upload Failed: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Error: ${e.message}")
            }
        }.start()
    }

    fun requestPinReset(email: String, memberName: String, pin: String, onResult: (Boolean) -> Unit) {
        Thread {
            try {
                val jsonBody = JsonObject().apply {
                    addProperty("action", "forgot_pin")
                    addProperty("email", email)
                    addProperty("name", memberName)
                    addProperty("pin", pin)
                }
                val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(WEB_APP_URL).post(body).build()

                client.newCall(request).execute().use { response ->
                    onResult(response.isSuccessful)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }.start()
    }
}
