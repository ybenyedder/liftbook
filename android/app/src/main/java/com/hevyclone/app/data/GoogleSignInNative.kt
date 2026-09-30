package com.hevyclone.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Native Google sign-in via Android Credential Manager: shows the device's Google
 * accounts in an in-app bottom sheet — no browser involved. Exchanges the returned
 * ID token against Supabase (grant_type=id_token).
 *
 * Result codes:
 *  - SUCCESS: session established
 *  - NO_ACCOUNT: no Google account on the device → caller should fall back to email sign-up
 *  - BROWSER_FALLBACK: user cancelled or another failure → caller may fall back to the browser flow
 *  - ERROR: session failure already surfaced via Cloud.authError
 */
object GoogleSignInNative {
    const val SUCCESS = "success"
    const val NO_ACCOUNT = "no_account"
    const val BROWSER_FALLBACK = "browser_fallback"
    const val ERROR = "error"

    fun launch(activityContext: Context, scope: CoroutineScope, onDone: (String) -> Unit) {
        val cm = CredentialManager.create(activityContext)
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(Cloud.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        scope.launch {
            try {
                val result = cm.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val ok = Cloud.signInWithGoogleIdToken(credential.idToken)
                    onDone(if (ok) SUCCESS else ERROR)
                } else {
                    onDone(BROWSER_FALLBACK)
                }
            } catch (e: NoCredentialException) {
                onDone(NO_ACCOUNT)
            } catch (e: GetCredentialException) {
                onDone(BROWSER_FALLBACK)
            } catch (e: GoogleIdTokenParsingException) {
                onDone(BROWSER_FALLBACK)
            } catch (e: Exception) {
                onDone(ERROR)
            }
        }
    }
}
