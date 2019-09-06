package com.revosleap.alerter.sevices

import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import com.revosleap.alerter.MainActivity
import com.revosleap.alerter.R
import com.revosleap.alerter.interfaces.Charge
import android.support.v4.app.NotificationCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.annotation.SuppressLint
import android.os.Build
import android.content.Context.NOTIFICATION_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.app.Notification
import android.graphics.Color


class BatteryChecker : Service() {
    private val binder = ServiceBinder()
    private var itsCharge: Charge? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerReceiver(chargeReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        hideIcon()
        return START_STICKY
    }

    fun setCharge(charge: Charge) {
        itsCharge = charge
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class ServiceBinder : Binder() {
        val batteryChecker: BatteryChecker
            get() = this@BatteryChecker
    }

    private fun showNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "screaming_goat"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screaming goat",
                NotificationManager.IMPORTANCE_MAX
            )
            // Configure the notification channel.
            notificationChannel.description = "Shut down screaming goat"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern =
                longArrayOf(0, 200, 400, 600, 800, 1000, 800, 600, 400, 200, 0)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Screaming Goat")
            //.setPriority(Notification.PRIORITY_MAX)
            .setContentTitle("Screaming goat")
            .setContentText("Shutdown Screaming goat")
        
        notificationManager.notify(1, notificationBuilder.build())
    }

    private val chargeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            itsCharge?.chargePercentage(level)
            if (level!! <= 20 && !isPowerConnected(context)) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
                )
                val player = MediaPlayer.create(context, R.raw.screaming_goat)
                player.start()
                showNotification()
            }
        }

    }

    fun isPowerConnected(context: Context): Boolean {
        val intentBatt = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intentBatt?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    private fun hideIcon() {
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

}
