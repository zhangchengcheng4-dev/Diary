package com.vibecoding.app

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibecoding.auth.data.AuthRepository
import com.vibecoding.auth.network.FakeAuthApi
import com.vibecoding.auth.session.SessionStore
import com.vibecoding.auth.ui.AuthScreen
import com.vibecoding.auth.ui.AuthViewModel
import com.vibecoding.auth.ui.AuthViewModelFactory
import com.vibecoding.auth.ui.SessionBootstrapState
import com.vibecoding.data.repository.LocalDiaryRepository
import com.vibecoding.recording.RecordingRepository
import com.vibecoding.recording.Step9ProcessingUseCase
import com.vibecoding.ui.placeholder.PlaceholderAppRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppRoot() {
    val context = LocalContext.current.applicationContext
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            AuthRepository(
                authApi = FakeAuthApi(),
                sessionStore = SessionStore(context)
            )
        )
    )
    val state by authViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            RecordingRepository(context).recoverOrCleanupRecordingsOnAppStart()
            val pending = LocalDiaryRepository(context).findRecoverableProcessingEntries()
            val processor = Step9ProcessingUseCase(context)
            pending.distinctBy { it.entryId }.forEach { entry ->
                processor.run(entry.entryId)
            }
        }
    }

    when (state.sessionState) {
        SessionBootstrapState.CHECKING -> CircularProgressIndicator()
        SessionBootstrapState.UNAUTHENTICATED -> AuthScreen(
            state = state,
            onEmailChanged = authViewModel::onEmailChanged,
            onPasswordChanged = authViewModel::onPasswordChanged,
            onSwitchMode = authViewModel::switchMode,
            onSubmit = authViewModel::submit
        )
        SessionBootstrapState.AUTHENTICATED -> PlaceholderAppRoot(
            onLogout = authViewModel::logout
        )
    }
}
