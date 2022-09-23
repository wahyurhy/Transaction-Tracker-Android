package com.wahyurhy.transactiontracker.ui.signup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.databinding.ActivitySignupBinding
import com.wahyurhy.transactiontracker.ui.login.LoginActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {

            val email = binding.edtEmail.editText?.text.toString().trim()
            val password = binding.edtPassword.editText?.text.toString().trim()
            val confirmPassword = binding.edtPasswordConfirmation.editText?.text.toString().trim()

            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    if (password == confirmPassword) {
                        progressBarStatus(true)
                        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                            if (it.isSuccessful) {
                                val intent = Intent(this, LoginActivity::class.java)
                                Toast.makeText(this, getString(R.string.sign_up_success), Toast.LENGTH_SHORT).show()
                                progressBarStatus(false)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                progressBarStatus(false)
                                Toast.makeText(this, it.exception?.message.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.password_is_not_match), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.empty_field_info), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.invalid_empty_email_info), Toast.LENGTH_SHORT).show()
            }
        }

        binding.login.setOnClickListener {
            finish()
        }
    }

    private fun progressBarStatus(isActive: Boolean) {
        when (isActive) {
            true -> binding.progressBar.visibility = View.VISIBLE
            false -> binding.progressBar.visibility = View.GONE
        }
    }
}