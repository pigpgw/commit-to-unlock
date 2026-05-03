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
import android.widget.Toast
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : Activity() {
    private lateinit var creditStore: CreditStore
    private lateinit var developerGateStore: DeveloperGateStore
    private lateinit var dogfoodEventStore: DogfoodEventStore
    private lateinit var emergencyUnlockStore: EmergencyUnlockStore
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var monitorStateStore: MonitorStateStore
    private lateinit var policyStore: PolicyStore
    private lateinit var statusText: TextView
    private lateinit var recentPackagesText: TextView
    private lateinit var policySummaryText: TextView
    private lateinit var dogfoodSummaryText: TextView
    private lateinit var eventLogText: TextView
    private lateinit var packageInput: EditText
    private lateinit var strictModeInput: CheckBox
    private lateinit var activeFromInput: EditText
    private lateinit var activeUntilInput: EditText
    private lateinit var applyPublicHolidaysInput: CheckBox
    private lateinit var manualHolidayInput: CheckBox
    private lateinit var emergencyReasonInput: EditText
    private val weekdayInputs = mutableMapOf<Int, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creditStore = CreditStore(this)
        developerGateStore = DeveloperGateStore(this)
        dogfoodEventStore = DogfoodEventStore(this)
        emergencyUnlockStore = EmergencyUnlockStore(this)
        foregroundReader = ForegroundAppReader(this)
        monitorStateStore = MonitorStateStore(this)
        policyStore = PolicyStore(this)
        if (developerGateStore.isAccepted()) {
            showMainPrototype()
        } else {
            setContentView(buildDeveloperGate())
        }
    }

    private fun showMainPrototype() {
        requestNotificationPermission()
        setContentView(buildContent())
        renderState()
    }

    override fun onResume() {
        super.onResume()
        if (developerGateStore.isAccepted()) {
            renderState()
        }
    }

    private fun buildDeveloperGate(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 72, 36, 48)
        }

        val title = TextView(this).apply {
            text = "개발자지만 난 괜찮아"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFF111827.toInt())
        }

        val question = TextView(this).apply {
            text = "개발자이신가요?"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFF111827.toInt())
            setPadding(0, 24, 0, 12)
        }

        val message = TextView(this).apply {
            text = "이 앱은 커밋, PR, 빌드 실패, 그리고 새벽 2시의 이상한 자신감을 이해하는 사람만 입장할 수 있습니다."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFF475569.toInt())
            setPadding(0, 0, 0, 28)
        }

        root.addView(title)
        root.addView(question)
        root.addView(message)
        root.addView(button("예, 커밋으로 증명하겠습니다") {
            developerGateStore.accept()
            dogfoodEventStore.record("developer_gate_accepted")
            showMainPrototype()
        })
        root.addView(button("아니오, 그냥 스크롤하러 왔습니다") {
            dogfoodEventStore.record("developer_gate_rejected")
            setContentView(buildRejectedGate())
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun buildRejectedGate(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 72, 36, 48)
        }

        val title = TextView(this).apply {
            text = "403: 개발자 인증 실패"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(0xFF111827.toInt())
        }

        val message = TextView(this).apply {
            text = "저리가. 여긴 SNS를 줄이려는 개발자 전용 던전입니다. 농담이고, 진짜 개발자라면 앱을 다시 열고 예를 누르세요."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFF475569.toInt())
            setPadding(0, 20, 0, 28)
        }

        root.addView(title)
        root.addView(message)
        root.addView(button("퇴장하기") { finishAndRemoveTask() })

        return ScrollView(this).apply { addView(root) }
    }

    private fun buildContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
        }

        val title = TextView(this).apply {
            text = "개발자지만 난 괜찮아"
            textSize = 24f
            setTextColor(0xFF111827.toInt())
        }

        val subtitle = TextView(this).apply {
            text = "코드를 냈으면 쉬는 시간도 떳떳하게. 지금은 로컬 Android 차단 프로토타입입니다."
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

        activeFromInput = EditText(this).apply {
            hint = "Active from HH:mm (blank = 00:00)"
            setSingleLine(true)
        }

        activeUntilInput = EditText(this).apply {
            hint = "Active until HH:mm (blank = 24:00)"
            setSingleLine(true)
        }

        applyPublicHolidaysInput = CheckBox(this).apply {
            text = "Apply on public holidays (manual placeholder)"
        }

        manualHolidayInput = CheckBox(this).apply {
            text = "Treat today as holiday"
        }

        policySummaryText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF334155.toInt())
            setPadding(0, 20, 0, 10)
        }

        emergencyReasonInput = EditText(this).apply {
            hint = "Emergency unlock reason (required)"
            minLines = 1
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
        root.addView(sectionLabel("Policy schedule"))
        weekdayInputs.clear()
        weekdayLabels.forEach { (day, label) ->
            val checkbox = CheckBox(this).apply { text = label }
            weekdayInputs[day] = checkbox
            root.addView(checkbox)
        }
        root.addView(activeFromInput)
        root.addView(activeUntilInput)
        root.addView(applyPublicHolidaysInput)
        root.addView(manualHolidayInput)
        root.addView(button("Save policy schedule") { savePolicy() })
        root.addView(button("Set mock free day until midnight") { setMockFreeDay() })
        root.addView(button("Clear mock free day") {
            creditStore.setFreeUntil(null)
            dogfoodEventStore.record("free_day_cleared")
            renderState()
        })
        root.addView(policySummaryText)
        root.addView(sectionLabel("Emergency unlock"))
        root.addView(emergencyReasonInput)
        root.addView(button("Emergency unlock 5 minutes") { startEmergencyUnlock(5) })
        root.addView(button("Emergency unlock 15 minutes") { startEmergencyUnlock(15) })
        root.addView(button("Emergency unlock 30 minutes") { startEmergencyUnlock(30) })
        root.addView(button("Clear emergency unlocks") {
            emergencyUnlockStore.clear()
            dogfoodEventStore.record("emergency_unlocks_cleared")
            renderState()
        })
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

    private fun sectionLabel(label: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(0xFF111827.toInt())
            setPadding(0, 22, 0, 8)
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

    private fun savePolicy() {
        val activeFrom = normalizedTime(activeFromInput.text.toString())
        val activeUntil = normalizedTime(activeUntilInput.text.toString())
        if (activeFrom == INVALID_TIME || activeUntil == INVALID_TIME) {
            Toast.makeText(this, "Use HH:mm time, for example 09:30", Toast.LENGTH_SHORT).show()
            dogfoodEventStore.record("policy_save_rejected", "invalid_time")
            renderState()
            return
        }

        val current = policyStore.read()
        val timezone = ZoneId.systemDefault().id
        val selectedWeekdays = weekdayInputs
            .filter { it.value.isChecked }
            .keys
            .sorted()
        val today = LocalDate.now(ZoneId.of(timezone)).toString()

        policyStore.save(current.copy(
            activeWeekdays = selectedWeekdays,
            activeFrom = activeFrom,
            activeUntil = activeUntil,
            applyOnPublicHolidays = applyPublicHolidaysInput.isChecked,
            manualHolidayDate = if (manualHolidayInput.isChecked) today else null,
            timezone = timezone
        ))

        dogfoodEventStore.record(
            "policy_saved",
            "weekdays=${selectedWeekdays.joinToString(",")} from=${activeFrom.orEmpty()} until=${activeUntil.orEmpty()} manualHoliday=${manualHolidayInput.isChecked}"
        )
        renderState()
    }

    private fun setMockFreeDay() {
        val policy = policyStore.read()
        val zoneId = runCatching { ZoneId.of(policy.timezone) }.getOrDefault(ZoneId.systemDefault())
        val freeUntil = LocalDate.now(zoneId)
            .plusDays(1)
            .atStartOfDay(zoneId)
            .minusNanos(1)
            .toInstant()
            .toString()

        creditStore.setFreeUntil(freeUntil)
        dogfoodEventStore.record("free_day_set", "until=$freeUntil source=mock")
        renderState()
    }

    private fun startEmergencyUnlock(durationMinutes: Int) {
        val reason = emergencyReasonInput.text.toString().trim()
        val policy = policyStore.read()
        val credit = creditStore.read()
        val now = Instant.now()

        when {
            reason.isBlank() -> {
                Toast.makeText(this, "Emergency unlock reason is required", Toast.LENGTH_SHORT).show()
                dogfoodEventStore.record("emergency_unlock_rejected", "missing_reason")
                return
            }
            credit.strictMode && durationMinutes == 30 -> {
                Toast.makeText(this, "Strict mode blocks 30 minute emergency unlocks", Toast.LENGTH_SHORT).show()
                dogfoodEventStore.record("emergency_unlock_rejected", "strict_mode_30")
                return
            }
            emergencyUnlockStore.countStartedToday(policy.timezone, now) >= DAILY_EMERGENCY_LIMIT -> {
                Toast.makeText(this, "Daily emergency unlock limit reached", Toast.LENGTH_SHORT).show()
                dogfoodEventStore.record("emergency_unlock_rejected", "daily_limit")
                return
            }
            emergencyUnlockStore.countStartedSince(now.minus(Duration.ofDays(7)), now) >= WEEKLY_EMERGENCY_LIMIT -> {
                Toast.makeText(this, "Weekly emergency unlock limit reached", Toast.LENGTH_SHORT).show()
                dogfoodEventStore.record("emergency_unlock_rejected", "weekly_limit")
                return
            }
        }

        val unlock = emergencyUnlockStore.start(durationMinutes, reason, now)
        dogfoodEventStore.record(
            "emergency_unlock_started",
            "id=${unlock.id} minutes=$durationMinutes reason=$reason"
        )
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
        val policy = policyStore.read()
        val activeUnlocks = emergencyUnlockStore.active()
        val foregroundPackage = foregroundPackageOrNull()
        val decision = PolicyDecisionEngine.evaluate(
            PolicyDecisionInput(
                currentPackage = foregroundPackage,
                ownPackage = packageName,
                now = Instant.now(),
                creditState = state,
                policyState = policy,
                activeEmergencyUnlocks = activeUnlocks,
                isPublicHoliday = false
            )
        )
        packageInput.setText(state.blockedTargets.joinToString(", "))
        strictModeInput.isChecked = state.strictMode
        weekdayInputs.forEach { (day, checkbox) ->
            checkbox.isChecked = policy.activeWeekdays.contains(day)
        }
        activeFromInput.setText(policy.activeFrom.orEmpty())
        activeUntilInput.setText(policy.activeUntil.orEmpty())
        applyPublicHolidaysInput.isChecked = policy.applyOnPublicHolidays
        manualHolidayInput.isChecked = policy.isManualHolidayActive()
        val recentPackages = recentExternalPackages()

        statusText.text = listOf(
            "Usage Access: ${if (PermissionChecks.hasUsageAccess(this)) "granted" else "missing"}",
            "Overlay Permission: ${if (PermissionChecks.canDrawOverlays(this)) "granted" else "missing"}",
            "Notification Permission: ${if (PermissionChecks.hasNotificationPermission(this)) "granted" else "missing"}",
            "Monitor service: ${if (monitorStateStore.isRunning()) "running" else "stopped"}",
            "Current foreground: ${foregroundPackage ?: foregroundUnavailableReason()}",
            "Remaining mock credit: ${state.remainingMinutes} minutes",
            "Mock free until: ${state.freeUntil ?: "none"}",
            "Blocked targets: ${state.blockedTargets.ifEmpty { listOf("none") }.joinToString(", ")}",
            "Strict mode: ${state.strictMode}",
            "Last updated: ${state.lastUpdatedAt}"
        ).joinToString("\n")

        policySummaryText.text = listOf(
            "Policy summary",
            "Decision: ${decision.reason.code} (${if (decision.allowed) "allowed" else "blocked"})",
            "Credit spend on use: ${decision.shouldSpendCredit}",
            "Active weekdays: ${policy.activeWeekdays.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "none"}",
            "Active time: ${(policy.activeFrom ?: "00:00")} - ${(policy.activeUntil ?: "24:00")}",
            "Manual holiday today: ${policy.isManualHolidayActive()}",
            "Public holiday setting: ${if (policy.applyOnPublicHolidays) "apply" else "skip"}",
            "Timezone: ${policy.timezone}",
            "Active emergency unlock: ${activeUnlocks.firstOrNull()?.expiresAt ?: "none"}"
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
            "Policy blocks: ${summary.policyBlocks}",
            "Emergency unlocks: ${summary.emergencyUnlocks}",
            "Mock free days: ${summary.freeDays}",
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

    private fun foregroundPackageOrNull(): String? {
        if (!PermissionChecks.hasUsageAccess(this)) return null
        return foregroundReader.currentForegroundPackage()
    }

    private fun foregroundUnavailableReason(): String {
        return if (PermissionChecks.hasUsageAccess(this)) {
            "unknown"
        } else {
            "unknown (usage access missing)"
        }
    }

    private fun normalizedTime(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (!TIME_REGEX.matches(trimmed)) return INVALID_TIME

        val hour = trimmed.substringBefore(":").toInt()
        val minute = trimmed.substringAfter(":").toInt()
        if (hour !in 0..23 || minute !in 0..59) return INVALID_TIME

        return "%02d:%02d".format(hour, minute)
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

    companion object {
        private const val INVALID_TIME = "__invalid_time__"
        private const val DAILY_EMERGENCY_LIMIT = 3
        private const val WEEKLY_EMERGENCY_LIMIT = 10
        private val TIME_REGEX = Regex("""^\d{1,2}:\d{2}$""")
        private val weekdayLabels = listOf(
            1 to "Monday",
            2 to "Tuesday",
            3 to "Wednesday",
            4 to "Thursday",
            5 to "Friday",
            6 to "Saturday",
            7 to "Sunday"
        )
    }
}
