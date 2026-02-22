package com.example.product_store_app.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object StatusBarColor {

    fun set(activity: Activity, color: Int, lightIcons: Boolean = false) {

        // Activar edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val decorView = activity.window.decorView as ViewGroup

        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->

            val statusBarInsets =
                insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val statusBarHeight = statusBarInsets.top

            decorView.findViewWithTag<View>("statusBarBackground")?.let {
                decorView.removeView(it)
            }

            val statusBarView = View(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    statusBarHeight
                )
                setBackgroundColor(color)
                tag = "statusBarBackground"
            }

            decorView.addView(statusBarView)

            insets
        }

        // Configurar iconos claros u oscuros
        WindowCompat.getInsetsController(activity.window, decorView)
            .isAppearanceLightStatusBars = lightIcons
    }
}