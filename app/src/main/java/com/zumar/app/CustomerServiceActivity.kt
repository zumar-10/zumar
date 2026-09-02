package com.zumar.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CustomerServiceActivity : AppCompatActivity() {

    // Numbers as provided: WhatsApp contact and call contact
    private val whatsappNumber = "2349166889556" // 09166889556 in international format
    private val callNumber = "07060698438"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_service)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnWhatsapp).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsappNumber"))
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnCall).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callNumber"))
            startActivity(intent)
        }
    }
}
