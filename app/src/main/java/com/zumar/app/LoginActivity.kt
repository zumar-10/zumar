package com.zumar.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zumar.app.util.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)

        val etIdentifier = findViewById<EditText>(R.id.etLoginIdentifier)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val tvError = findViewById<TextView>(R.id.tvLoginError)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val identifier = etIdentifier.text.toString().trim()
            val password = etPassword.text.toString()

            if (identifier.isEmpty() || password.isEmpty()) {
                showError(tvError, "Please enter your email/phone and password.")
                return@setOnClickListener
            }

            val hashed = SessionManager.hash(password)
            val matchedEmail = session.checkCredentials(identifier, hashed)
            if (matchedEmail == null) {
                showError(tvError, "We couldn't match that email/phone and password to any account.")
            } else {
                val intent = Intent(this, OtpActivity::class.java)
                intent.putExtra(OtpActivity.EXTRA_EMAIL, matchedEmail)
                startActivity(intent)
            }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun showError(tv: TextView, message: String) {
        tv.text = message
        tv.visibility = TextView.VISIBLE
    }
}
