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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppDrawerActivity : AppCompatActivity() {

    companion object {
        private const val GRID_COLUMNS = 4   // wider grid — more apps visible without scrolling
    }

    private lateinit var etSearch: EditText
    private lateinit var rvDrawer: RecyclerView
    private lateinit var adapter:  DrawerAdapter

    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)

        etSearch = findViewById(R.id.etSearch)
        rvDrawer = findViewById(R.id.rvDrawer)
        rvDrawer.layoutManager = GridLayoutManager(this, GRID_COLUMNS)

        adapter = DrawerAdapter(emptyList())
        rvDrawer.adapter = adapter

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

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
    }

    private inner class DrawerAdapter(private var apps: List<AppInfo>) :
        RecyclerView.Adapter<DrawerAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView  = view.findViewById(R.id.appName)
        }

        fun setData(newApps: List<AppInfo>) {
            apps = newApps
            notifyDataSetChanged()
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
