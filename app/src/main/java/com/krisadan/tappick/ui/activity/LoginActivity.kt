package com.krisadan.tappick.ui.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.krisadan.tappick.data.repository.MemberRepository
import com.krisadan.tappick.data.repository.SessionManager
import com.krisadan.tappick.databinding.ActivityLoginBinding
import com.krisadan.tappick.databinding.BottomSheetForgotPinBinding
import com.krisadan.tappick.util.NfcSimulationHelper
import com.krisadan.tappick.util.ToastHelper

class LoginActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityLoginBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var memberRepository: MemberRepository
    private lateinit var sessionManager: SessionManager
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        memberRepository = MemberRepository.getInstance(this)
        sessionManager = SessionManager.getInstance(this)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            ToastHelper.showToast(this, "เครื่องนี้ไม่รองรับ NFC")
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.ivNfcIcon.apply {
            if (NfcSimulationHelper.IS_SIMULATION_ENABLED) {
                setOnClickListener {
                    NfcSimulationHelper.showManualNfcDialog(this@LoginActivity) { id ->
                        handleNfcScanned(id)
                    }
                }
            } else {
                isClickable = false
                isFocusable = false
                background = null
            }
        }

        binding.btnSwitchToPin.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnForgotPin.setOnClickListener {
            showForgotPinDialog()
        }
    }

    private fun showForgotPinDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetForgotPinBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sheetBinding.btnSend.setOnClickListener {
            val email = sheetBinding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                val member = memberRepository.getMembers().find { it.email?.equals(email, ignoreCase = true) == true }
                if (member != null) {
                    sheetBinding.btnSend.isEnabled = false
                    sheetBinding.btnSend.text = "กำลังส่ง..."
                    
                    com.krisadan.tappick.util.GoogleSheetsHelper.requestPinReset(email, member.name, member.pin) { success ->
                        runOnUiThread {
                            if (success) {
                                ToastHelper.showToast(this, "ส่งรหัส PIN ไปที่ $email เรียบร้อยแล้ว")
                                bottomSheetDialog.dismiss()
                            } else {
                                sheetBinding.btnSend.isEnabled = true
                                sheetBinding.btnSend.text = "ส่งคำขอ"
                                ToastHelper.showToast(this, "เกิดข้อผิดพลาดในการส่ง กรุณาลองใหม่")
                            }
                        }
                    }
                } else {
                    ToastHelper.showToast(this, "ไม่พบอีเมลนี้ในระบบ")
                }
            } else {
                ToastHelper.showToast(this, "กรุณากรอกอีเมล")
            }
        }

        bottomSheetDialog.show()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, this, 
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or 
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        val id = tag?.id?.joinToString("") { "%02X".format(it.toInt() and 0xFF) } ?: return
        runOnUiThread {
            handleNfcScanned(id)
        }
    }

    private fun handleNfcScanned(id: String) {
        if (isProcessing) return
        isProcessing = true
        
        val member = memberRepository.getMembers().find { it.nfcId == id }
        if (member != null) {
            sessionManager.login(member.id)
            ToastHelper.showToast(this, "ยินดีต้อนรับคุณ ${member.name}")
            
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            isProcessing = false
            ToastHelper.showToast(this, "ยังไม่มีรายชื่ออยู่ในระบบ โปรดติดต่อผู้ดูแล", Toast.LENGTH_LONG)
        }
    }
}
