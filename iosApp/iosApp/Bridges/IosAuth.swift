import AuthenticationServices
import CryptoKit
import FirebaseCore
import FirebaseAppCheck
import FirebaseAuth
import UIKit
import ComposeApp

/// Firebase Auth + App Check for iOS, with Sign in with Apple for linking the anonymous account.
final class IosAuth: NSObject, NativeAuth {
    private var appleFlow: AppleSignInFlow?

    func currentUserId() -> String? { Auth.auth().currentUser?.uid }

    func currentProvider() -> String? {
        guard let user = Auth.auth().currentUser else { return nil }
        if user.isAnonymous { return "anonymous" }
        return user.providerData.map(\.providerID).first { $0 == "apple.com" || $0 == "google.com" } ?? "anonymous"
    }

    func currentEmail() -> String? { Auth.auth().currentUser?.email }

    func signInAnonymously(callback: ResultCallback) {
        if let uid = Auth.auth().currentUser?.uid {
            callback.onResult(value: uid, error: nil)
            return
        }
        Auth.auth().signInAnonymously { result, error in
            callback.onResult(value: result?.user.uid, error: result == nil ? (error?.localizedDescription ?? "sign-in failed") : nil)
        }
    }

    func idToken(forceRefresh: Bool, callback: ResultCallback) {
        guard let user = Auth.auth().currentUser else {
            callback.onResult(value: nil, error: "not signed in")
            return
        }
        user.getIDTokenForcingRefresh(forceRefresh) { token, error in
            callback.onResult(value: token, error: token == nil ? (error?.localizedDescription ?? "no token") : nil)
        }
    }

    func appCheckToken(callback: ResultCallback) {
        AppCheck.appCheck().token(forcingRefresh: false) { token, error in
            callback.onResult(value: token?.token, error: token == nil ? (error?.localizedDescription ?? "app check failed") : nil)
        }
    }

    func supportsApple() -> Bool { true }
    // Google sign-in is offered on Android only; iOS uses Sign in with Apple (App Store guideline 4.8).
    func supportsGoogle() -> Bool { false }

    func linkGoogle(callback: ResultCallback) {
        callback.onResult(value: nil, error: "Google sign-in is not offered on iOS")
    }

    func linkApple(callback: ResultCallback) {
        let flow = AppleSignInFlow { [weak self] result in
            self?.appleFlow = nil
            switch result {
            case .failure(let error):
                let cancelled = (error as? ASAuthorizationError)?.code == .canceled
                callback.onResult(value: nil, error: cancelled ? "cancelled" : error.localizedDescription)
            case .success(let credential):
                Self.linkOrSignIn(credential, callback: callback)
            }
        }
        appleFlow = flow
        flow.start()
    }

    /// Keeps the anonymous user's data by linking; if this Apple ID is already a user, switch to it.
    private static func linkOrSignIn(_ credential: AuthCredential, callback: ResultCallback) {
        let finish: (AuthDataResult?, Error?) -> Void = { result, error in
            callback.onResult(value: result?.user.uid, error: result == nil ? (error?.localizedDescription ?? "sign-in failed") : nil)
        }
        guard let user = Auth.auth().currentUser, user.isAnonymous else {
            Auth.auth().signIn(with: credential, completion: finish)
            return
        }
        user.link(with: credential) { result, error in
            if let nsError = error as NSError?, nsError.code == AuthErrorCode.credentialAlreadyInUse.rawValue {
                let updated = nsError.userInfo[AuthErrorUserInfoUpdatedCredentialKey] as? AuthCredential ?? credential
                Auth.auth().signIn(with: updated, completion: finish)
            } else {
                finish(result, error)
            }
        }
    }

    func signOut() { try? Auth.auth().signOut() }

    func deviceId() -> String { UIDevice.current.identifierForVendor?.uuidString ?? "unknown-ios" }

    func appVersion() -> String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }
}

/// One Sign in with Apple request, with the nonce Firebase needs.
private final class AppleSignInFlow: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private let completion: (Result<AuthCredential, Error>) -> Void
    private let nonce = AppleSignInFlow.randomNonce()

    init(completion: @escaping (Result<AuthCredential, Error>) -> Void) {
        self.completion = completion
    }

    func start() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email]
        request.nonce = SHA256.hash(data: Data(nonce.utf8)).map { String(format: "%02x", $0) }.joined()
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let apple = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = apple.identityToken,
              let token = String(data: tokenData, encoding: .utf8) else {
            completion(.failure(NSError(domain: "AppleSignIn", code: 1, userInfo: [NSLocalizedDescriptionKey: "No identity token"])))
            return
        }
        completion(.success(OAuthProvider.appleCredential(withIDToken: token, rawNonce: nonce, fullName: apple.fullName)))
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        completion(.failure(error))
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }

    private static func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var generator = SystemRandomNumberGenerator()
        return String((0..<length).map { _ in charset[Int(generator.next() % UInt64(charset.count))] })
    }
}

/// App Attest on real devices (iOS 14+), DeviceCheck before that; debug provider in debug builds.
final class IchingAppCheckFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        #if DEBUG
        return AppCheckDebugProvider(app: app)
        #else
        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }
        return DeviceCheckProvider(app: app)
        #endif
    }
}
