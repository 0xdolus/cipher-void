package com.ciphervoid.launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfo(
    val name:        String,
    val packageName: String,
    val icon:        Drawable
)

class AppLoader(private val context: Context) {

    fun load(): List<AppInfo> {
        val pm     = context.packageManager
        // ACTION_MAIN + CATEGORY_LAUNCHER gives exactly the same list the system launcher sees —
        // user-facing apps only, no internal services or hidden system packages
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { ri ->
                AppInfo(
                    name        = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    icon        = ri.loadIcon(pm)
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}

