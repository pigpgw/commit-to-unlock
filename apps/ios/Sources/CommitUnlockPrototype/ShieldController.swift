import Foundation

#if canImport(FamilyControls) && canImport(ManagedSettings)
import FamilyControls
import ManagedSettings

@MainActor
final class ShieldController: ObservableObject {
    @Published private(set) var authorizationStatus: AuthorizationStatus = .notDetermined

    private let store = ManagedSettingsStore()

    func requestAuthorization() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            authorizationStatus = AuthorizationCenter.shared.authorizationStatus
        } catch {
            authorizationStatus = AuthorizationCenter.shared.authorizationStatus
        }
    }

    func apply(selection: FamilyActivitySelection, creditState: CreditState) {
        guard creditState.remainingMinutes <= 0 else {
            clear()
            return
        }

        store.shield.applications = selection.applicationTokens
        store.shield.webDomains = selection.webDomainTokens
        store.shield.applicationCategories = ShieldSettings.ActivityCategoryPolicy.specific(selection.categoryTokens)
    }

    func clear() {
        store.clearAllSettings()
    }
}
#else
@MainActor
final class ShieldController: ObservableObject {
    @Published private(set) var authorizationStatusDescription = "FamilyControls unavailable in this build environment"

    func requestAuthorization() async {}
    func clear() {}
}
#endif
