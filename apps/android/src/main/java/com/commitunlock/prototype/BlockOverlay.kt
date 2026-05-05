package com.commitunlock.prototype

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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
    ): Boolean {
        if (overlayView != null) return true
        val canAddCredit = !strictMode

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                UiKit.dp(context, 20),
                UiKit.dp(context, 32),
                UiKit.dp(context, 20),
                UiKit.dp(context, 32)
            )
            setBackgroundColor(0xF20F172A.toInt())
        }

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                UiKit.dp(context, 18),
                UiKit.dp(context, 20),
                UiKit.dp(context, 18),
                UiKit.dp(context, 20)
            )
            background = UiKit.rounded(context, 0xFF111827.toInt(), radiusDp = 8, strokeColor = 0xFF334155.toInt())
        }

        val title = TextView(context).apply {
            text = "Commit first. Scroll later."
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        val message = TextView(context).apply {
            text = "Your local credit ledger is empty, so this target is paused."
            textSize = 16f
            setTextColor(0xFFE5E7EB.toInt())
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(context, 14), 0, UiKit.dp(context, 14))
        }

        val targetState = UiKit.monoBlock(context).apply {
            text = "Target: $packageName\nReason: $reasonCode\nRemaining credit: 0 minutes\nStrict mode: ${if (strictMode) "on" else "off"}"
            setTextColor(0xFFE2E8F0.toInt())
            gravity = Gravity.CENTER
            background = UiKit.rounded(context, 0xFF0B1220.toInt(), radiusDp = 8, strokeColor = 0xFF334155.toInt())
        }

        val nextAction = TextView(context).apply {
            text = if (strictMode) {
                "Open Commit Unlock to review your policy or add credit from the app."
            } else {
                "Prototype mode: patch in test credit here, or open Commit Unlock to review targets and dogfood evidence."
            }
            textSize = 15f
            setTextColor(0xFFE2E8F0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(context, 16), 0, UiKit.dp(context, 10))
        }

        val openApp = UiKit.button(context, "Review in Commit Unlock", UiKit.ButtonTone.PRIMARY, onOpenApp)
        val addCredit = UiKit.button(context, "Patch in 5 test minutes", UiKit.ButtonTone.SECONDARY, onAddCredit)

        val strictModeNotice = TextView(context).apply {
            text = "Strict mode is on. The overlay test-credit shortcut is disabled."
            textSize = 15f
            setTextColor(0xFFCBD5E1.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        panel.addView(title)
        panel.addView(message)
        panel.addView(targetState)
        panel.addView(nextAction)
        panel.addView(openApp)
        if (canAddCredit) {
            panel.addView(addCredit)
        } else {
            panel.addView(strictModeNotice)
        }
        container.addView(panel)

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

        return runCatching {
            windowManager.addView(container, params)
            overlayView = container
        }.isSuccess
    }

    fun hide() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }
}
