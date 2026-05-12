package com.krisadan.tappick.ui.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.krisadan.tappick.data.model.HistoryEntry
import com.krisadan.tappick.data.model.Member
import com.krisadan.tappick.data.model.TransactionItem
import com.krisadan.tappick.data.repository.HistoryRepository
import com.krisadan.tappick.data.repository.MemberRepository
import com.krisadan.tappick.data.repository.ProductRepository
import com.krisadan.tappick.data.repository.RoleRepository
import com.krisadan.tappick.data.repository.SessionManager
import com.krisadan.tappick.databinding.ActivityMainBinding
import com.krisadan.tappick.ui.adapter.MainProductAdapter
import com.krisadan.tappick.util.GoogleSheetsHelper
import com.krisadan.tappick.util.SunmiPrinterHelper
import com.krisadan.tappick.util.ToastHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ProductRepository
    private lateinit var memberRepository: MemberRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MainProductAdapter
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        repository = ProductRepository.getInstance(this)
        memberRepository = MemberRepository.getInstance(this)
        roleRepository = RoleRepository.getInstance(this)
        historyRepository = HistoryRepository.getInstance(this)
        sessionManager = SessionManager.getInstance(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnMenu.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        binding.btnConfirm.setOnClickListener {
            handleConfirm()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshProducts(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupRecyclerView()

        SunmiPrinterHelper.getInstance(this)
    }

    override fun onResume() {
        super.onResume()
        
        nfcAdapter?.enableReaderMode(this, { },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or 
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)

        val members = memberRepository.getMembers()
        val authRequired = if (members.isEmpty()) false else !sessionManager.isLoggedIn()

        if (authRequired) {
            val intent = Intent(this, PinActivity::class.java)
            startActivity(intent)
            return
        }

        refreshProducts(binding.etSearch.text.toString())
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun refreshProducts(query: String = "") {
        val allProducts = repository.getProducts()
        val currentMember = getCurrentMember()
        
        val effectiveMember = currentMember ?: memberRepository.getMembers().let { 
            if (it.size == 1 && it[0].roleId == "admin_role") it[0] else null 
        }

        val role = effectiveMember?.let { m -> roleRepository.getRoles().find { it.id == m.roleId } }
        
        var filteredProducts = if (role != null) {
            if (role.id == "admin_role") {
                allProducts
            } else {
                allProducts.filter { (role.permissions[it.id] ?: 0) > 0 }
            }
        } else {
            emptyList()
        }

        if (query.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { it.name.contains(query, ignoreCase = true) }
        }

        val remainingCounts = mutableMapOf<String, Int?>()
        if (role != null) {
            if (role.id == "admin_role") {
                for (product in filteredProducts) {
                    remainingCounts[product.id] = null
                }
            } else {
                val memberId = effectiveMember.id
                val productConfigs = filteredProducts.associate { 
                    it.id to (role.getQuotaConfigsMap()[it.id] ?: com.krisadan.tappick.data.model.QuotaConfig())
                }
                
                val usageCounts = historyRepository.getUsageCounts(memberId, productConfigs)
                
                for (product in filteredProducts) {
                    val maxQty = role.getPermissionsMap()[product.id] ?: 0
                    val usedQty = usageCounts[product.id] ?: 0
                    remainingCounts[product.id] = (maxQty - usedQty).coerceAtLeast(0)
                }
            }
        }
        
        adapter.setData(filteredProducts, remainingCounts)
    }

    private fun getCurrentMember(): Member? {
        val memberId = sessionManager.getMemberId() ?: return null
        return memberRepository.getMembers().find { it.id == memberId }
    }

    private fun handleConfirm() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            sessionManager.logout()
            navigateToLogin()
            return
        }

        val member = getCurrentMember() ?: memberRepository.getMembers().find { it.roleId == "admin_role" }
        if (member == null) {
            ToastHelper.showToast(this, "ไม่พบข้อมูลผู้ใช้งาน")
            return
        }

        val entry = HistoryEntry(
            memberId = member.id,
            memberName = member.name,
            items = selectedItems
        )
        historyRepository.addEntry(entry)

        val role = roleRepository.getRoles().find { it.id == member.roleId }
        GoogleSheetsHelper.uploadEntry(entry, role?.name ?: "-")

        printReceipt(member, selectedItems)

        sessionManager.logout()
        ToastHelper.showToast(this, "บันทึกการเบิกเรียบร้อย")
        
        navigateToLogin()
    }

    private fun navigateToLogin() {
        adapter.clearQuantities()
        val intent = Intent(this, PinActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun printReceipt(member: Member, items: List<TransactionItem>) {
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
        val now = Date()
        
        val dateStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)
        
        val printerHelper = SunmiPrinterHelper.getInstance(this)
        printerHelper.printReceipt("TapPick Receipt", dateStr, timeStr, member.name, items)
        
        ToastHelper.showToast(this, "สั่งปริ้นท์บิลเรียบร้อย")
    }

    private fun setupRecyclerView() {
        adapter = MainProductAdapter(emptyList())
        binding.rvProducts.adapter = adapter
    }
}
