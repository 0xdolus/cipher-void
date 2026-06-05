package com.ciphervoid.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME    = "cv_prefs"
        const val EXTRA_SLOT    = "slot_key"
        private const val DOCK_SIZE = 4
        private const val HOME_SIZE = 9
    }

    private lateinit var dockContainer: LinearLayout
    private lateinit var homeContainer: LinearLayout
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dockContainer = findViewById(R.id.dockContainer)
        homeContainer = findViewById(R.id.homeContainer)

        // Load app list on background thread — icons are slow on budget hardware
        Thread {
            allApps = AppLoader(this).load()
            runOnUiThread { buildSlots() }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        // Refresh after returning from PinAppActivity
        if (allApps.isNotEmpty()) buildSlots()
    }

    private fun buildSlots() {
        buildSection(dockContainer, "dock", DOCK_SIZE)
        buildSection(homeContainer, "home", HOME_SIZE)
    }

    private fun buildSection(container: LinearLayout, prefix: String, count: Int) {
        container.removeAllViews()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        for (i in 0 until count) {
            val key = "${prefix}_$i"
            val pkg = prefs.getString(key, null)
            val app = pkg?.let { p -> allApps.find { it.packageName == p } }

            val row = layoutInflater.inflate(R.layout.item_slot, container, false)

            row.findViewById<ImageView>(R.id.slotIcon).apply {
                if (app != null) setImageDrawable(app.icon) else setImageDrawable(null)
            }
            row.findViewById<TextView>(R.id.slotName).text = app?.name ?: "[ slot $i — empty ]"
            row.findViewById<TextView>(R.id.slotChange).setOnClickListener {
                val intent = Intent(this, PinAppActivity::class.java)
                intent.putExtra(EXTRA_SLOT, key)
                startActivity(intent)
            }

            container.addView(row)
        }
    }
}
