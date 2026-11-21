package com.iching.ai.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingWorkPolicy
import com.iching.ai.IChingApplication

class DailyHexagramWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyHexagramWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        (context.applicationContext as? IChingApplication)?.analytics?.logEvent("widget_added", emptyMap())
        MidnightRefreshWorker.schedule(context, ExistingWorkPolicy.REPLACE)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        (context.applicationContext as? IChingApplication)?.analytics?.logEvent("widget_removed", emptyMap())
        MidnightRefreshWorker.cancel(context)
    }
}
