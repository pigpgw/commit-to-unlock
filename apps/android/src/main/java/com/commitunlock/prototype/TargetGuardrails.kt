package com.commitunlock.prototype

data class TargetGuardrailResult(
    val accepted: List<String>,
    val rejected: List<TargetRejection>
)

data class TargetRejection(
    val rawTarget: String,
    val normalizedTarget: String?,
    val reason: TargetRejectionReason
)

enum class TargetRejectionReason(val code: String) {
    EMPTY("empty"),
    INVALID_PACKAGE("invalid_package"),
    OWN_PACKAGE("own_package"),
    SYSTEM_CRITICAL("system_critical"),
    DUPLICATE("duplicate")
}

object TargetGuardrails {
    private val packagePattern =
        Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    private val deniedExactPackages = setOf(
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.apps.safetycenter",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home"
    )

    private val deniedPrefixes = listOf(
        "com.android.settings.",
        "com.android.systemui.",
        "com.android.permissioncontroller.",
        "com.google.android.permissioncontroller.",
        "com.google.android.gms.",
        "com.android.launcher.",
        "com.google.android.apps.nexuslauncher.",
        "com.sec.android.app.launcher.",
        "com.miui.home."
    )

    fun normalizeTargets(
        rawTargets: Iterable<String>,
        ownPackage: String
    ): TargetGuardrailResult {
        val accepted = mutableListOf<String>()
        val rejected = mutableListOf<TargetRejection>()
        val seen = mutableSetOf<String>()
        val normalizedOwnPackage = ownPackage.trim()

        rawTargets.forEach { rawTarget ->
            val target = rawTarget.trim()
            val rejectionReason = when {
                target.isEmpty() -> TargetRejectionReason.EMPTY
                !packagePattern.matches(target) -> TargetRejectionReason.INVALID_PACKAGE
                target == normalizedOwnPackage -> TargetRejectionReason.OWN_PACKAGE
                isSystemCritical(target) -> TargetRejectionReason.SYSTEM_CRITICAL
                !seen.add(target) -> TargetRejectionReason.DUPLICATE
                else -> null
            }

            if (rejectionReason == null) {
                accepted += target
            } else {
                rejected += TargetRejection(
                    rawTarget = rawTarget,
                    normalizedTarget = target.takeIf { it.isNotEmpty() },
                    reason = rejectionReason
                )
            }
        }

        return TargetGuardrailResult(
            accepted = accepted,
            rejected = rejected
        )
    }

    private fun isSystemCritical(target: String): Boolean {
        return target in deniedExactPackages || deniedPrefixes.any { target.startsWith(it) }
    }
}
