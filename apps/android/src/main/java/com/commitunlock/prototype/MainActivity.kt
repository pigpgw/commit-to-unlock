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
    private lateinit var heroValueText: TextView
    private lateinit var heroMetaText: TextView
    private lateinit var focusLoopText: TextView
    private lateinit var nextActionButton: Button
    private lateinit var overviewText: TextView
    private lateinit var setupChecklistText: TextView
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

        heroValueText = UiKit.heroValue(this)
        heroMetaText = UiKit.heroMeta(this)
        focusLoopText = UiKit.heroMeta(this)
        nextActionButton = button("Continue", UiKit.ButtonTone.PRIMARY) { renderState() }
        overviewText = UiKit.infoBlock(
            this,
            textColor = UiKit.COLOR_PRIMARY_DARK,
            backgroundColor = UiKit.COLOR_PRIMARY_BG,
            strokeColor = 0xFFBFDBFE.toInt()
        )
        setupChecklistText = UiKit.infoBlock(this)
        statusText = UiKit.monoBlock(this)

        privacySummaryText = UiKit.caption(
            this,
            "Local only: foreground package names plus selected-target overlay. Full disclosure and export controls are in Monitor evidence below."
        )
        privacyDisclosureText = UiKit.caption(this)

        packageInput = UiKit.input(this, "Package names, comma/newline separated (ex: com.android.chrome)", minLines = 2).apply {
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

        policySummaryText = UiKit.infoBlock(this)

        questTitleInput = UiKit.input(this, "Daily quest title (ex: fix Android policy UI)").apply {
            minLines = 1
        }

        questRequiredInput = UiKit.checkbox(this, "Required for free day", checked = true)

        questSummaryText = UiKit.infoBlock(this)

        emergencyReasonInput = UiKit.input(this, "Emergency unlock reason (required)").apply {
            minLines = 1
        }

        recentPackagesText = UiKit.infoBlock(this)

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
        root.addView(UiKit.pill(
            this,
            "LOCAL RC 0.1 / DEVELOPER MODE",
            color = UiKit.COLOR_ACCENT,
            backgroundColor = UiKit.COLOR_ACCENT_BG,
            strokeColor = 0xFF99F6E4.toInt()
        ))
        UiKit.addGap(root, 10)
        root.addView(title)
        root.addView(subtitle)

        val hero = UiKit.heroPanel(this)
        hero.addView(UiKit.pill(
            this,
            "SHIP PROOF -> EARN TIME",
            color = 0xFF5EEAD4.toInt(),
            backgroundColor = 0xFF12313A.toInt(),
            strokeColor = 0xFF1F4D5A.toInt()
        ))
        hero.addView(heroValueText)
        hero.addView(heroMetaText)
        hero.addView(nextActionButton)
        UiKit.addGap(hero, 8)
        hero.addView(focusLoopText)
        root.addView(hero)
        root.addView(overviewText)
        UiKit.addPanelGap(root)
    }

    private fun addPermissionSection(root: LinearLayout) {
        val section = sectionPanel(
            "Setup access",
            "No screen capture, server sync, or uninstall lock. Android only reports foreground package names."
        )
        section.addView(setupChecklistText)
        section.addView(privacySummaryText)
        UiKit.addGap(section, 8)
        section.addView(button("Open Usage Access", UiKit.ButtonTone.PRIMARY) {
            openUsageAccessSettings()
        })
        section.addView(button("Open Overlay Settings") {
            openOverlaySettings()
        })
        section.addView(button("Request Notifications") {
            requestNotificationPermission()
        })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addTargetInputSection(root: LinearLayout) {
        val section = sectionPanel(
            "Target apps",
            "Choose only the apps you want to pause. Settings, launchers, and Commit Unlock stay open."
        )
        section.addView(packageInput)
        section.addView(strictModeInput)
        section.addView(recentPackagesText)
        section.addView(button("Save targets", UiKit.ButtonTone.PRIMARY) { saveTargets() })
        section.addView(button("Add latest foreground app") { addLatestExternalPackage() })
        section.addView(button("Prepare Chrome demo", UiKit.ButtonTone.SECONDARY) { prepareChromeDemo() })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addPolicySection(root: LinearLayout) {
        val section = sectionPanel(
            "Schedule and bypasses",
            "Active days, time windows, free day, manual holiday, and emergency unlock are checked before credit."
        )
        weekdayInputs.clear()
        weekdayLabels.forEach { (day, label) ->
            val checkbox = UiKit.checkbox(this, label)
            weekdayInputs[day] = checkbox
            section.addView(checkbox)
        }
        section.addView(activeFromInput)
        section.addView(activeUntilInput)
        section.addView(manualHolidayInput)
        section.addView(button("Save schedule", UiKit.ButtonTone.PRIMARY) { savePolicy() })
        section.addView(button("Free day until midnight") { setMockFreeDay() })
        section.addView(button("Clear free day", UiKit.ButtonTone.GHOST) {
            creditStore.setFreeUntil(null)
            dogfoodEventStore.record("free_day_cleared")
            renderState()
        })
        section.addView(button("Clear all bypasses", UiKit.ButtonTone.GHOST) { clearAllBypasses() })
        section.addView(policySummaryText)
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addQuestSection(root: LinearLayout) {
        val section = sectionPanel(
            "Proof quest",
            "Plans do not unlock anything until mock proof completion is recorded."
        )
        section.addView(questTitleInput)
        section.addView(questRequiredInput)
        section.addView(button("Add quest plan", UiKit.ButtonTone.PRIMARY) { addDailyQuest() })
        section.addView(button("Complete next with mock proof") { completeNextQuestWithMockProof() })
        section.addView(button("Clear today's quests", UiKit.ButtonTone.GHOST) { clearDailyQuests() })
        section.addView(questSummaryText)
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addEmergencySection(root: LinearLayout) {
        val section = sectionPanel(
            "Emergency bypass",
            "Short escape hatch with reason logging. Strict mode disables the longest option."
        )
        section.addView(emergencyReasonInput)
        section.addView(button("Unlock 5 minutes") { startEmergencyUnlock(5) })
        section.addView(button("Unlock 15 minutes") { startEmergencyUnlock(15) })
        section.addView(button("Unlock 30 minutes") { startEmergencyUnlock(30) })
        section.addView(button("Clear emergency bypasses", UiKit.ButtonTone.GHOST) {
            emergencyUnlockStore.clear()
            dogfoodEventStore.record("emergency_unlocks_cleared")
            renderState()
        })
        root.addView(section)
        UiKit.addPanelGap(root)
    }

    private fun addTargetAndCreditSection(root: LinearLayout) {
        val section = sectionPanel(
            "Test credit",
            "Local minutes for emulator and device dogfood. Capped at ${LocalCreditPolicy.MAX_LOCAL_TEST_MINUTES} minutes."
        )
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
        val section = sectionPanel(
            "Evidence and export",
            "Foreground service status, 14-day gate data, local export, and event log."
        )
        section.addView(button("Start monitor", UiKit.ButtonTone.PRIMARY) {
            startMonitorFromUi()
        })
        section.addView(button("Stop monitor", UiKit.ButtonTone.DANGER) {
            stopMonitorFromUi()
        })
        section.addView(button("Refresh status") { renderState() })
        section.addView(statusText)
        section.addView(privacyDisclosureText)
        section.addView(dogfoodSummaryText)
        section.addView(dogfoodReviewText)
        section.addView(button("Share full export") { shareDogfoodExport() })
        section.addView(button("Share redacted export") { shareDogfoodExport(redactSensitive = true) })
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
            TargetInputParser.parse(packageInput.text.toString()),
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
        if (targets.isEmpty()) {
            Toast.makeText(this, "No valid target saved. Add a package like com.android.chrome.", Toast.LENGTH_LONG).show()
        }
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
        if (selectedWeekdays.isEmpty()) {
            Toast.makeText(this, "Pick at least one active day, or the blocker will never run.", Toast.LENGTH_LONG).show()
            dogfoodEventStore.record("policy_save_rejected", "no_active_weekdays")
            renderState()
            return
        }
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

    private fun prepareChromeDemo() {
        val now = Instant.now()
        val timezone = ZoneId.systemDefault().id
        creditStore.save(
            CreditState(
                remainingMinutes = 0,
                blockedTargets = listOf(DEMO_TARGET_PACKAGE),
                freeUntil = null,
                strictMode = false,
                lastUpdatedAt = now.toString()
            )
        )
        policyStore.save(
            PolicyState(
                activeWeekdays = (1..7).toList(),
                activeFrom = null,
                activeUntil = null,
                applyOnPublicHolidays = false,
                manualHolidayDate = null,
                timezone = timezone
            )
        )
        emergencyUnlockStore.clear()
        dogfoodEventStore.recordStructured(
            type = "demo_setup_prepared",
            target = DEMO_TARGET_PACKAGE,
            policyReason = "all_days_credit_empty",
            creditRemaining = 0
        )
        Toast.makeText(this, "Chrome demo ready: all days, zero credit, no bypasses.", Toast.LENGTH_LONG).show()
        renderState()
    }

    private fun clearAllBypasses() {
        val policy = policyStore.read()
        creditStore.setFreeUntil(null)
        emergencyUnlockStore.clear()
        policyStore.save(policy.copy(manualHolidayDate = null))
        dogfoodEventStore.record("bypasses_cleared")
        Toast.makeText(this, "Free day, manual holiday, and emergency unlocks cleared.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Grant Usage Access first, then open the target app once.", Toast.LENGTH_LONG).show()
            renderState()
            return
        }

        val latestExternalPackage = foregroundReader.recentForegroundPackages()
            .firstOrNull {
                TargetGuardrails.normalizeTargets(listOf(it), packageName).accepted.isNotEmpty()
            }

        if (latestExternalPackage == null) {
            dogfoodEventStore.record("recent_external_package_missing")
            Toast.makeText(this, "No recent external package found. Open Chrome or YouTube once, then come back.", Toast.LENGTH_LONG).show()
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
        Toast.makeText(this, "Added $latestExternalPackage", Toast.LENGTH_SHORT).show()
        renderState()
    }

    private fun startMonitorFromUi() {
        val state = creditStore.read()
        val usageAccessGranted = PermissionChecks.hasUsageAccess(this)
        val overlayGranted = PermissionChecks.canDrawOverlays(this)
        val notificationGranted = PermissionChecks.hasNotificationPermission(this)
        val setup = SetupChecklist.evaluate(
            SetupChecklistState(
                usageAccessGranted = usageAccessGranted,
                overlayGranted = overlayGranted,
                notificationGranted = notificationGranted,
                blockedTargetCount = state.blockedTargets.size,
                monitorRunning = false
            )
        )

        if (!setup.canStartMonitor) {
            dogfoodEventStore.record(
                "monitor_start_rejected",
                setup.missingItems.joinToString(",") { it.name.lowercase() }
            )
            when {
                !usageAccessGranted -> {
                    Toast.makeText(this, "Usage Access is required before the monitor can start.", Toast.LENGTH_LONG).show()
                    openUsageAccessSettings()
                }
                !overlayGranted -> {
                    Toast.makeText(this, "Overlay permission is required before the blocker can show.", Toast.LENGTH_LONG).show()
                    openOverlaySettings()
                }
                state.blockedTargets.isEmpty() -> {
                    Toast.makeText(this, "Add at least one target package first.", Toast.LENGTH_LONG).show()
                }
            }
            renderState()
            return
        }

        monitorStateStore.setDesiredRunning(true)
        dogfoodEventStore.record("monitor_start_requested")
        if (!startMonitorServiceSafely()) {
            renderState()
            return
        }
        Toast.makeText(this, "Monitor started. Open a selected app with zero credit to test blocking.", Toast.LENGTH_LONG).show()
        renderState()
    }

    private fun stopMonitorFromUi() {
        monitorStateStore.setDesiredRunning(false)
        monitorStateStore.clearHeartbeat()
        dogfoodEventStore.record("monitor_stop_requested")
        runCatching {
            stopService(Intent(this, MonitorService::class.java))
        }.onFailure { error ->
            dogfoodEventStore.record("monitor_stop_failed", error.errorDetail())
            Toast.makeText(this, "Monitor stop failed. Close the app or force stop from Android settings.", Toast.LENGTH_LONG).show()
        }
        renderState()
    }

    private fun startMonitorServiceSafely(): Boolean {
        return runCatching {
            startForegroundService(Intent(this, MonitorService::class.java))
        }.onFailure { error ->
            monitorStateStore.setDesiredRunning(false)
            monitorStateStore.clearHeartbeat()
            dogfoodEventStore.record("monitor_start_failed", error.errorDetail())
            Toast.makeText(this, "Monitor could not start. Check permissions and battery/background restrictions.", Toast.LENGTH_LONG).show()
        }.isSuccess
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
        val setupChecklist = SetupChecklist.evaluate(
            SetupChecklistState(
                usageAccessGranted = usageAccessGranted,
                overlayGranted = overlayGranted,
                notificationGranted = notificationGranted,
                blockedTargetCount = state.blockedTargets.size,
                monitorRunning = monitorRuntime.state == MonitorRuntimeState.RUNNING
            )
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

        heroValueText.text = "${state.remainingMinutes} min"
        heroMetaText.text = buildHeroMetaCopy(
            state = state,
            decision = decision,
            monitorRuntime = monitorRuntime,
            setupChecklist = setupChecklist
        )
        focusLoopText.text = buildFocusLoopCopy(
            state = state,
            dogfoodSummary = dogfoodSummary,
            setupChecklist = setupChecklist
        )
        configureNextActionButton(
            state = state,
            decision = decision,
            monitorRuntime = monitorRuntime,
            usageAccessGranted = usageAccessGranted,
            overlayGranted = overlayGranted
        )

        overviewText.text = buildOverviewCopy(
            state = state,
            decision = decision,
            monitorRuntime = monitorRuntime,
            usageAccessGranted = usageAccessGranted,
            overlayGranted = overlayGranted,
            notificationGranted = notificationGranted,
            setupChecklist = setupChecklist
        )

        setupChecklistText.text = buildSetupChecklistCopy(
            setupChecklist = setupChecklist,
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
            "Policy snapshot",
            "${if (decision.allowed) "[open]" else "[paused]"} ${decision.reason.code} / spend=${decision.shouldSpendCredit}",
            "Days: ${weekdaySummary(policy.activeWeekdays)}",
            "Hours: ${(policy.activeFrom ?: "00:00")} - ${(policy.activeUntil ?: "24:00")}",
            "Bypasses: free=${state.freeUntil ?: "none"} emergency=${activeUnlocks.firstOrNull()?.expiresAt ?: "none"} holiday=${policy.isManualHolidayActive()}",
            "Timezone: ${policy.timezone}"
        ).joinToString("\n")

        questSummaryText.text = PrototypeText.questSummary(quests, state)

        recentPackagesText.text = buildString {
            append("Recently seen apps\n")
            if (recentPackages.isEmpty()) {
                append("No external package yet. Open YouTube, Chrome, or your real target once, then return.")
            } else {
                append(recentPackages.mapIndexed { index, value -> "${index + 1}. $value" }.joinToString("\n"))
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
            "Runtime failures: ${dogfoodSummary.runtimeFailures}",
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

    private fun buildHeroMetaCopy(
        state: CreditState,
        decision: PolicyDecision,
        monitorRuntime: MonitorRuntimeSnapshot,
        setupChecklist: SetupChecklistResult
    ): String {
        val mode = when {
            !setupChecklist.canStartMonitor -> "Setup not ready"
            !setupChecklist.readyForBlocking -> "Monitor not running"
            state.freeUntil != null -> "Free day active"
            state.remainingMinutes > 0 -> "Credit ready"
            decision.allowed -> "Allowed by ${decision.reason.code}"
            else -> "Selected target paused"
        }
        val targetLabel = "${state.blockedTargets.size} target${if (state.blockedTargets.size == 1) "" else "s"}"
        val blockerLabel = if (setupChecklist.readyForBlocking) "blocker ready" else "blocker pending"

        return listOf(
            mode,
            "$targetLabel / monitor ${monitorRuntime.state.code} / $blockerLabel"
        ).joinToString("\n")
    }

    private fun buildFocusLoopCopy(
        state: CreditState,
        dogfoodSummary: DogfoodSummary,
        setupChecklist: SetupChecklistResult
    ): String {
        val proofSignal = when {
            dogfoodSummary.dailyQuestMockCompletions > 0 -> "${dogfoodSummary.dailyQuestMockCompletions} proof completions logged"
            state.remainingMinutes > 0 -> "credit is waiting to be spent"
            setupChecklist.readyForBlocking -> "open a target with 0 min to test the pause"
            else -> setupChecklist.nextAction
        }

        return "Loop: proof -> credit -> selected apps\nNow: $proofSignal"
    }

    private fun configureNextActionButton(
        state: CreditState,
        decision: PolicyDecision,
        monitorRuntime: MonitorRuntimeSnapshot,
        usageAccessGranted: Boolean,
        overlayGranted: Boolean
    ) {
        val label: String
        val action: () -> Unit
        when {
            !usageAccessGranted -> {
                label = "Grant Usage Access"
                action = { openUsageAccessSettings() }
            }
            !overlayGranted -> {
                label = "Allow Overlay"
                action = { openOverlaySettings() }
            }
            state.blockedTargets.isEmpty() -> {
                label = "Prepare Chrome Demo"
                action = { prepareChromeDemo() }
            }
            monitorRuntime.state != MonitorRuntimeState.RUNNING -> {
                label = "Start Monitor"
                action = { startMonitorFromUi() }
            }
            !decision.allowed && state.remainingMinutes <= 0 -> {
                label = "Add 5 Test Minutes"
                action = {
                    creditStore.addMinutes(5)
                    dogfoodEventStore.recordStructured(
                        type = "credit_added",
                        creditRemaining = creditStore.read().remainingMinutes,
                        detail = "source=hero minutes=5"
                    )
                    renderState()
                }
            }
            else -> {
                label = "Refresh Status"
                action = { renderState() }
            }
        }

        nextActionButton.text = label
        nextActionButton.setOnClickListener { action() }
    }

    private fun buildOverviewCopy(
        state: CreditState,
        decision: PolicyDecision,
        monitorRuntime: MonitorRuntimeSnapshot,
        usageAccessGranted: Boolean,
        overlayGranted: Boolean,
        notificationGranted: Boolean,
        setupChecklist: SetupChecklistResult
    ): String {
        val permissionCount = listOf(usageAccessGranted, overlayGranted, notificationGranted).count { it }
        val nextStep = when {
            !setupChecklist.canStartMonitor -> "Next: ${setupChecklist.nextAction}"
            state.blockedTargets.isEmpty() -> "Next: open one distracting app, come back, then tap 'Add latest foreground app'."
            !monitorRuntime.desiredRunning -> "Next: start the monitor when you are ready to dogfood."
            state.freeUntil != null -> "Mode: free day is active. Enjoy it without negotiating with yourself."
            state.remainingMinutes > 0 -> "Mode: ${state.remainingMinutes} earned minutes are ready for selected apps."
            decision.reason == PolicyDecisionReason.OWN_APP -> "Mode: dashboard open. Open a selected target to test the pause screen."
            decision.allowed -> "Mode: allowed by ${decision.reason.code}. No credit spend right now."
            else -> "Mode: selected apps pause until you earn, add, or override credit."
        }

        return listOf(
            "Focus home",
            "Credit ${state.remainingMinutes} min / ${state.blockedTargets.size} target${if (state.blockedTargets.size == 1) "" else "s"}",
            "Setup $permissionCount/3 permissions / monitor ${monitorRuntime.state.code} / ${if (setupChecklist.readyForBlocking) "ready" else "not ready"}",
            nextStep
        ).joinToString("\n")
    }

    private fun buildSetupChecklistCopy(
        setupChecklist: SetupChecklistResult,
        usageAccessGranted: Boolean,
        overlayGranted: Boolean,
        notificationGranted: Boolean
    ): String {
        val missing = setupChecklist.missingItems
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.label }
            ?: "none"

        val usageLine = statusLine("Usage Access", usageAccessGranted, "foreground app detection")
        val overlayLine = statusLine("Overlay", overlayGranted, "pause screen")
        val notificationLine = statusLine("Notifications", notificationGranted, "service status")
        val targetLine = statusLine("Target package", setupChecklist.missingItems.none { it == SetupChecklistItem.BLOCKED_TARGET }, "selected apps only")
        val monitorLine = statusLine("Monitor", setupChecklist.missingItems.none { it == SetupChecklistItem.MONITOR_STOPPED }, "live blocker")

        return listOf(
            "Setup checklist",
            usageLine,
            overlayLine,
            notificationLine,
            targetLine,
            monitorLine,
            "Missing: $missing",
            "Next: ${setupChecklist.nextAction}"
        ).joinToString("\n")
    }

    private fun shortGrant(granted: Boolean): String {
        return if (granted) "ok" else "missing"
    }

    private fun statusLine(label: String, ok: Boolean, detail: String): String {
        return "${if (ok) "[ok]" else "[fix]"} $label - $detail"
    }

    private fun weekdaySummary(days: List<Int>): String {
        if (days.isEmpty()) return "none"
        val labels = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        return days.sorted().joinToString(" ") { labels.getValue(it) }
    }

    private fun shareDogfoodExport(redactSensitive: Boolean = false) {
        val export = dogfoodEventStore.exportTsv(redactSensitive = redactSensitive)
        val label = if (redactSensitive) "redacted " else ""
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/tab-separated-values"
            putExtra(Intent.EXTRA_SUBJECT, "Commit Unlock ${label}dogfood export")
            putExtra(Intent.EXTRA_TEXT, export)
        }
        runCatching {
            startActivity(Intent.createChooser(intent, "Share ${label}dogfood export"))
        }.onFailure { error ->
            dogfoodEventStore.record("dogfood_export_share_failed", "redacted=$redactSensitive ${error.errorDetail()}")
            Toast.makeText(this, "No share target opened. Use adb export from the Android README.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
            }.onFailure { error ->
                dogfoodEventStore.record("notification_permission_request_failed", error.errorDetail())
                Toast.makeText(this, "Notification permission request could not open.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Notifications do not need runtime approval on this Android version.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUsageAccessSettings(): Boolean {
        return openExternalActivity(
            intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            failureEventDetail = "usage_access",
            failureToast = "Usage Access settings could not open. Open Android Settings manually."
        )
    }

    private fun openOverlaySettings(): Boolean {
        return openExternalActivity(
            intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ),
            failureEventDetail = "overlay",
            failureToast = "Overlay settings could not open. Open Android Settings manually."
        )
    }

    private fun openExternalActivity(
        intent: Intent,
        failureEventDetail: String,
        failureToast: String
    ): Boolean {
        return runCatching {
            startActivity(intent)
        }.onFailure { error ->
            dogfoodEventStore.record("settings_open_failed", "$failureEventDetail ${error.errorDetail()}")
            Toast.makeText(this, failureToast, Toast.LENGTH_LONG).show()
        }.isSuccess
    }

    private fun Throwable.errorDetail(): String {
        return "${javaClass.simpleName}:${message.orEmpty()}"
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
        private const val DEMO_TARGET_PACKAGE = "com.android.chrome"
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
