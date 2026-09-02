package com.zumar.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zumar.app.util.SessionManager

/**
 * Demo-only OTP screen. No real email is sent — the generated code is
 * shown directly on screen so the flow can be tested end to end. Wiring
 * this up to a real email/SMS provider is a later step once a backend
 * is in place.
 */
class OtpActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        session = SessionManager(this)

        val email = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        val code = SessionManager.generateOtp()

        val tvEmail = findViewById<TextView>(R.id.tvOtpEmail)
        val tvDemoCode = findViewById<TextView>(R.id.tvDemoCode)
        val etCode = findViewById<EditText>(R.id.etOtpCode)
        val tvError = findViewById<TextView>(R.id.tvOtpError)
        val btnVerify = findViewById<Button>(R.id.btnVerifyOtp)
        val btnBack = findViewById<TextView>(R.id.btnOtpBack)

        tvEmail.text = "Enter the 4-digit code sent to\n$email"
        tvDemoCode.text = "Your code is $code"

        btnVerify.setOnClickListener {
            val entered = etCode.text.toString().trim()
            if (entered != code) {
                tvError.text = "That code doesn't match. Try again."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            session.completeLogin(email)
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        btnBack.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}
