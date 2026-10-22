package io.github.luogen520.miaosuan

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 深浅色跟着上次的偏好来，没设过就听系统的
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            prefs.getInt("nightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )
    }
}
