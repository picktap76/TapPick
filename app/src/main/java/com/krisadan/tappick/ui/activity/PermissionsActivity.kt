package com.krisadan.tappick.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.krisadan.tappick.R
import com.krisadan.tappick.data.model.Role
import com.krisadan.tappick.data.repository.ProductRepository
import com.krisadan.tappick.data.repository.RoleRepository
import com.krisadan.tappick.databinding.ActivityPermissionsBinding
import com.krisadan.tappick.databinding.BottomSheetAddPermissionBinding
import com.krisadan.tappick.databinding.ItemPermissionQuantityBinding
import com.krisadan.tappick.databinding.ItemRoleDropdownBinding
import com.krisadan.tappick.util.ToastHelper

class PermissionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionsBinding
    private lateinit var productRepository: ProductRepository
    private lateinit var roleRepository: RoleRepository
    
    private var selectedRole: Role? = null
    private var roles: List<Role> = emptyList()

    private val presetColors = listOf("#0055D4", "#22C55E", "#EF4444", "#F59E0B", "#8B5CF6", "#EC4899")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        productRepository = ProductRepository.getInstance(this)
        roleRepository = RoleRepository.getInstance(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddPermission.setOnClickListener {
            showAddPermissionBottomSheet()
        }

        binding.actvRole.setOnItemClickListener { parent, _, position, _ ->
            val role = parent.adapter.getItem(position) as? Role
            role?.let { selectRole(it) }
        }

        loadRoles()
    }

    private fun loadRoles() {
        roles = roleRepository.getRoles().filter { it.id != "admin_role" }
        val adapter = RoleAdapter(this, roles)
        binding.actvRole.setAdapter(adapter)

        if (roles.isNotEmpty()) {
            binding.llContent.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
            if (selectedRole == null) {
                selectRole(roles[0])
            } else {
                val current = roles.find { it.id == selectedRole?.id }
                if (current != null) {
                    selectRole(current)
                } else {
                    selectRole(roles[0])
                }
            }
        } else {
            updateUIForNoRole()
        }
    }

    private fun selectRole(role: Role) {
        selectedRole = role
        binding.actvRole.setText(role.name, false)
        binding.tvInfoDesc.text = "ผู้ใช้ที่มีระดับ \"${role.name}\" ทุกคน จะสามารถเบิกสิ่งของได้ตามจำนวนที่กำหนดไว้ข้างต้นต่อครั้ง"
        setupPermissionsList()
    }

    private fun renderColorOptions(container: LinearLayout, selectedColor: String, allowCustom: Boolean = false, onColorSelected: (String) -> Unit) {
        container.removeAllViews()
        val size = (32 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()

        val allColors = presetColors.toMutableList()
        if (!allColors.any { it.equals(selectedColor, ignoreCase = true) }) {
            allColors.add(selectedColor)
        }

        for (colorHex in allColors) {
            val frameLayout = FrameLayout(this)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(0, 0, margin, 0)
            frameLayout.layoutParams = params

            val circle = View(this)
            circle.layoutParams = FrameLayout.LayoutParams(size, size)
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(android.graphics.Color.parseColor(colorHex))
            
            if (colorHex.equals(selectedColor, ignoreCase = true)) {
                drawable.setStroke((2 * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#001A41"))
            }
            
            circle.background = drawable
            circle.setOnClickListener { onColorSelected(colorHex) }

            frameLayout.addView(circle)
            container.addView(frameLayout)
        }

        if (allowCustom) {
            val frameLayout = FrameLayout(this)
            val params = LinearLayout.LayoutParams(size, size)
            frameLayout.layoutParams = params

            val addButton = android.widget.ImageButton(this).apply {
                layoutParams = FrameLayout.LayoutParams(size, size)
                setImageResource(R.drawable.ic_add)
                setBackgroundResource(R.drawable.bg_circle_button)
                val p = (6 * resources.displayMetrics.density).toInt()
                setPadding(p, p, p, p)
                imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.primary_blue))
            }
            
            addButton.setOnClickListener {
                showCustomColorDialog(onColorSelected)
            }

            frameLayout.addView(addButton)
            container.addView(frameLayout)
        }
    }

    private fun showCustomColorDialog(onColorSelected: (String) -> Unit) {
        val editText = android.widget.EditText(this).apply {
            hint = "#RRGGBB"
            setText("#")
            setSelection(1)
            inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(android.text.InputFilter.LengthFilter(7))
        }

        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this)
        container.setPadding(padding, padding / 2, padding, 0)
        container.addView(editText)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ระบุรหัสสี (HEX)")
            .setView(container)
            .setPositiveButton("ตกลง") { _, _ ->
                val hex = editText.text.toString().trim()
                if (hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    onColorSelected(hex.uppercase())
                } else {
                    ToastHelper.showToast(this, "รูปแบบรหัสสีไม่ถูกต้อง (ตัวอย่าง: #0055D4)")
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun updateUIForNoRole() {
        selectedRole = null
        binding.llContent.visibility = View.GONE
        binding.llEmptyState.visibility = View.VISIBLE
        binding.actvRole.setText("ยังไม่มีรายการ", false)
        binding.tvInfoDesc.text = "กรุณาเลือกหรือเพิ่มระดับผู้ใช้เพื่อกำหนดจำนวนการเบิก"
        binding.llPermissionsList.removeAllViews()
    }

    private fun setupPermissionsList() {
        binding.llPermissionsList.removeAllViews()
        val role = selectedRole ?: return
        val products = productRepository.getProducts()

        if (products.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "ยังไม่มีรายการสินค้า กรุณาเพิ่มสินค้าก่อน"
            emptyText.setPadding(0, 32, 0, 0)
            binding.llPermissionsList.addView(emptyText)
            return
        }

        val permissions = role.getPermissionsMap()

        for (product in products) {
            val itemBinding = ItemPermissionQuantityBinding.inflate(layoutInflater, binding.llPermissionsList, false)
            itemBinding.tvItemName.text = product.name
            var qty = permissions[product.id] ?: 0
            itemBinding.tvQuantity.text = qty.toString()

            itemBinding.btnPlus.setOnClickListener {
                qty++
                itemBinding.tvQuantity.text = qty.toString()
                permissions[product.id] = qty
                roleRepository.updateRole(role)
            }

            itemBinding.btnMinus.setOnClickListener {
                if (qty > 0) {
                    qty--
                    itemBinding.tvQuantity.text = qty.toString()
                    permissions[product.id] = qty
                    roleRepository.updateRole(role)
                }
            }

            binding.llPermissionsList.addView(itemBinding.root)
        }
    }

    private fun showAddPermissionBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddPermissionBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetTitle.text = "เพิ่มระดับผู้ใช้ใหม่"
        sheetBinding.tvSheetSubtitle.text = "กรอกชื่อระดับที่ต้องการเพิ่มในระบบ"

        var tempSelectedColor = presetColors[0]

        fun refreshBSColors(selected: String) {
            renderColorOptions(sheetBinding.llColorOptions, selected, allowCustom = true) { clicked ->
                tempSelectedColor = clicked
                refreshBSColors(clicked)
            }
        }
        refreshBSColors(tempSelectedColor)

        sheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sheetBinding.btnAdd.setOnClickListener {
            val name = sheetBinding.etPermissionName.text.toString()
            if (name.isNotEmpty()) {
                val newRole = Role(name = name, color = tempSelectedColor)
                roleRepository.addRole(newRole)
                selectedRole = newRole
                loadRoles()
                bottomSheetDialog.dismiss()
            } else {
                ToastHelper.showToast(this, "กรุณากรอกชื่อระดับผู้ใช้")
            }
        }

        bottomSheetDialog.show()
    }

    private fun showEditPermissionBottomSheet(role: Role) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddPermissionBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetTitle.text = "แก้ไขระดับผู้ใช้"
        sheetBinding.tvSheetSubtitle.text = "แก้ไขข้อมูลระดับผู้ใช้ในระบบ"
        sheetBinding.etPermissionName.setText(role.name)
        sheetBinding.btnAdd.text = "บันทึกการแก้ไข"

        var tempSelectedColor = role.color ?: presetColors[0]

        fun refreshBSColors(selected: String) {
            renderColorOptions(sheetBinding.llColorOptions, selected, allowCustom = true) { clicked ->
                tempSelectedColor = clicked
                refreshBSColors(clicked)
            }
        }
        refreshBSColors(tempSelectedColor)

        sheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sheetBinding.btnAdd.setOnClickListener {
            val name = sheetBinding.etPermissionName.text.toString().trim()
            if (name.isNotEmpty()) {
                role.name = name
                role.color = tempSelectedColor
                roleRepository.updateRole(role)
                
                // If the edited role is the currently selected one, refresh its info
                if (selectedRole?.id == role.id) {
                    selectRole(role)
                }
                
                loadRoles()
                bottomSheetDialog.dismiss()
                ToastHelper.showToast(this, "แก้ไขข้อมูลเรียบร้อย")
            } else {
                ToastHelper.showToast(this, "กรุณากรอกชื่อระดับผู้ใช้")
            }
        }

        bottomSheetDialog.show()
    }

    private inner class RoleAdapter(context: android.content.Context, private val rolesList: List<Role>) :
        android.widget.ArrayAdapter<Role>(context, R.layout.item_role_dropdown, rolesList) {

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
            rowBinding.root.isClickable = false
            rowBinding.root.isFocusable = false

            val isSelected = role?.id == selectedRole?.id
            if (isSelected) {
                rowBinding.root.setBackgroundColor(context.getColor(R.color.primary_blue_light))
                rowBinding.tvRoleName.setTextColor(context.getColor(R.color.primary_blue))
                rowBinding.ivCheck.visibility = View.VISIBLE
            } else {
                rowBinding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                rowBinding.tvRoleName.setTextColor(context.getColor(R.color.text_title))
                rowBinding.ivCheck.visibility = View.GONE
            }

            rowBinding.btnEditRole.visibility = View.VISIBLE
            rowBinding.btnEditRole.setOnClickListener {
                role?.let { r ->
                    showEditPermissionBottomSheet(r)
                    binding.actvRole.dismissDropDown()
                }
            }

            if (role?.isDeletable == false) {
                rowBinding.btnDeleteRole.visibility = View.GONE
            } else {
                rowBinding.btnDeleteRole.visibility = View.VISIBLE
                rowBinding.btnDeleteRole.setOnClickListener {
                    role?.let { r ->
                        roleRepository.deleteRole(r.id)
                        loadRoles()
                        binding.actvRole.showDropDown()
                    }
                }
            }

            return rowBinding.root
        }

        override fun getFilter(): android.widget.Filter {
            return object : android.widget.Filter() {
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
