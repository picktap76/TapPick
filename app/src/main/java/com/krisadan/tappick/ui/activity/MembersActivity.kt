package com.krisadan.tappick.ui.activity

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.krisadan.tappick.R
import com.krisadan.tappick.data.model.Member
import com.krisadan.tappick.data.model.Role
import com.krisadan.tappick.data.repository.HistoryRepository
import com.krisadan.tappick.data.repository.MemberRepository
import com.krisadan.tappick.data.repository.RoleRepository
import com.krisadan.tappick.databinding.ActivityMembersBinding
import com.krisadan.tappick.databinding.BottomSheetAddMemberBinding
import com.krisadan.tappick.databinding.ItemRoleDropdownBinding
import com.krisadan.tappick.ui.adapter.MemberAdapter
import com.krisadan.tappick.util.NfcSimulationHelper
import com.krisadan.tappick.util.PaginationHelper
import com.krisadan.tappick.util.ToastHelper

class MembersActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityMembersBinding
    private lateinit var memberRepository: MemberRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var historyRepository: HistoryRepository
    private var nfcAdapter: NfcAdapter? = null
    
    private lateinit var paginationHelper: PaginationHelper<Member>
    private lateinit var memberAdapter: MemberAdapter
    
    private var roles: List<Role> = emptyList()
    private var selectedRoleForNewMember: Role? = null

    private var isWaitingForNfc = false
    private var onNfcScanned: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            ToastHelper.showToast(this, "เครื่องนี้ไม่รองรับ NFC")
        }
        
        memberRepository = MemberRepository.getInstance(this)
        roleRepository = RoleRepository.getInstance(this)
        historyRepository = HistoryRepository.getInstance(this)
        
        roles = roleRepository.getRoles()
        setupRecyclerView()

        paginationHelper = PaginationHelper(binding.pagination.root) {
            loadMembers(binding.etSearch.text.toString())
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddMember.setOnClickListener {
            showAddMemberBottomSheet()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadMembers(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadMembers()
    }

    private fun setupRecyclerView() {
        memberAdapter = MemberAdapter(emptyList(), roles.associateBy { it.id }, 
            onEdit = { showEditMemberBottomSheet(it) },
            onDelete = { showDeleteConfirmDialog(it) }
        )
        binding.rvMembers.adapter = memberAdapter
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, this, 
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or 
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        val id = tag?.id?.joinToString("") { "%02x".format(it) }?.uppercase() ?: return
        if (isWaitingForNfc) {
            runOnUiThread { onNfcScanned?.invoke(id) }
        }
    }

    private fun loadMembers(query: String = "") {
        val allMembers = memberRepository.getMembers()
        val filteredMembers = if (query.isEmpty()) {
            allMembers
        } else {
            allMembers.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        paginationHelper.update(filteredMembers.size)
        val members = paginationHelper.getPaginatedList(filteredMembers)
        
        binding.tvCount.text = "ทั้งหมด ${filteredMembers.size} คน"

        if (members.isEmpty() && query.isEmpty()) {
            binding.llEmptyState.visibility = View.VISIBLE
            binding.rvMembers.visibility = View.GONE
        } else {
            binding.llEmptyState.visibility = View.GONE
            binding.rvMembers.visibility = View.VISIBLE
            memberAdapter.updateData(members, roles.associateBy { it.id })
        }
    }

    private fun showEditMemberBottomSheet(member: Member) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddMemberBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.llScanNfc.visibility = View.GONE
        sheetBinding.llMemberForm.visibility = View.VISIBLE
        
        sheetBinding.tvFormTitle.text = "แก้ไขข้อมูลสมาชิก"
        sheetBinding.etMemberName.setText(member.name)
        sheetBinding.btnAddMemberConfirm.text = "บันทึกการแก้ไข"

        val adapter = RoleDropdownAdapter(this, roles)
        sheetBinding.actvRole.setAdapter(adapter)
        
        var selectedRole = roles.find { it.id == member.roleId } ?: roles.firstOrNull()
        selectedRole?.let { 
            sheetBinding.actvRole.setText(it.name, false)
        }

        sheetBinding.actvRole.setOnItemClickListener { parent, _, position, _ ->
            selectedRole = parent.adapter.getItem(position) as? Role
        }

        sheetBinding.btnCancelForm.setOnClickListener { bottomSheetDialog.dismiss() }

        sheetBinding.btnAddMemberConfirm.setOnClickListener {
            val name = sheetBinding.etMemberName.text.toString().trim()
            val roleId = selectedRole?.id

            if (name.isNotEmpty() && roleId != null) {
                val oldName = member.name
                member.name = name
                member.roleId = roleId
                memberRepository.updateMember(member)
                
                if (oldName != name) {
                    historyRepository.updateMemberName(member.id, name)
                }

                loadMembers(binding.etSearch.text.toString())
                bottomSheetDialog.dismiss()
                ToastHelper.showToast(this, "แก้ไขข้อมูลสมาชิกเรียบร้อย")
            } else if (name.isEmpty()) {
                ToastHelper.showToast(this, "กรุณากรอกชื่อสมาชิก")
            }
        }

        bottomSheetDialog.show()
    }

    private fun showDeleteConfirmDialog(member: Member) {
        AlertDialog.Builder(this)
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณต้องการลบสมาชิก \"${member.name}\" ใช่หรือไม่?")
            .setPositiveButton("ลบ") { _, _ ->
                memberRepository.deleteMember(member.id)
                loadMembers(binding.etSearch.text.toString())
                ToastHelper.showToast(this, "ลบสมาชิกเรียบร้อย")
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun showAddMemberBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddMemberBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        var scannedNfcId: String? = null
        isWaitingForNfc = true

        onNfcScanned = { id ->
            scannedNfcId = id
            isWaitingForNfc = false
            sheetBinding.llScanNfc.visibility = View.GONE
            sheetBinding.llMemberForm.visibility = View.VISIBLE
            ToastHelper.showToast(this, "สแกนบัตรสำเร็จ")
        }

        bottomSheetDialog.setOnDismissListener {
            isWaitingForNfc = false
            onNfcScanned = null
        }

        val adapter = RoleDropdownAdapter(this, roles)
        sheetBinding.actvRole.setAdapter(adapter)
        
        selectedRoleForNewMember = if (roles.isNotEmpty()) roles[0] else null
        selectedRoleForNewMember?.let { 
            sheetBinding.actvRole.setText(it.name, false)
        }

        sheetBinding.actvRole.setOnItemClickListener { parent, _, position, _ ->
            selectedRoleForNewMember = parent.adapter.getItem(position) as? Role
        }

        sheetBinding.btnCancelScan.setOnClickListener { bottomSheetDialog.dismiss() }
        sheetBinding.btnCancelForm.setOnClickListener { bottomSheetDialog.dismiss() }

        sheetBinding.ivNfcIcon.apply {
            if (NfcSimulationHelper.IS_SIMULATION_ENABLED) {
                setOnClickListener {
                    NfcSimulationHelper.showManualNfcDialog(this@MembersActivity) { id ->
                        onNfcScanned?.invoke(id)
                    }
                }
            } else {
                isClickable = false
                isFocusable = false
                background = null
            }
        }

        sheetBinding.btnAddMemberConfirm.setOnClickListener {
            val name = sheetBinding.etMemberName.text.toString()
            val roleId = selectedRoleForNewMember?.id
            val nfcId = scannedNfcId

            if (name.isNotEmpty() && roleId != null && nfcId != null) {
                val newMember = Member(nfcId = nfcId, name = name, roleId = roleId)
                memberRepository.addMember(newMember)
                loadMembers()
                bottomSheetDialog.dismiss()
                ToastHelper.showToast(this, "เพิ่มสมาชิกเรียบร้อย")
            } else if (name.isEmpty()) {
                ToastHelper.showToast(this, "กรุณากรอกชื่อสมาชิก")
            } else if (roleId == null) {
                ToastHelper.showToast(this, "กรุณาเลือกระดับผู้ใช้")
            }
        }

        bottomSheetDialog.show()
    }

    private inner class RoleDropdownAdapter(context: Context, private val rolesList: List<Role>) :
        ArrayAdapter<Role>(context, R.layout.item_role_dropdown, rolesList) {

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowBinding = if (convertView == null) {
                ItemRoleDropdownBinding.inflate(LayoutInflater.from(context), parent, false)
            } else {
                ItemRoleDropdownBinding.bind(convertView)
            }
            
            val role = getItem(position)
            rowBinding.tvRoleName.text = role?.name
            rowBinding.btnEditRole.visibility = View.GONE
            rowBinding.btnDeleteRole.visibility = View.GONE
            rowBinding.root.isClickable = false
            rowBinding.root.isFocusable = false

            val isSelected = role?.id == selectedRoleForNewMember?.id
            if (isSelected) {
                rowBinding.root.setBackgroundColor(context.getColor(R.color.primary_blue_light))
                rowBinding.tvRoleName.setTextColor(context.getColor(R.color.primary_blue))
                rowBinding.ivCheck.visibility = View.VISIBLE
            } else {
                rowBinding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                rowBinding.tvRoleName.setTextColor(context.getColor(R.color.text_title))
                rowBinding.ivCheck.visibility = View.GONE
            }

            return rowBinding.root
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val results = FilterResults()
                    results.values = rolesList
                    results.count = rolesList.size
                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }

                override fun convertResultToString(resultValue: Any?): CharSequence {
                    return (resultValue as Role).name
                }
            }
        }
    }
}
