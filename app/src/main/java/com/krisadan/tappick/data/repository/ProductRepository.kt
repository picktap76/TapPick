package com.krisadan.tappick.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krisadan.tappick.data.model.Product
import java.io.File
import java.io.FileOutputStream

class ProductRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tappick_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val appContext = context.applicationContext
    
    @Volatile
    private var cachedProducts: MutableList<Product>? = null

    companion object {
        @Volatile
        private var INSTANCE: ProductRepository? = null

        fun getInstance(context: Context): ProductRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProductRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    @Synchronized
    fun getProducts(): List<Product> {
        return cachedProducts ?: run {
            val json = prefs.getString("products", null)
            val products: MutableList<Product> = if (json != null) {
                try {
                    val type = object : TypeToken<MutableList<Product>>() {}.type
                    gson.fromJson(json, type) ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            cachedProducts = products
            products
        }
    }

    @Synchronized
    fun saveProducts(products: List<Product>) {
        cachedProducts = products.toMutableList()
        try {
            val json = gson.toJson(products)
            prefs.edit().putString("products", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateProduct(updatedProduct: Product) {
        val products = getProducts().toMutableList()
        val index = products.indexOfFirst { it.id == updatedProduct.id }
        if (index != -1) {
            products[index] = updatedProduct
            saveProducts(products)
        }
    }

    fun addProduct(product: Product) {
        val products = getProducts().toMutableList()
        products.add(product)
        saveProducts(products)
    }

    fun deleteProduct(productId: String) {
        val products = getProducts().toMutableList()
        products.removeAll { it.id == productId }
        saveProducts(products)
    }

    fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val fileName = "product_${System.currentTimeMillis()}.jpg"
            val file = File(appContext.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
