package com.smartchat.feature.auth.login

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
import androidx.compose.material3.OutlinedButton
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
fun LoginScreen(
    viewModel: LoginViewModel,
    onRegister: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("login_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SmartChat", style = MaterialTheme.typography.headlineLarge)
        Text("University MVP", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().testTag("login_email")
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().testTag("login_password")
        )
        state.errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().testTag("login_submit")
        ) {
            if (state.isLoading) CircularProgressIndicator() else Text("Login")
        }
        OutlinedButton(
            onClick = onRegister,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create an account")
        }
    }
}
