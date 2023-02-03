package com.wahyurhy.transactiontracker.ui.addtransaction

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.databinding.ActivityAddTransactionBinding
import com.wahyurhy.transactiontracker.notification.MonthlyCreateTransaction
import com.wahyurhy.transactiontracker.utils.*
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding

//    private lateinit var broadcastReceiver: MonthlyCreateTransaction
    private var user: FirebaseUser? = null
    private var uid: String? = null
    private var sdf: SimpleDateFormat? = null
    private var currentDate: Date? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var dbRef: DatabaseReference? = null
    private var auth: FirebaseAuth? = null
    private var completeListener: DatabaseReference.CompletionListener? = null

    private var isSubmitted: Boolean = false
    private var date: Long = 0
    private var paymentAmount: Double = 0.0
    private var invertedDate: Long = 0
    private var whatsApp = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        backgroundThread = HandlerThread("BackgroundThread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.edWhatsApp.setMaskingPhoneNumber("")
        binding.ibContact.setOnClickListener {
            val intentPickContact = Intent(Intent.ACTION_PICK)
            intentPickContact.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            startActivityForResult(intentPickContact, SELECT_PHONE_NUMBER)
        }

        binding.edAmount.apply {
            setMaskingMoney("Rp. ")
            setText("")
        }

        user = Firebase.auth.currentUser
        uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference(uid!!)
        }
        auth = Firebase.auth

        // --- data picker ---
        sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        currentDate = sdf?.parse(sdf?.format(System.currentTimeMillis()).toString())
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
        showLoading(true)
        backgroundHandler?.post {
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

                    val transactionID = StringBuilder()
                    transactionID.append(dbRef?.push()?.key!! + "0")

                    invertedDate = date * -1 //convert millis value to negative, so it can be sort as descending order

                    val encryptedWhatsApp = encryptAES(whatsApp, SECRET_KEY)

                    Toast.makeText(this, whatsApp, Toast.LENGTH_SHORT).show()

                    val transaction = TransactionModel(
                        transactionID.toString(),
                        name,
                        encryptedWhatsApp,
                        paymentAmount,
                        date,
                        false,
                        invertedDate,
                        paymentAmount,
                        0.0,
                        0.0
                    )

//                    broadcastReceiver = MonthlyCreateTransaction(transactionID.toString().dropLast(1), name, whatsApp, paymentAmount, date)

                    if (dbRef != null) {
                        completeListener =
                            DatabaseReference.CompletionListener { databaseError, _ ->
                                if (databaseError == null) {
                                    // code yang akan dijalankan jika tidak terjadi error
                                    Log.d("AddTransactionActivity", "saveTransactionData: masuk addOnCompleteListener")
//                                    broadcastReceiver.setMonthlyNotification(this)
                                    Toast.makeText(this, getString(R.string.data_inserted_success), Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Log.e("AddTransactionActivity", "saveTransactionData: Error: ${databaseError.message}")
                                    runOnUiThread {
                                        showLoading(false)
                                        Toast.makeText(this, "Error ${databaseError.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        dbRef!!.child(transactionID.toString()).setValue(transaction, completeListener)
                        isSubmitted = true
                    }
                }
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

    private fun showLoading(isLoading: Boolean) {
        when (isLoading) {
            true -> binding.progressBar.visibility = View.VISIBLE
            false -> binding.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Thread.interrupted()
        backgroundThread?.quitSafely()
    }
}