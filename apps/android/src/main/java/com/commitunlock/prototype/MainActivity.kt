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
    private lateinit var dailyQuestStore: DailyQuestStore
    private lateinit var developerGateStore: DeveloperGateStore
    private lateinit var dogfoodEventStore: DogfoodEventStore
    private lateinit var emergencyUnlockStore: EmergencyUnlockStore
    private lateinit var foregroundReader: ForegroundAppReader
    private lateinit var monitorStateStore: MonitorStateStore
    private lateinit var policyStore: PolicyStore
    private lateinit var overviewText: TextView
    private lateinit var statusText: TextView
    private lateinit var privacySummaryText: TextView
    private lateinit var privacyDisclosureText: TextView
    private lateinit var recentPackagesText: TextView
    private lateinit var policySummaryText: TextView
    private lateinit var questSummaryText: TextView
    private lateinit var dogfoodSummaryText: TextView
    private lateinit var dogfoodReviewText: TextView
    private lateinit var eventLogText: TextView
    private lateinit var packageInput: EditText
    private lateinit var strictModeInput: CheckBox
    private lateinit var activeFromInput: EditText
    private lateinit var activeUntilInput: EditText
    private lateinit var manualHolidayInput: CheckBox
    private lateinit var questTitleInput: EditText
    private lateinit var questRequiredInput: CheckBox
    private lateinit var emergencyReasonInput: EditText
    private val weekdayInputs = mutableMapOf<Int, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creditStore = CreditStore(this)
        dailyQuestStore = DailyQuestStore(this)
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
        val root = UiKit.root(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 64), UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 40))
        }

        val title = UiKit.title(this, "개발자지만 난 괜찮아").apply {
            gravity = Gravity.CENTER
        }

        val question = UiKit.heading(this, "개발자이신가요?").apply {
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@MainActivity, 22), 0, UiKit.dp(this@MainActivity, 10))
        }

        val message = UiKit.body(
            this,
            "커밋과 빌드 실패를 이해하면 입장 가능."
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 28))
        }

        val terminal = UiKit.terminalBlock(
            this,
            "$ whoami\nfrontend_dev\n$ unlock --reason=ship_code\nwaiting for proof"
        )

        root.addView(title)
        root.addView(question)
        root.addView(message)
        root.addView(terminal)
        UiKit.addGap(root, 12)
        root.addView(button("예, 커밋으로 증명하겠습니다", UiKit.ButtonTone.PRIMARY) {
            developerGateStore.accept()
            dogfoodEventStore.record("developer_gate_accepted")
            showMainPrototype()
        })
        UiKit.addGap(root, 8)
        root.addView(button("아니오, 그냥 스크롤하러 왔습니다") {
            dogfoodEventStore.record("developer_gate_rejected")
            setContentView(buildRejectedGate())
        })

        return UiKit.page(this, root)
    }

    private fun buildRejectedGate(): ScrollView {
        val root = UiKit.root(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 64), UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 40))
        }

        val title = UiKit.heading(this, "403: 개발자 인증 실패").apply {
            gravity = Gravity.CENTER
        }

        val message = UiKit.body(
            this,
            "저리가. 여긴 SNS를 줄이려는 개발자 전용 던전입니다. 농담이고, 진짜 개발자라면 앱을 다시 열고 예를 누르세요."
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@MainActivity, 18), 0, UiKit.dp(this@MainActivity, 28))
        }

        root.addView(title)
        root.addView(message)
        root.addView(button("퇴장하기", UiKit.ButtonTone.DANGER) { finishAndRemoveTask() })

        return UiKit.page(this, root)
    }

    private fun buildContent(): ScrollView {
        val root = UiKit.root(this)

        val title = UiKit.title(this, "개발자지만 난 괜찮아")

        val subtitle = UiKit.body(
            this,
            "코드를 냈으면 쉬는 시간도 떳떳하게. 선택한 방해 앱만 로컬 크레딧으로 잠급니다."
        ).apply {
            setPadding(0, UiKit.dp(this@MainActivity, 8), 0, UiKit.dp(this@MainActivity, 14))
        }

        overviewText = UiKit.noticeBlock(this)
        statusText = UiKit.monoBlock(this)

        privacySummaryText = UiKit.caption(
            this,
            "Local only: foreground package names plus selected-target overlay. Full disclosure and export controls are in Monitor evidence below."
        )
        privacyDisclosureText = UiKit.caption(this)

        packageInput = UiKit.input(this, "Package names, comma separated (ex: com.instagram.android)", minLines = 2).apply {
            minLines = 2
        }

        strictModeInput = UiKit.checkbox(this, "Strict mode: hide quick test unlock")

        activeFromInput = UiKit.input(this, "Active from HH:mm (blank = 00:00)").apply {
            setSingleLine(true)
        }

        activeUntilInput = UiKit.input(this, "Active until HH:mm (blank = 24:00)").apply {
            setSingleLine(true)
        }

        manualHolidayInput = UiKit.checkbox(this, "Treat today as holiday")

        policySummaryText = UiKit.monoBlock(this)

        questTitleInput = UiKit.input(this, "Daily quest title (ex: fix Android policy UI)").apply {
            minLines = 1
        }

        questRequiredInput = UiKit.checkbox(this, "Required for free day", checked = true)

        questSummaryText = UiKit.monoBlock(this)

        emergencyReasonInput = UiKit.input(this, "Emergency unlock reason (required)").apply {
            minLines = 1
        }

        recentPackagesText = UiKit.monoBlock(this)

        dogfoodSummaryText = UiKit.monoBlock(this)

        dogfoodReviewText = UiKit.monoBlock(this)

        eventLogText = UiKit.monoBlock(this)

        addHeaderSection(root, title, subtitle)
        addPermissionSection(root)
        addTargetInputSection(root)
        addPolicySection(root)
        addQuestSection(root)
        addEmergencySection(root)
        addTargetAndCreditSection(root)
        addMonitorAndDogfoodSection(root)

        return UiKit.page(this, root)
    }

    private fun addHeaderSection(root: LinearLayout, title: TextView, subtitle: TextView) {
        root.addView(UiKit.pill(this, "LOCAL RC 0.1 / SELECTED TARGETS ONLY"))
        UiKit.addGap(root, 10)
        root.addView(title)
        root.addView(subtitle)
        root.addView(overviewText)
        UiKit.addPanelGap(root)
    }

    private fun addPermissionSection(root: LinearLayout) {
        val section = sectionPanel("Permissions", "No screen capture, no server sync, no uninstall lock. The app only watches package names you allow Android to report.")
        section.addView(privacySummaryText)
        UiKit.addGap(section, 8)
        section.addView(button("Open Usage Access Settings", UiKit.ButtonTone.PRIMARY) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        section.addView(button("Open Overlay Permission Settings") {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addTargetInputSection(root: LinearLayout) {
        val section = sectionPanel("Distracting apps", "Pick the apps you want to pause. Settings, launchers, and Commit Unlock are always left alone.")
        section.addView(packageInput)
        section.addView(strictModeInput)
        section.addView(recentPackagesText)
        section.addView(button("Save blocked packages", UiKit.ButtonTone.PRIMARY) { saveTargets() })
        section.addView(button("Use latest app I opened") { addLatestExternalPackage() })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addPolicySection(root: LinearLayout) {
        val section = sectionPanel("Policy schedule", "Weekdays, time window, free day, manual holiday, and emergency unlock are evaluated before credit.")
        weekdayInputs.clear()
        weekdayLabels.forEach { (day, label) ->
            val checkbox = UiKit.checkbox(this, label)
            weekdayInputs[day] = checkbox
            section.addView(checkbox)
        }
        section.addView(activeFromInput)
        section.addView(activeUntilInput)
        section.addView(manualHolidayInput)
        section.addView(button("Save policy schedule", UiKit.ButtonTone.PRIMARY) { savePolicy() })
        section.addView(button("Set mock free day until midnight") { setMockFreeDay() })
        section.addView(button("Clear mock free day", UiKit.ButtonTone.GHOST) {
            creditStore.setFreeUntil(null)
            dogfoodEventStore.record("free_day_cleared")
            renderState()
        })
        section.addView(policySummaryText)
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addQuestSection(root: LinearLayout) {
        val section = sectionPanel("Daily quest", "Quest plans do not unlock anything until mock proof completion is recorded.")
        section.addView(questTitleInput)
        section.addView(questRequiredInput)
        section.addView(button("Add daily quest plan", UiKit.ButtonTone.PRIMARY) { addDailyQuest() })
        section.addView(button("Complete next quest with mock proof") { completeNextQuestWithMockProof() })
        section.addView(button("Clear today's quests", UiKit.ButtonTone.GHOST) { clearDailyQuests() })
        section.addView(questSummaryText)
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addEmergencySection(root: LinearLayout) {
        val section = sectionPanel("Emergency unlock", "Short escape hatch with reason logging. Strict mode disables the longest option.")
        section.addView(emergencyReasonInput)
        section.addView(button("Emergency unlock 5 minutes") { startEmergencyUnlock(5) })
        section.addView(button("Emergency unlock 15 minutes") { startEmergencyUnlock(15) })
        section.addView(button("Emergency unlock 30 minutes") { startEmergencyUnlock(30) })
        section.addView(button("Clear emergency unlocks", UiKit.ButtonTone.GHOST) {
            emergencyUnlockStore.clear()
            dogfoodEventStore.record("emergency_unlocks_cleared")
            renderState()
        })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addTargetAndCreditSection(root: LinearLayout) {
        val section = sectionPanel("Mock credit", "Local minutes for emulator and device dogfood.")
        section.addView(button("Add 5 test minutes", UiKit.ButtonTone.PRIMARY) {
            creditStore.addMinutes(5)
            dogfoodEventStore.recordStructured(
                type = "credit_added",
                creditRemaining = creditStore.read().remainingMinutes,
                detail = "source=main minutes=5"
            )
            renderState()
        })
        section.addView(button("Spend 1 test minute") {
            creditStore.spendMinute()
            dogfoodEventStore.recordStructured(
                type = "credit_spent",
                creditRemaining = creditStore.read().remainingMinutes,
                detail = "source=main minutes=1"
            )
            renderState()
        })
        section.addView(button("Reset credit to 0", UiKit.ButtonTone.DANGER) {
            creditStore.resetCredit()
            dogfoodEventStore.recordStructured(
                type = "credit_reset",
                creditRemaining = creditStore.read().remainingMinutes,
                detail = "source=main"
            )
            renderState()
        })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addMonitorAndDogfoodSection(root: LinearLayout) {
        val section = sectionPanel("Monitor and dogfood evidence", "Foreground service status, 14-day gate data, local export, and event log.")
        section.addView(button("Start monitor service", UiKit.ButtonTone.PRIMARY) {
            monitorStateStore.setDesiredRunning(true)
            dogfoodEventStore.record("monitor_start_requested")
            startForegroundService(Intent(this, MonitorService::class.java))
            renderState()
        })
        section.addView(button("Stop monitor service", UiKit.ButtonTone.DANGER) {
            monitorStateStore.setDesiredRunning(false)
            monitorStateStore.clearHeartbeat()
            dogfoodEventStore.record("monitor_stop_requested")
            stopService(Intent(this, MonitorService::class.java))
            renderState()
        })
        section.addView(button("Refresh status") { renderState() })
        section.addView(statusText)
        section.addView(privacyDisclosureText)
        section.addView(dogfoodSummaryText)
        section.addView(dogfoodReviewText)
        section.addView(button("Share dogfood export") { shareDogfoodExport() })
        section.addView(button("Clear dogfood events", UiKit.ButtonTone.GHOST) {
            dogfoodEventStore.clear()
            renderState()
        })
        section.addView(eventLogText)
        root.addView(section)
    }

    private fun button(
        label: String,
        tone: UiKit.ButtonTone = UiKit.ButtonTone.SECONDARY,
        action: () -> Unit
    ): Button {
        return UiKit.button(this, label, tone, action)
    }

    private fun sectionPanel(title: String, subtitle: String? = null): LinearLayout {
        return UiKit.section(this, title, subtitle)
    }

    private fun saveTargets() {
        val result = TargetGuardrails.normalizeTargets(
            packageInput.text.toString().split(","),
            packageName
        )
        val targets = result.accepted

        val current = creditStore.read()
        creditStore.save(current.copy(
            blockedTargets = targets,
            strictMode = strictModeInput.isChecked,
            lastUpdatedAt = Instant.now().toString()
        ))
        dogfoodEventStore.record(
            "targets_saved",
            "count=${targets.size} rejected=${result.rejected.size} strict=${strictModeInput.isChecked}"
        )
        recordRejectedTargets(result.rejected)
        showRejectedTargetToast(result.rejected)
        renderState()
    }

    private fun savePolicy() {
        val activeFrom = TimeInputParser.normalize(activeFromInput.text.toString())
        val activeUntil = TimeInputParser.normalize(activeUntilInput.text.toString())
        if (activeFrom is TimeInputValue.Invalid || activeUntil is TimeInputValue.Invalid) {
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
            activeFrom = activeFrom.valueOrNull(),
            activeUntil = activeUntil.valueOrNull(),
            applyOnPublicHolidays = false,
            manualHolidayDate = if (manualHolidayInput.isChecked) today else null,
            timezone = timezone
        ))

        dogfoodEventStore.record(
            "policy_saved",
            "weekdays=${selectedWeekdays.joinToString(",")} from=${activeFrom.valueOrNull().orEmpty()} until=${activeUntil.valueOrNull().orEmpty()} manualHoliday=${manualHolidayInput.isChecked}"
        )
        renderState()
    }

    private fun setMockFreeDay() {
        grantFreeDayUntilMidnight("manual_mock")
        renderState()
    }

    private fun addDailyQuest() {
        val title = questTitleInput.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "Daily quest title is required", Toast.LENGTH_SHORT).show()
            dogfoodEventStore.record("daily_quest_rejected", "missing_title")
            return
        }

        val policy = policyStore.read()
        val quest = dailyQuestStore.add(
            title = title,
            required = questRequiredInput.isChecked,
            timezone = policy.timezone
        )
        dogfoodEventStore.record(
            "daily_quest_added",
            "id=${quest.id} required=${quest.required} title=${quest.title}"
        )
        questTitleInput.setText("")
        renderState()
    }

    private fun completeNextQuestWithMockProof() {
        val policy = policyStore.read()
        val quest = dailyQuestStore.completeNextWithMockProof(policy.timezone)
        if (quest == null) {
            Toast.makeText(this, "No planned quest is waiting for mock proof", Toast.LENGTH_SHORT).show()
            dogfoodEventStore.record("daily_quest_mock_proof_rejected", "no_planned_quest")
            renderState()
            return
        }

        dogfoodEventStore.record(
            "daily_quest_mock_completed",
            "id=${quest.id} required=${quest.required} title=${quest.title}"
        )

        val quests = dailyQuestStore.read(policy.timezone)
        if (DailyQuestPolicy.shouldGrantFreeDay(quests)) {
            val freeUntil = grantFreeDayUntilMidnight("daily_quest")
            dogfoodEventStore.record(
                "daily_quest_free_day_granted",
                "until=$freeUntil required_completed=${quests.count { it.required && it.status == DailyQuestStatus.COMPLETED }}"
            )
        }
        renderState()
    }

    private fun clearDailyQuests() {
        val policy = policyStore.read()
        dailyQuestStore.clearToday(policy.timezone)
        dogfoodEventStore.record("daily_quests_cleared")
        renderState()
    }

    private fun grantFreeDayUntilMidnight(source: String): String {
        val policy = policyStore.read()
        val zoneId = runCatching { ZoneId.of(policy.timezone) }.getOrDefault(ZoneId.systemDefault())
        val freeUntil = LocalDate.now(zoneId)
            .plusDays(1)
            .atStartOfDay(zoneId)
            .minusNanos(1)
            .toInstant()
            .toString()

        creditStore.setFreeUntil(freeUntil)
        dogfoodEventStore.record("free_day_set", "until=$freeUntil source=$source")
        return freeUntil
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
            .firstOrNull {
                TargetGuardrails.normalizeTargets(listOf(it), packageName).accepted.isNotEmpty()
            }

        if (latestExternalPackage == null) {
            dogfoodEventStore.record("recent_external_package_missing")
            renderState()
            return
        }

        val current = creditStore.read()
        val result = TargetGuardrails.normalizeTargets(
            current.blockedTargets.plus(latestExternalPackage),
            packageName
        )
        val nextTargets = result.accepted

        creditStore.save(current.copy(
            blockedTargets = nextTargets,
            strictMode = strictModeInput.isChecked,
            lastUpdatedAt = Instant.now().toString()
        ))
        recordRejectedTargets(result.rejected)
        dogfoodEventStore.recordStructured(
            type = "target_added",
            target = latestExternalPackage
        )
        renderState()
    }

    private fun recordRejectedTargets(rejected: List<TargetRejection>) {
        rejected.forEach { rejection ->
            dogfoodEventStore.recordStructured(
                type = "target_rejected",
                target = rejection.normalizedTarget,
                detail = "reason=${rejection.reason.code}"
            )
        }
    }

    private fun showRejectedTargetToast(rejected: List<TargetRejection>) {
        if (rejected.isEmpty()) return
        val summary = rejected
            .groupingBy { it.reason.code }
            .eachCount()
            .entries
            .joinToString(", ") { "${it.key}=${it.value}" }
        Toast.makeText(this, "Skipped unsafe targets: $summary", Toast.LENGTH_LONG).show()
    }

    private fun renderState() {
        val state = creditStore.read()
        val policy = policyStore.read()
        val activeUnlocks = emergencyUnlockStore.active()
        val quests = dailyQuestStore.read(policy.timezone)
        val dogfoodEvents = dogfoodEventStore.read()
        val usageAccessGranted = PermissionChecks.hasUsageAccess(this)
        val overlayGranted = PermissionChecks.canDrawOverlays(this)
        val notificationGranted = PermissionChecks.hasNotificationPermission(this)
        val foregroundPackage = foregroundPackageOrNull(usageAccessGranted)
        val dogfoodSummary = dogfoodEventStore.summary(dogfoodEvents)
        val dogfoodReview = DogfoodReviewEngine.analyze(dogfoodEvents, dogfoodSummary)
        val monitorRuntime = monitorStateStore.runtimeStatus(
            serviceRunning = MonitorServiceInspector.isMonitorServiceRunning(this)
        )
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
        manualHolidayInput.isChecked = policy.isManualHolidayActive()
        val recentPackages = recentExternalPackages(usageAccessGranted)

        overviewText.text = buildOverviewCopy(
            state = state,
            decision = decision,
            monitorRuntime = monitorRuntime,
            usageAccessGranted = usageAccessGranted,
            overlayGranted = overlayGranted,
            notificationGranted = notificationGranted
        )

        statusText.text = listOf(
            "Technical snapshot",
            "Credit: ${state.remainingMinutes} min / ${decision.reason.code}",
            "Monitor: ${monitorRuntime.state.code} (desired=${if (monitorRuntime.desiredRunning) "on" else "off"}, heartbeat=${PrototypeText.monitorHeartbeat(monitorRuntime)})",
            "Permissions: usage=${shortGrant(usageAccessGranted)} overlay=${shortGrant(overlayGranted)} notify=${shortGrant(notificationGranted)}",
            "Foreground: ${foregroundPackage ?: foregroundUnavailableReason(usageAccessGranted)}",
            "Targets: ${state.blockedTargets.ifEmpty { listOf("none") }.joinToString(", ")}",
            "Free until: ${state.freeUntil ?: "none"} / strict=${state.strictMode}"
        ).joinToString("\n")

        privacyDisclosureText.text = PermissionDisclosureCopy.build(
            PermissionDisclosureState(
                usageAccessGranted = usageAccessGranted,
                overlayGranted = overlayGranted,
                notificationGranted = notificationGranted,
                dogfoodEventCount = dogfoodSummary.eventCount
            )
        )

        policySummaryText.text = listOf(
            "Policy summary",
            "Decision: ${decision.reason.code} (${if (decision.allowed) "allowed" else "blocked"})",
            "Credit spend on use: ${decision.shouldSpendCredit}",
            "Active weekdays: ${policy.activeWeekdays.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "none"}",
            "Active time: ${(policy.activeFrom ?: "00:00")} - ${(policy.activeUntil ?: "24:00")}",
            "Manual holiday today: ${policy.isManualHolidayActive()}",
            "Public holiday source: not connected in local MVP",
            "Timezone: ${policy.timezone}",
            "Active emergency unlock: ${activeUnlocks.firstOrNull()?.expiresAt ?: "none"}"
        ).joinToString("\n")

        questSummaryText.text = PrototypeText.questSummary(quests, state)

        recentPackagesText.text = buildString {
            append("Recent external packages\n")
            if (recentPackages.isEmpty()) {
                append("none")
            } else {
                append(recentPackages.joinToString("\n"))
            }
        }

        dogfoodSummaryText.text = listOf(
            "Dogfood summary (last 14 days)",
            "Monitor enabled days: ${dogfoodSummary.monitorEnabledDays} / 8 target",
            "Blocked attempts: ${dogfoodSummary.blockedAttempts} / 8 target",
            "Policy blocks: ${dogfoodSummary.policyBlocks}",
            "Emergency unlocks: ${dogfoodSummary.emergencyUnlocks}",
            "Mock free days: ${dogfoodSummary.freeDays}",
            "Daily quests added: ${dogfoodSummary.dailyQuestsAdded}",
            "Daily quest mock completions: ${dogfoodSummary.dailyQuestMockCompletions}",
            "Permission failures: ${dogfoodSummary.permissionFailures}",
            "Overlay open-app actions: ${dogfoodSummary.overlayOpens}",
            "Overlay test-credit unlocks: ${dogfoodSummary.overlayCreditAdds}",
            "Overlay show failures: ${dogfoodSummary.overlayFailures}",
            "Automatic credit spends: ${dogfoodSummary.automaticCreditSpends}",
            "Manual credit changes: ${dogfoodSummary.manualCreditChanges}",
            "Stored dogfood events: ${dogfoodSummary.eventCount}"
        ).joinToString("\n")

        dogfoodReviewText.text = DogfoodReviewRenderer.render(dogfoodReview)

        eventLogText.text = buildString {
            append("Dogfood event log\n")
            val events = dogfoodEvents.take(50)
            if (events.isEmpty()) {
                append("none")
            } else {
                append(events.joinToString("\n") { event -> PrototypeText.dogfoodEvent(event) })
            }
        }
    }

    private fun recentExternalPackages(hasUsageAccess: Boolean): List<String> {
        if (!hasUsageAccess) return emptyList()
        return foregroundReader.recentForegroundPackages()
            .filter { TargetGuardrails.normalizeTargets(listOf(it), packageName).accepted.isNotEmpty() }
    }

    private fun foregroundPackageOrNull(hasUsageAccess: Boolean): String? {
        if (!hasUsageAccess) return null
        return foregroundReader.currentForegroundPackage()
    }

    private fun foregroundUnavailableReason(hasUsageAccess: Boolean): String {
        return PrototypeText.foregroundUnavailableReason(
            hasUsageAccess = hasUsageAccess
        )
    }

    private fun buildOverviewCopy(
        state: CreditState,
        decision: PolicyDecision,
        monitorRuntime: MonitorRuntimeSnapshot,
        usageAccessGranted: Boolean,
        overlayGranted: Boolean,
        notificationGranted: Boolean
    ): String {
        val permissionCount = listOf(usageAccessGranted, overlayGranted, notificationGranted).count { it }
        val nextStep = when {
            !usageAccessGranted || !overlayGranted -> "Next: finish permissions so the blocker can actually work."
            state.blockedTargets.isEmpty() -> "Next: open one distracting app, come back, then tap 'Use latest app I opened'."
            !monitorRuntime.desiredRunning -> "Next: start the monitor when you are ready to dogfood."
            state.freeUntil != null -> "Mode: free day is active. Enjoy it without negotiating with yourself."
            state.remainingMinutes > 0 -> "Mode: ${state.remainingMinutes} earned minutes are ready for selected apps."
            decision.allowed -> "Mode: allowed by ${decision.reason.code}. No credit spend right now."
            else -> "Mode: selected apps pause until you earn, add, or override credit."
        }

        return listOf(
            "Today: ${state.remainingMinutes} min left, ${state.blockedTargets.size} target${if (state.blockedTargets.size == 1) "" else "s"}",
            "Setup: $permissionCount/3 permissions, monitor ${monitorRuntime.state.code}",
            nextStep
        ).joinToString("\n")
    }

    private fun shortGrant(granted: Boolean): String {
        return if (granted) "ok" else "missing"
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
        private const val DAILY_EMERGENCY_LIMIT = 3
        private const val WEEKLY_EMERGENCY_LIMIT = 10
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
