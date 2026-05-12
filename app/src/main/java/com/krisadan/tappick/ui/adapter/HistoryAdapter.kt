package com.krisadan.tappick.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krisadan.tappick.data.model.Role
import com.krisadan.tappick.databinding.ItemHistoryRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var flattenedItems: List<HistoryRowData>,
    private var roles: Map<String, Role>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val thaiLocale = Locale("th", "TH")
    private val sdfDate = SimpleDateFormat("dd/MM/yy", thaiLocale)
    private val sdfTime = SimpleDateFormat("HH:mm", thaiLocale)

    data class HistoryRowData(
        val timestamp: Long,
        val memberName: String,
        val roleId: String?,
        val productName: String,
        val quantity: Int
    )

    class ViewHolder(val binding: ItemHistoryRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = flattenedItems[position]
        val role = roles[item.roleId]
        
        holder.binding.tvDate.text = sdfDate.format(Date(item.timestamp))
        holder.binding.tvTime.text = sdfTime.format(Date(item.timestamp))
        holder.binding.tvUserName.text = item.memberName
        holder.binding.tvItemName.text = item.productName
        holder.binding.tvQuantity.text = item.quantity.toString()
        
        val roleName = role?.name ?: "ไม่ระบุ"
        holder.binding.tvUserRole.text = roleName
        
        val roleColorHex = role?.color ?: "#0055D4"
        val roleColor = try { Color.parseColor(roleColorHex) } catch (e: Exception) { Color.parseColor("#0055D4") }
        
        holder.binding.tvUserRole.setTextColor(roleColor)
        val badgeBg = GradientDrawable().apply {
            cornerRadius = 4 * holder.itemView.resources.displayMetrics.density
            val alphaColor = (38 shl 24) or (roleColor and 0x00FFFFFF)
            setColor(alphaColor)
        }
        holder.binding.tvUserRole.background = badgeBg
    }

    override fun getItemCount() = flattenedItems.size

    fun updateData(newItems: List<HistoryRowData>, newRoles: Map<String, Role>) {
        this.flattenedItems = newItems
        this.roles = newRoles
        notifyDataSetChanged()
    }
}
