package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.TitularDto
import cat.agrisync.viewmodel.TitularManagementViewModel

@Composable
internal fun TitularManagementScreen(
    viewModel: TitularManagementViewModel,
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
                    Text("Gestio de Titulars", style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = { viewModel.showCreateDialog() }) {
                    Text("+ Nou Titular")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Buscador
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Cercar per NIF o nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                }
                ui.filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hi ha titulars", style = MaterialTheme.typography.bodyLarge)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ui.pageItems, key = { it.id }) { titular ->
                            TitularManagementCard(
                                titular = titular,
                                isEditingThis = ui.editingTitular?.id == titular.id,
                                editNom = ui.editNom,
                                editNif = ui.editNif,
                                isEditing = ui.isEditing,
                                onStartEdit = { viewModel.startEdit(titular) },
                                onCancelEdit = viewModel::cancelEdit,
                                onEditNom = viewModel::onEditNom,
                                onEditNif = viewModel::onEditNif,
                                onSave = viewModel::saveEdit,
                                onDelete = { viewModel.deleteTitular(titular.id) },
                                onShare = { viewModel.openShareDialog(titular) }
                            )
                        }
                    }
                    // Paginació
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = viewModel::prevPage, enabled = ui.currentPage > 0) {
                            Text("< Anterior")
                        }
                        Text(" Pag. ${ui.currentPage + 1}/${ui.totalPages} ")
                        TextButton(onClick = viewModel::nextPage, enabled = ui.currentPage + 1 < ui.totalPages) {
                            Text("Seguent >")
                        }
                    }
                }
            }
        }

        // Dialog crear titular
        if (ui.showCreateDialog) {
            CreateTitularDialog(
                nom = ui.newNom,
                nif = ui.newNif,
                isCreating = ui.isCreating,
                onNomChange = viewModel::onNewNom,
                onNifChange = viewModel::onNewNif,
                onConfirm = viewModel::createTitular,
                onDismiss = viewModel::hideCreateDialog
            )
        }

        val shareTargetTitular = ui.shareTargetTitular
        if (ui.showShareDialog && shareTargetTitular != null) {
            ShareTitularDialog(
                titular = shareTargetTitular,
                oficines = ui.oficines,
                officeShares = ui.officeShares,
                selectedOficinaId = ui.newShareOficinaId,
                selectedScope = ui.newShareScope,
                isSaving = ui.isSharing,
                onSelectOficina = viewModel::onNewShareOficina,
                onSelectScope = viewModel::onNewShareScope,
                onConfirm = viewModel::createOfficeShare,
                onDeleteShare = viewModel::deleteOfficeShare,
                onDismiss = viewModel::closeShareDialog
            )
        }
    }
}

@Composable
private fun TitularManagementCard(
    titular: TitularDto,
    isEditingThis: Boolean,
    editNom: String,
    editNif: String,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditNom: (String) -> Unit,
    onEditNif: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isEditingThis) {
                Text("Editant titular", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = editNom,
                    onValueChange = onEditNom,
                    label = { Text("Nom / Rao social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editNif,
                    onValueChange = onEditNif,
                    label = { Text("NIF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = !isEditing) { Text("Guardar") }
                    OutlinedButton(onClick = onCancelEdit, enabled = !isEditing) { Text("Cancel·lar") }
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
                        Text(titular.nom_rao, style = MaterialTheme.typography.titleMedium)
                        Text("NIF: ${titular.nif ?: "-"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (titular.updated_at != null) {
                            Text(
                                "Ultima edicio: ${titular.updated_at.replace("T", " ").take(16)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onStartEdit) { Text("Editar") }
                        TextButton(onClick = onShare) { Text("Compartir") }
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
            text = { Text("Segur que vols eliminar el titular \"${titular.nom_rao}\"? Aquesta accio no es pot desfer.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel·lar") }
            }
        )
    }
}

@Composable
private fun ShareTitularDialog(
    titular: TitularDto,
    oficines: List<cat.agrisync.data.OficinaDto>,
    officeShares: List<cat.agrisync.data.OficinaTitularCompartitDto>,
    selectedOficinaId: String,
    selectedScope: String,
    isSaving: Boolean,
    onSelectOficina: (String) -> Unit,
    onSelectScope: (String) -> Unit,
    onConfirm: () -> Unit,
    onDeleteShare: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir titular") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Titular: ${titular.nom_rao}")
                Text(
                    "Comparteix la part agricola, ramadera o comuna amb una altra oficina. Despres, el manager receptor podra assignar els seus tecnics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (officeShares.isEmpty()) {
                    Text("Encara no hi ha comparticions actives.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    officeShares.forEach { share ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(share.oficina?.nom ?: share.oficina_id)
                                    Text("Scope compartit: ${share.scope}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(
                                    onClick = { onDeleteShare(share.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("Treure") }
                            }
                        }
                    }
                }

                Text("Nova comparticio", style = MaterialTheme.typography.titleSmall)
                if (oficines.isEmpty()) {
                    Text("No hi ha mes oficines disponibles per compartir aquest titular.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    SearchableSelectionField(
                        items = oficines,
                        selectedItem = oficines.find { it.id == selectedOficinaId },
                        onSelect = { oficina -> onSelectOficina(oficina?.id ?: "") },
                        itemLabel = { it.nom },
                        itemSearchText = { it.nom },
                        label = "Oficina receptora",
                        placeholder = "Cerca una oficina"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("lectura", "agricola", "ramader", "comu").forEach { scope ->
                            FilterChip(
                                selected = selectedScope == scope,
                                onClick = { onSelectScope(scope) },
                                label = { Text(scope) }
                            )
                        }
                    }
                    if (isSaving) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving && oficines.isNotEmpty() && selectedOficinaId.isNotBlank()
            ) { Text("Compartir") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) { Text("Tancar") }
        }
    )
}

@Composable
private fun CreateTitularDialog(
    nom: String,
    nif: String,
    isCreating: Boolean,
    onNomChange: (String) -> Unit,
    onNifChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nou titular") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = onNomChange,
                    label = { Text("Nom / Rao social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nif,
                    onValueChange = onNifChange,
                    label = { Text("NIF (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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

