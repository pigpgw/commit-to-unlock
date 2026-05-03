# Mobile Credit Mock Contract

Sprint 1-3 use local-only credit state. Android stores this in SharedPreferences; iOS stores this in UserDefaults or an App Group container. This is the canonical minimum shape that the later API must preserve.

```ts
export interface MobileCreditState {
  remainingMinutes: number;
  blockedTargets: string[];
  strictMode: boolean;
  lastUpdatedAt: string;
}
```

## Invariants

- `remainingMinutes` is an integer minute balance and must never be negative.
- `blockedTargets` is platform-specific. Do not assume values are human-readable across platforms.
- `strictMode` means local convenience shortcuts are reduced. It does not mean tamper-proof control.
- `lastUpdatedAt` is an ISO 8601 UTC string.
- Android local dogfood automatically spends 1 minute after 60 seconds of interactive foreground use on a blocked target.

## Android Mapping

- `blockedTargets`: Android package names, for example `com.instagram.android`.
- `remainingMinutes == 0`: show overlay when a blocked package is foreground.
- `remainingMinutes > 0`: allow access.
- `remainingMinutes > 0` while a blocked target stays foreground: spend 1 minute per 60 seconds of interactive use.
- Target selection starts as manual package input. Production Android should prefer recent UsageStats-derived package suggestions over broad installed-app scanning.

## iOS Mapping

- `blockedTargets`: serialized references to selected FamilyActivitySelection tokens or selection storage keys.
- `remainingMinutes == 0`: apply ManagedSettings shields.
- `remainingMinutes > 0`: clear shields.
- iOS selected targets are opaque privacy-preserving values; the UI should not promise app names for selected targets.

## Future Server Sync

`GET /credits/today` must include these four fields. Optional server metadata such as `policyVersion`, `serverTime`, or `source` can be added later without changing this minimum contract.

## Out of Scope

- GitHub/API sync.
- iOS elapsed-usage spend before DeviceActivity validation.
- Parental controls or managed-device policy.
