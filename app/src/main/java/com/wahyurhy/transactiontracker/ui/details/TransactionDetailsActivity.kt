package com.wahyurhy.transactiontracker.ui.details

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.databinding.ActivityTransactionDetailsBinding
import com.wahyurhy.transactiontracker.databinding.EditDialogBinding
import com.wahyurhy.transactiontracker.utils.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailsActivity : AppCompatActivity() {

    private val TAG = TransactionDetailsActivity::class.java.simpleName

    private var date: Long = 0
    private var invertedDate: Long = 0
    private var amountLeft = 0.0
    private var amountOver = 0.0
    private var amountCurrently = 0.0
    private var status = false

    private lateinit var binding: ActivityTransactionDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        val transactionIDExtra = intent.getStringExtra(TRANSACTION_ID_EXTRA).toString()
        val nameExtra = intent.getStringExtra(NAME_EXTRA).toString()
        val dateExtra = intent.getLongExtra(DATE_EXTRA, 0)
        val amountExtra = intent.getDoubleExtra(AMOUNT_EXTRA, 0.0)
        val dateInvertedExtra = intent.getLongExtra(DATE_INVERTED_EXTRA, 0)
        val whatsAppExtra = intent.getStringExtra(WHATS_APP_EXTRA).toString()

        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.btnEdit.setOnClickListener {
            openEditDialog(nameExtra, transactionIDExtra)
        }

        binding.edNominalPayment.apply {
            setMaskingMoney("")
        }

        logicBusinessSetUp()

        setValuesToViews()

        binding.btnDelete.setOnClickListener {
            deleteData(transactionIDExtra)
        }

        binding.btnSave.setOnClickListener {
            saveTransactionData(
                transactionIDExtra,
                nameExtra,
                whatsAppExtra,
                amountExtra,
                dateExtra,
                status,
                dateInvertedExtra,
                amountLeft,
                amountOver,
                amountCurrently
            )
        }
    }

    private fun deleteData(transactionID: String) {
        val alertBox = AlertDialog.Builder(this)
        alertBox.setTitle(getString(R.string.confirmation))
        alertBox.setMessage(getString(R.string.confirmation_info))
        alertBox.setPositiveButton(getString(R.string.yes_answer)) { _: DialogInterface, _: Int ->
            deleteRecord(
                transactionID
            )
        }
        alertBox.setNegativeButton(getString(R.string.no_answer)) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        val alertDialog = alertBox.create()
        alertDialog.show()
    }

    private fun deleteRecord(transactionID: String) {
        val lastCharOfTransactionID = transactionID.takeLast(1).toInt()
        val dropLastOfTransactionID = transactionID.dropLast(1)

        val user = Firebase.auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            for (i in lastCharOfTransactionID..12) {
                if (i > 9) {
                    val dbRef = FirebaseDatabase.getInstance().getReference(uid).child(dropLastOfTransactionID + 9 + i.toString())
                    dbRef.removeValue()
                } else {
                    val dbRef = FirebaseDatabase.getInstance().getReference(uid).child(dropLastOfTransactionID + i.toString())
                    dbRef.removeValue()
                }

            }
        }
        finish()
    }

    private fun logicBusinessSetUp() {
        binding.edNominalPayment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {
                binding.tvAmountCurrently.text = resources.getString(R.string.rupiah_s_edit_text, s) // Rp s
                Log.d(TAG, "onTextChanged: value s = $s")
                val shouldPayAmount = intent.getDoubleExtra(AMOUNT_EXTRA, 0.0).toString()

                charSequenceEmptyChecker(s, shouldPayAmount)

                val localeID = Locale("in", "ID")
                val formatRupiah: NumberFormat = NumberFormat.getCurrencyInstance(localeID)

                val limitLength = formatRupiah.format(shouldPayAmount.toDouble()).replace(",00", "").replace("[Rp, ]".toRegex(), "").length
                binding.edNominalPayment.limitLength(limitLength)

                if (amountLeft <= 0.0) {
                    status = true
                    binding.tvAmountLeft.text = getString(R.string.payed_off)
                    binding.tvAmountLeft.setTextColor(ContextCompat.getColor(applicationContext, R.color.green))
                    showDeleteText()

                    amountOver = (amountLeft * -1)

                    if (amountOver > 0.0) {
                        val amountOverRupiahFormat = formatRupiah.format(amountOver).replace(",00", "")
                        binding.tvAmountLeft.text = resources.getString(R.string.payed_over, amountOverRupiahFormat)

                        Snackbar.make(binding.snackbarLayout, resources.getString(R.string.info_amount_over, amountOverRupiahFormat), Snackbar.LENGTH_LONG)
                            .setDuration(7000)
                            .setAnchorView(binding.btnSave)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setAction("OK") {
                                hideSoftKeyboard()
                            }
                            .show()
                    }
                } else {
                    status = false
                    binding.tvAmountLeft.text = formatRupiah.format(amountLeft).replace(",00", "")
                    binding.tvAmountLeft.setTextColor(ContextCompat.getColor(applicationContext, R.color.black))
                }
                when {
                    s.length > 1 -> {
                        showDeleteText()
                        isEnableBtnSave(true)
                    }
                    s.isEmpty() -> {
                        binding.deleteText.visibility = View.GONE
                        isEnableBtnSave(false)
                    }
                    s == "0" -> {
                        binding.deleteText.visibility = View.GONE
                        isEnableBtnSave(false)
                    }
                }
                Log.d(TAG, "onTextChanged: length of shouldPay = ${formatRupiah.format(shouldPayAmount.toDouble()).replace(",00", "").replace("[Rp, ]".toRegex(), "").length} of ${formatRupiah.format(shouldPayAmount.toDouble()).replace(",00", "").replace("[Rp, ]".toRegex(), "")}")
                Log.d(TAG, "onTextChanged: amountLeft: $amountLeft")
                Log.d(TAG, "onTextChanged: amountOver: $amountOver")
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    private fun charSequenceEmptyChecker(s: CharSequence, shouldPayAmount: String) {
        amountCurrently = if (s.isEmpty()) {
            0.0
        } else {
            s.toString().replace("[Rp,. ]".toRegex(), "").toDouble()
        }
        Log.d(TAG, "onTextChanged: amountCurrently: $amountCurrently")

        amountLeft = if (s.isEmpty()) {
            shouldPayAmount.toDouble() - amountLeft
        } else {
            shouldPayAmount.toDouble() - s.toString().replace("[Rp,. ]".toRegex(), "").toDouble()
        }
    }

    private fun hideSoftKeyboard() {
        currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun saveTransactionData(
        transactionID: String,
        name: String,
        whatsApp: String,
        amount: Double,
        date: Long,
        status: Boolean,
        dateInverted: Long,
        amountLeft: Double,
        amountOver: Double,
        amountPayed: Double
    ) {
        showLoading(true)
        val user = Firebase.auth.currentUser
        val uid = user?.uid
        val calendar: Calendar = Calendar.getInstance()
        val dateFromLong = Date(date)

        calendar.time = dateFromLong
        calendar.add(Calendar.MONTH, 1)
        val nextMonth: Date = calendar.time

        val invertedDateAddOneMonth = nextMonth.time * -1

        val encryptedWhatsApp = encryptAES(whatsApp, SECRET_KEY)

        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference(uid)
            val transaction = TransactionModel(transactionID, name, encryptedWhatsApp, amount, date, status, dateInverted, amountLeft, amountOver, amountPayed)

            checkIsPayedOffForAddNextMonthTransaction(
                amountLeft,
                dbRef,
                name,
                encryptedWhatsApp,
                amount,
                nextMonth,
                invertedDateAddOneMonth
            )

            dbRef.child(transactionID).setValue(transaction)
                .addOnCompleteListener {
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.data_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { err ->
                    showLoading(false)
                    Toast.makeText(this, "Error ${err.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkIsPayedOffForAddNextMonthTransaction(
        amountLeft: Double,
        dbRef: DatabaseReference,
        name: String,
        encryptedWhatsApp: String,
        amount: Double,
        nextMonth: Date,
        invertedDateAddOneMonth: Long
    ) {
        val status = intent.getBooleanExtra(STATUS_EXTRA, false)

        if (amountLeft <= 0.0 && !status) {
            val newTransactionID = dbRef.push().key!! + "0"
            val transactionAddOneMonth = TransactionModel(
                newTransactionID,
                name,
                encryptedWhatsApp,
                amount,
                nextMonth.time,
                false,
                invertedDateAddOneMonth,
                amountLeft,
                0.0,
                0.0
            )
            dbRef.child(newTransactionID).setValue(transactionAddOneMonth)
        }
    }

    private fun setValuesToViews() {
        binding.tvName.text = intent.getStringExtra(NAME_EXTRA)

        val date: Long = intent.getLongExtra(DATE_EXTRA, 0)
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val result = Date(date)
        binding.tvDate.text = simpleDateFormat.format(result)

        val localeID = Locale("in", "ID")
        val formatRupiah: NumberFormat = NumberFormat.getCurrencyInstance(localeID)
        val shouldPay = intent.getDoubleExtra(AMOUNT_EXTRA, 0.0)
        val amountPayed = intent.getDoubleExtra(AMOUNT_PAYED, 0.0)

        binding.tvAmountShouldPay.text = formatRupiah.format(shouldPay).replace(",00", "")
        binding.tvAmountLeft.text = formatRupiah.format(amountLeft).replace(",00", "")

        val showAmountCurrently = amountPayed.toInt()
        binding.edNominalPayment.setText(showAmountCurrently.toString())
    }

    private fun openEditDialog(name: String, transactionID: String) {
        val mDialog = AlertDialog.Builder(this)
        val bind: EditDialogBinding = EditDialogBinding.inflate(layoutInflater)
        mDialog.setView(bind.root)

        bind.edNameDialog.setText(intent.getStringExtra(NAME_EXTRA).toString())
        bind.edWhatsAppDialog.setMaskingPhoneNumber("")
        bind.edWhatsAppDialog.setText(intent.getStringExtra(WHATS_APP_EXTRA).toString().replace(".", ""))

        setRequiredDataDialog(bind, name)

        bind.edDate.setOnClickListener {
            clickDatePicker(bind)
        }

        val alertDialog = mDialog.create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        bind.ibContact.setOnClickListener {
            val intentPickContact = Intent(Intent.ACTION_PICK)
            intentPickContact.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            startActivityForResult(intentPickContact, CONTACT_PICK_CODE)
            alertDialog.dismiss()
        }

        bind.btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        if (date <= 0) {
            date = intent.getLongExtra(DATE_EXTRA, 0)
            invertedDate = date * -1
        }

        bind.btnUpdate.setOnClickListener {
            updateTransactionData(
                transactionID,
                bind.edNameDialog.text.toString().trim(),
                bind.edWhatsAppDialog.text.toString().replace("-", "").trim(),
                bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble(),
                date,
                status = amountCurrently >= bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble(),
                invertedDate,
                amountLeft = bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble() - amountCurrently,
                amountOver,
                amountCurrently
            )
            alertDialog.dismiss()
            Toast.makeText(applicationContext, getString(R.string.transaction_data_updated), Toast.LENGTH_LONG).show()
            val intentRefresh = Intent(this, TransactionDetailsActivity::class.java)

            intentRefresh.putExtra(TRANSACTION_ID_EXTRA, transactionID)
            intentRefresh.putExtra(NAME_EXTRA, bind.edNameDialog.text.toString().trim())
            intentRefresh.putExtra(DATE_EXTRA, date)
            intentRefresh.putExtra(DATE_INVERTED_EXTRA, invertedDate)
            intentRefresh.putExtra(AMOUNT_EXTRA, bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble())
            intentRefresh.putExtra(STATUS_EXTRA, amountCurrently >= bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble())
            intentRefresh.putExtra(WHATS_APP_EXTRA, bind.edWhatsAppDialog.text.toString().replace("-", "").trim())
            intentRefresh.putExtra(AMOUNT_LEFT_EXTRA, bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble() - amountCurrently)
            intentRefresh.putExtra(AMOUNT_OVER_EXTRA, amountOver)
            intentRefresh.putExtra(AMOUNT_PAYED, amountCurrently)

            startActivity(intentRefresh)
            this.finish()
        }
    }

    private fun setRequiredDataDialog(bind: EditDialogBinding, name: String) {
        val localeID = Locale("in", "ID")
        val formatRupiah: NumberFormat = NumberFormat.getCurrencyInstance(localeID)
        bind.edAmount.setText(formatRupiah.format(intent.getDoubleExtra(AMOUNT_EXTRA, 0.0)).replace(",00", ""))
        bind.edAmount.setMaskingMoney("Rp")


        val date: Long = intent.getLongExtra(DATE_EXTRA, 0)
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val result = Date(date)
        bind.edDate.setText(simpleDateFormat.format(result))

        bind.tvTitle.text = resources.getString(R.string.edit_title, name)

        bind.edDate.setOnClickListener {
            clickDatePicker(bind)
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

                val mDialog = AlertDialog.Builder(this)
                val bind: EditDialogBinding = EditDialogBinding.inflate(layoutInflater)
                mDialog.setView(bind.root)

                val transactionIDExtra = intent.getStringExtra(TRANSACTION_ID_EXTRA).toString()

                bind.edNameDialog.setText(name)
                bind.edWhatsAppDialog.setMaskingPhoneNumber("")
                bind.edWhatsAppDialog.setText(number)

                setRequiredDataDialog(bind, intent.getStringExtra(NAME_EXTRA).toString())

                val alertDialog = mDialog.create()
                alertDialog.setCanceledOnTouchOutside(false)
                alertDialog.show()

                bind.ibContact.setOnClickListener {
                    val intentPickContact = Intent(Intent.ACTION_PICK)
                    intentPickContact.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                    startActivityForResult(intentPickContact, CONTACT_PICK_CODE)
                    alertDialog.dismiss()
                }

                bind.btnClose.setOnClickListener {
                    alertDialog.dismiss()
                }

                if (date <= 0) {
                    date = intent.getLongExtra(DATE_EXTRA, 0)
                    invertedDate = date * -1
                }

                bind.btnUpdate.setOnClickListener {
                    updateTransactionData(
                        transactionIDExtra,
                        bind.edNameDialog.text.toString().trim(),
                        bind.edWhatsAppDialog.text.toString().replace("-", "").trim(),
                        bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble(),
                        date,
                        status = amountCurrently >= bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble(),
                        invertedDate,
                        amountLeft = bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble() - amountCurrently,
                        amountOver,
                        amountCurrently
                    )
                    alertDialog.dismiss()
                    Toast.makeText(applicationContext, getString(R.string.transaction_data_updated), Toast.LENGTH_LONG).show()
                    val intentRefresh = Intent(this, TransactionDetailsActivity::class.java)

                    intentRefresh.putExtra(TRANSACTION_ID_EXTRA, transactionIDExtra)
                    intentRefresh.putExtra(NAME_EXTRA, bind.edNameDialog.text.toString().trim())
                    intentRefresh.putExtra(DATE_EXTRA, date)
                    intentRefresh.putExtra(DATE_INVERTED_EXTRA, invertedDate)
                    intentRefresh.putExtra(AMOUNT_EXTRA, bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble())
                    intentRefresh.putExtra(STATUS_EXTRA, amountCurrently >= bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble())
                    intentRefresh.putExtra(WHATS_APP_EXTRA, bind.edWhatsAppDialog.text.toString().replace("-", "").trim())
                    intentRefresh.putExtra(AMOUNT_LEFT_EXTRA, bind.edAmount.text.toString().replace("[Rp,. ]".toRegex(), "").toDouble() - amountCurrently)
                    intentRefresh.putExtra(AMOUNT_OVER_EXTRA, amountOver)
                    intentRefresh.putExtra(AMOUNT_PAYED, amountCurrently)

                    startActivity(intentRefresh)
                    this.finish()
                }
            }
            cursor?.close()
        }
    }

    private fun updateTransactionData(
        transactionID: String,
        name: String,
        whatsApp: String,
        payment: Double,
        date: Long,
        status: Boolean,
        invertedDate: Long,
        amountLeft: Double,
        amountOver: Double,
        amountCurrently: Double
    ) {
        val user = Firebase.auth.currentUser
        val uid = user?.uid

        val encryptedWhatsApp = encryptAES(whatsApp, SECRET_KEY)

        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference(uid)
            val transactionInfo = TransactionModel(transactionID, name, encryptedWhatsApp, payment, date, status, invertedDate, amountLeft, amountOver, amountCurrently)
            dbRef.child(transactionID).setValue(transactionInfo)
        }
    }

    private fun deleteText() {
        binding.edNominalPayment.setText("")
    }

    private fun showDeleteText() {
        binding.deleteText.visibility = View.VISIBLE
        binding.deleteText.setOnClickListener {
            deleteText()
            binding.deleteText.visibility = View.GONE
        }
    }

    private fun isEnableBtnSave(isActive: Boolean) {
        when (isActive) {
            true -> {
                binding.btnSave.backgroundTintList = getColorStateList(R.color.green)
                binding.btnSave.isEnabled = isActive
            }
            false -> {
                binding.btnSave.backgroundTintList = getColorStateList(R.color.gray)
                binding.btnSave.isEnabled = isActive
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        when (isLoading) {
            true -> binding.progressBar.visibility = View.VISIBLE
            false -> binding.progressBar.visibility = View.GONE
        }
    }

    private fun clickDatePicker(bind: EditDialogBinding) {
        val myCalendar = Calendar.getInstance()
        val year = myCalendar.get(Calendar.YEAR)
        val month = myCalendar.get(Calendar.MONTH)
        val day = myCalendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->

                val selectedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
                bind.edDate.text = null

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val showDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                val theDate = sdf.parse(selectedDate)
                date = theDate!!.time //convert date to millisecond
                bind.edDate.setText(showDate.format(date))
                invertedDate = date * -1

            },
            year,
            month,
            day
        )
        dpd.show()
    }
}