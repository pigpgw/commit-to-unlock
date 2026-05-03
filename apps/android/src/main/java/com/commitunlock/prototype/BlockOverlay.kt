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

    fun show(
        packageName: String,
        strictMode: Boolean,
        reasonCode: String,
        onOpenApp: () -> Unit,
        onAddCredit: () -> Unit
    ) {
        if (overlayView != null) return
        val canAddCredit = !strictMode

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xF2111827.toInt())
        }

        val title = TextView(context).apply {
            text = "No leisure credit left"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val message = TextView(context).apply {
            text = "This app is paused because the local policy decision is blocked."
            textSize = 17f
            setTextColor(0xFFE5E7EB.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val targetState = TextView(context).apply {
            text = "Target: $packageName\nReason: $reasonCode\nRemaining credit: 0 minutes\nStrict mode: ${if (strictMode) "on" else "off"}"
            textSize = 15f
            setTextColor(0xFFCBD5E1.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val nextAction = TextView(context).apply {
            text = if (strictMode) {
                "Open Commit Unlock to review your policy or add credit from the app."
            } else {
                "Prototype mode: add test credit here, or open Commit Unlock to review your targets and dogfood log."
            }
            textSize = 15f
            setTextColor(0xFFE2E8F0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 26)
        }

        val openApp = Button(context).apply {
            text = "Review in Commit Unlock"
            setOnClickListener { onOpenApp() }
        }

        val addCredit = Button(context).apply {
            text = "Add 5 test minutes (prototype)"
            setOnClickListener { onAddCredit() }
        }

        val strictModeNotice = TextView(context).apply {
            text = "Strict mode is on. The overlay test-credit shortcut is disabled."
            textSize = 15f
            setTextColor(0xFFCBD5E1.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        container.addView(title)
        container.addView(message)
        container.addView(targetState)
        container.addView(nextAction)
        container.addView(openApp)
        if (canAddCredit) {
            container.addView(addCredit)
        } else {
            container.addView(strictModeNotice)
        }

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
