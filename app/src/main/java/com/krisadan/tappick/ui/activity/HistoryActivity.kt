package com.krisadan.tappick.ui.activity

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.krisadan.tappick.data.model.HistoryEntry
import com.krisadan.tappick.data.repository.HistoryRepository
import com.krisadan.tappick.data.repository.MemberRepository
import com.krisadan.tappick.data.repository.RoleRepository
import com.krisadan.tappick.data.repository.SessionManager
import com.krisadan.tappick.databinding.ActivityHistoryBinding
import com.krisadan.tappick.ui.adapter.HistoryAdapter
import com.krisadan.tappick.util.PaginationHelper
import com.krisadan.tappick.util.ToastHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyRepository: HistoryRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var memberRepository: MemberRepository
    private lateinit var sessionManager: SessionManager
    private var nfcAdapter: NfcAdapter? = null

    private lateinit var paginationHelper: PaginationHelper<HistoryEntry>
    private lateinit var historyAdapter: HistoryAdapter

    private var startDate: Long? = null
    private var endDate: Long? = null
    private var selectedRoleId: String? = null

    private var dataToExport: List<List<String>> = emptyList()
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { saveXlsxToFile(it) }
    }

    private val thaiLocale = Locale("th", "TH")
    private val displayDateFormat = SimpleDateFormat("dd/MM/yy", thaiLocale)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        historyRepository = HistoryRepository.getInstance(this)
        roleRepository = RoleRepository.getInstance(this)
        memberRepository = MemberRepository.getInstance(this)
        sessionManager = SessionManager.getInstance(this)

        setupRecyclerView()

        paginationHelper = PaginationHelper(binding.pagination.root) {
            setupHistoryList()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnFilterDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnFilterRole.setOnClickListener {
            showRoleFilterDialog()
        }

        binding.btnExport.setOnClickListener {
            if (isAdmin()) {
                handleExport()
            } else {
                ToastHelper.showToast(this, "เฉพาะผู้ดูแลเท่านั้นที่สามารถส่งออกข้อมูลได้")
            }
        }

        setupHistoryList()
        checkExportPermission()
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

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList(), roleRepository.getRoles().associateBy { it.id })
        binding.rvHistory.adapter = historyAdapter
    }

    private fun isAdmin(): Boolean {
        val memberId = sessionManager.getMemberId()
        val currentMember = if (memberId != null) {
            memberRepository.getMembers().find { it.id == memberId }
        } else null

        val effectiveMember = currentMember ?: memberRepository.getMembers().let { 
            if (it.size == 1 && it[0].roleId == "admin_role") it[0] else null 
        }
        
        return effectiveMember?.roleId == "admin_role" || memberRepository.getMembers().isEmpty()
    }

    private fun checkExportPermission() {
        if (!isAdmin()) {
            binding.btnExport.alpha = 0.5f
        }
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("เลือกช่วงวันที่")
        
        if (startDate != null && endDate != null) {
            builder.setSelection(Pair(startDate, endDate))
        }

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            startDate = selection.first
            endDate = selection.second
            
            if (startDate != null && endDate != null) {
                val startStr = displayDateFormat.format(Date(startDate!!))
                val endStr = displayDateFormat.format(Date(endDate!!))
                binding.tvSelectedDate.text = "$startStr - $endStr"
            } else {
                binding.tvSelectedDate.text = "ทั้งหมด"
            }
            setupHistoryList()
        }
        picker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showRoleFilterDialog() {
        val roles = roleRepository.getRoles()
        val roleNames = mutableListOf("ทั้งหมด")
        roleNames.addAll(roles.map { it.name })

        AlertDialog.Builder(this)
            .setTitle("เลือกระดับผู้ใช้งาน")
            .setItems(roleNames.toTypedArray()) { _, which ->
                if (which == 0) {
                    selectedRoleId = null
                    binding.tvSelectedRole.text = "ทั้งหมด"
                } else {
                    val role = roles[which - 1]
                    selectedRoleId = role.id
                    binding.tvSelectedRole.text = role.name
                }
                setupHistoryList()
            }
            .show()
    }

    private fun setupHistoryList() {
        val allEntries = historyRepository.getHistory()
        val membersMap = memberRepository.getMembers().associateBy { it.id }
        
        val filteredEntries = allEntries.filter { entry ->
            val dateMatch = if (startDate != null && endDate != null) {
                val entryCal = Calendar.getInstance()
                entryCal.timeInMillis = entry.timestamp
                entryCal.set(Calendar.HOUR_OF_DAY, 0)
                entryCal.set(Calendar.MINUTE, 0)
                entryCal.set(Calendar.SECOND, 0)
                entryCal.set(Calendar.MILLISECOND, 0)
                
                val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                startCal.timeInMillis = startDate!!
                val localStart = Calendar.getInstance()
                localStart.set(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                localStart.set(Calendar.MILLISECOND, 0)

                val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                endCal.timeInMillis = endDate!!
                val localEnd = Calendar.getInstance()
                localEnd.set(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                localEnd.set(Calendar.MILLISECOND, 0)

                entryCal.timeInMillis in localStart.timeInMillis..localEnd.timeInMillis
            } else {
                true
            }

            val roleMatch = if (selectedRoleId != null) {
                val member = membersMap[entry.memberId]
                member?.roleId == selectedRoleId
            } else {
                true
            }

            dateMatch && roleMatch
        }

        paginationHelper.update(filteredEntries.size)
        val pagedEntries = paginationHelper.getPaginatedList(filteredEntries)

        val flattenedData = pagedEntries.flatMap { entry ->
            val member = membersMap[entry.memberId]
            entry.items.map { item ->
                HistoryAdapter.HistoryRowData(
                    timestamp = entry.timestamp,
                    memberName = entry.memberName,
                    roleId = member?.roleId,
                    productName = item.productName,
                    quantity = item.quantity
                )
            }
        }

        historyAdapter.updateData(flattenedData, roleRepository.getRoles().associateBy { it.id })
    }

    private fun handleExport() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("เลือกช่วงวันที่ต้องการบันทึก")
        val picker = builder.build()
        
        picker.addOnPositiveButtonClickListener { selection ->
            val exportStart = selection.first
            val exportEnd = selection.second
            
            if (exportStart != null && exportEnd != null) {
                prepareExportData(exportStart, exportEnd)
            }
        }
        picker.show(supportFragmentManager, "EXPORT_DATE_PICKER")
    }

    private fun prepareExportData(start: Long, end: Long) {
        val allEntries = historyRepository.getHistory()
        val membersMap = memberRepository.getMembers().associateBy { it.id }
        val rolesMap = roleRepository.getRoles().associateBy { it.id }
        val sdfDate = SimpleDateFormat("dd/MM/yy", thaiLocale)
        val sdfTime = SimpleDateFormat("HH:mm", thaiLocale)
        
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("วันที่", "เวลา", "ชื่อผู้ใช้งาน", "ระดับ", "รายการ", "จำนวน"))
        
        val filtered = allEntries.filter { entry ->
            val entryCal = Calendar.getInstance()
            entryCal.timeInMillis = entry.timestamp
            entryCal.set(Calendar.HOUR_OF_DAY, 0)
            entryCal.set(Calendar.MINUTE, 0)
            entryCal.set(Calendar.SECOND, 0)
            entryCal.set(Calendar.MILLISECOND, 0)

            val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            startCal.timeInMillis = start
            val localStart = Calendar.getInstance()
            localStart.set(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            localStart.set(Calendar.MILLISECOND, 0)

            val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            endCal.timeInMillis = end
            val localEnd = Calendar.getInstance()
            localEnd.set(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            localEnd.set(Calendar.MILLISECOND, 0)

            entryCal.timeInMillis in localStart.timeInMillis..localEnd.timeInMillis
        }
        
        if (filtered.isEmpty()) {
            ToastHelper.showToast(this, "ไม่พบข้อมูลในช่วงวันที่เลือก")
            return
        }

        for (entry in filtered) {
            val dateStr = sdfDate.format(Date(entry.timestamp))
            val timeStr = sdfTime.format(Date(entry.timestamp))
            val member = membersMap[entry.memberId]
            val role = rolesMap[member?.roleId]
            val roleName = role?.name ?: "-"
            
            for (item in entry.items) {
                rows.add(listOf(dateStr, timeStr, entry.memberName, roleName, item.productName, item.quantity.toString()))
            }
        }
        
        dataToExport = rows
        val fileName = "History_${SimpleDateFormat("ddMMyy", thaiLocale).format(Date())}.xlsx"
        exportLauncher.launch(fileName)
    }

    private fun saveXlsxToFile(uri: android.net.Uri) {
        binding.btnExport.isEnabled = false
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("กำลังบันทึกไฟล์...")
            .setCancelable(false)
            .show()

        Thread {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                    val sheet = workbook.createSheet("History")
                    
                    dataToExport.forEachIndexed { rowIndex, rowData ->
                        val row = sheet.createRow(rowIndex)
                        rowData.forEachIndexed { colIndex, cellData ->
                            val cell = row.createCell(colIndex)
                            cell.setCellValue(cellData)
                        }
                    }
                    
                    workbook.write(outputStream)
                    workbook.close()
                }
                runOnUiThread {
                    progressDialog.dismiss()
                    binding.btnExport.isEnabled = true
                    ToastHelper.showToast(this, "บันทึกไฟล์ XLSX เรียบร้อยแล้ว")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressDialog.dismiss()
                    binding.btnExport.isEnabled = true
                    ToastHelper.showToast(this, "เกิดข้อผิดพลาดในการบันทึก: ${e.message}")
                }
            }
        }.start()
    }
}
