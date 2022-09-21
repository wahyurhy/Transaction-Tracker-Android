package com.wahyurhy.transactiontracker.ui.signup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
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
                                Toast.makeText(this, "Sign up Successful", Toast.LENGTH_SHORT).show()
                                progressBarStatus(false)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                progressBarStatus(false)
                                Toast.makeText(this, it.exception?.message.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Password is not Matching", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Empty Fields Are no Allowed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid or Empty Email", Toast.LENGTH_SHORT).show()
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