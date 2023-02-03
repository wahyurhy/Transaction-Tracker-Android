package com.wahyurhy.transactiontracker.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.recyclerview.widget.ListAdapter
import com.wahyurhy.transactiontracker.utils.SECRET_KEY
import com.wahyurhy.transactiontracker.utils.decryptAES
import com.wahyurhy.transactiontracker.utils.encryptAES

class TransactionAdapter(private val transactionList: ArrayList<TransactionModel>) :
    ListAdapter<TransactionModel, TransactionAdapter.ViewHolder>(DiffCallback()) {

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
        val currentTransaction = transactionList[position]
        holder.apply {

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
                        tvTransactionAmount.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.green
                            )
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
                            tvTransactionAmount.setTextColor(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.red
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = transactionList.size

    class ViewHolder(itemView: View, clickListener: onItemClickListener) :
        RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvTransactionAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvState: TextView = itemView.findViewById(R.id.tvState)
        val typeIcon: ImageView = itemView.findViewById(R.id.typeIcon)

        init {
            itemView.setOnClickListener {
                clickListener.onItemClick(adapterPosition)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
        override fun areItemsTheSame(
            oldItem: TransactionModel,
            newItem: TransactionModel
        ): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(
            oldItem: TransactionModel,
            newItem: TransactionModel
        ): Boolean =
            oldItem == newItem

    }
}