package com.wahyurhy.transactiontracker.ui.addtransaction

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.databinding.ActivityAddTransactionBinding
import com.wahyurhy.transactiontracker.utils.SELECT_PHONE_NUMBER
import com.wahyurhy.transactiontracker.utils.setMaskingMoney
import com.wahyurhy.transactiontracker.utils.setMaskingPhoneNumber
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding

    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var isSubmitted: Boolean = false
    private var date: Long = 0
    private var paymentAmount: Double = 0.0
    private var invertedDate: Long = 0
    private var whatsApp = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.edWhatsApp.setMaskingPhoneNumber("")
        binding.ibContact.setOnClickListener {
            val intentPickContact = Intent(Intent.ACTION_PICK)
            intentPickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
            startActivityForResult(intentPickContact, SELECT_PHONE_NUMBER)
        }

        binding.edAmount.apply {
            setMaskingMoney("Rp. ")
            setText("")
        }

        val user = Firebase.auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference(uid)
        }
        auth = Firebase.auth

        // --- data picker ---
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val currentDate = sdf.parse(sdf.format(System.currentTimeMillis()))
        date = currentDate!!.time
        binding.edDate.setOnClickListener {
            clickDatePicker()
        }

        binding.btnSave.setOnClickListener {
            if (!isSubmitted) {
                saveTransactionData()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "You have saved the transaction data", Snackbar.LENGTH_LONG).show()
            }
        }


    }

    private fun saveTransactionData() {
        val name = binding.edName.text.toString().trim()
        whatsApp = binding.edWhatsApp.text.toString().trim().replace("-", "")
        val amount = binding.edAmount.text.toString().trim()

        when {
            name.isEmpty() -> binding.edName.error = "Please enter name"
            amount.isEmpty() -> binding.edAmount.error = "Please enter amount"
            else -> {

                when {
                    binding.edAmount.text.toString()[0] == 'R' -> paymentAmount = binding.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble()
                }

                val transactionID = dbRef.push().key!!

                invertedDate = date * -1 //convert millis value to negative, so it can be sort as descending order

                val transaction = TransactionModel(
                    transactionID,
                    name,
                    whatsApp,
                    paymentAmount,
                    date,
                    false,
                    invertedDate,
                    paymentAmount,
                    0.0,
                    0.0
                )

                dbRef.child(transactionID).setValue(transaction)
                    .addOnCompleteListener {
                        Log.d("AddTransactionActivity", "saveTransactionData: masuk addOnCompleteListener")
                        Toast.makeText(this, "Data Inserted Successfully", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }.addOnFailureListener { err ->
                        Toast.makeText(this, "Error ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                isSubmitted = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHONE_NUMBER && resultCode == Activity.RESULT_OK) {
            val contactUri = data?.data ?: return
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
            val cursor = applicationContext.contentResolver.query(contactUri, projection, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = cursor.getString(nameIndex)
                var number = cursor.getString(numberIndex)

                when {
                    number[0].toString() == "+" -> number = number.replace("+62", "")
                    number[0].toString() == "6" -> number = number.replace("62", "")
                }

                binding.edName.setText(name)
                binding.edWhatsApp.setText(number)
            }

            cursor?.close()
        }
    }

    private fun clickDatePicker() {
        val myCalendar = Calendar.getInstance()
        val year = myCalendar.get(Calendar.YEAR)
        val month = myCalendar.get(Calendar.MONTH)
        val day = myCalendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->

                val selectedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
                binding.edDate.text = null

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val showDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                val theDate = sdf.parse(selectedDate)
                date = theDate!!.time //convert date to millisecond
                binding.edDate.setText(showDate.format(date))

            },
            year,
            month,
            day
        )
        dpd.show()
    }
}