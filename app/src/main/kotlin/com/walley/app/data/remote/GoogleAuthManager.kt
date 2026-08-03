package com.walley.app.data.remote

import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.walley.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

/**
 * Google identity (Credential Manager) + Drive scope authorization (Play Services Authorization
 * API) for Backup & Restore. Two separate Google APIs are involved because Credential Manager
 * only establishes identity — it doesn't grant scoped Drive access; the Authorization API is
 * Google's current (non-deprecated) replacement for `GoogleSignInClient`'s scope requests.
 *
 * Requesting the `drive.appdata` scope may require interactive consent the first time, surfaced
 * as an [AuthorizationResult.hasResolution] / [AuthorizationResult.pendingIntent] pair that the UI
 * layer must launch itself (see [resultFromIntent]) — this class can't complete that alone since
 * it has no Activity to launch through.
 */
@Singleton
class GoogleAuthManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }

    /** Signs in via Credential Manager and returns the account's email, for display only. */
    suspend fun signIn(activityContext: Context): String {
        val credentialManager = CredentialManager.create(activityContext)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = credentialManager.getCredential(activityContext, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type returned by Credential Manager"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).id
    }

    /**
     * Requests (or silently renews) the `drive.appdata` scope. If [AuthorizationResult.hasResolution]
     * is true, the caller must launch [AuthorizationResult.pendingIntent] for user consent and then
     * pass the resulting [Intent] to [resultFromIntent] to obtain the final, token-bearing result.
     */
    suspend fun requestDriveAuthorization(): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        return authorizationClient.authorize(request).await()
    }

    fun resultFromIntent(intent: Intent): AuthorizationResult = authorizationClient.getAuthorizationResultFromIntent(intent)
}
