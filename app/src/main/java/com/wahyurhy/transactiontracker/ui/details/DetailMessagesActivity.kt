package com.wahyurhy.transactiontracker.ui.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.databinding.ActivityDetailMessagesBinding
import com.wahyurhy.transactiontracker.databinding.ActivityMainBinding

class DetailMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailMessagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }

        if (intent.hasExtra("messages")) {
            val messages = intent.getStringExtra("messages")
            binding.messages.text = messages
            binding.messages.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", binding.messages.text.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
            }
        }
    }
}