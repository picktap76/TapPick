package com.krisadan.tappick.ui.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            nfcAdapter = try { NfcAdapter.getDefaultAdapter(this) } catch (e: Exception) { null }
            
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

            setupUI()
            setupRecyclerView()
            SunmiPrinterHelper.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastHelper.showToast(this, "เกิดข้อผิดพลาดในการเริ่มต้นแอป")
        }
    }

    private fun setupUI() {
        binding.btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        binding.btnConfirm.setOnClickListener {
            handleConfirm()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable { refreshProducts(s?.toString() ?: "") }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        try {
            nfcAdapter?.enableReaderMode(this, { },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or 
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or 
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)

            if (checkAuth()) {
                refreshProducts(binding.etSearch.text.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAuth(): Boolean {
        val members = memberRepository.getMembers()
        if (members.isEmpty()) return true
        
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, PinActivity::class.java))
            return false
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        try {
            nfcAdapter?.disableReaderMode(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshProducts(query: String = "") {
        try {
            val allProducts = repository.getProducts()
            val currentMember = getCurrentMember()
            val members = memberRepository.getMembers()
            
            val effectiveMember = currentMember ?: members.let { 
                if (it.size == 1 && it[0].roleId == "admin_role") it[0] else null 
            }

            val role = effectiveMember?.let { m -> roleRepository.getRoles().find { it.id == m.roleId } }
            
            var filteredProducts = if (role != null) {
                if (role.id == "admin_role") allProducts else allProducts.filter { (role.permissions[it.id] ?: 0) > 0 }
            } else emptyList()

            if (query.isNotEmpty()) {
                filteredProducts = filteredProducts.filter { it.name.contains(query, ignoreCase = true) }
            }

            val remainingCounts = mutableMapOf<String, Int?>()
            if (role != null) {
                if (role.id == "admin_role") {
                    filteredProducts.forEach { remainingCounts[it.id] = null }
                } else {
                    val usageCounts = historyRepository.getUsageCounts(effectiveMember!!.id, role.getQuotaConfigsMap())
                    filteredProducts.forEach {
                        val maxQty = role.getPermissionsMap()[it.id] ?: 0
                        val usedQty = usageCounts[it.id] ?: 0
                        remainingCounts[it.id] = (maxQty - usedQty).coerceAtLeast(0)
                    }
                }
            }
            adapter.setData(filteredProducts, remainingCounts)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentMember(): Member? {
        val memberId = sessionManager.getMemberId() ?: return null
        return memberRepository.getMembers().find { it.id == memberId }
    }

    private fun handleConfirm() {
        try {
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

            val entry = HistoryEntry(memberId = member.id, memberName = member.name, items = selectedItems)
            historyRepository.addEntry(entry)

            val role = roleRepository.getRoles().find { it.id == member.roleId }
            GoogleSheetsHelper.uploadEntry(entry, role?.name ?: "-")
            printReceipt(member, selectedItems)

            sessionManager.logout()
            ToastHelper.showToast(this, "บันทึกการเบิกเรียบร้อย")
            navigateToLogin()
        } catch (e: Exception) {
            e.printStackTrace()
            ToastHelper.showToast(this, "เกิดข้อผิดพลาดในการบันทึก")
        }
    }

    private fun navigateToLogin() {
        adapter.clearQuantities()
        val intent = Intent(this, PinActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun printReceipt(member: Member, items: List<TransactionItem>) {
        try {
            val now = Date()
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(now)
            val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(now)
            SunmiPrinterHelper.getInstance(this).printReceipt("TapPick Receipt", dateStr, timeStr, member.name, items)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        adapter = MainProductAdapter(emptyList())
        binding.rvProducts.adapter = adapter
    }
}
