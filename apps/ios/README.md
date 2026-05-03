# iOS Local Shield Prototype

This directory prepares the iOS prototype structure. The current machine does not have the Xcode app selected, so this is source/design scaffolding only until Xcode and a real Apple Developer team are configured.

## Required Targets

1. `CommitUnlockPrototype` iOS app target
2. `CommitUnlockDeviceActivityMonitor` extension
3. `CommitUnlockShieldConfiguration` extension
4. `CommitUnlockShieldAction` extension

Each target that uses Screen Time APIs needs the Family Controls entitlement. Request distribution access for the app and each extension bundle ID.

## Required Capabilities

- Family Controls
- App Groups, recommended group id: `group.com.commitunlock.prototype`

## Local Mock Contract

The iOS prototype uses the same local state shape as Android:

```swift
struct CreditState: Codable, Equatable {
    var remainingMinutes: Int
    var blockedTargets: [String]
    var strictMode: Bool
    var lastUpdatedAt: String
}
```

`blockedTargets` maps to serialized FamilyActivitySelection tokens in iOS, not package names.

## Intended Behavior

- Request Family Controls authorization.
- Let the user choose apps/web domains with FamilyActivityPicker.
- Save selection tokens locally.
- Apply shields when `remainingMinutes == 0`.
- Clear shields when `remainingMinutes > 0`.
- Do not connect to GitHub or the API in this sprint.
