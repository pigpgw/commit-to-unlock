package com.commitunlock.prototype

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.Instant

class MainActivity : Activity() {
    private lateinit var creditStore: CreditStore
    private lateinit var statusText: TextView
    private lateinit var packageInput: EditText
    private lateinit var strictModeInput: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creditStore = CreditStore(this)
        requestNotificationPermission()
        setContentView(buildContent())
        renderState()
    }

    override fun onResume() {
        super.onResume()
        renderState()
    }

    private fun buildContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
        }

        val title = TextView(this).apply {
            text = "Commit Unlock Prototype"
            textSize = 24f
            setTextColor(0xFF111827.toInt())
        }

        val subtitle = TextView(this).apply {
            text = "Local Android blocker using Usage Access + overlay. No GitHub or server sync yet."
            textSize = 15f
            setTextColor(0xFF475569.toInt())
            setPadding(0, 8, 0, 20)
        }

        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF111827.toInt())
            setPadding(0, 0, 0, 18)
        }

        packageInput = EditText(this).apply {
            hint = "Package names, comma separated (ex: com.instagram.android)"
            minLines = 2
        }

        strictModeInput = CheckBox(this).apply {
            text = "Strict mode mock flag"
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(statusText)
        root.addView(button("Open Usage Access Settings") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        root.addView(button("Open Overlay Permission Settings") {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        })
        root.addView(packageInput)
        root.addView(strictModeInput)
        root.addView(button("Save blocked packages") { saveTargets() })
        root.addView(button("Add 5 test minutes") {
            creditStore.addMinutes(5)
            renderState()
        })
        root.addView(button("Spend 1 test minute") {
            creditStore.spendMinute()
            renderState()
        })
        root.addView(button("Start monitor service") {
            startForegroundService(Intent(this, MonitorService::class.java))
            renderState()
        })
        root.addView(button("Stop monitor service") {
            stopService(Intent(this, MonitorService::class.java))
            renderState()
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun button(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            gravity = Gravity.CENTER
            setOnClickListener { action() }
        }
    }

    private fun saveTargets() {
        val targets = packageInput.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val current = creditStore.read()
        creditStore.save(current.copy(
            blockedTargets = targets,
            strictMode = strictModeInput.isChecked,
            lastUpdatedAt = Instant.now().toString()
        ))
        renderState()
    }

    private fun renderState() {
        val state = creditStore.read()
        packageInput.setText(state.blockedTargets.joinToString(", "))
        strictModeInput.isChecked = state.strictMode

        statusText.text = listOf(
            "Usage Access: ${if (PermissionChecks.hasUsageAccess(this)) "granted" else "missing"}",
            "Overlay Permission: ${if (PermissionChecks.canDrawOverlays(this)) "granted" else "missing"}",
            "Remaining mock credit: ${state.remainingMinutes} minutes",
            "Blocked targets: ${state.blockedTargets.ifEmpty { listOf("none") }.joinToString(", ")}",
            "Strict mode: ${state.strictMode}",
            "Last updated: ${state.lastUpdatedAt}"
        ).joinToString("\n")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }
}
