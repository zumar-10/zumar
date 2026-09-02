package com.zumar.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zumar.app.util.SessionManager

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var matchedEmail: String? = null
    private var code: String = ""

    private lateinit var stepIdentify: View
    private lateinit var stepOtp: View
    private lateinit var stepReset: View
    private lateinit var stepDone: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        session = SessionManager(this)

        stepIdentify = findViewById(R.id.stepIdentify)
        stepOtp = findViewById(R.id.stepOtp)
        stepReset = findViewById(R.id.stepReset)
        stepDone = findViewById(R.id.stepDone)

        findViewById<TextView>(R.id.btnFpBack).setOnClickListener { finish() }

        val etIdentifier = findViewById<EditText>(R.id.etFpIdentifier)
        val tvIdentifyError = findViewById<TextView>(R.id.tvFpIdentifyError)
        findViewById<Button>(R.id.btnFpSendCode).setOnClickListener {
            val identifier = etIdentifier.text.toString().trim()
            val email = session.findEmailByIdentifier(identifier)
            if (email == null) {
                tvIdentifyError.text = "No account matches that email or phone number."
                tvIdentifyError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            matchedEmail = email
            code = SessionManager.generateOtp()
            findViewById<TextView>(R.id.tvFpOtpTarget).text = "Enter the code sent to\n$email"
            findViewById<TextView>(R.id.tvFpDemoCode).text = "Your code is $code"
            showStep(stepOtp)
        }

        val etOtp = findViewById<EditText>(R.id.etFpOtpCode)
        val tvOtpError = findViewById<TextView>(R.id.tvFpOtpError)
        findViewById<Button>(R.id.btnFpVerifyCode).setOnClickListener {
            val entered = etOtp.text.toString().trim()
            if (entered != code) {
                tvOtpError.text = "That code doesn't match. Try again."
                tvOtpError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            showStep(stepReset)
        }

        val etNewPassword = findViewById<EditText>(R.id.etFpNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etFpConfirmPassword)
        val tvResetError = findViewById<TextView>(R.id.tvFpResetError)
        findViewById<Button>(R.id.btnFpUpdatePassword).setOnClickListener {
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            if (newPassword.length < 6) {
                tvResetError.text = "Password should be at least 6 characters."
                tvResetError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                tvResetError.text = "Passwords do not match."
                tvResetError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            session.resetPassword(matchedEmail!!, SessionManager.hash(newPassword))
            showStep(stepDone)
        }

        findViewById<Button>(R.id.btnFpBackToLogin).setOnClickListener { finish() }
    }

    private fun showStep(step: View) {
        stepIdentify.visibility = if (step == stepIdentify) View.VISIBLE else View.GONE
        stepOtp.visibility = if (step == stepOtp) View.VISIBLE else View.GONE
        stepReset.visibility = if (step == stepReset) View.VISIBLE else View.GONE
        stepDone.visibility = if (step == stepDone) View.VISIBLE else View.GONE
    }
}
