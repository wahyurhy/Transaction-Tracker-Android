package com.wahyurhy.transactiontracker.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.databinding.ActivityMainBinding
import com.wahyurhy.transactiontracker.ui.addtransaction.AddTransactionActivity
import com.wahyurhy.transactiontracker.ui.fragments.ProfileFragment
import com.wahyurhy.transactiontracker.ui.fragments.TransactionFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        
        val transactionFragment = TransactionFragment()
        val profileFragment = ProfileFragment()
        binding.chipAppBar.setItemSelected(R.id.ic_transaction, true)
        makeCurrentFragment(transactionFragment)
        binding.chipAppBar.setOnItemSelectedListener {
            when (it) {
                R.id.ic_transaction -> makeCurrentFragment(transactionFragment)
                R.id.ic_profile -> makeCurrentFragment(profileFragment)
            }
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fl_wrapper, fragment)
            commit()
        }
    }

    fun floatingButton(view: View) {
        val intent = Intent(this@MainActivity, AddTransactionActivity::class.java)
        startActivity(intent)
    }
}