package com.krisadan.tappick.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.PrinterSdk.Printer
import com.sunmi.printerx.api.LineApi
import com.sunmi.printerx.enums.Align
import com.sunmi.printerx.enums.DividingLine
import com.sunmi.printerx.enums.SettingItem
import com.sunmi.printerx.style.BaseStyle
import com.sunmi.printerx.style.TextStyle

class SunmiPrinterHelper private constructor(private val context: Context) {
    private var printer: Printer? = null

    companion object {
        private const val TAG = "SunmiPrinterHelper"
        
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
                    Log.d(TAG, "Default printer initialized")
                }
                override fun onPrinters(printers: MutableList<Printer>?) {
                    if (printer == null && !printers.isNullOrEmpty()) {
                        printer = printers[0]
                        Log.d(TAG, "Printer found from list")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Sunmi Printer SDK", e)
        }
    }

    fun isPrinterReady(): Boolean {
        val printer = this.printer ?: return false
        return try {
            val status = printer.queryApi().status
            Log.d(TAG, "Printer status: $status")
            status.name == "NORMAL"
        } catch (e: Exception) {
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

    fun getLineApi(): LineApi? {
        if (!isPrinterReady()) {
            Log.e(TAG, "Printer is not ready (Check paper or cover)")
        }
        return printer?.lineApi()
    }

    fun printText(text: String, size: Int = 24, isBold: Boolean = false, align: Align = Align.LEFT, autoOut: Boolean = true) {
        val lineApi = getLineApi() ?: return

        try {
            lineApi.apply {
                initLine(BaseStyle.getStyle().setAlign(align))
                printText(text, TextStyle.getStyle().setTextSize(size).enableBold(isBold))
                if (autoOut) autoOut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Printing failed", e)
        }
    }

    fun printReceipt(title: String, date: String, time: String, userName: String, items: List<com.krisadan.tappick.data.model.TransactionItem>) {
        val lineApi = getLineApi() ?: return
        try {
            lineApi.apply {
                
                initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
                printText(title, TextStyle.getStyle().setTextSize(32).enableBold(true))
                
                printDividingLine(DividingLine.DOTTED, 2)

                initLine(BaseStyle.getStyle().setAlign(Align.LEFT))
                printText("วันที่: $date", TextStyle.getStyle().setTextSize(24))
                printText("เวลา: $time", TextStyle.getStyle().setTextSize(24))
                printText("ผู้ใช้: $userName", TextStyle.getStyle().setTextSize(24))
                
                printDividingLine(DividingLine.DOTTED, 2)

                printText("รายการเบิก:", TextStyle.getStyle().setTextSize(24).enableBold(true))
                for (item in items) {
                    printText("- ${item.productName} x ${item.quantity}", TextStyle.getStyle().setTextSize(24))
                }
                printDividingLine(DividingLine.SOLID, 2)
                
                initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
                printText("ขอบคุณที่ใช้บริการ", TextStyle.getStyle().setTextSize(20))
                
                lineWrap(3)
                autoOut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Receipt Printing failed", e)
        }
    }
    
    fun lineWrap(lines: Int) {
        val lineApi = getLineApi() ?: return
        try {
            repeat(lines) {
                lineApi.printText("\n", TextStyle.getStyle())
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
    }
}
