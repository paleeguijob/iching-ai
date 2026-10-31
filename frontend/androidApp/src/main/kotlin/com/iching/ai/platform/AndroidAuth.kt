package com.iching.ai.platform

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Firebase Auth + App Check for Android. Google sign-in uses Credential Manager. */
class AndroidAuth(
    private val context: Context,
    private val activities: ActivityTracker,
    private val versionName: String,
) : NativeAuth {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun currentProvider(): String? {
        val user = auth.currentUser ?: return null
        if (user.isAnonymous) return "anonymous"
        return user.providerData.map { it.providerId }.firstOrNull { it == "google.com" || it == "apple.com" } ?: "anonymous"
    }

    override fun currentEmail(): String? = auth.currentUser?.email

    override fun signInAnonymously(callback: ResultCallback) {
        auth.currentUser?.let { return callback.onResult(it.uid, null) }
        auth.signInAnonymously()
            .addOnSuccessListener { callback.onResult(it.user?.uid, if (it.user == null) "no user" else null) }
            .addOnFailureListener { callback.onResult(null, it.message ?: "sign-in failed") }
    }

    override fun idToken(forceRefresh: Boolean, callback: ResultCallback) {
        val user = auth.currentUser ?: return callback.onResult(null, "not signed in")
        user.getIdToken(forceRefresh)
            .addOnSuccessListener { callback.onResult(it.token, if (it.token == null) "no token" else null) }
            .addOnFailureListener { callback.onResult(null, it.message ?: "token failed") }
    }

    override fun appCheckToken(callback: ResultCallback) {
        FirebaseAppCheck.getInstance().getAppCheckToken(false)
            .addOnSuccessListener { callback.onResult(it.token, null) }
            .addOnFailureListener { callback.onResult(null, it.message ?: "app check failed") }
    }

    override fun supportsApple(): Boolean = false
    override fun supportsGoogle(): Boolean = webClientId() != null

    override fun linkApple(callback: ResultCallback) = callback.onResult(null, "Sign in with Apple is not offered on Android")

    override fun linkGoogle(callback: ResultCallback) {
        val activity = activities.current ?: return callback.onResult(null, "no activity")
        val clientId = webClientId() ?: return callback.onResult(null, "default_web_client_id missing from google-services.json")
        scope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .build()
                val result = CredentialManager.create(activity)
                    .getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
                val google = GoogleIdTokenCredential.createFrom(result.credential.data)
                linkOrSignIn(GoogleAuthProvider.getCredential(google.idToken, null), callback)
            } catch (e: GetCredentialCancellationException) {
                callback.onResult(null, "cancelled")
            } catch (e: Exception) {
                callback.onResult(null, e.message ?: "google sign-in failed")
            }
        }
    }

    /** Keeps the anonymous user's data by linking; if this Google account is already a user, switch to it. */
    private fun linkOrSignIn(credential: AuthCredential, callback: ResultCallback) {
        val user = auth.currentUser
        val task = if (user != null && user.isAnonymous) user.linkWithCredential(credential) else auth.signInWithCredential(credential)
        task.addOnSuccessListener { callback.onResult(it.user?.uid, null) }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthUserCollisionException) {
                    auth.signInWithCredential(e.updatedCredential ?: credential)
                        .addOnSuccessListener { callback.onResult(it.user?.uid, null) }
                        .addOnFailureListener { callback.onResult(null, it.message ?: "sign-in failed") }
                } else {
                    callback.onResult(null, e.message ?: "link failed")
                }
            }
    }

    override fun signOut() = auth.signOut()

    @SuppressLint("HardwareIds")
    override fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-android"

    override fun appVersion(): String = versionName

    /** Generated by the google-services plugin from the OAuth client in google-services.json. */
    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else null
    }
}
