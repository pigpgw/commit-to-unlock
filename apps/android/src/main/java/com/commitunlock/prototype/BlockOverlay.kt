package com.commitunlock.prototype

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun show(packageName: String, onOpenApp: () -> Unit, onAddCredit: () -> Unit) {
        if (overlayView != null) return

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xF2111827.toInt())
        }

        val title = TextView(context).apply {
            text = "Blocked"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val message = TextView(context).apply {
            text = "$packageName is locked because mock credit is 0 minutes."
            textSize = 17f
            setTextColor(0xFFE5E7EB.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 28)
        }

        val openApp = Button(context).apply {
            text = "Open Commit Unlock"
            setOnClickListener { onOpenApp() }
        }

        val addCredit = Button(context).apply {
            text = "Add 5 test minutes"
            setOnClickListener { onAddCredit() }
        }

        container.addView(title)
        container.addView(message)
        container.addView(openApp)
        container.addView(addCredit)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = container
        windowManager.addView(container, params)
    }

    fun hide() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }
}
