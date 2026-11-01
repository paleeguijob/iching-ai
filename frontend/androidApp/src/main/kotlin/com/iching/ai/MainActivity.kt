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

    /** FCM puts the message's data in the launch intent when the user taps a background notification. */
    private fun openFromPush(intent: Intent?) {
        if (intent?.getStringExtra("type") != "daily") return
        intent.removeExtra("type")
        app.analytics.logEvent("push_open", emptyMap())
        PendingNavigation.requestDaily()
    }
}
