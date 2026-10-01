package com.hevyclone.app.data

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
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
 *  - CANCELLED: user closed the sheet → do nothing
 *  - ERROR: failure — Cloud.authError carries the cause (no silent browser bounce)
 */
object GoogleSignInNative {
    const val SUCCESS = "success"
    const val NO_ACCOUNT = "no_account"
    const val CANCELLED = "cancelled"
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
                val credential = cm.getCredential(activityContext, request).credential
                val idToken: String? = when {
                    credential is GoogleIdTokenCredential -> credential.idToken
                    // some Play Services versions hand the same token back as a raw
                    // CustomCredential carrying the Google ID token type + Bundle
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    else -> null
                }
                if (idToken != null) {
                    val ok = Cloud.signInWithGoogleIdToken(idToken)
                    onDone(if (ok) SUCCESS else ERROR)
                } else {
                    Cloud.authError = "Connexion Google : type de compte inattendu (${credential.type}). Réessaie."
                    onDone(ERROR)
                }
            } catch (e: NoCredentialException) {
                onDone(NO_ACCOUNT)
            } catch (e: GetCredentialCancellationException) {
                onDone(CANCELLED)
            } catch (e: GetCredentialException) {
                Cloud.authError = "Connexion Google native a échoué (${e.javaClass.simpleName}) — réessaie dans quelques minutes."
                onDone(ERROR)
            } catch (e: GoogleIdTokenParsingException) {
                Cloud.authError = "Connexion Google : jeton illisible — réessaie."
                onDone(ERROR)
            } catch (e: Exception) {
                Cloud.authError = "Connexion Google : ${e.javaClass.simpleName} — vérifie Google Play Services."
                onDone(ERROR)
            }
        }
    }
}
