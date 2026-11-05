package za.co.rbi.st10448886.stressless

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

// This is the OAuth "Web client" ID from google-services.json (client_type: 3).
// It is not a secret — Google's own docs confirm this ID is safe to ship in
// app code for installed/mobile apps. If you ever regenerate the Firebase
// project or change the SHA-1 fingerprint, update this constant to match
// the new "client_type": 3 entry in google-services.json.
private const val GOOGLE_WEB_CLIENT_ID =
    "225144372421-ejhmc2olsu45r312h57iuu885it1nrlr.apps.googleusercontent.com"

/**
 * LoginScreen — email/password sign-in plus Google Sign-In (SSO).
 * On success, TaskRepository.login()/signInWithGoogleCredential() authenticate
 * against Firebase Auth; the caller (MainActivity's nav host) then pulls the
 * user's tasks from Firestore and navigates to the dashboard.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoToRegister: () -> Unit) {
    val language = TaskRepository.language.value
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Holds the last error message shown to the user (null = no error)
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ---------- Google Sign-In (SSO) setup ----------
    // Built once per composition; requests an ID token (for Firebase auth)
    // and the user's email.
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Handles the result of the Google account picker activity
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                // Exchange the Google ID token for a Firebase session
                loading = true
                scope.launch {
                    val result = TaskRepository.signInWithGoogleCredential(idToken)
                    loading = false
                    result.onSuccess { onLoginSuccess() }.onFailure { error = it.message }
                }
            } else {
                error = "Google sign-in did not return a token"
            }
        } catch (e: ApiException) {
            error = "Google sign-in failed (code ${e.statusCode})"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(Strings.tr("welcome_back", language), style = MaterialTheme.typography.headlineMedium)
        Text(Strings.tr("login_continue", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text(Strings.tr("email", language)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text(Strings.tr("password", language)) },
            // Masks the password as dots — never shown in plain text on screen
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // Inline error message, shown only after a failed attempt
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                error = null
                loading = true
                scope.launch {
                    // Delegates to Firebase Auth via TaskRepository; the raw
                    // password is sent once over HTTPS and never stored locally.
                    val result = TaskRepository.login(email.trim(), password)
                    loading = false
                    result.onSuccess { onLoginSuccess() }.onFailure { error = it.message }
                }
            },
            // Disabled while a request is in flight or fields are empty,
            // to prevent duplicate submissions / crashes on blank input.
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(Strings.tr("login", language))
        }

        Spacer(Modifier.height(16.dp))
        Text(Strings.tr("or_continue_with", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                error = null
                googleLauncher.launch(googleSignInClient.signInIntent)
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Google") }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onGoToRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(Strings.tr("no_account", language))
        }
    }
}