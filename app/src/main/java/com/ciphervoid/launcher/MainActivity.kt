package com.ciphervoid.launcher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), StatsProvider.Callback {

    companion object {
        private const val GRID_COLUMNS   = 3
        private const val DOCK_SIZE      = 4
        private const val HOME_APP_LIMIT = 9
    }

    private lateinit var tvStats:       TextView
    private lateinit var rvApps:        RecyclerView
    private lateinit var searchTrigger: TextView
    private lateinit var dock:          LinearLayout

    private lateinit var dockIcons: List<ImageView>
    private lateinit var dockNames: List<TextView>

    private lateinit var statsProvider: StatsProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStats       = findViewById(R.id.tvStats)
        rvApps        = findViewById(R.id.rvApps)
        searchTrigger = findViewById(R.id.searchTrigger)
        dock          = findViewById(R.id.dock)

        dockIcons = listOf(
            findViewById(R.id.dockIcon0), findViewById(R.id.dockIcon1),
            findViewById(R.id.dockIcon2), findViewById(R.id.dockIcon3)
        )
        dockNames = listOf(
            findViewById(R.id.dockName0), findViewById(R.id.dockName1),
            findViewById(R.id.dockName2), findViewById(R.id.dockName3)
        )

        rvApps.isNestedScrollingEnabled = false
        rvApps.layoutManager = GridLayoutManager(this, GRID_COLUMNS)

        statsProvider = StatsProvider(this)
        searchTrigger.setOnClickListener { openDrawer() }

        // Long-press anywhere on the dock to open settings
        dock.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        statsProvider.start(this)
        // Refresh apps in case pins changed while in SettingsActivity
        loadApps()
    }

    override fun onPause() {
        super.onPause()
        statsProvider.stop()
    }

    override fun onStats(s: StatsProvider.StatsSnapshot) {
        val lines = mutableListOf<String>()
        lines.add("os       android")
        if (s.kernel != "n/a") lines.add("kernel   ${s.kernel}")
        if (s.uptime != "n/a") lines.add("uptime   ${s.uptime}")
        lines.add("battery  ${s.battery}")
        lines.add("ram      ${s.ram}")
        lines.add("storage  ${s.storage}")
        tvStats.text = lines.joinToString("\n")
    }

    private fun openDrawer() {
        startActivity(Intent(this, AppDrawerActivity::class.java))
    }

    private fun loadApps() {
        Thread {
            val apps = AppLoader(this).load()
            runOnUiThread {
                rvApps.adapter = AppAdapter(resolveHomeApps(apps))
                setupDock(apps)
            }
        }.start()
    }

    // Read home grid slots from prefs — fall back to alphabetical first 9 if empty
    private fun resolveHomeApps(all: List<AppInfo>): List<AppInfo> {
        val prefs    = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val resolved = (0 until HOME_APP_LIMIT).mapNotNull { i ->
            prefs.getString("home_$i", null)?.let { pkg -> all.find { it.packageName == pkg } }
        }
        return if (resolved.isEmpty()) all.take(HOME_APP_LIMIT) else resolved
    }

    private fun setupDock(all: List<AppInfo>) {
        val prefs     = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val dockSlots = listOf(
            findViewById<LinearLayout>(R.id.dockSlot0),
            findViewById<LinearLayout>(R.id.dockSlot1),
            findViewById<LinearLayout>(R.id.dockSlot2),
            findViewById<LinearLayout>(R.id.dockSlot3)
        )
        for (i in 0 until DOCK_SIZE) {
            // Use pinned package if set, otherwise fall back to alphabetical position
            val pkg = prefs.getString("dock_$i", null)
            val app = if (pkg != null) all.find { it.packageName == pkg } else all.getOrNull(i)
            app?.let {
                dockIcons[i].setImageDrawable(it.icon)
                dockNames[i].text = it.name
                dockSlots[i].setOnClickListener { _ -> launchApp(it.packageName) }
            }
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
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
