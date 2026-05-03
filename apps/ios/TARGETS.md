# iOS Target Design

## CommitUnlockPrototype

Main SwiftUI app.

- Owns onboarding and mock credit controls.
- Requests Family Controls authorization.
- Presents FamilyActivityPicker.
- Writes mock credit state and selection tokens to the shared app group.
- Calls the shield controller to apply or clear local shields.

## CommitUnlockDeviceActivityMonitor

Future extension for usage thresholds and scheduled monitoring.

- Not required for the first local toggle test.
- Add after the app can apply/clear ManagedSettings shields from local credit state.

## CommitUnlockShieldConfiguration

Custom shield UI extension.

- Displays why the selected app/domain is blocked.
- Shows remaining mock credit as `0`.
- Keeps copy focused on selected-app shielding, not device-wide lock.

## CommitUnlockShieldAction

Handles shield button taps.

- Primary action opens the main app.
- Secondary action remains disabled in strict mode.
- No remote override or API call in this sprint.

## Entitlement Notes

- Development builds require Family Controls capability on every bundle ID that imports FamilyControls, DeviceActivity, ManagedSettings, or ManagedSettingsUI.
- Distribution requires requesting Family Controls entitlement approval from Apple for the main app and extensions.
