# iOS Local Shield

This directory contains the iOS source/design skeleton for Commit-to-Unlock. It is not the current runnable MVP because this machine still needs Xcode app setup, Apple Developer team configuration, and Family Controls entitlement work.

Android remains the active enforcement gate. iOS should start only after the project has real-device Android evidence or when an iOS-capable machine is ready.

## Intended User Promise

iOS should match the Android product boundary:

- the user selects apps or web domains
- local mock credit decides shield on/off
- the app does not promise whole-phone lock
- the app does not prevent uninstall
- settings, account, delete, and permission paths remain reachable

## Required Targets

See [TARGETS.md](TARGETS.md) for the detailed target plan.

| Target | Role |
| --- | --- |
| `CommitUnlockPrototype` | SwiftUI app for onboarding, authorization, picker, and mock credit controls. |
| `CommitUnlockDeviceActivityMonitor` | Future extension for schedules and usage thresholds. |
| `CommitUnlockShieldConfiguration` | Custom shield UI that explains why a target is blocked. |
| `CommitUnlockShieldAction` | Shield button handling, with strict-mode-safe actions. |

## Required Capabilities

- Family Controls
- App Groups, recommended id: `group.com.commitunlock.prototype`

Every bundle ID that imports Screen Time APIs needs the relevant capability. Distribution requires Apple approval for Family Controls entitlement on the main app and extension bundle IDs.

## Local Mock Contract

iOS uses the same concept as Android:

```swift
struct CreditState: Codable, Equatable {
    var remainingMinutes: Int
    var blockedTargets: [String]
    var strictMode: Bool
    var lastUpdatedAt: String
}
```

`blockedTargets` maps to serialized `FamilyActivitySelection` tokens on iOS, not Android package names.

## First Build Goal

When Xcode and entitlement setup are ready, implement the smallest local loop:

1. Request Family Controls authorization.
2. Present `FamilyActivityPicker`.
3. Save selected tokens locally.
4. Apply ManagedSettings shields when `remainingMinutes == 0`.
5. Clear shields when `remainingMinutes > 0`.
6. Show authorization, selection, credit, and shield state in the SwiftUI app.

Do not add GitHub, API sync, account login, payment, or parent/school mode in this iOS step.
