package com.vibecoding.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecoding.auth.model.AuthErrorCode
import com.vibecoding.ui.placeholder.theme.PlaceholderColors

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSwitchMode: (AuthMode) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderColors.Background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("\u767b\u5f55 / \u6ce8\u518c", color = PlaceholderColors.PrimaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSwitchMode(AuthMode.LOGIN) },
                enabled = state.mode != AuthMode.LOGIN
            ) {
                Text("\u767b\u5f55")
            }
            Button(
                onClick = { onSwitchMode(AuthMode.REGISTER) },
                enabled = state.mode != AuthMode.REGISTER
            ) {
                Text("\u6ce8\u518c")
            }
        }
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("\u90ae\u7bb1") },
            singleLine = true
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("\u5bc6\u7801") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        state.errorCode?.let { code ->
            Text(errorToMessage(code), color = PlaceholderColors.Accent)
        }
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isLoading) "\u5904\u7406\u4e2d..." else if (state.mode == AuthMode.LOGIN) "\u767b\u5f55" else "\u6ce8\u518c")
        }
        Text(
            text = "\u5f00\u53d1\u6a21\u5f0f\uff1a\u4efb\u610f\u90ae\u7bb1\u5bc6\u7801\u53ef\u767b\u5f55",
            fontSize = 12.sp,
            color = PlaceholderColors.PrimaryText
        )
    }
}

private fun errorToMessage(code: AuthErrorCode): String {
    return when (code) {
        AuthErrorCode.INVALID_CREDENTIALS -> "\u90ae\u7bb1\u6216\u5bc6\u7801\u9519\u8bef"
        AuthErrorCode.USER_NOT_FOUND -> "\u8d26\u53f7\u4e0d\u5b58\u5728"
        AuthErrorCode.EMAIL_ALREADY_REGISTERED -> "\u8d26\u53f7\u5df2\u6ce8\u518c"
        AuthErrorCode.SERVER_ERROR -> "\u670d\u52a1\u5f02\u5e38"
        AuthErrorCode.NETWORK_ERROR -> "\u7f51\u7edc\u5f02\u5e38"
    }
}
