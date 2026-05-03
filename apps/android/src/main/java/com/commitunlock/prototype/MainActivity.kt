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
    private lateinit var dogfoodEventStore: DogfoodEventStore
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var monitorStateStore: MonitorStateStore
    private lateinit var statusText: TextView
    private lateinit var recentPackagesText: TextView
    private lateinit var dogfoodSummaryText: TextView
    private lateinit var eventLogText: TextView
    private lateinit var packageInput: EditText
    private lateinit var strictModeInput: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creditStore = CreditStore(this)
        dogfoodEventStore = DogfoodEventStore(this)
        foregroundReader = ForegroundAppReader(this)
        monitorStateStore = MonitorStateStore(this)
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

        recentPackagesText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF334155.toInt())
            setPadding(0, 14, 0, 10)
        }

        dogfoodSummaryText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF334155.toInt())
            setPadding(0, 20, 0, 0)
        }

        eventLogText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF334155.toInt())
            setPadding(0, 20, 0, 0)
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
        root.addView(recentPackagesText)
        root.addView(button("Save blocked packages") { saveTargets() })
        root.addView(button("Add latest external package") { addLatestExternalPackage() })
        root.addView(button("Add 5 test minutes") {
            creditStore.addMinutes(5)
            dogfoodEventStore.record("credit_added", "source=main minutes=5")
            renderState()
        })
        root.addView(button("Spend 1 test minute") {
            creditStore.spendMinute()
            dogfoodEventStore.record("credit_spent", "source=main minutes=1")
            renderState()
        })
        root.addView(button("Reset credit to 0") {
            creditStore.resetCredit()
            dogfoodEventStore.record("credit_reset", "source=main")
            renderState()
        })
        root.addView(button("Start monitor service") {
            monitorStateStore.setRunning(true)
            dogfoodEventStore.record("monitor_start_requested")
            startForegroundService(Intent(this, MonitorService::class.java))
            renderState()
        })
        root.addView(button("Stop monitor service") {
            monitorStateStore.setRunning(false)
            dogfoodEventStore.record("monitor_stop_requested")
            stopService(Intent(this, MonitorService::class.java))
            renderState()
        })
        root.addView(button("Refresh status") { renderState() })
        root.addView(dogfoodSummaryText)
        root.addView(button("Share dogfood export") { shareDogfoodExport() })
        root.addView(button("Clear dogfood events") {
            dogfoodEventStore.clear()
            renderState()
        })
        root.addView(eventLogText)

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
        dogfoodEventStore.record("targets_saved", "count=${targets.size} strict=${strictModeInput.isChecked}")
        renderState()
    }

    private fun addLatestExternalPackage() {
        if (!PermissionChecks.hasUsageAccess(this)) {
            dogfoodEventStore.record("permission_missing", "usage_access")
            renderState()
            return
        }

        val latestExternalPackage = foregroundReader.recentForegroundPackages()
            .firstOrNull { it != packageName }

        if (latestExternalPackage == null) {
            dogfoodEventStore.record("recent_external_package_missing")
            renderState()
            return
        }

        val current = creditStore.read()
        val nextTargets = current.blockedTargets
            .plus(latestExternalPackage)
            .distinct()

        creditStore.save(current.copy(
            blockedTargets = nextTargets,
            strictMode = strictModeInput.isChecked,
            lastUpdatedAt = Instant.now().toString()
        ))
        dogfoodEventStore.record("target_added", latestExternalPackage)
        renderState()
    }

    private fun renderState() {
        val state = creditStore.read()
        packageInput.setText(state.blockedTargets.joinToString(", "))
        strictModeInput.isChecked = state.strictMode
        val recentPackages = recentExternalPackages()

        statusText.text = listOf(
            "Usage Access: ${if (PermissionChecks.hasUsageAccess(this)) "granted" else "missing"}",
            "Overlay Permission: ${if (PermissionChecks.canDrawOverlays(this)) "granted" else "missing"}",
            "Notification Permission: ${if (PermissionChecks.hasNotificationPermission(this)) "granted" else "missing"}",
            "Monitor service: ${if (monitorStateStore.isRunning()) "running" else "stopped"}",
            "Current foreground: ${currentForegroundPackage()}",
            "Remaining mock credit: ${state.remainingMinutes} minutes",
            "Blocked targets: ${state.blockedTargets.ifEmpty { listOf("none") }.joinToString(", ")}",
            "Strict mode: ${state.strictMode}",
            "Last updated: ${state.lastUpdatedAt}"
        ).joinToString("\n")

        recentPackagesText.text = buildString {
            append("Recent external packages\n")
            if (recentPackages.isEmpty()) {
                append("none")
            } else {
                append(recentPackages.joinToString("\n"))
            }
        }

        val summary = dogfoodEventStore.summary()
        dogfoodSummaryText.text = listOf(
            "Dogfood summary (last 14 days)",
            "Monitor enabled days: ${summary.monitorEnabledDays} / 8 target",
            "Blocked attempts: ${summary.blockedAttempts} / 8 target",
            "Permission failures: ${summary.permissionFailures}",
            "Overlay open-app actions: ${summary.overlayOpens}",
            "Overlay test-credit unlocks: ${summary.overlayCreditAdds}",
            "Automatic credit spends: ${summary.automaticCreditSpends}",
            "Manual credit changes: ${summary.manualCreditChanges}",
            "Stored dogfood events: ${summary.eventCount}"
        ).joinToString("\n")

        eventLogText.text = buildString {
            append("Dogfood event log\n")
            val events = dogfoodEventStore.read().take(50)
            if (events.isEmpty()) {
                append("none")
            } else {
                append(events.joinToString("\n") { event -> formatDogfoodEvent(event) })
            }
        }
    }

    private fun formatDogfoodEvent(event: DogfoodEvent): String {
        return listOf(event.timestamp.toString(), event.type, event.detail)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    private fun recentExternalPackages(): List<String> {
        if (!PermissionChecks.hasUsageAccess(this)) return emptyList()
        return foregroundReader.recentForegroundPackages()
            .filter { it != packageName }
    }

    private fun currentForegroundPackage(): String {
        if (!PermissionChecks.hasUsageAccess(this)) return "unknown (usage access missing)"
        return foregroundReader.currentForegroundPackage() ?: "unknown"
    }

    private fun shareDogfoodExport() {
        val export = dogfoodEventStore.exportTsv()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/tab-separated-values"
            putExtra(Intent.EXTRA_SUBJECT, "Commit Unlock dogfood export")
            putExtra(Intent.EXTRA_TEXT, export)
        }
        startActivity(Intent.createChooser(intent, "Share dogfood export"))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        renderState()
    }
}
