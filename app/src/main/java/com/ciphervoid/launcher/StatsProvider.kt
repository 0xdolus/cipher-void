package com.ciphervoid.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.StatFs

/**
 * Polls system stats every 3s on the main thread via Handler.
 * Fires onStats() callback to whoever called start() — MainActivity owns the UI update.
 */
class StatsProvider(private val context: Context) {

    companion object {
        private const val POLL_INTERVAL_MS = 3000L
    }

    data class StatsSnapshot(
        val ram:      String,   // e.g. "2.1 / 6.0 gb"
        val battery:  String,   // e.g. "87% charging"
        val uptime:   String,   // e.g. "3h 22m"
        val kernel:   String,   // e.g. "5.4.210"
        val storage:  String    // e.g. "45 / 128 gb"
    )

    interface Callback {
        fun onStats(s: StatsSnapshot)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var callback: Callback? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            callback?.onStats(collect())
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start(cb: Callback) {
        callback = cb
        handler.post(pollRunnable)
    }

    fun stop() {
        callback = null
        handler.removeCallbacks(pollRunnable)
    }

    private fun collect(): StatsSnapshot {
        return StatsSnapshot(
            ram     = getRam(),
            battery = getBattery(),
            uptime  = getUptime(),
            kernel  = getKernel(),
            storage = getStorage()
        )
    }

    private fun getRam(): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val totalGb = info.totalMem.toFloat() / (1024 * 1024 * 1024)
        val usedGb  = (info.totalMem - info.availMem).toFloat() / (1024 * 1024 * 1024)
        return "%.1f / %.1f gb".format(usedGb, totalGb)
    }

    private fun getBattery(): String {
        // Sticky intent — no receiver registration needed
        val intent = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct    = if (scale > 0) level * 100 / scale else 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
        return if (charging) "$pct% charging" else "$pct%"
    }

    private fun getUptime(): String {
        return try {
            // First field in /proc/uptime is total seconds since boot as a float
            val raw     = java.io.File("/proc/uptime").readText().trim()
            val seconds = raw.split(" ")[0].toDouble().toLong()
            val hours   = seconds / 3600
            val minutes = (seconds % 3600) / 60
            "${hours}h ${minutes}m"
        } catch (e: Exception) {
            "n/a"
        }
    }

    private fun getKernel(): String {
        return try {
            // /proc/version: "Linux version X.Y.Z-... (build info)"
            // Third token is the version string; trim everything after the first "-"
            val raw = java.io.File("/proc/version").readText().trim()
            raw.split(" ")[2].substringBefore("-")
        } catch (e: Exception) {
            "n/a"
        }
    }

    private fun getStorage(): String {
        return try {
            val stat      = StatFs("/data")
            val totalBytes = stat.blockSizeLong * stat.blockCountLong
            val availBytes = stat.blockSizeLong * stat.availableBlocksLong
            val usedGb    = (totalBytes - availBytes).toFloat() / (1024 * 1024 * 1024)
            val totalGb   = totalBytes.toFloat() / (1024 * 1024 * 1024)
            "%.0f / %.0f gb".format(usedGb, totalGb)
        } catch (e: Exception) {
            "n/a"
        }
    }
}
