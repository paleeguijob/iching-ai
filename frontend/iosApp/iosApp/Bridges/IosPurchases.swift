import Foundation
import RevenueCat
import ComposeApp

/// RevenueCat on iOS. Products come from the *current* offering: Monthly, Annual and a custom
/// package for the coin pack (product id containing "coins", e.g. iching_coins_10).
final class IosPurchases: NSObject, NativePurchases {
    private let configured: Bool

    init(configured: Bool) {
        self.configured = configured
    }

    private func notConfigured(_ callback: ResultCallback) {
        callback.onResult(value: nil, error: "RevenueCat is not configured (REVENUECAT_IOS_KEY)")
    }

    func logIn(uid: String, callback: ResultCallback) {
        guard configured else { return notConfigured(callback) }
        Purchases.shared.logIn(uid) { _, _, error in
            callback.onResult(value: error == nil ? "ok" : nil, error: error?.localizedDescription)
        }
    }

    func offerings(callback: ResultCallback) {
        guard configured else { return notConfigured(callback) }
        Purchases.shared.getOfferings { offerings, error in
            guard let packages = offerings?.current?.availablePackages else {
                callback.onResult(value: nil, error: error?.localizedDescription ?? "no current offering")
                return
            }
            let items: [[String: Any]] = packages.compactMap { pkg in
                let product = pkg.storeProduct
                let kind: String
                switch pkg.packageType {
                case .monthly: kind = "monthly"
                case .annual: kind = "yearly"
                default:
                    guard product.productIdentifier.contains("coins") else { return nil }
                    kind = "coins"
                }
                var trial: Any = NSNull()
                if let intro = product.introductoryDiscount, intro.paymentMode == .freeTrial {
                    trial = "ทดลองฟรี \(Self.days(intro.subscriptionPeriod)) วัน"
                }
                return [
                    "packageId": pkg.identifier,
                    "productId": product.productIdentifier,
                    "kind": kind,
                    "title": product.localizedTitle,
                    "price": product.localizedPriceString,
                    "period": kind == "monthly" ? "เดือน" : (kind == "yearly" ? "ปี" : NSNull()),
                    "trial": trial,
                ]
            }
            let data = (try? JSONSerialization.data(withJSONObject: items)) ?? Data("[]".utf8)
            callback.onResult(value: String(data: data, encoding: .utf8), error: nil)
        }
    }

    private static func days(_ period: SubscriptionPeriod) -> Int {
        switch period.unit {
        case .day: return period.value
        case .week: return period.value * 7
        case .month: return period.value * 30
        case .year: return period.value * 365
        @unknown default: return period.value
        }
    }

    func purchase(packageId: String, callback: ResultCallback) {
        guard configured else { return notConfigured(callback) }
        Purchases.shared.getOfferings { offerings, error in
            guard let pkg = offerings?.current?.availablePackages.first(where: { $0.identifier == packageId }) else {
                callback.onResult(value: nil, error: error?.localizedDescription ?? "package not found")
                return
            }
            Purchases.shared.purchase(package: pkg) { _, _, error, userCancelled in
                if userCancelled {
                    callback.onResult(value: "cancelled", error: nil)
                } else if let error {
                    callback.onResult(value: nil, error: error.localizedDescription)
                } else {
                    callback.onResult(value: "purchased", error: nil)
                }
            }
        }
    }

    func restore(callback: ResultCallback) {
        guard configured else { return notConfigured(callback) }
        Purchases.shared.restorePurchases { _, error in
            callback.onResult(value: error == nil ? "ok" : nil, error: error?.localizedDescription)
        }
    }

    func manageSubscriptionsUrl() -> String { "https://apps.apple.com/account/subscriptions" }

    func logOut() {
        guard configured else { return }
        Purchases.shared.logOut { _, _ in }
    }
}
