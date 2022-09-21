package com.wahyurhy.transactiontracker.data.source.local.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction")
data class TransactionModel(
    @PrimaryKey(autoGenerate = false)
    @NonNull
    @ColumnInfo(name = "id")
    var id: String? = null,

    @ColumnInfo(name = "name")
    var name: String? = null,

    @ColumnInfo(name = "whatsAppNumber")
    var whatsAppNumber: String? = null,

    @ColumnInfo(name = "paymentAmount")
    var paymentAmount: Double? = null,

    @ColumnInfo(name = "dueDateTransaction")
    var dueDateTransaction: Long? = null,

    @ColumnInfo(name = "stateTransaction")
    var stateTransaction: Boolean? = null,

    @ColumnInfo(name = "invertedDate")
    var invertedDate: Long? = null,

    @ColumnInfo(name = "amountLeft")
    var amountLeft: Double? = null,

    @ColumnInfo(name = "amountOver")
    var amountOver: Double? = null,

    @ColumnInfo(name = "amountPayed")
    var amountPayed: Double? = null
)