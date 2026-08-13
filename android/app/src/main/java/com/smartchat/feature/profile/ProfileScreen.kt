package com.smartchat.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartchat.core.ui.components.ErrorMessage
import com.smartchat.core.ui.components.LoadingIndicator
import com.smartchat.repository.ProfileRepository
import com.smartchat.BuildConfig
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
@Composable
fun ProfileScreen(profileRepository: ProfileRepository) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(profileRepository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(16.dp).testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        when {
            state.isLoading -> LoadingIndicator(Modifier.weight(1f))
            state.errorMessage != null -> ErrorMessage(
                state.errorMessage.orEmpty(),
                Modifier.fillMaxWidth(),
                viewModel::loadProfile
            )
            state.user != null -> {
                val user = state.user
                Spacer(Modifier.height(20.dp))
                user?.profileImageUrl?.let { imageUrl ->

                    val fullImageUrl = if (imageUrl.startsWith("http")) {
                        imageUrl
                    } else {
                        BuildConfig.API_BASE_URL.trimEnd('/') + imageUrl
                    }

                    AsyncImage(
                        model = fullImageUrl,
                        contentDescription = "Profile image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = user?.displayName.orEmpty(),
                    onValueChange = {},
                    label = { Text("Display name") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = user?.email.orEmpty(),
                    onValueChange = {},
                    label = { Text("Email") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Profile editing is disabled because it is outside the current MVP contract.")
            }
        }
    }
}
