package com.ciphervoid.launcher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PinAppActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvPick:   RecyclerView
    private lateinit var adapter:  PickAdapter

    private var allApps: List<AppInfo> = emptyList()
    private var slotKey: String        = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_app)

        slotKey  = intent.getStringExtra(SettingsActivity.EXTRA_SLOT) ?: ""
        etSearch = findViewById(R.id.etSearch)
        rvPick   = findViewById(R.id.rvPick)

        rvPick.layoutManager = LinearLayoutManager(this)
        adapter = PickAdapter(emptyList())
        rvPick.adapter = adapter

        setupSearch()
        loadApps()
    }

    private fun loadApps() {
        Thread {
            allApps = AppLoader(this).load()
            runOnUiThread { adapter.setData(allApps) }
        }.start()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query    = s?.toString()?.trim() ?: ""
                val filtered = if (query.isEmpty()) allApps
                               else allApps.filter { it.name.contains(query, ignoreCase = true) }
                adapter.setData(filtered)
            }
        })
    }

    // Save chosen package to prefs and return to SettingsActivity
    private fun pinApp(app: AppInfo) {
        getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(slotKey, app.packageName)
            .apply()
        finish()
    }

    // ─── Inner adapter ────────────────────────────────────────────────────────

    private inner class PickAdapter(private var apps: List<AppInfo>) :
        RecyclerView.Adapter<PickAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.listIcon)
            val name: TextView  = view.findViewById(R.id.listName)
        }

        fun setData(newApps: List<AppInfo>) {
            apps = newApps
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.name
            holder.itemView.setOnClickListener { pinApp(app) }
        }

        override fun getItemCount() = apps.size
    }
}
