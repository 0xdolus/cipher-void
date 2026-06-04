package com.ciphervoid.launcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), StatsProvider.Callback {

    companion object {
        private const val CLOCK_INTERVAL_MS  = 1000L
        private const val CURSOR_INTERVAL_MS = 500L
        private const val SWIPE_THRESHOLD_PX = 200
    }

    private lateinit var lockScreen:  LinearLayout
    private lateinit var homeScreen:  LinearLayout
    private lateinit var tvTime:      TextView
    private lateinit var tvDate:      TextView
    private lateinit var tvPrompt:    TextView
    private lateinit var tvStats:     TextView

    private val handler = Handler(Looper.getMainLooper())
    private var cursorVisible = true
    private lateinit var statsProvider: StatsProvider

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault())

    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            tvTime.text = timeFmt.format(now)
            tvDate.text = dateFmt.format(now).lowercase()
            handler.postDelayed(this, CLOCK_INTERVAL_MS)
        }
    }

    private val cursorRunnable = object : Runnable {
        override fun run() {
            cursorVisible = !cursorVisible
            tvPrompt.text = if (cursorVisible) "root@android:~$ _" else "root@android:~$  "
            handler.postDelayed(this, CURSOR_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lockScreen = findViewById(R.id.lockScreen)
        homeScreen = findViewById(R.id.homeScreen)
        tvTime     = findViewById(R.id.tvTime)
        tvDate     = findViewById(R.id.tvDate)
        tvPrompt   = findViewById(R.id.tvPrompt)
        tvStats    = findViewById(R.id.tvStats)

        statsProvider = StatsProvider(this)
        setupSlideToUnlock()
    }

    override fun onResume() {
        super.onResume()
        showLockScreen()
        handler.post(clockRunnable)
        handler.post(cursorRunnable)
        statsProvider.start(this)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable)
        handler.removeCallbacks(cursorRunnable)
        statsProvider.stop()
    }

    // StatsProvider.Callback — fires every 3s, always on main thread
    override fun onStats(s: StatsProvider.StatsSnapshot) {
        tvStats.text =
            "os       android\n" +
            "kernel   ${s.kernel}\n" +
            "uptime   ${s.uptime}\n" +
            "battery  ${s.battery}\n" +
            "ram      ${s.ram}\n" +
            "storage  ${s.storage}"
    }

    private fun showLockScreen() {
        lockScreen.visibility = View.VISIBLE
        homeScreen.visibility = View.GONE
    }

    private fun showHomeScreen() {
        lockScreen.visibility = View.GONE
        homeScreen.visibility = View.VISIBLE
    }

    private fun setupSlideToUnlock() {
        var touchStartX = 0f
        lockScreen.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { touchStartX = event.x; true }
                MotionEvent.ACTION_UP   -> {
                    if (event.x - touchStartX > SWIPE_THRESHOLD_PX) showHomeScreen()
                    true
                }
                else -> false
            }
        }
    }
}
