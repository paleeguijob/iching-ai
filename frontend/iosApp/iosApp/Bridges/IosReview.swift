import StoreKit
import UIKit
import ComposeApp

/// The App Store's own rating sheet. iOS shows it at most three times a year and never says whether it showed.
final class IosReview: NSObject, NativeReview {
    func requestReview(callback: ResultCallback) {
        DispatchQueue.main.async {
            let scene = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first { $0.activationState == .foregroundActive }
            guard let scene else {
                callback.onResult(value: nil, error: "no active window scene")
                return
            }
            if #available(iOS 16.0, *) {
                AppStore.requestReview(in: scene)
            } else {
                SKStoreReviewController.requestReview(in: scene)
            }
            callback.onResult(value: "ok", error: nil)
        }
    }

    /// Empty until the App Store id exists (docs/RELEASE.md section 9); Settings then hides the rate-app row.
    func storeUrls() -> [String] { [] }

    func platform() -> String { "ios" }
}
