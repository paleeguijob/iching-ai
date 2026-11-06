package com.iching.ai.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging

/** Keep the channel id and topic in step with backend/functions/src/core/daily.ts and index.ts. */
private const val DAILY_TOPIC = "daily-hexagram-th"
const val DAILY_CHANNEL_ID = "daily"

class AndroidNotifications(private val context: Context) : NativeNotifications {
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingPermission: ResultCallback? = null

    fun createChannel() {
        val channel = NotificationChannel(DAILY_CHANNEL_ID, "กวาประจำวัน", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** The launcher has to be registered by the activity while it is being created. */
    fun attach(launcher: ActivityResultLauncher<String>?) {
        permissionLauncher = launcher
    }

    fun onPermissionResult(granted: Boolean) {
        pendingPermission?.onResult(if (granted) "granted" else "denied", null)
        pendingPermission = null
    }

    override fun isSupported() = true

    override fun permissionGranted() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun requestPermission(callback: ResultCallback) {
        val launcher = permissionLauncher
        // Below Android 13 there is no runtime permission: only the system settings can change it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || launcher == null) {
            callback.onResult(if (permissionGranted()) "granted" else "denied", null)
            return
        }
        pendingPermission?.onResult("denied", null)
        pendingPermission = callback
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun setDailySubscribed(enabled: Boolean, callback: ResultCallback) {
        val messaging = FirebaseMessaging.getInstance()
        // Auto-init is off in the manifest so nobody gets an FCM token before opting in.
        if (enabled) messaging.isAutoInitEnabled = true
        val task = if (enabled) messaging.subscribeToTopic(DAILY_TOPIC) else messaging.unsubscribeFromTopic(DAILY_TOPIC)
        task.addOnCompleteListener {
            if (it.isSuccessful) callback.onResult("ok", null)
            else callback.onResult(null, it.exception?.message ?: "FCM topic change failed")
        }
    }

    override fun openSystemSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
