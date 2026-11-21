package com.iching.ai.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/** Redraws the widget just after Bangkok midnight, then sets itself up for the next one. */
class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        DailyHexagramWidget().updateAll(applicationContext)
        if (hasWidgets(applicationContext)) schedule(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        private const val NAME = "daily-hexagram-widget-midnight"

        /** A second of slack so the redraw lands on the new day. */
        private const val SLACK_MILLIS = 1_000L

        suspend fun hasWidgets(context: Context): Boolean =
            GlanceAppWidgetManager(context).getGlanceIds(DailyHexagramWidget::class.java).isNotEmpty()

        /**
         * REPLACE restarts the timer (new widget), KEEP leaves a pending one alone (app start), and the worker itself
         * appends the next one because it is still running.
         */
        fun schedule(context: Context, policy: ExistingWorkPolicy) {
            val delay = millisUntilBangkokMidnight(Clock.System.now().toEpochMilliseconds()) + SLACK_MILLIS
            val request = OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, policy, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
