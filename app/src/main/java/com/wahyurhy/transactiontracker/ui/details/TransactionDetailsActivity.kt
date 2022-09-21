package com.wahyurhy.transactiontracker.ui.details

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
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
        val statusExtra = intent.getBooleanExtra(STATUS_EXTRA, false)
        val dateInvertedExtra = intent.getLongExtra(DATE_INVERTED_EXTRA, 0)
        val whatsAppExtra = intent.getStringExtra(WHATS_APP_EXTRA).toString()

        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.btnEdit.setOnClickListener {
            openEditDialog(intent.getStringExtra(NAME_EXTRA).toString())
        }

        binding.edNominalPayment.apply {
            setMaskingMoney("")
        }

        binding.edNominalPayment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {
                binding.tvAmountCurrently.text = "Rp$s"
                Log.d(TAG, "onTextChanged: value s = $s")
                val shouldPayAmount = intent.getDoubleExtra(AMOUNT_EXTRA, 0.0).toString()

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

                val localeID = Locale("in", "ID")
                val formatRupiah: NumberFormat = NumberFormat.getCurrencyInstance(localeID)

                val limitLength = formatRupiah.format(shouldPayAmount.toDouble()).replace(",00", "").replace("[Rp, ]".toRegex(), "").length
                binding.edNominalPayment.limitLength(limitLength)

                if (amountLeft <= 0.0) {
                    status = true
                    binding.tvAmountLeft.text = "Sudah Lunas!"
                    binding.tvAmountLeft.setTextColor(resources.getColor(R.color.green))
                    showDeleteText()

                    amountOver = (amountLeft * -1)

                    if (amountOver > 0.0) {
                        val amountOverRupiahFormat = formatRupiah.format(amountOver).replace(",00", "")
                        binding.tvAmountLeft.text = "Sudah Lunas dan\nlebih $amountOverRupiahFormat"
                        Snackbar.make(binding.constraintLayout, "Uang berlebih sebesar $amountOverRupiahFormat akan ditabung pada tagihan selanjutnya", Snackbar.LENGTH_INDEFINITE)
                            .setAction("OK") {
                                currentFocus?.let { view ->
                                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                }
                            }
                            .show()
                    }
                } else {
                    status = false
                    binding.tvAmountLeft.text = formatRupiah.format(amountLeft).replace(",00", "")
                    binding.tvAmountLeft.setTextColor(resources.getColor(R.color.black))
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

        setValuesToViews()

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
        val user = Firebase.auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference(uid)
            val transactionInfo = TransactionModel(transactionID, name, whatsApp, amount, date, status, dateInverted, amountLeft, amountOver, amountPayed)
            dbRef.child(transactionID).setValue(transactionInfo)
                .addOnCompleteListener {
                    Toast.makeText(this, "Data Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { err ->
                    Toast.makeText(this, "Error ${err.message}", Toast.LENGTH_SHORT).show()
                }
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
        val amountLeftExtra = intent.getDoubleExtra(AMOUNT_LEFT_EXTRA, 0.0)
        val amountOverExtra = intent.getDoubleExtra(AMOUNT_OVER_EXTRA, 0.0)
        val amountPayed = intent.getDoubleExtra(AMOUNT_PAYED, 0.0)
        binding.tvAmountShouldPay.text = formatRupiah.format(shouldPay).replace(",00", "")
        binding.tvAmountLeft.text = formatRupiah.format(amountLeft).replace(",00", "")

        val showAmountCurrently = amountPayed.toInt()
        binding.edNominalPayment.setText(showAmountCurrently.toString())
    }

    private fun openEditDialog(name: String) {
        val mDialog = AlertDialog.Builder(this)
        val bind: EditDialogBinding = EditDialogBinding.inflate(layoutInflater)
        mDialog.setView(bind.root)

        bind.edName.setText(intent.getStringExtra(NAME_EXTRA).toString())

        val localeID = Locale("in", "ID")
        val formatRupiah: NumberFormat = NumberFormat.getCurrencyInstance(localeID)
        bind.edAmount.setText(formatRupiah.format(intent.getDoubleExtra(AMOUNT_EXTRA, 0.0)).replace(",00", ""))

        bind.edWhatsApp.setText(intent.getStringExtra(WHATS_APP_EXTRA).toString().replace(".", ""))

        val date: Long = intent.getLongExtra(DATE_EXTRA, 0)
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val result = Date(date)
        bind.edDate.setText(simpleDateFormat.format(result))

        bind.tvTitle.text = resources.getString(R.string.edit_data_title, intent.getStringExtra(NAME_EXTRA))

        val alertDialog = mDialog.create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        bind.btnClose.setOnClickListener {
            alertDialog.dismiss()
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
}