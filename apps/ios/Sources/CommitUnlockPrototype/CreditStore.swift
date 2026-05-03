import Foundation

final class CreditStore: ObservableObject {
    @Published private(set) var state: CreditState

    private let defaults: UserDefaults
    private let key = "mobile_credit_state"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if
            let data = defaults.data(forKey: key),
            let decoded = try? JSONDecoder().decode(CreditState.self, from: data)
        {
            self.state = decoded
        } else {
            self.state = .empty
        }
    }

    func save(_ state: CreditState) {
        self.state = state
        if let data = try? JSONEncoder().encode(state) {
            defaults.set(data, forKey: key)
        }
    }

    func add(minutes: Int) {
        save(CreditState(
            remainingMinutes: max(0, state.remainingMinutes + minutes),
            blockedTargets: state.blockedTargets,
            strictMode: state.strictMode,
            lastUpdatedAt: ISO8601DateFormatter().string(from: Date())
        ))
    }

    func spendOneMinute() {
        add(minutes: -1)
    }
}
