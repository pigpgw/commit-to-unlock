package com.commitunlock.prototype

object ForegroundPackageResolver {
    fun resolveForPolicy(
        rawForegroundPackage: String?,
        ownPackage: String,
        showingBlockedPackage: String?,
        lastResolvedPackage: String?
    ): String? {
        if (
            showingBlockedPackage != null &&
            (rawForegroundPackage == null || rawForegroundPackage == ownPackage)
        ) {
            return showingBlockedPackage
        }

        if (rawForegroundPackage == null) {
            return lastResolvedPackage
        }

        return rawForegroundPackage
    }
}
