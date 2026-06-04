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

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CLOCK_INTERVAL_MS  = 1000L   // clock ticks every second
        private const val CURSOR_INTERVAL_MS = 500L    // cursor blinks twice per second
        private const val SWIPE_THRESHOLD_PX = 200     // minimum rightward drag to unlock
    }

    private lateinit var lockScreen:    LinearLayout
    private lateinit var homeScreen:    LinearLayout
    private lateinit var tvTime:        TextView
    private lateinit var tvDate:        TextView
    private lateinit var tvPrompt:      TextView

    private val handler = Handler(Looper.getMainLooper())
    private var cursorVisible = true

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault())

    // Updates time + date labels every second
    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            tvTime.text = timeFmt.format(now)
            tvDate.text = dateFmt.format(now).lowercase()
            handler.postDelayed(this, CLOCK_INTERVAL_MS)
        }
    }

    // Toggles the trailing underscore to simulate a blinking terminal cursor
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

        setupSlideToUnlock()
    }

    override fun onResume() {
        super.onResume()
        // Always return to lock screen when activity comes back to foreground —
        // covers the case where the user pressed Home inside a launched app
        showLockScreen()
        handler.post(clockRunnable)
        handler.post(cursorRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Stop all ticking while off-screen to save battery
        handler.removeCallbacks(clockRunnable)
        handler.removeCallbacks(cursorRunnable)
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
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Only unlock on a rightward swipe past the threshold
                    if (event.x - touchStartX > SWIPE_THRESHOLD_PX) {
                        showHomeScreen()
                    }
                    true
                }
                else -> false
            }
        }
    }
}


