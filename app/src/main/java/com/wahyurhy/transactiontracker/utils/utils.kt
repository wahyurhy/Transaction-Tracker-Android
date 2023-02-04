package com.wahyurhy.transactiontracker.utils

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

val now: Long = System.currentTimeMillis()
val sdf: SimpleDateFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)
val currentYear: Date = Date(now)

enum class Bulan(val value: Int) {
    January(0),
    February(1),
    March(2),
    April(3),
    May(4),
    June(5),
    July(6),
    August(7),
    September(8),
    October(9),
    November(10),
    December(11);
}

fun EditText.setMaskingMoney(currencyText: String) {
    this.addTextChangedListener(object : MyTextWatcher {
        val editTextWeakReference: WeakReference<EditText> = WeakReference<EditText>(this@setMaskingMoney)
        override fun afterTextChanged(editable: Editable?) {
            val editText = editTextWeakReference.get() ?: return
            val s = editable.toString()
            editText.removeTextChangedListener(this)
            val cleanString = s.replace("[Rp,. ]".toRegex(), "")
            val newval = currencyText + cleanString.monetize()

            editText.setText(newval)
            editText.setSelection(newval.length)
            editText.addTextChangedListener(this)
        }
    })
}

interface MyTextWatcher: TextWatcher {
    override fun onTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {}
    override fun beforeTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {}
}

fun String.monetize(): String = if (this.isEmpty()) "0" else DecimalFormat("#,###").format(this.replace("[^\\d]".toRegex(),"").toLong()).replace(",", ".")

fun EditText.setMaskingPhoneNumber(currencyText: String) {
    this.addTextChangedListener(object : MyTextWatcher {
        val editTExtWeakReference: WeakReference<EditText> = WeakReference<EditText>(this@setMaskingPhoneNumber)
        override fun afterTextChanged(editable: Editable?) {
            val editText = editTExtWeakReference.get() ?: return
            val s = editable.toString()
            editText.removeTextChangedListener(this)
            val cleanString = s.replace("[,. ]".toRegex(), "")
            val newval = "0" + currencyText + cleanString.phoneNumberFormat()

            editText.setText(newval)
            editText.setSelection(newval.length)
            editText.addTextChangedListener(this)
        }
    })
}

fun String.phoneNumberFormat(): String = if (this.isEmpty()) "" else DecimalFormat("#,####").format(this.replace("[^\\d]".toRegex(),"").toLong()).replace("[,. ]".toRegex(), "-")

fun EditText.limitLength(maxLength: Int) {
    filters = arrayOf(InputFilter.LengthFilter(maxLength))
}
