package com.wahyurhy.transactiontracker.ui.login

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.databinding.ActivityLoginBinding
import com.wahyurhy.transactiontracker.ui.main.MainActivity
import com.wahyurhy.transactiontracker.ui.signup.SignupActivity

class LoginActivity : AppCompatActivity() {

    private var auth: FirebaseAuth = Firebase.auth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.register.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        emailLogin()

        // Configure Google Signin
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("244448595042-2gs2pf7e5c1igrlklqtafhra6v6u6uip.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut()
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(ContentValues.TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(ContentValues.TAG, "Google sign in failed!", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        progressBarStatus(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "signInWithCredential:success")
                    val intent = Intent(this, MainActivity::class.java)
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    progressBarStatus(false)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Log.w(ContentValues.TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object{
        const val RC_SIGN_IN = 1001
    }

    private fun emailLogin() {
        binding.btnLogin.setOnClickListener {

            val email = binding.edtEmail.editText?.text.toString()
            val password = binding.edtPassword.editText?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressBarStatus(true)
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        progressBarStatus(false)
                        startActivity(intent)
                    } else {
                        progressBarStatus(false)
                        Toast.makeText(this, it.exception?.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are no Allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun progressBarStatus(isActive: Boolean) {
        when (isActive) {
            true -> binding.progressBar.visibility = View.VISIBLE
            false -> binding.progressBar.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            Intent(this, MainActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
            }
        }
    }
}