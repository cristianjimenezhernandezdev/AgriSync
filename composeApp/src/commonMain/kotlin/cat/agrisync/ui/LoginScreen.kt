package cat.agrisync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cat.agrisync.viewmodel.LoginViewModel

@Composable
internal fun LoginScreen(viewModel: LoginViewModel, serverInfo: String? = null) {
    val uiState by viewModel.uiState.collectAsState()
    val showEmailError = !uiState.error.isNullOrBlank() && uiState.email.isBlank()
    val showPasswordError = !uiState.error.isNullOrBlank() && uiState.password.isBlank()

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("AgriSync", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Gestio centralitzada de titulars, mòdul agrícola i mòdul ramader.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    placeholder = { Text("nom@domini.cat") },
                    singleLine = true,
                    isError = showEmailError,
                    supportingText = {
                        if (showEmailError) {
                            Text("L'email és obligatori")
                        } else {
                            Text("Introdueix l'usuari de Supabase Auth")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    placeholder = { Text("Minim 6 caracters") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = showPasswordError,
                    supportingText = {
                        if (showPasswordError) {
                            Text("La contrasenya és obligatòria")
                        } else {
                            Text("La contrasenya no es mostra a pantalla")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!uiState.error.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Button(
                    onClick = viewModel::login,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(" Verificant...")
                    } else {
                        Text("Entrar")
                    }
                }

                Text(
                    "Si el login és correcte però no entres, revisa que el teu usuari també existeixi a public.tecnic i estigui actiu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!serverInfo.isNullOrBlank()) {
                    Text(
                        "Servidor: $serverInfo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
