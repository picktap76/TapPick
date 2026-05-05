package com.krisadan.tappick.ui.activity

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.krisadan.tappick.R
import com.krisadan.tappick.data.model.Product
import com.krisadan.tappick.data.repository.HistoryRepository
import com.krisadan.tappick.data.repository.ProductRepository
import com.krisadan.tappick.databinding.ActivityEditItemsBinding
import com.krisadan.tappick.databinding.BottomSheetAddProductBinding
import com.krisadan.tappick.ui.adapter.EditProductAdapter
import com.krisadan.tappick.util.ToastHelper
import java.io.File

class EditItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditItemsBinding
    private lateinit var repository: ProductRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var adapter: EditProductAdapter
    
    private var selectedProductForImage: Product? = null
    private var newProductImageUri: String? = null
    private var currentBottomSheetImageView: ImageView? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val internalPath = repository.saveImageToInternalStorage(it)
            if (internalPath != null) {
                selectedProductForImage?.let { product ->
                    product.imageUri = internalPath
                    repository.updateProduct(product)
                    setupProductList()
                    selectedProductForImage = null
                }
                
                currentBottomSheetImageView?.let { imageView ->
                    newProductImageUri = internalPath
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.setImageURI(Uri.fromFile(File(internalPath)))
                    imageView.setPadding(0, 0, 0, 0)
                    imageView.alpha = 1.0f
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = ProductRepository.getInstance(this)
        historyRepository = HistoryRepository.getInstance(this)

        setupRecyclerView()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            currentFocus?.clearFocus()
            finish()
        }

        binding.btnAddProduct.setOnClickListener {
            showAddProductBottomSheet()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setupProductList(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupProductList()
    }

    private fun setupRecyclerView() {
        adapter = EditProductAdapter(emptyList(),
            onEdit = { showEditProductBottomSheet(it) },
            onDelete = { showDeleteConfirmDialog(it) }
        )
        binding.rvProducts.adapter = adapter
    }

    private fun setupProductList(query: String = "") {
        val allProducts = repository.getProducts()
        val products = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateData(products)
    }

    private fun showEditProductBottomSheet(product: Product) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddProductBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        sheetBinding.tvSheetTitle.text = "แก้ไขรายการสิ่งของ"
        sheetBinding.tvSheetSubtitle.text = "แก้ไขข้อมูลสิ่งของในระบบ"
        sheetBinding.etProductName.setText(product.name)
        sheetBinding.btnAdd.text = "บันทึกการแก้ไข"
        
        newProductImageUri = product.imageUri
        if (product.imageUri != null) {
            val file = File(product.imageUri!!)
            if (file.exists()) {
                sheetBinding.ivAddImage.scaleType = ImageView.ScaleType.CENTER_CROP
                sheetBinding.ivAddImage.setImageURI(Uri.fromFile(file))
                sheetBinding.ivAddImage.setPadding(0, 0, 0, 0)
                sheetBinding.ivAddImage.alpha = 1.0f
            }
        }

        sheetBinding.llAddImage.setOnClickListener {
            selectedProductForImage = product
            currentBottomSheetImageView = sheetBinding.ivAddImage
            imagePickerLauncher.launch("image/*")
        }

        sheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sheetBinding.btnAdd.setOnClickListener {
            val name = sheetBinding.etProductName.text.toString().trim()
            if (name.isNotEmpty()) {
                val oldName = product.name
                product.name = name
                product.imageUri = newProductImageUri
                repository.updateProduct(product)
                
                if (oldName != name) {
                    historyRepository.updateProductName(product.id, name)
                }
                
                setupProductList(binding.etSearch.text.toString())
                bottomSheetDialog.dismiss()
                ToastHelper.showToast(this, "แก้ไขข้อมูลเรียบร้อย")
            } else {
                ToastHelper.showToast(this, "กรุณากรอกชื่อสิ่งของ")
            }
        }

        bottomSheetDialog.show()
    }

    private fun showDeleteConfirmDialog(product: Product) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณต้องการลบรายการ \"${product.name}\" ใช่หรือไม่?")
            .setPositiveButton("ลบ") { _, _ ->
                repository.deleteProduct(product.id)
                setupProductList(binding.etSearch.text.toString())
                ToastHelper.showToast(this, "ลบรายการเรียบร้อย")
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun showAddProductBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddProductBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        newProductImageUri = null
        currentBottomSheetImageView = sheetBinding.ivAddImage

        sheetBinding.llAddImage.setOnClickListener {
            selectedProductForImage = null
            imagePickerLauncher.launch("image/*")
        }

        sheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sheetBinding.btnAdd.setOnClickListener {
            val name = sheetBinding.etProductName.text.toString()
            if (name.isNotEmpty()) {
                val newProduct = Product(
                    name = name,
                    imageUri = newProductImageUri
                )
                repository.addProduct(newProduct)
                setupProductList()
                bottomSheetDialog.dismiss()
            } else {
                ToastHelper.showToast(this, "กรุณากรอกชื่อสิ่งของ")
            }
        }

        bottomSheetDialog.show()
    }
}
