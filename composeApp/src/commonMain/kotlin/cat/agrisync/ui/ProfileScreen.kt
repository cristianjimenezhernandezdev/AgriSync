package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cat.agrisync.data.OficinaDto
import cat.agrisync.data.TecnicDto
import cat.agrisync.viewmodel.ProfileViewModel

@Composable
internal fun ProfileScreen(
    viewModel: ProfileViewModel,
    tecnic: TecnicDto,
    oficina: OficinaDto?,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    val currentTecnic = ui.currentTecnic

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("El meu perfil", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onBack) { Text("Tornar") }
            }

            // Card principal amb dades editables
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (ui.isEditing) {
                        // Mode edició
                        Text(
                            "Editant perfil",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = ui.nom,
                            onValueChange = viewModel::onNomChange,
                            label = { Text("Nom") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.email,
                            onValueChange = viewModel::onEmailChange,
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ui.telefon,
                            onValueChange = viewModel::onTelefonChange,
                            label = { Text("Telefon") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Camps no editables
                        ProfileRow("Rol", currentTecnic.rol ?: "tecnic")
                        ProfileRow("Oficina", oficina?.nom ?: currentTecnic.oficina_id)
                        ProfileRow("Actiu", if (currentTecnic.actiu) "Si" else "No")
                        AuditSection(
                            updatedAt = currentTecnic.updated_at,
                            updatedBy = formatActorLabel(ui.updatedByLabel, currentTecnic.updated_by)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = viewModel::saveProfile,
                                enabled = !ui.isSaving
                            ) { Text("Guardar") }
                            OutlinedButton(
                                onClick = viewModel::cancelEditing,
                                enabled = !ui.isSaving
                            ) { Text("Cancel\u00b7lar") }
                            if (ui.isSaving) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    } else {
                        // Mode visualització
                        ProfileRow("Nom", currentTecnic.nom)
                        ProfileRow("Email", currentTecnic.email ?: "\u2014")
                        ProfileRow("Telefon", currentTecnic.telefon ?: "\u2014")
                        ProfileRow("Rol", currentTecnic.rol ?: "tecnic")
                        ProfileRow("Oficina", oficina?.nom ?: currentTecnic.oficina_id)
                        ProfileRow("Actiu", if (currentTecnic.actiu) "Si" else "No")
                        ProfileRow("User ID", currentTecnic.user_id ?: "\u2014")
                        AuditSection(
                            updatedAt = currentTecnic.updated_at,
                            updatedBy = formatActorLabel(ui.updatedByLabel, currentTecnic.updated_by)
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::startEditing) {
                                Text("Editar perfil")
                            }
                            OutlinedButton(onClick = viewModel::showPasswordDialog) {
                                Text("Canviar password")
                            }
                        }
                    }
                }
            }
        }

        // Dialog canvi password
        if (ui.showPasswordDialog) {
            ChangePasswordDialog(
                newPassword = ui.newPassword,
                confirmPassword = ui.confirmPassword,
                isChanging = ui.isChangingPassword,
                onNewPasswordChange = viewModel::onNewPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onConfirm = viewModel::changePassword,
                onDismiss = viewModel::hidePasswordDialog
            )
        }
    }
}

@Composable
private fun AuditSection(updatedAt: String?, updatedBy: String) {
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    Text("Auditoria", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    ProfileRow("Ultima actualitzacio", formatTimestamp(updatedAt))
    ProfileRow("Ultim editor", updatedBy)
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ChangePasswordDialog(
    newPassword: String,
    confirmPassword: String,
    isChanging: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canviar password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nou password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                    Text(
                        "Els passwords no coincideixen",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isChanging) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isChanging) { Text("Canviar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isChanging) { Text("Cancel\u00b7lar") }
        }
    )
}

