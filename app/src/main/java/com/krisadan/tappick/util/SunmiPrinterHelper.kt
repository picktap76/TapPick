package com.krisadan.tappick.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import woyou.aidlservice.jiuiv5.IWoyouService
import woyou.aidlservice.jiuiv5.ICallback

class SunmiPrinterHelper private constructor(private val context: Context) {
    private var woyouService: IWoyouService? = null

    companion object {
        @Volatile
        private var INSTANCE: SunmiPrinterHelper? = null

        fun getInstance(context: Context): SunmiPrinterHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SunmiPrinterHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent()
        intent.setPackage("com.sunmi.extprinterservice")
        intent.action = "com.sunmi.extprinterservice.PrinterService"
        
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                woyouService = IWoyouService.Stub.asInterface(service)
                Log.d("SunmiPrinter", "Service Connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                woyouService = null
                Log.d("SunmiPrinter", "Service Disconnected")
            }
        }
        
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun printText(text: String) {
        try {
            woyouService?.printText(text, object : ICallback.Stub() {
                override fun onRunResult(isSuccess: Boolean, code: Int, msg: String?) {
                    Log.d("SunmiPrinter", "Print Result: $isSuccess, Code: $code")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("SunmiPrinter", "Command Sent: $text")
    }
    
    fun lineWrap(lines: Int) {
        try {
            woyouService?.lineWrap(lines, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cutPaper() {
        try {
            woyouService?.cutPaper(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
