import FirebaseAppCheck
import FirebaseCore
import RevenueCat
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // App Check must be set up before FirebaseApp.configure().
        AppCheck.setAppCheckProviderFactory(IchingAppCheckFactory())
        FirebaseApp.configure()

        let revenueCatKey = (Bundle.main.object(forInfoDictionaryKey: "RevenueCatAPIKey") as? String) ?? ""
        let hasRevenueCat = !revenueCatKey.isEmpty && !revenueCatKey.hasPrefix("$(")
        if hasRevenueCat {
            Purchases.configure(withAPIKey: revenueCatKey)
        }

        let bridges = NativeBridges(
            auth: IosAuth(),
            purchases: IosPurchases(configured: hasRevenueCat),
            ads: IosAds.shared,
            analytics: IosAnalytics(),
            notifications: IosNotifications()
        )
        MainViewControllerKt.doInitKoin(bridges: bridges)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear { IosAds.shared.gatherConsentAndStart() }
        }
    }
}
