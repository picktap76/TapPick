package com.krisadan.tappick.util

import android.graphics.Typeface
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.krisadan.tappick.R

class PaginationHelper<T>(
    private val container: View,
    private val onPageChanged: () -> Unit
) {
    private val tvTotalItems: TextView = container.findViewById(R.id.tvTotalItems)
    private val llPagesContainer: LinearLayout = container.findViewById(R.id.llPagesContainer)
    private val btnPrevPage: ImageButton = container.findViewById(R.id.btnPrevPage)
    private val btnNextPage: ImageButton = container.findViewById(R.id.btnNextPage)
    private val btnPageSize: View = container.findViewById(R.id.btnPageSize)
    private val tvPageSize: TextView = container.findViewById(R.id.tvPageSize)

    var currentPage = 1
        private set
    var pageSize = 10
        private set
    private var totalItems = 0

    init {
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                onPageChanged()
            }
        }
        btnNextPage.setOnClickListener {
            if (currentPage < getTotalPages()) {
                currentPage++
                onPageChanged()
            }
        }
        btnPageSize.setOnClickListener {
            showPageSizeDialog()
        }
    }

    fun update(totalItems: Int) {
        this.totalItems = totalItems
        val totalPages = getTotalPages()
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages
        } else if (totalPages == 0) {
            currentPage = 1
        }

        tvTotalItems.text = "ทั้งหมด $totalItems รายการ"
        tvPageSize.text = "$pageSize / หน้า"
        
        renderPageNumbers(totalPages)
        
        btnPrevPage.isEnabled = currentPage > 1
        btnNextPage.isEnabled = currentPage < totalPages
        
        btnPrevPage.alpha = if (btnPrevPage.isEnabled) 1.0f else 0.3f
        btnNextPage.alpha = if (btnNextPage.isEnabled) 1.0f else 0.3f
    }

    private fun getTotalPages(): Int {
        return Math.ceil(totalItems.toDouble() / pageSize).toInt()
    }

    fun getPaginatedList(allData: List<T>): List<T> {
        val fromIndex = (currentPage - 1) * pageSize
        val toIndex = Math.min(fromIndex + pageSize, allData.size)
        return if (fromIndex < allData.size) {
            allData.subList(fromIndex, toIndex)
        } else {
            emptyList()
        }
    }

    private fun renderPageNumbers(totalPages: Int) {
        llPagesContainer.removeAllViews()
        if (totalPages <= 1) return

        
        
        val start = Math.max(1, currentPage - 2)
        val end = Math.min(totalPages, start + 4)
        
        val actualStart = Math.max(1, Math.min(start, totalPages - 4))

        for (i in actualStart..end) {
            val tv = TextView(container.context).apply {
                text = i.toString()
                layoutParams = LinearLayout.LayoutParams(
                    (28 * resources.displayMetrics.density).toInt(),
                    (28 * resources.displayMetrics.density).toInt()
                ).apply {
                    setMargins((2 * resources.displayMetrics.density).toInt(), 0, (2 * resources.displayMetrics.density).toInt(), 0)
                }
                gravity = android.view.Gravity.CENTER
                textSize = 11f
                
                if (i == currentPage) {
                    setBackgroundResource(R.drawable.bg_badge_blue) 
                    setTextColor(container.context.getColor(R.color.primary_blue))
                    setTypeface(null, Typeface.BOLD)
                } else {
                    setTextColor(container.context.getColor(R.color.text_subtitle))
                }
                
                setOnClickListener {
                    if (currentPage != i) {
                        currentPage = i
                        onPageChanged()
                    }
                }
            }
            llPagesContainer.addView(tv)
        }
    }

    private fun showPageSizeDialog() {
        val options = arrayOf("5", "10", "20", "50")
        AlertDialog.Builder(container.context)
            .setTitle("จำนวนรายการต่อหน้า")
            .setItems(options) { _, which ->
                pageSize = options[which].toInt()
                currentPage = 1
                onPageChanged()
            }
            .show()
    }
}
