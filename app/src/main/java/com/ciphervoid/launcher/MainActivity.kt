package com.ciphervoid.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Stage 1 shell — minimal activity, just proves the launcher registers and launches.
 * Black screen + green prompt text. No logic yet.
 * Full lock screen, home screen, stats, and app drawer come in Stages 2–5.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
