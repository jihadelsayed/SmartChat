package com.smartchat.feature.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag("register_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = { Text("Display name") },
            enabled = !state.isLoading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            enabled = !state.isLoading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            supportingText = { Text("8+ characters with uppercase, lowercase, and a number") },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isLoading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::register,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) CircularProgressIndicator() else Text("Register")
        }
        Button(onClick = onBack, enabled = !state.isLoading) { Text("Back to login") }
    }
}
