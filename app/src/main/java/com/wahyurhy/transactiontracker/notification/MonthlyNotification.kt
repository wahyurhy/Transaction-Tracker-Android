package com.wahyurhy.transactiontracker.notification

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.ui.main.MainActivity
import com.wahyurhy.transactiontracker.utils.*
import java.text.NumberFormat
import java.util.*

class MonthlyNotification : BroadcastReceiver() {

    private var formatRupiah: NumberFormat? = null

    override fun onReceive(context: Context, intent: Intent) {
        executeThread {
            val title = intent.getStringExtra("title") as String
            val message = intent.getStringExtra("message") as String
            showNotification(context, title, message)
        }
    }

    fun setMonthlyNotification(context: Context, name: String, amount: Double, dueDate: Long) {
        formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        val intent = Intent(context, MonthlyNotification::class.java)
        intent.putExtra("title", "Jatuh tempo untuk $name")
        intent.putExtra(
            "message",
            "Jumlah total pembayaran sebesar ${formatRupiah?.format(amount)?.replace(",00", "")}"
        )

        val pendingIntent =
            PendingIntent.getBroadcast(context, ID_REPEATING, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val interval = 30L * 24 * 60 * 60 * 1000
        val targetHour = 8
        val targetMinute = 0
        val targetSecond = 0
        val targetTimeInMilliseconds = targetHour * 60 * 60 * 1000 + targetMinute * 60 * 1000 + targetSecond * 1000
        val nextNotificationTime = dueDate + interval - (dueDate + interval) % (24 * 60 * 60 * 1000) + targetTimeInMilliseconds

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            nextNotificationTime,
            pendingIntent
        )

//        Toast.makeText(context, "Monthly Notification ON", Toast.LENGTH_SHORT).show()
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MonthlyNotification::class.java)
        val requestCode = ID_REPEATING
        val pendingIntent =
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
        pendingIntent.cancel()
        alarmManager.cancel(pendingIntent)
        Toast.makeText(context, "Monthly Notification OFF", Toast.LENGTH_SHORT).show()
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val pendingIntent = getPendingIntent(context)
        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
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
        val intent = Intent(context, MainActivity::class.java)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}