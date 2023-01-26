package com.wahyurhy.transactiontracker.notification

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.ui.fragments.TransactionFragment
import com.wahyurhy.transactiontracker.utils.*
import java.util.*

class MonthlyCreateTransaction(
    private val transactionID: String,
    private val name: String,
    private val whatsApp: String,
    private val paymentAmount: Double,
    private val date: Long
) : BroadcastReceiver() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var transactionIDMonthly: StringBuilder
    private lateinit var dateFromLong: Date
    private lateinit var calendar: Calendar
    private lateinit var nextMonth: Date
    private lateinit var transaction: TransactionModel

    override fun onReceive(context: Context, intent: Intent) {
        executeThread {
            showNotification(context)
        }
    }

    fun setMonthlyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MonthlyCreateTransaction::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, ID_REPEATING, intent, PendingIntent.FLAG_IMMUTABLE)

        // set to notify next month
        val calendar = Calendar.getInstance()
        var currentMonth = calendar.get(Calendar.MONTH)

        currentMonth++

        if (currentMonth > Calendar.DECEMBER) {
            currentMonth = Calendar.JANUARY
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1)
        }

        calendar.set(Calendar.MONTH, currentMonth)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendar.set(Calendar.DAY_OF_MONTH, maxDay)

        val thenTime = calendar.timeInMillis

        val user = Firebase.auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference(uid)
        }

        createMultiTransaction()

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            thenTime,
            AlarmManager.INTERVAL_DAY + 365,
            pendingIntent
        )

//        Toast.makeText(context, "Monthly Notification ON", Toast.LENGTH_SHORT).show()
    }

    private fun createMultiTransaction() {
        transactionIDMonthly = StringBuilder()
        dateFromLong = Date(date)
        calendar = Calendar.getInstance()
        var invertedDate: Long
        for (i in 1..12) {

            transactionIDMonthly.setLength(0)
            transactionIDMonthly.append(this.transactionID)
            transactionIDMonthly.append(i)

            if (i >= 10) {
                transactionIDMonthly.setLength(0)
                transactionIDMonthly.append(this.transactionID)
                transactionIDMonthly.append(9)
                transactionIDMonthly.append(i)
            }

            calendar.time = dateFromLong
            calendar.add(Calendar.MONTH, i)
            nextMonth = calendar.time

            invertedDate = nextMonth.time * -1

            transaction = TransactionModel(
                transactionIDMonthly.toString(),
                name,
                whatsApp,
                paymentAmount,
                nextMonth.time,
                false,
                invertedDate,
                paymentAmount,
                0.0,
                0.0
            )

            dbRef.child(transactionIDMonthly.toString()).setValue(transaction)
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MonthlyCreateTransaction::class.java)
        val requestCode = ID_REPEATING
        val pendingIntent =
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
        pendingIntent.cancel()
        alarmManager.cancel(pendingIntent)
        Toast.makeText(context, "Monthly Notification OFF", Toast.LENGTH_SHORT).show()
    }

    private fun showNotification(context: Context) {
        val pendingIntent = getPendingIntent(context)
        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationStyle = NotificationCompat.InboxStyle()
        val contentString = context.resources.getString(R.string.due_date_notification_content)
        val nameClient = String.format(contentString, name)

        val mBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(context.getString(R.string.notif_today_due_date_title))
            .setContentText(nameClient)
            .setStyle(notificationStyle)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            channel.description = NOTIFICATION_CHANNEL_NAME
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)

            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)
            mNotificationManager.createNotificationChannel(channel)
        }
        val notification = mBuilder.build()
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent(context: Context): PendingIntent? {
        val intent = Intent(context, TransactionFragment::class.java)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}