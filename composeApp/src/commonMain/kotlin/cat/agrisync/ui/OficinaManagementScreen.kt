package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.OficinaDto
import cat.agrisync.viewmodel.OficinaManagementViewModel

@Composable
internal fun OficinaManagementScreen(
    viewModel: OficinaManagementViewModel,
    onBack: () -> Unit
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("< Tornar") }
                    Text("Gestio d'Oficines", style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = { viewModel.showCreateDialog() }) {
                    Text("+ Nova Oficina")
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                }
                ui.oficines.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hi ha oficines", style = MaterialTheme.typography.bodyLarge)
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ui.oficines, key = { it.id }) { oficina ->
                            OficinaCard(
                                oficina = oficina,
                                isEditingThis = ui.editingOficina?.id == oficina.id,
                                editNom = ui.editNom,
                                isEditing = ui.isEditing,
                                onStartEdit = { viewModel.startEdit(oficina) },
                                onCancelEdit = viewModel::cancelEdit,
                                onEditNom = viewModel::onEditNom,
                                onSave = viewModel::saveEdit,
                                onDelete = { viewModel.deleteOficina(oficina.id) }
                            )
                        }
                    }
                }
            }
        }

        // Dialog crear oficina
        if (ui.showCreateDialog) {
            AlertDialog(
                onDismissRequest = viewModel::hideCreateDialog,
                title = { Text("Crear nova oficina") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ui.newNom,
                            onValueChange = viewModel::onNewNom,
                            label = { Text("Nom de l'oficina") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (ui.isCreating) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::createOficina, enabled = !ui.isCreating) { Text("Crear") }
                },
                dismissButton = {
                    OutlinedButton(onClick = viewModel::hideCreateDialog, enabled = !ui.isCreating) { Text("Cancel\u00b7lar") }
                }
            )
        }
    }
}

@Composable
private fun OficinaCard(
    oficina: OficinaDto,
    isEditingThis: Boolean,
    editNom: String,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditNom: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isEditingThis) {
                Text("Editant oficina", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = editNom,
                    onValueChange = onEditNom,
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = !isEditing) { Text("Guardar") }
                    OutlinedButton(onClick = onCancelEdit, enabled = !isEditing) { Text("Cancel\u00b7lar") }
                    if (isEditing) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(oficina.nom, style = MaterialTheme.typography.titleMedium)
                        Text("ID: ${oficina.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onStartEdit) { Text("Editar") }
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Eliminar") }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar eliminacio") },
            text = { Text("Segur que vols eliminar l'oficina \"${oficina.nom}\"? No es pot si te tecnics assignats.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel\u00b7lar") }
            }
        )
    }
}

