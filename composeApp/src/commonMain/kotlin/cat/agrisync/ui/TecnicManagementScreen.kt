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
                                onOpenDetail = { onOpenDetail(tecnic.id) }
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
                password = ui.newPassword,
                rol = ui.newRol,
                oficinaId = ui.newOficinaId,
                oficines = ui.oficines,
                isCreating = ui.isCreating,
                onNomChange = viewModel::onNewNom,
                onEmailChange = viewModel::onNewEmail,
                onPasswordChange = viewModel::onNewPassword,
                onRolChange = viewModel::onNewRol,
                onOficinaChange = viewModel::onNewOficina,
                onConfirm = viewModel::createTecnic,
                onDismiss = viewModel::hideCreateDialog
            )
        }
    }
}

@Composable
private fun TecnicCard(
    tecnic: TecnicDto,
    oficines: List<OficinaDto>,
    onToggleActiu: () -> Unit,
    onOpenDetail: () -> Unit
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(tecnic.rol ?: "tecnic") })
                    AssistChip(onClick = {}, label = { Text(oficinaNom) })
                    if (tecnic.user_id != null) {
                        AssistChip(onClick = {}, label = { Text("Amb login") })
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Switch(checked = tecnic.actiu, onCheckedChange = { onToggleActiu() })
                Text(if (tecnic.actiu) "Actiu" else "Inactiu", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onOpenDetail) { Text("Detalls") }
            }
        }
    }
}

@Composable
private fun CreateTecnicDialog(
    nom: String, email: String, password: String, rol: String, oficinaId: String,
    oficines: List<OficinaDto>, isCreating: Boolean,
    onNomChange: (String) -> Unit, onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit, onRolChange: (String) -> Unit,
    onOficinaChange: (String) -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nou tecnic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nom, onValueChange = onNomChange, label = { Text("Nom complet") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("Email (sera el login)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

                // Selector d'oficina
                Text("Oficina:", style = MaterialTheme.typography.labelMedium)
                oficines.forEach { ofi ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = oficinaId == ofi.id, onClick = { onOficinaChange(ofi.id) })
                        Text(ofi.nom)
                    }
                }

                // Selector de rol
                Text("Rol:", style = MaterialTheme.typography.labelMedium)
                listOf("tecnic", "oficina_manager", "admin").forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = rol == r, onClick = { onRolChange(r) })
                        Text(r)
                    }
                }

                if (isCreating) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
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

