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
        private const val HOME_APP_LIMIT = 9   // first 9 apps on home — full list lives in drawer
    }

    private lateinit var tvStats:       TextView
    private lateinit var rvApps:        RecyclerView
    private lateinit var searchTrigger: TextView

    private lateinit var dockIcons: List<ImageView>
    private lateinit var dockNames: List<TextView>

    private lateinit var statsProvider: StatsProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStats       = findViewById(R.id.tvStats)
        rvApps        = findViewById(R.id.rvApps)
        searchTrigger = findViewById(R.id.searchTrigger)

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

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        statsProvider.start(this)
    }

    override fun onPause() {
        super.onPause()
        statsProvider.stop()
    }

    // StatsProvider.Callback
    override fun onStats(s: StatsProvider.StatsSnapshot) {
        // Skip rows that the device blocks via SELinux — don't show "n/a" clutter
        val lines = mutableListOf<String>()
        lines.add("os       android")
        if (s.kernel  != "n/a") lines.add("kernel   ${s.kernel}")
        if (s.uptime  != "n/a") lines.add("uptime   ${s.uptime}")
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
                // Home screen shows only the first HOME_APP_LIMIT apps — full list is in the drawer
                rvApps.adapter = AppAdapter(apps.take(HOME_APP_LIMIT))
                setupDock(apps)
            }
        }.start()
    }

    private fun setupDock(apps: List<AppInfo>) {
        val dockApps  = apps.take(DOCK_SIZE)
        val dockSlots = listOf(
            findViewById<LinearLayout>(R.id.dockSlot0),
            findViewById<LinearLayout>(R.id.dockSlot1),
            findViewById<LinearLayout>(R.id.dockSlot2),
            findViewById<LinearLayout>(R.id.dockSlot3)
        )
        dockApps.forEachIndexed { i, app ->
            dockIcons[i].setImageDrawable(app.icon)
            dockNames[i].text = app.name
            dockSlots[i].setOnClickListener { launchApp(app.packageName) }
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
