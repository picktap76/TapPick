package com.krisadan.tappick.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.krisadan.tappick.R
import com.krisadan.tappick.data.repository.MemberRepository
import com.krisadan.tappick.data.repository.SessionManager
import com.krisadan.tappick.databinding.ActivityPinBinding
import com.krisadan.tappick.util.ToastHelper

class PinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinBinding
    private lateinit var memberRepository: MemberRepository
    private lateinit var sessionManager: SessionManager
    private var currentPin = ""
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memberRepository = MemberRepository.getInstance(this)
        sessionManager = SessionManager.getInstance(this)

        // If called for result (adding member), it's not login mode
        isLoginMode = callingActivity == null

        if (!isLoginMode) {
            binding.btnSwitchToNfc.visibility = View.GONE
        }

        setupKeypad()
        updatePinDots()

        binding.btnSwitchToNfc.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupKeypad() {
        val digitButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )

        digitButtons.forEach { button ->
            button.setOnClickListener {
                if (currentPin.length < 4) {
                    currentPin += button.text
                    updatePinDots()
                    if (currentPin.length == 4) {
                        if (isLoginMode) {
                            handleLogin()
                        } else {
                            finishWithResult()
                        }
                    }
                }
            }
        }

        binding.btnBackspace.setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.dropLast(1)
                updatePinDots()
            }
        }
    }

    private fun handleLogin() {
        val member = memberRepository.getMembers().find { it.pin == currentPin }
        if (member != null) {
            sessionManager.login(member.id)
            ToastHelper.showToast(this, "ยินดีต้อนรับคุณ ${member.name}")
            
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            currentPin = ""
            updatePinDots()
            ToastHelper.showToast(this, "รหัส PIN ไม่ถูกต้อง")
        }
    }

    private fun updatePinDots() {
        val dots = listOf(binding.pin1, binding.pin2, binding.pin3, binding.pin4)
        dots.forEachIndexed { index, imageView ->
            if (index < currentPin.length) {
                imageView.setImageResource(R.drawable.bg_pin_circle_filled)
            } else {
                imageView.setImageResource(R.drawable.bg_pin_circle_empty)
            }
        }
    }

    private fun finishWithResult() {
        val resultIntent = Intent()
        resultIntent.putExtra("EXTRA_PIN", currentPin)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
