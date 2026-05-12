package com.krisadan.tappick.ui.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
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
    private var remainingCounts = mapOf<String, Int?>() 

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rootLayout: ViewGroup = view as ViewGroup
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

        val addOneWithAnimation = {
            val current = quantities[product.id] ?: 0
            val rem = remainingCounts[product.id]
            
            if (rem == null || current < rem) {
                animateImageToPlusButton(holder)
                val newQty = current + 1
                quantities[product.id] = newQty
                holder.tvQuantity.text = newQty.toString()
            } else {
                ToastHelper.showToast(holder.itemView.context, "เกินจำนวนที่สามารถเบิกได้")
            }
        }

        holder.imgProduct.setOnClickListener {
            addOneWithAnimation()
        }

        holder.btnPlus.setOnClickListener {
            addOneWithAnimation()
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

    private fun animateImageToPlusButton(holder: ViewHolder) {
        val context = holder.itemView.context
        val parentView = holder.itemView.rootView as ViewGroup
        
        
        val animImageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(holder.imgProduct.width, holder.imgProduct.height)
            setImageDrawable(holder.imgProduct.drawable)
            scaleType = holder.imgProduct.scaleType
            if (holder.imgProduct.paddingTop > 0) {
                setPadding(holder.imgProduct.paddingLeft, holder.imgProduct.paddingTop, 
                           holder.imgProduct.paddingRight, holder.imgProduct.paddingBottom)
            }
        }
        
        parentView.addView(animImageView)

        
        val startLoc = IntArray(2)
        holder.imgProduct.getLocationInWindow(startLoc)
        val endLoc = IntArray(2)
        holder.btnPlus.getLocationInWindow(endLoc)

        animImageView.x = startLoc[0].toFloat()
        animImageView.y = startLoc[1].toFloat()

        
        val moveX = ObjectAnimator.ofFloat(animImageView, View.X, endLoc[0].toFloat() + (holder.btnPlus.width / 2) - (holder.imgProduct.width / 2))
        val moveY = ObjectAnimator.ofFloat(animImageView, View.Y, endLoc[1].toFloat() + (holder.btnPlus.height / 2) - (holder.imgProduct.height / 2))
        val scaleX = ObjectAnimator.ofFloat(animImageView, View.SCALE_X, 1f, 0.01f)
        val scaleY = ObjectAnimator.ofFloat(animImageView, View.SCALE_Y, 1f, 0.01f)
        val fade = ObjectAnimator.ofFloat(animImageView, View.ALPHA, 1f, 0.5f)

        val animatorSet = AnimatorSet().apply {
            playTogether(moveX, moveY, scaleX, scaleY, fade)
            duration = 600
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parentView.removeView(animImageView)
                }
            })
        }
        
        animatorSet.start()
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
