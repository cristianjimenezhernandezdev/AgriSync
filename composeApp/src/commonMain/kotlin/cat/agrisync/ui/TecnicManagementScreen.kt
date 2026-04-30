package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cat.agrisync.data.OficinaDto
import cat.agrisync.data.TecnicDto
import cat.agrisync.viewmodel.TecnicManagementViewModel

@Composable
internal fun TecnicManagementScreen(
    viewModel: TecnicManagementViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val ui by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("< Tornar") }
                    Text("Gestio de Tecnics", style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = { viewModel.showCreateDialog() }) {
                    Text("+ Nou Tecnic")
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                ui.error != null -> Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ui.tecnics, key = { it.id }) { tecnic ->
                            TecnicCard(
                                tecnic = tecnic,
                                oficines = ui.oficines,
                                onToggleActiu = { viewModel.toggleActiu(tecnic) },
                                onOpenDetail = { onOpenDetail(tecnic.id) },
                                onResetPassword = { viewModel.showPasswordDialog(tecnic) },
                                onDelete = { viewModel.showDeleteDialog(tecnic) }
                            )
                        }
                    }
                }
            }
        }

        // Dialog crear nou tècnic
        if (ui.showCreateDialog) {
            CreateTecnicDialog(
                nom = ui.newNom,
                email = ui.newEmail,
                telefon = ui.newTelefon,
                password = ui.newPassword,
                rol = ui.newRol,
                oficinaId = ui.newOficinaId,
                oficines = ui.oficines,
                isCreating = ui.isCreating,
                onNomChange = viewModel::onNewNom,
                onEmailChange = viewModel::onNewEmail,
                onTelefonChange = viewModel::onNewTelefon,
                onPasswordChange = viewModel::onNewPassword,
                onRolChange = viewModel::onNewRol,
                onOficinaChange = viewModel::onNewOficina,
                onConfirm = viewModel::createTecnic,
                onDismiss = viewModel::hideCreateDialog
            )
        }

        // Dialog reset password
        val pwTecnic = ui.passwordTecnic
        if (ui.showPasswordDialog && pwTecnic != null) {
            ResetPasswordDialog(
                tecnicNom = pwTecnic.nom,
                tecnicEmail = pwTecnic.email ?: "-",
                password = ui.resetPassword,
                confirmPassword = ui.resetPasswordConfirm,
                isResetting = ui.isResettingPassword,
                onPasswordChange = viewModel::onResetPassword,
                onConfirmChange = viewModel::onResetPasswordConfirm,
                onConfirm = viewModel::confirmResetPassword,
                onDismiss = viewModel::hidePasswordDialog
            )
        }

        val deleteTecnic = ui.deleteTecnic
        if (ui.showDeleteDialog && deleteTecnic != null) {
            DeleteTecnicDialog(
                tecnic = deleteTecnic,
                isDeleting = ui.isDeleting,
                onConfirm = viewModel::confirmDeleteTecnic,
                onDismiss = viewModel::hideDeleteDialog
            )
        }
    }
}

@Composable
private fun TecnicCard(
    tecnic: TecnicDto,
    oficines: List<OficinaDto>,
    onToggleActiu: () -> Unit,
    onOpenDetail: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    val oficinaNom = oficines.find { it.id == tecnic.oficina_id }?.nom ?: tecnic.oficina_id

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tecnic.nom, style = MaterialTheme.typography.titleMedium)
                Text(tecnic.email ?: "-", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tecnic.telefon ?: "-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(tecnic.rol ?: "tecnic") })
                    AssistChip(onClick = {}, label = { Text(oficinaNom) })
                    if (tecnic.user_id != null) {
                        AssistChip(onClick = {}, label = { Text("Amb login") })
                    } else {
                        AssistChip(onClick = {}, label = { Text("Sense login") })
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Switch(checked = tecnic.actiu, onCheckedChange = { onToggleActiu() })
                Text(if (tecnic.actiu) "Actiu" else "Inactiu", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onOpenDetail) { Text("Detalls") }
                if (tecnic.user_id != null) {
                    TextButton(onClick = onResetPassword) { Text("Password") }
                }
                TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun CreateTecnicDialog(
    nom: String, email: String, telefon: String, password: String, rol: String, oficinaId: String,
    oficines: List<OficinaDto>, isCreating: Boolean,
    onNomChange: (String) -> Unit, onEmailChange: (String) -> Unit,
    onTelefonChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit, onRolChange: (String) -> Unit,
    onOficinaChange: (String) -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nou tecnic") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nom,
                        onValueChange = onNomChange,
                        label = { Text("Nom complet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email (sera el login)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = telefon,
                        onValueChange = onTelefonChange,
                        label = { Text("Telefon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Oficina:", style = MaterialTheme.typography.labelMedium)
                }
                items(oficines, key = { it.id }) { ofi ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = oficinaId == ofi.id, onClick = { onOficinaChange(ofi.id) })
                        Text(ofi.nom)
                    }
                }
                item {
                    Text("Rol:", style = MaterialTheme.typography.labelMedium)
                }
                items(listOf("tecnic", "oficina_manager", "admin"), key = { it }) { currentRol ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = rol == currentRol, onClick = { onRolChange(currentRol) })
                        Text(currentRol)
                    }
                }
                if (isCreating) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isCreating) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun ResetPasswordDialog(
    tecnicNom: String,
    tecnicEmail: String,
    password: String,
    confirmPassword: String,
    isResetting: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Canviar password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tecnic: $tecnicNom", style = MaterialTheme.typography.bodyMedium)
                Text("Email: $tecnicEmail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Nou password (minim 6 caracters)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmChange,
                    label = { Text("Confirmar password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        "Els passwords no coincideixen",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isResetting) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isResetting) { Text("Canviar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isResetting) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun DeleteTecnicDialog(
    tecnic: TecnicDto,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar tecnic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("S'eliminara el tecnic '${tecnic.nom}' del sistema.")
                if (tecnic.user_id != null) {
                    Text(
                        "Com que te login associat, tambe s'intentara eliminar el seu usuari de Supabase Auth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Aquest tecnic no te login, aixi que nomes s'eliminara el registre funcional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Aquesta accio es destructiva i les assignacions de titulars d'aquest tecnic tambe desapareixeran.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                if (isDeleting) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Eliminar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancel·lar") }
        }
    )
}
