import SwiftUI

struct ContentView: View {
    @StateObject private var creditStore = CreditStore()
    @StateObject private var shieldController = ShieldController()

    var body: some View {
        NavigationStack {
            Form {
                Section("Mock Credit") {
                    LabeledContent("Remaining", value: "\(creditStore.state.remainingMinutes) minutes")
                    Button("Add 5 test minutes") {
                        creditStore.add(minutes: 5)
                    }
                    Button("Spend 1 test minute") {
                        creditStore.spendOneMinute()
                    }
                }

                Section("Family Controls") {
                    Button("Request Authorization") {
                        Task { await shieldController.requestAuthorization() }
                    }
                    Button("Clear Shields") {
                        shieldController.clear()
                    }
                }
            }
            .navigationTitle("Commit Unlock")
        }
    }
}
