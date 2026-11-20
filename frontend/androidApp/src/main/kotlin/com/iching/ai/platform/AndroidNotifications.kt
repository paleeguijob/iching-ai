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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.ui.strings.ThDailyStrings

/** Keep the channel id in step with backend/functions/src/core/daily.ts and index.ts; topics are in DailyTopic.kt. */
const val DAILY_CHANNEL_ID = "daily"

class AndroidNotifications(private val context: Context) : NativeNotifications {
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingPermission: ResultCallback? = null

    /** Creating a channel that exists only renames it, so this also follows a change of the app language. */
    fun createChannel(name: String = ThDailyStrings.title) {
        val channel = NotificationChannel(DAILY_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
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

    override fun setDailySubscribed(enabled: Boolean, lang: AppLanguage, callback: ResultCallback) {
        val messaging = FirebaseMessaging.getInstance()
        // Auto-init is off in the manifest so nobody gets an FCM token before opting in.
        if (enabled) messaging.isAutoInitEnabled = true
        // Always leave the other language's topic; when switching off, leave both.
        val own = dailyTopicFor(lang)
        val first = if (enabled) messaging.subscribeToTopic(own) else messaging.unsubscribeFromTopic(own)
        val second = messaging.unsubscribeFromTopic(otherTopic(lang))
        Tasks.whenAll(first, second).addOnCompleteListener {
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
