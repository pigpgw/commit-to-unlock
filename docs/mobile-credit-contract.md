# Mobile Credit Mock Contract

Sprint 1-3 use local-only credit state. Android stores this in SharedPreferences; iOS stores this in UserDefaults or an App Group container. This is the canonical minimum shape that the later API must preserve.

```ts
export interface MobileCreditState {
  remainingMinutes: number;
  blockedTargets: string[];
  freeUntil?: string;
  strictMode: boolean;
  lastUpdatedAt: string;
}
```

Daily Quest is a local proof label, not an unlock checkbox:

```ts
export type MobileDailyQuestStatus = "planned" | "proof_seen" | "completed" | "rejected";

export type MobileDailyQuestProofType = "commit" | "pull_request" | "review" | "mock";

export interface MobileDailyQuest {
  id: string;
  title: string;
  required: boolean;
  proofType?: MobileDailyQuestProofType;
  proofRef?: string;
  status: MobileDailyQuestStatus;
  createdAt: string;
  completedAt?: string;
}
```

## Invariants

- `remainingMinutes` is an integer minute balance and must never be negative.
- `blockedTargets` is platform-specific. Do not assume values are human-readable across platforms.
- `freeUntil` is optional. When present and in the future, selected targets are allowed without spending credit.
- `strictMode` means local convenience shortcuts are reduced. It does not mean tamper-proof control.
- `lastUpdatedAt` is an ISO 8601 UTC string.
- Android local dogfood automatically spends 1 minute after 60 seconds of interactive foreground use on a blocked target.
- Daily Quest `planned` never unlocks by itself. Required quests must become `completed` through proof, currently Android local `mock`, before they can set `freeUntil`.

## Android Mapping

- `blockedTargets`: Android package names, for example `com.instagram.android`.
- `remainingMinutes == 0`: show overlay when a blocked package is foreground.
- `remainingMinutes > 0`: allow access.
- `freeUntil` in the future: allow access and log `free_day`-style policy reasons without spending credit.
- `remainingMinutes > 0` while a blocked target stays foreground: spend 1 minute per 60 seconds of interactive use.
- Target selection starts as manual package input. Production Android should prefer recent UsageStats-derived package suggestions over broad installed-app scanning.
- Android stores today's `MobileDailyQuest[]` locally. Completing all required quests with mock proof sets `freeUntil` to local midnight and writes dogfood events.

## iOS Mapping

- `blockedTargets`: serialized references to selected FamilyActivitySelection tokens or selection storage keys.
- `remainingMinutes == 0`: apply ManagedSettings shields.
- `remainingMinutes > 0`: clear shields.
- `freeUntil` in the future: clear shields until that timestamp, then re-evaluate.
- iOS selected targets are opaque privacy-preserving values; the UI should not promise app names for selected targets.
- iOS should keep the same Daily Quest status semantics even if proof completion is driven by a server later.

## Future Server Sync

`GET /credits/today` must include the required fields and may omit `freeUntil` when no free-day policy is active. Optional server metadata such as `policyVersion`, `serverTime`, or `source` can be added later without changing this minimum contract.

## Out of Scope

- GitHub/API sync.
- iOS elapsed-usage spend before DeviceActivity validation.
- Parental controls or managed-device policy.
