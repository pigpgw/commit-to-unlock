import Foundation

struct CreditState: Codable, Equatable {
    var remainingMinutes: Int
    var blockedTargets: [String]
    var strictMode: Bool
    var lastUpdatedAt: String

    static let empty = CreditState(
        remainingMinutes: 0,
        blockedTargets: [],
        strictMode: false,
        lastUpdatedAt: ISO8601DateFormatter().string(from: Date())
    )
}
