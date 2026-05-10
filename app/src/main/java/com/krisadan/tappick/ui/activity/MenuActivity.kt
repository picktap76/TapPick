package com.krisadan.tappick.ui.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.krisadan.tappick.R
import com.krisadan.tappick.data.model.Member
import com.krisadan.tappick.data.repository.MemberRepository
import com.krisadan.tappick.data.repository.SessionManager
import com.krisadan.tappick.databinding.ActivityMenuBinding
import com.krisadan.tappick.databinding.ItemMenuBinding

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding
    private lateinit var memberRepository: MemberRepository
    private lateinit var sessionManager: SessionManager
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        memberRepository = MemberRepository.getInstance(this)
        sessionManager = SessionManager.getInstance(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupMenuItems()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, { },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or 
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun setupMenuItems() {
        val effectiveMember = getEffectiveMember()
        val isAdmin = effectiveMember?.roleId == "admin_role" || memberRepository.getMembers().isEmpty()

        
        setupMenuItem(
            binding.menuHistory,
            R.drawable.ic_history,
            "ดูประวัติการเบิก",
            "ตรวจสอบรายการเบิกสิ่งของทั้งหมด"
        ) {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        
        if (isAdmin) {
            setupMenuItem(
                binding.menuEditItems,
                R.drawable.ic_box,
                "แก้ไขรายการสิ่งของ",
                "เพิ่ม ลบ หรือแก้ไขรายการสิ่งของที่สามารถเบิกได้"
            ) {
                if (checkMemberExists()) {
                    startActivity(Intent(this, EditItemsActivity::class.java))
                }
            }
        } else {
            binding.menuEditItems.root.visibility = View.GONE
        }

        
        if (isAdmin) {
            setupMenuItem(
                binding.menuPermissions,
                R.drawable.ic_person_settings,
                "แก้ไขระดับผู้ใช้และจำนวนการเบิก",
                "กำหนดระดับผู้ใช้และจำนวนการเบิกต่อรายการ"
            ) {
                if (checkMemberExists()) {
                    startActivity(Intent(this, PermissionsActivity::class.java))
                }
            }
        } else {
            binding.menuPermissions.root.visibility = View.GONE
        }

        
        if (isAdmin) {
            setupMenuItem(
                binding.menuMembers,
                R.drawable.ic_people,
                "แก้ไขสมาชิก",
                "เพิ่ม ลบ หรือแก้ไขข้อมูลสมาชิกในระบบ"
            ) {
                startActivity(Intent(this, MembersActivity::class.java))
            }
        } else {
            binding.menuMembers.root.visibility = View.GONE
        }
    }

    private fun setupMenuItem(
        itemBinding: ItemMenuBinding,
        iconRes: Int,
        title: String,
        desc: String,
        onClick: () -> Unit
    ) {
        itemBinding.imgMenu.setImageResource(iconRes)
        itemBinding.tvMenuTitle.text = title
        itemBinding.tvMenuDesc.text = desc
        itemBinding.root.setOnClickListener { onClick() }
    }

    private fun getEffectiveMember(): Member? {
        val memberId = sessionManager.getMemberId()
        val currentMember = if (memberId != null) {
            memberRepository.getMembers().find { it.id == memberId }
        } else null

        return currentMember ?: memberRepository.getMembers().let { 
            if (it.size == 1 && it[0].roleId == "admin_role") it[0] else null
        }
    }

    private fun checkMemberExists(): Boolean {
        val members = memberRepository.getMembers()
        val hasAdmin = members.any { it.roleId == "admin_role" }
        
        if (!hasAdmin) {
            AlertDialog.Builder(this)
                .setTitle("ไม่พบข้อมูลผู้ดูแล")
                .setMessage("กรุณาเพิ่มสมาชิกที่มีระดับเป็น \"ผู้ดูแล\" อย่างน้อย 1 คน ก่อนเข้าใช้งานเมนูนี้")
                .setPositiveButton("ไปที่หน้าสมาชิก") { _, _ ->
                    startActivity(Intent(this, MembersActivity::class.java))
                }
                .setNegativeButton("ยกเลิก", null)
                .show()
            return false
        }
        return true
    }
}
