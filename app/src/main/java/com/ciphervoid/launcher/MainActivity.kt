package com.ciphervoid.launcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), StatsProvider.Callback {

    companion object {
        private const val CLOCK_INTERVAL_MS  = 1000L
        private const val CURSOR_INTERVAL_MS = 500L
        private const val SWIPE_THRESHOLD_PX = 200
        private const val GRID_COLUMNS       = 3
        private const val DOCK_SIZE          = 4
    }

    // Views — lock screen
    private lateinit var lockScreen: LinearLayout
    private lateinit var tvTime:     TextView
    private lateinit var tvDate:     TextView
    private lateinit var tvPrompt:   TextView

    // Views — home screen
    private lateinit var homeScreen: LinearLayout
    private lateinit var tvStats:    TextView
    private lateinit var rvApps:     RecyclerView

    // Dock icon + label pairs, indexed 0–3
    private lateinit var dockIcons: List<ImageView>
    private lateinit var dockNames: List<TextView>

    private val handler      = Handler(Looper.getMainLooper())
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

        // Lock screen views
        lockScreen = findViewById(R.id.lockScreen)
        tvTime     = findViewById(R.id.tvTime)
        tvDate     = findViewById(R.id.tvDate)
        tvPrompt   = findViewById(R.id.tvPrompt)

        // Home screen views
        homeScreen = findViewById(R.id.homeScreen)
        tvStats    = findViewById(R.id.tvStats)
        rvApps     = findViewById(R.id.rvApps)

        // Dock — collect into indexed lists so the loop in setupDock() stays clean
        dockIcons = listOf(
            findViewById(R.id.dockIcon0), findViewById(R.id.dockIcon1),
            findViewById(R.id.dockIcon2), findViewById(R.id.dockIcon3)
        )
        dockNames = listOf(
            findViewById(R.id.dockName0), findViewById(R.id.dockName1),
            findViewById(R.id.dockName2), findViewById(R.id.dockName3)
        )

        // RecyclerView must not try to scroll inside the NestedScrollView
        rvApps.isNestedScrollingEnabled = false
        rvApps.layoutManager = GridLayoutManager(this, GRID_COLUMNS)

        statsProvider = StatsProvider(this)

        setupSlideToUnlock()
        loadApps()
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

    override fun onStats(s: StatsProvider.StatsSnapshot) {
        tvStats.text =
            "os       android\n" +
            "kernel   ${s.kernel}\n" +
            "uptime   ${s.uptime}\n" +
            "battery  ${s.battery}\n" +
            "ram      ${s.ram}\n" +
            "storage  ${s.storage}"
    }

    private fun loadApps() {
        // PackageManager icon loading can be slow on budget hardware — keep it off the UI thread
        Thread {
            val apps = AppLoader(this).load()
            runOnUiThread {
                rvApps.adapter = AppAdapter(apps)
                setupDock(apps)
            }
        }.start()
    }

    private fun setupDock(apps: List<AppInfo>) {
        // Take the first DOCK_SIZE apps alphabetically — user can reorder in a later stage
        val dockApps = apps.take(DOCK_SIZE)
        dockApps.forEachIndexed { i, app ->
            dockIcons[i].setImageDrawable(app.icon)
            dockNames[i].text = app.name
            // Tap any dock slot to launch that app
            listOf(
                findViewById<LinearLayout>(R.id.dockSlot0),
                findViewById<LinearLayout>(R.id.dockSlot1),
                findViewById<LinearLayout>(R.id.dockSlot2),
                findViewById<LinearLayout>(R.id.dockSlot3)
            )[i].setOnClickListener { launchApp(app.packageName) }
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
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

    // ─── Inner adapter ────────────────────────────────────────────────────────

    private inner class AppAdapter(private val apps: List<AppInfo>) :
        RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView  = view.findViewById(R.id.appName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.name
            holder.itemView.setOnClickListener { launchApp(app.packageName) }
        }

        override fun getItemCount() = apps.size
    }
}
