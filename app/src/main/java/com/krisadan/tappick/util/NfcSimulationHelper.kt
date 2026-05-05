package com.krisadan.tappick.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.krisadan.tappick.R

object NfcSimulationHelper {
    
    const val IS_SIMULATION_ENABLED = false

    fun showManualNfcDialog(context: Context, callback: (String) -> Unit) {
        if (!IS_SIMULATION_ENABLED) return

        val density = context.resources.displayMetrics.density
        val paddingSize = (24 * density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingSize, (16 * density).toInt(), paddingSize, (8 * density).toInt())
        }

        val tvSubtitle = TextView(context).apply {
            text = "ระบุรหัสประจำตัวบัตร (Hex) เพื่อจำลองการแตะ"
            textSize = 14f
            setTextColor(context.getColor(R.color.text_subtitle))
            setPadding(0, 0, 0, (12 * density).toInt())
        }

        val editText = EditText(context).apply {
            hint = "เช่น: 04A1B2C3"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
            background = null
            textSize = 18f
            setTextColor(context.getColor(R.color.text_title))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            val editPadding = (16 * density).toInt()
            setPadding(editPadding, editPadding, editPadding, editPadding)
        }

        val inputContainer = LinearLayout(context).apply {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(context.getColor(R.color.white))
                cornerRadius = 12 * density
                setStroke((1 * density).toInt(), context.getColor(R.color.card_border))
            }
            background = bg
            addView(editText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        layout.addView(tvSubtitle)
        layout.addView(inputContainer)
        
        AlertDialog.Builder(context)
            .setTitle("จำลองการแตะบัตร NFC")
            .setView(layout)
            .setPositiveButton("จำลองการแตะ") { _, _ ->
                val id = editText.text.toString().trim().uppercase()
                if (id.isNotEmpty()) {
                    callback(id)
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }
}
