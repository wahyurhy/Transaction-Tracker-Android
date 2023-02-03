package com.wahyurhy.transactiontracker.utils

import java.util.concurrent.Executor
import java.util.concurrent.Executors

val SELECT_PHONE_NUMBER = 2
val CONTACT_PICK_CODE = 2

val TRANSACTION_ID_EXTRA = "transactionID"
val TRANSACTION_ID_WORKER = "transactionIDWorker"
val NOTIFICATION_CHANNEL_ID = "notify-due_date"
val NOTIFICATION_ID = 32
val NOTIFICATION_CHANNEL_NAME = "Due Date Notify"
val NAME_EXTRA = "name"
val NAME_WORKER_EXTRA = "name"
val DATE_EXTRA = "date"
val DATE_INVERTED_EXTRA = "dateInverted"
val STATUS_EXTRA = "status"
val AMOUNT_EXTRA = "amount"
val WHATS_APP_EXTRA = "whatsApp"
val AMOUNT_LEFT_EXTRA = "amountLeft"
val AMOUNT_OVER_EXTRA = "amountOver"
val AMOUNT_PAYED = "amountPayed"
val CHANNEL_ID = "channel_01"
val CHANNEL_NAME = "wahyu channel"
val ID_REPEATING = 101
val SECRET_KEY = "PGKJHEDNMLSGACBAPGKJHEDNMLSFECBA"

val SINGLE_EXECUTOR = Executors.newSingleThreadExecutor()

fun executeThread(f: () -> Unit) {
    SINGLE_EXECUTOR.execute(f)
}