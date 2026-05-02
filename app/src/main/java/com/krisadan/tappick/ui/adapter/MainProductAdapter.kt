package com.krisadan.tappick.ui.adapter

import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krisadan.tappick.R
import com.krisadan.tappick.data.model.Product
import com.krisadan.tappick.data.model.TransactionItem
import com.krisadan.tappick.util.ToastHelper
import java.io.File

class MainProductAdapter(private var products: List<Product>) :
    RecyclerView.Adapter<MainProductAdapter.ViewHolder>() {

    private val quantities = mutableMapOf<String, Int>()
    private var remainingCounts = mapOf<String, Int?>() // productId -> remaining count (null means unlimited)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvRemaining: TextView = view.findViewById(R.id.tvRemaining)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.tvProductName.text = product.name
        
        // Handle Remaining Count display
        val remaining = remainingCounts[product.id]
        val remainingText = if (remaining == null) "ไม่จำกัด" else remaining.toString()
        val fullText = "คงเหลือ: $remainingText"
        val spannable = SpannableString(fullText)
        val color = if (remaining == null || remaining > 0) {
            ContextCompat.getColor(holder.itemView.context, R.color.text_green)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.text_red)
        }
        
        val startIndex = fullText.indexOf(remainingText)
        if (startIndex != -1 && remainingText.isNotEmpty()) {
            spannable.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                startIndex + remainingText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        holder.tvRemaining.text = spannable

        if (product.imageUri != null) {
            val file = File(product.imageUri!!)
            if (file.exists()) {
                holder.imgProduct.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.imgProduct.setImageURI(Uri.fromFile(file))
                holder.imgProduct.setPadding(0, 0, 0, 0)
            } else {
                holder.imgProduct.scaleType = ImageView.ScaleType.CENTER_INSIDE
                holder.imgProduct.setImageResource(R.drawable.ic_image)
                val p = (32 * holder.itemView.resources.displayMetrics.density).toInt()
                holder.imgProduct.setPadding(p, p, p, p)
            }
        } else {
            holder.imgProduct.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.imgProduct.setImageResource(R.drawable.ic_image)
            val p = (32 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.imgProduct.setPadding(p, p, p, p)
        }

        val currentQty = quantities[product.id] ?: 0
        holder.tvQuantity.text = currentQty.toString()

        holder.btnPlus.setOnClickListener {
            val current = quantities[product.id] ?: 0
            val remaining = remainingCounts[product.id]
            
            if (remaining == null || current < remaining) {
                val newQty = current + 1
                quantities[product.id] = newQty
                holder.tvQuantity.text = newQty.toString()
            } else {
                ToastHelper.showToast(holder.itemView.context, "เกินจำนวนที่สามารถเบิกได้")
            }
        }

        holder.btnMinus.setOnClickListener {
            val current = quantities[product.id] ?: 0
            if (current > 0) {
                val newQty = current - 1
                quantities[product.id] = newQty
                holder.tvQuantity.text = newQty.toString()
            }
        }
    }

    override fun getItemCount() = products.size

    fun getSelectedItems(): List<TransactionItem> {
        return products.filter { (quantities[it.id] ?: 0) > 0 }
            .map { TransactionItem(it.id, it.name, quantities[it.id] ?: 0) }
    }

    fun clearQuantities() {
        quantities.clear()
        notifyDataSetChanged()
    }

    fun updateData(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }

    fun updateRemainingCounts(counts: Map<String, Int?>) {
        this.remainingCounts = counts
        notifyDataSetChanged()
    }
}
