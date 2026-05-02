package com.krisadan.tappick.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krisadan.tappick.data.model.Member
import com.krisadan.tappick.data.model.Role
import com.krisadan.tappick.databinding.ItemMemberBinding

class MemberAdapter(
    private var members: List<Member>,
    private var roles: Map<String, Role>,
    private val onEdit: (Member) -> Unit,
    private val onDelete: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = members[position]
        val role = roles[member.roleId]
        
        holder.binding.tvMemberName.text = member.name
        holder.binding.tvRole.text = role?.name ?: "ไม่ระบุ"
        
        val roleColorHex = role?.color ?: "#0055D4"
        val roleColor = try { Color.parseColor(roleColorHex) } catch (e: Exception) { Color.parseColor("#0055D4") }
        
        holder.binding.tvRole.setTextColor(roleColor)
        val badgeBg = GradientDrawable().apply {
            cornerRadius = 4 * holder.itemView.resources.displayMetrics.density
            val alphaColor = (38 shl 24) or (roleColor and 0x00FFFFFF)
            setColor(alphaColor)
        }
        holder.binding.tvRole.background = badgeBg

        holder.binding.btnEdit.setOnClickListener { onEdit(member) }
        holder.binding.btnDelete.setOnClickListener { onDelete(member) }
    }

    override fun getItemCount() = members.size

    fun updateData(newMembers: List<Member>, newRoles: Map<String, Role>) {
        this.members = newMembers
        this.roles = newRoles
        notifyDataSetChanged()
    }
}
