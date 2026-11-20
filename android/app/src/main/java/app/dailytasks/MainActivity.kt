package app.dailytasks

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dailytasks.ui.AppRoot
import app.dailytasks.ui.theme.DailyTasksTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by vm.devicePrefs.collectAsStateWithLifecycle()
            val dark = when (prefs.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            DailyTasksTheme(dark = dark, dynamicColor = prefs.dynamicColor) {
                AppRoot(vm = vm, getIdToken = { googleIdToken(this) })
            }
        }
    }
}

/**
 * Google sign-in through Credential Manager. Returns a Google ID token that
 * the repository exchanges for a Firebase session. The web client id comes
 * from google-services.json (as a generated string resource).
 */
suspend fun googleIdToken(activity: Activity): String {
    val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
    require(resId != 0) { "Google sign-in is not set up in Firebase yet (no web client id)." }
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(activity.getString(resId))
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val result = CredentialManager.create(activity).getCredential(activity, request)
    return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
}
