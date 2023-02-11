package com.wahyurhy.transactiontracker.adapter

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.notification.MonthlyNotification
import com.wahyurhy.transactiontracker.utils.SECRET_KEY
import com.wahyurhy.transactiontracker.utils.decryptAES
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : ListAdapter<TransactionModel, TransactionAdapter.ViewHolder>(DiffCallback) {

    private lateinit var mListener: onItemClickListener

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(clickListener: onItemClickListener) {
        mListener = clickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_list_item, parent, false)
        return ViewHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentTransaction = getItem(position)
        holder.apply {

            idTransaction = currentTransaction.id as String
            name = currentTransaction.name as String
            dueDateTransaction = currentTransaction.dueDateTransaction as Long
            invertedDate = currentTransaction.invertedDate as Long
            paymentAmount = currentTransaction.paymentAmount
            whatsAppNumber = currentTransaction.whatsAppNumber as String
            stateTransaction = currentTransaction.stateTransaction as Boolean

            tvName.text = currentTransaction.name

            try {
                val decryptedWhatsApp = decryptAES(currentTransaction.whatsAppNumber.toString(), SECRET_KEY)
                currentTransaction.whatsAppNumber = decryptedWhatsApp
            } catch (e: Exception) {
                Log.e("TransactionAdapter", "onBindViewHolder: ${e.message}")
            }

            val localeID = Locale("in", "ID")
            val formatRupiah: NumberFormat = NumberFormat.getCurrencyInstance(localeID)
            tvTransactionAmount.text =
                formatRupiah.format(currentTransaction.paymentAmount).replace(",00", "")

            val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val result = Date(currentTransaction.dueDateTransaction!!)

            tvDate.text = simpleDateFormat.format(result)

            when {
                currentTransaction.stateTransaction == true -> {
                    tvState.text = itemView.context.resources.getString(R.string.paid_off)
                    tvState.setBackgroundResource(R.drawable.bg_state_true)
                    if (currentTransaction.amountOver != 0.0) {
                        tvState.text = itemView.context.resources.getString(
                            R.string.over_paid,
                            formatRupiah.format(currentTransaction.amountOver).replace(",00", "")
                        )
                    }
                }
                currentTransaction.stateTransaction == false -> {
                    tvState.text = itemView.context.resources.getString(R.string.paid_yet)
                    tvState.setBackgroundResource(R.drawable.bg_state_false)


                    val currentAmountLeft = currentTransaction.amountLeft as Double
                    val currentPaymentAmount = currentTransaction.paymentAmount as Double

                    if (currentAmountLeft > 0.0) {
                        if (currentAmountLeft < currentPaymentAmount) {
                            tvState.text = itemView.context.resources.getString(
                                R.string.still_left,
                                formatRupiah.format(currentAmountLeft).replace(",00", "")
                            )
                            tvState.setBackgroundResource(R.drawable.bg_state_unfinished)
                        }
                    }
                }
            }
        }
    }

    class ViewHolder(itemView: View, clickListener: onItemClickListener) :
        RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvTransactionAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvState: TextView = itemView.findViewById(R.id.tvState)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        var idTransaction = ""
        var name = ""
        var dueDateTransaction = 0L
        var invertedDate = 0L
        var paymentAmount: Double? = 0.0
        var stateTransaction = false
        var whatsAppNumber = ""


        val typeIcon: ImageView = itemView.findViewById(R.id.typeIcon)

        init {
            itemView.setOnClickListener {
                clickListener.onItemClick(adapterPosition)
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showDialog(idTransaction, name, dueDateTransaction, invertedDate, paymentAmount, whatsAppNumber, stateTransaction)
                }
                true
            }
        }

        private fun showDialog(
            idTransaction: String,
            name: String,
            dueDateTransaction: Long,
            invertedDate: Long,
            paymentAmount: Double?,
            whatsAppNumber: String,
            stateTransaction: Boolean
        ) {
            val builder = AlertDialog.Builder(itemView.context)
            builder.setTitle(itemView.resources.getString(R.string.choose_action_info))
            builder.setPositiveButton(itemView.resources.getString(R.string.mark_as_paid_off)) { _, _ ->
                markAsDone(idTransaction, name, dueDateTransaction, invertedDate, paymentAmount, whatsAppNumber, stateTransaction)
            }
            builder.setNegativeButton("Cancel") { _, _ -> }
            builder.show()
        }

        private fun markAsDone(
            idTransaction: String,
            name: String,
            dueDateTransaction: Long,
            invertedDate: Long,
            paymentAmount: Double?,
            whatsAppNumber: String,
            stateTransaction: Boolean
        ) {
            progressBar.visibility = View.VISIBLE
            val user = Firebase.auth.currentUser
            val uid = user?.uid

            val broadcastReceiver = MonthlyNotification()

            val calendar: Calendar = Calendar.getInstance()
            val dateFromLong = Date(dueDateTransaction)
            calendar.time = dateFromLong
            calendar.add(Calendar.MONTH, 1)
            val nextMonth: Date = calendar.time

            val invertedDateAddOneMonth = nextMonth.time * -1

            if (uid != null) {
                val dbRef = FirebaseDatabase.getInstance().getReference(uid)
                val transactionInfo = TransactionModel(idTransaction, name, whatsAppNumber, paymentAmount, dueDateTransaction, true, invertedDate, 0.0, 0.0, paymentAmount)

                createAddNextMonthTransaction(
                    dbRef,
                    name,
                    whatsAppNumber,
                    paymentAmount,
                    nextMonth,
                    invertedDateAddOneMonth,
                    stateTransaction,
                    broadcastReceiver
                )

                dbRef.child(idTransaction).setValue(transactionInfo)
                    .addOnCompleteListener {
                        progressBar.visibility = View.GONE
                        broadcastReceiver.setMonthlyNotification(itemView.context, name, paymentAmount!!.toDouble(), calendar.time.time)
                        Toast.makeText(itemView.context, itemView.resources.getString(R.string.success_to_mark), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { err ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(itemView.context, "Error ${err.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun createAddNextMonthTransaction(
            dbRef: DatabaseReference,
            name: String,
            whatsAppNumber: String,
            paymentAmount: Double?,
            nextMonth: Date,
            invertedDateAddOneMonth: Long,
            stateTransaction: Boolean,
            broadcastReceiver: MonthlyNotification
        ) {

            if (!stateTransaction) {
                val newTransactionID = dbRef.push().key!! + "0"
                val transactionAddOneMonth = TransactionModel(
                    newTransactionID,
                    name,
                    whatsAppNumber,
                    paymentAmount,
                    nextMonth.time,
                    false,
                    invertedDateAddOneMonth,
                    paymentAmount,
                    0.0,
                    0.0
                )
                broadcastReceiver.setMonthlyNotification(itemView.context, name, paymentAmount!!.toDouble(), nextMonth.time)
                dbRef.child(newTransactionID).setValue(transactionAddOneMonth)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TransactionModel>() {
            override fun areItemsTheSame(
                oldItem: TransactionModel,
                newItem: TransactionModel
            ): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: TransactionModel,
                newItem: TransactionModel
            ): Boolean =
                oldItem == newItem

        }
    }
}