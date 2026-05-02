package com.krisadan.tappick.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.krisadan.tappick.R
import com.krisadan.tappick.data.model.Product
import com.krisadan.tappick.databinding.ItemEditProductBinding
import java.io.File

class EditProductAdapter(
    private var products: List<Product>,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<EditProductAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemEditProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.binding.tvProductName.text = product.name
        
        if (product.imageUri != null) {
            val file = File(product.imageUri!!)
            if (file.exists()) {
                holder.binding.imgProduct.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.binding.imgProduct.setImageURI(Uri.fromFile(file))
                holder.binding.imgProduct.setPadding(0, 0, 0, 0)
            } else {
                setDefaultImage(holder.binding.imgProduct)
            }
        } else {
            setDefaultImage(holder.binding.imgProduct)
        }

        holder.binding.btnEditProduct.setOnClickListener { onEdit(product) }
        holder.binding.btnDeleteProduct.setOnClickListener { onDelete(product) }
    }

    private fun setDefaultImage(imageView: ImageView) {
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.setImageResource(R.drawable.ic_image)
        val p = (12 * imageView.resources.displayMetrics.density).toInt()
        imageView.setPadding(p, p, p, p)
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}
