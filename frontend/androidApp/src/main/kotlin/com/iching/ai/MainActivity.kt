package com.iching.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.iching.ai.navigation.PendingNavigation
import com.iching.ai.widget.DailyHexagramWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as IChingApplication

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            app.notifications.onPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        app.notifications.attach(notificationPermission)
        // Consent form (EEA/UK only) before any ad is requested.
        app.ads.gatherConsent(this)
        if (savedInstanceState == null) openFromPush(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromPush(intent)
    }

    override fun onStart() {
        super.onStart()
        // Opening the app is one of the moments the widget catches up with the day and the language.
        lifecycleScope.launch { DailyHexagramWidget().updateAll(applicationContext) }
    }

    override fun onDestroy() {
        if (isFinishing) app.notifications.attach(null)
        super.onDestroy()
    }

    /**
     * FCM puts the message's data in the launch intent when the user taps a background notification.
     * Any other type is ignored, which is also how v1.1 treats a campaign push.
     */
    private fun openFromPush(intent: Intent?) {
        val type = intent?.getStringExtra("type")
        if (type != "daily" && type != "campaign") return
        intent.removeExtra("type")
        if (type == "campaign") {
            app.analytics.logEvent("push_open", mapOf("type" to "campaign"))
            PendingNavigation.requestYear()
        } else if (intent.getStringExtra("source") == "widget") {
            intent.removeExtra("source")
            app.analytics.logEvent("widget_tap", emptyMap())
            PendingNavigation.requestDaily()
        } else {
            app.analytics.logEvent("push_open", emptyMap())
            PendingNavigation.requestDaily()
        }
    }
}
