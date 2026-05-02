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
    
    private var cachedProducts: MutableList<Product>? = null

    companion object {
        @Volatile
        private var INSTANCE: ProductRepository? = null

        fun getInstance(context: Context): ProductRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProductRepository(context).also { INSTANCE = it }
            }
        }
    }

    fun getProducts(): List<Product> {
        if (cachedProducts == null) {
            val json = prefs.getString("products", null)
            cachedProducts = if (json != null) {
                val type = object : TypeToken<MutableList<Product>>() {}.type
                gson.fromJson(json, type)
            } else {
                getDefaultProducts().toMutableList()
            }
        }
        return cachedProducts ?: emptyList()
    }

    fun saveProducts(products: List<Product>) {
        cachedProducts = products.toMutableList()
        val json = gson.toJson(products)
        prefs.edit().putString("products", json).apply()
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

    private fun getDefaultProducts(): List<Product> {
        return emptyList()
    }

    fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val fileName = "product_${System.currentTimeMillis()}.jpg"
            val file = File(appContext.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
