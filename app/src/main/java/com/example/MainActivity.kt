package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.auth.AuthScreen
import com.example.data.local.AfiDatabase
import com.example.data.local.LocalAuthManager
import com.example.data.model.LocalUser
import com.example.ui.AfiMainApp
import com.example.ui.theme.AfiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AfiTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val authManager = remember(context) {
        val db = AfiDatabase.getInstance(context)
        LocalAuthManager(context, db.afiDao())
    }

    var isCheckingSession by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<LocalUser?>(null) }

    LaunchedEffect(authManager) {
        currentUser = authManager.getActiveUser()
        isCheckingSession = false
    }

    if (isCheckingSession) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (currentUser == null) {
        AuthScreen(
            authManager = authManager,
            onAuthSuccess = { user ->
                currentUser = user
            }
        )
    } else {
        val user = currentUser!!
        key(user.id) {
            AfiMainApp(
                currentUser = user,
                onSignOut = {
                    authManager.logout()
                    currentUser = null
                }
            )
        }
    }
}
