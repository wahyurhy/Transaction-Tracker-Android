package com.wahyurhy.transactiontracker.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.databinding.FragmentProfileBinding
import com.wahyurhy.transactiontracker.ui.login.LoginActivity

class ProfileFragment : Fragment() {

    private var auth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
//    private val uid = user?.uid
//    private var dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference(uid!!)

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        user = Firebase.auth.currentUser

        logout()

        accountDetails()
    }

    private fun accountDetails() {
        user?.reload()
        user?.let {
            val username = user!!.displayName
            val email = user!!.email

            if (user!!.isEmailVerified) {
                binding.verifiedStatus.visibility = View.VISIBLE
                binding.unverifiedStatus.visibility = View.GONE

                binding.verifiedStatus.setOnClickListener {
                    Toast.makeText(
                        this@ProfileFragment.activity,
                        getString(R.string.verified_account_info),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                binding.unverifiedStatus.visibility = View.VISIBLE
                binding.verifiedStatus.visibility = View.GONE

                binding.unverifiedStatus.setOnClickListener {
                    user?.sendEmailVerification()?.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                this@ProfileFragment.activity,
                                getString(R.string.check_email_info),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ProfileFragment.activity,
                                it.exception?.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }


            val name = if (username == null || username == "") {
                val splitValue = email?.split("@")
                splitValue?.get(0)
            } else {
                username
            }
            binding.tvName.text = name.toString()
            binding.picture.text = name?.get(0).toString().uppercase()
            binding.tvEmail.text = email.toString()
        }
    }

    private fun logout() {
        binding.btnLogout.setOnClickListener {
            auth?.signOut()
            Intent(this.activity, LoginActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity?.startActivity(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        auth = null
        user = null
        _binding = null
    }

}