import AppTrackingTransparency
import FirebaseAnalytics
import FirebaseCrashlytics
import GoogleMobileAds
import UIKit
import UserMessagingPlatform
import ComposeApp

/// AdMob rewarded ads (Google Mobile Ads SDK 12 Swift names). Order before any ad request:
/// UMP consent form (EEA/UK only) → App Tracking Transparency prompt → start the SDK.
final class IosAds: NSObject, NativeAds, FullScreenContentDelegate {
    static let shared = IosAds()

    private var ad: RewardedAd?
    private var loading = false
    private var started = false
    private var pending: ResultCallback?
    private var earned = false

    private var adUnitId: String {
        (Bundle.main.object(forInfoDictionaryKey: "AdMobRewardedUnitId") as? String) ?? ""
    }

    /// Call once the first screen is visible.
    func gatherConsentAndStart() {
        ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters()) { [weak self] _ in
            ConsentForm.loadAndPresentIfRequired(from: nil) { _ in
                self?.requestTrackingThenStart()
            }
        }
    }

    private func requestTrackingThenStart() {
        guard ConsentInformation.shared.canRequestAds else { return }
        if #available(iOS 14.5, *) {
            ATTrackingManager.requestTrackingAuthorization { [weak self] _ in
                DispatchQueue.main.async { self?.start() }
            }
        } else {
            start()
        }
    }

    private func start() {
        guard !started else { return }
        started = true
        MobileAds.shared.start { [weak self] _ in self?.preload() }
    }

    func preload() {
        DispatchQueue.main.async { [self] in
            guard started, ad == nil, !loading, !adUnitId.isEmpty else { return }
            loading = true
            RewardedAd.load(with: adUnitId, request: Request()) { [weak self] loaded, _ in
                self?.loading = false
                self?.ad = loaded
                self?.ad?.fullScreenContentDelegate = self
            }
        }
    }

    func showRewarded(callback: ResultCallback) {
        DispatchQueue.main.async { [self] in
            guard let ad, let root = Self.topViewController() else {
                preload()
                callback.onResult(value: nil, error: "no ad loaded")
                return
            }
            self.ad = nil
            earned = false
            pending = callback
            ad.present(from: root) { [weak self] in self?.earned = true }
        }
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        pending?.onResult(value: earned ? "rewarded" : "dismissed", error: nil)
        pending = nil
        preload()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        pending?.onResult(value: nil, error: error.localizedDescription)
        pending = nil
        preload()
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

final class IosAnalytics: NSObject, NativeAnalytics {
    func logEvent(name: String, params: [String: String]) {
        Analytics.logEvent(name, parameters: params.mapValues { String($0.prefix(100)) })
    }

    func setUserId(uid: String?) {
        Analytics.setUserID(uid)
        Crashlytics.crashlytics().setUserID(uid ?? "")
    }

    func recordError(message: String) {
        Crashlytics.crashlytics().record(error: NSError(domain: "IChing", code: 0, userInfo: [NSLocalizedDescriptionKey: message]))
    }
}

/// Push needs an APNs key from the Apple Developer account (docs/RELEASE.md, section 9). Until then
/// the app hides the daily push switch.
final class IosNotifications: NSObject, NativeNotifications {
    func isSupported() -> Bool { false }
    func permissionGranted() -> Bool { false }
    func requestPermission(callback: ResultCallback) { callback.onResult(value: "denied", error: nil) }
    func setDailySubscribed(enabled: Bool, callback: ResultCallback) { callback.onResult(value: "ok", error: nil) }
    func openSystemSettings() {}
}
