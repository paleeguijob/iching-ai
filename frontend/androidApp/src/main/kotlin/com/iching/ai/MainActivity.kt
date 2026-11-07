package com.iching.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.iching.ai.navigation.PendingNavigation

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
        } else {
            app.analytics.logEvent("push_open", emptyMap())
            PendingNavigation.requestDaily()
        }
    }
}
