package com.krisadan.tappick.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.PrinterSdk.Printer
import com.sunmi.printerx.api.LineApi
import com.sunmi.printerx.enums.Align
import com.sunmi.printerx.enums.SettingItem
import com.sunmi.printerx.style.BaseStyle
import com.sunmi.printerx.style.TextStyle

class SunmiPrinterHelper private constructor(private val context: Context) {
    private var printer: Printer? = null

    companion object {
        private const val TAG = "SunmiPrinterHelper"
        private const val MAX_LINE_WIDTH = 30
        
        @Volatile
        private var INSTANCE: SunmiPrinterHelper? = null

        fun getInstance(context: Context): SunmiPrinterHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SunmiPrinterHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        setLog(true)
        initPrinter()
    }

    fun setLog(enable: Boolean) {
        PrinterSdk.getInstance().log(enable, TAG)
    }

    private fun initPrinter() {
        try {
            PrinterSdk.getInstance().getPrinter(context, object : PrinterSdk.PrinterListen {
                override fun onDefPrinter(printer: Printer?) {
                    this@SunmiPrinterHelper.printer = printer
                    if (printer != null) {
                        Log.d(TAG, "Default printer initialized")
                    } else {
                        Log.e(TAG, "No default printer found")
                    }
                }
                override fun onPrinters(printers: MutableList<Printer>?) {
                    if (printer == null && !printers.isNullOrEmpty()) {
                        printer = printers[0]
                        Log.d(TAG, "Printer selected from list")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Sunmi Printer SDK", e)
        }
    }

    fun isPrinterReady(): Boolean {
        val printer = this.printer
        if (printer == null) {
            initPrinter()
            return false
        }
        return try {
            val status = printer.queryApi().status
            val statusName = status.name
            Log.d(TAG, "Printer status check: $statusName")
            statusName == "NORMAL" || statusName == "READY"
        } catch (e: Exception) {
            Log.e(TAG, "Printer status check failed", e)
            initPrinter() 
            false
        }
    }

    fun showPrinterSettings(activity: Activity) {
        try {
            PrinterSdk.getInstance().startSettings(activity, SettingItem.ALL)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open printer settings", e)
        }
    }

    private fun getLineApiInternal(): LineApi? {
        return try {
            printer?.lineApi()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get LineApi", e)
            null
        }
    }

    fun getLineApi(): LineApi? {
        if (!isPrinterReady()) {
            Log.e(TAG, "Printer is not ready (Check paper, cover or connection)")
            return null
        }
        return getLineApiInternal()
    }

    fun printReceipt(title: String, date: String, time: String, userName: String, items: List<com.krisadan.tappick.data.model.TransactionItem>) {
        if (!isPrinterReady()) {
            Log.e(TAG, "Printer not ready for receipt")
            return
        }

        val lineApi = getLineApiInternal() ?: return
        
        try {
            lineApi.apply {
                val sep = "------------------------------"

                initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
                printText(title, TextStyle.getStyle().setTextSize(32).enableBold(true))

                initLine(BaseStyle.getStyle().setAlign(Align.LEFT))
                printText(sep, TextStyle.getStyle().setTextSize(24))

                initLine(BaseStyle.getStyle().setAlign(Align.LEFT))
                printText(formatLine("วันที่:", date), TextStyle.getStyle().setTextSize(24))
                printText(formatLine("เวลา:", time), TextStyle.getStyle().setTextSize(24))
                printText(formatLine("ผู้ใช้:", userName), TextStyle.getStyle().setTextSize(24))

                printText(sep, TextStyle.getStyle().setTextSize(24))

                printText("รายการเบิก:", TextStyle.getStyle().setTextSize(24).enableBold(true))

                for (item in items) {
                    initLine(BaseStyle.getStyle().setAlign(Align.LEFT))
                    val rawName = item.productName
                    val itemName = if (getVisualWidth(rawName) > 18) {
                        truncateThai(rawName, 15) + "..."
                    } else {
                        rawName
                    }
                    val line = formatLine("- $itemName", "x${item.quantity}")
                    printText(line, TextStyle.getStyle().setTextSize(24))
                }

                initLine(BaseStyle.getStyle().setAlign(Align.LEFT))
                printText(sep, TextStyle.getStyle().setTextSize(24))

                initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
                printText("ขอบคุณที่ใช้บริการ", TextStyle.getStyle().setTextSize(24))

                repeat(4) {
                    printText(" \n", TextStyle.getStyle())
                }

                autoOut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Receipt Printing failed", e)
        }
    }

    private fun formatLine(left: String, right: String): String {
        val leftWidth = getVisualWidth(left)
        val rightWidth = getVisualWidth(right)
        val spaceCount = MAX_LINE_WIDTH - (leftWidth + rightWidth)
        
        return if (spaceCount > 0) {
            left + " ".repeat(spaceCount) + right
        } else {
            "$left $right"
        }
    }

    private fun getVisualWidth(text: String): Int {
        val thaiCombiningChars = Regex("[\u0E31\u0E34-\u0E3A\u0E47-\u0E4E]")
        return text.replace(thaiCombiningChars, "").length
    }

    private fun truncateThai(text: String, targetWidth: Int): String {
        var currentWidth = 0
        val result = StringBuilder()
        val thaiCombiningChars = Regex("[\u0E31\u0E34-\u0E3A\u0E47-\u0E4E]")
        
        for (char in text) {
            if (!char.toString().matches(thaiCombiningChars)) {
                currentWidth++
            }
            if (currentWidth > targetWidth) break
            result.append(char)
        }
        return result.toString()
    }
    
    fun lineWrap(lines: Int) {
        val lineApi = getLineApi() ?: return
        try {
            repeat(lines) {
                lineApi.printText(" \n", TextStyle.getStyle())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Line wrap failed", e)
        }
    }

    fun cutPaper() {
        lineWrap(3)
    }
    
    fun release() {
        PrinterSdk.getInstance().destroy()
        printer = null
    }
}
