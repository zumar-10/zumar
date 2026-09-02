package com.zumar.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zumar.app.model.User
import com.zumar.app.util.SessionManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        session = SessionManager(this)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etMiddleName = findViewById<EditText>(R.id.etMiddleName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val spGender = findViewById<Spinner>(R.id.spGender)
        val etDob = findViewById<EditText>(R.id.etDob)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etState = findViewById<EditText>(R.id.etState)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etPin = findViewById<EditText>(R.id.etPin)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val tvError = findViewById<TextView>(R.id.tvRegisterError)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)

        val genders = arrayOf("Select gender", "Female", "Male", "Prefer not to say")
        spGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val middleName = etMiddleName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val gender = spGender.selectedItem.toString()
            val dob = etDob.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val state = etState.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            val pin = etPin.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || email.isEmpty() ||
                address.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
            ) {
                showError(tvError, "Please fill in all required fields.")
                return@setOnClickListener
            }
            if (gender == genders[0]) { showError(tvError, "Please select your gender."); return@setOnClickListener }
            if (password != confirmPassword) { showError(tvError, "Passwords do not match."); return@setOnClickListener }
            if (password.length < 6) { showError(tvError, "Password should be at least 6 characters."); return@setOnClickListener }
            if (pin.length != 4) { showError(tvError, "Transaction PIN must be exactly 4 digits."); return@setOnClickListener }
            if (!cbTerms.isChecked) { showError(tvError, "Please accept the Terms and Privacy Policy."); return@setOnClickListener }

            val user = User(
                firstName = firstName, middleName = middleName, lastName = lastName,
                phone = phone, email = email,
                hashedPassword = SessionManager.hash(password),
                address = address, state = state, dob = dob, gender = gender,
                hashedPin = SessionManager.hash(pin),
                balance = 0.0
            )

            val success = session.registerUser(user)
            if (!success) {
                showError(tvError, "An account with this email or phone number already exists.")
                return@setOnClickListener
            }

            val intent = Intent(this, OtpActivity::class.java)
            intent.putExtra(OtpActivity.EXTRA_EMAIL, email)
            startActivity(intent)
            finish()
        }

        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showError(tv: TextView, message: String) {
        tv.text = message
        tv.visibility = TextView.VISIBLE
    }
}
