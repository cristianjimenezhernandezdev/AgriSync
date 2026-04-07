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
import cat.agrisync.data.TitularDto
import cat.agrisync.viewmodel.TecnicDetailViewModel

@Composable
internal fun TecnicDetailScreen(
    viewModel: TecnicDetailViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    var pendingDeleteAssignacioId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.error != null -> Column(Modifier.padding(24.dp)) {
                TextButton(onClick = onBack) { Text("< Tornar") }
                Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) { Text("< Tornar a tecnics") }
                            Text("Detall Tecnic", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    // Dades del tècnic editables
                    item {
                        Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Dades del tecnic", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                                OutlinedTextField(value = ui.editNom, onValueChange = viewModel::onEditNom, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = ui.editEmail, onValueChange = viewModel::onEditEmail, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                // Selector d'oficina
                                Text("Oficina:", style = MaterialTheme.typography.labelMedium)
                                ui.oficines.forEach { ofi ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = ui.editOficinaId == ofi.id, onClick = { viewModel.onEditOficina(ofi.id) })
                                        Text(ofi.nom)
                                    }
                                }

                                // Selector de rol
                                Text("Rol:", style = MaterialTheme.typography.labelMedium)
                                listOf("tecnic", "oficina_manager", "admin").forEach { r ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = ui.editRol == r, onClick = { viewModel.onEditRol(r) })
                                        Text(r)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = viewModel::saveTecnic) { Text("Guardar canvis") }
                                }

                                // Info
                                ui.tecnic?.let { t ->
                                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                    Text("ID: ${t.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("User ID: ${t.user_id ?: "Sense login"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Actiu: ${if (t.actiu) "Si" else "No"}", style = MaterialTheme.typography.bodySmall)
                                    Text("Ultima actualitzacio: ${formatTimestamp(t.updated_at)}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Ultim editor: ${formatActorLabel(ui.updatedByLabel, t.updated_by)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (t.user_id == null) {
                                        Text(
                                            "Aquest tecnic no te compte Auth i no pot entrar a l'aplicacio fins que se li crei un login.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Canvi de password
                                    if (t.user_id != null) {
                                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                        if (ui.showPasswordField) {
                                            OutlinedTextField(
                                                value = ui.newPassword,
                                                onValueChange = viewModel::onNewPassword,
                                                label = { Text("Nou password (minim 6 caracters)") },
                                                singleLine = true,
                                                visualTransformation = PasswordVisualTransformation(),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = viewModel::changePassword) { Text("Canviar password") }
                                                OutlinedButton(onClick = viewModel::togglePasswordField) { Text("Cancel·lar") }
                                            }
                                        } else {
                                            OutlinedButton(onClick = viewModel::togglePasswordField) { Text("Canviar password") }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Assignacions
                    item {
                        Text("Assignacions de titulars", style = MaterialTheme.typography.titleMedium)
                    }

                    if (ui.assignacions.isEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Cap titular assignat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "Si aquest tecnic te rol 'tecnic', no veura titulars a la home fins que tingui alguna assignacio activa.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(ui.assignacions, key = { it.id }) { assig ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(assig.titular?.nom_rao ?: assig.titular_id, style = MaterialTheme.typography.bodyLarge)
                                        Text("NIF: ${assig.titular?.nif ?: "-"} · Scope: ${assig.scope}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { pendingDeleteAssignacioId = assig.id }) {
                                        Text("✕", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Afegir assignació
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Afegir assignacio", style = MaterialTheme.typography.titleSmall)

                                // Selector titular
                                SearchableSelectionField(
                                    items = ui.allTitulars,
                                    selectedItem = ui.allTitulars.find { it.id == ui.newTitularId },
                                    onSelect = { titular -> viewModel.onNewTitular(titular?.id ?: "") },
                                    itemLabel = { "${it.nom_rao} (${it.nif ?: "-"})" },
                                    itemSearchText = { "${it.nom_rao} ${it.nif ?: ""}" },
                                    label = "Titular",
                                    placeholder = "Cerca per nom o NIF"
                                )

                                // Selector scope
                                Text("Scope:", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("comu", "agricola", "ramader", "lectura").forEach { s ->
                                        FilterChip(
                                            selected = ui.newScope == s,
                                            onClick = { viewModel.onNewScope(s) },
                                            label = { Text(s) }
                                        )
                                    }
                                }

                                Button(onClick = viewModel::addAssignacio) {
                                    Text("Afegir")
                                }
                            }
                        }
                    }
                }
            }
        }

        val pendingAssignacio = ui.assignacions.find { it.id == pendingDeleteAssignacioId }
        if (pendingAssignacio != null) {
            AlertDialog(
                onDismissRequest = { pendingDeleteAssignacioId = null },
                title = { Text("Eliminar assignacio") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("S'eliminara l'assignacio del titular '${pendingAssignacio.titular?.nom_rao ?: pendingAssignacio.titular_id}'.")
                        Text(
                            "Aquesta accio es immediata i el tecnic pot perdre acces a aquest titular segons el seu rol i la resta d'assignacions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAssignacio(pendingAssignacio.id)
                            pendingDeleteAssignacioId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pendingDeleteAssignacioId = null }) { Text("Cancel·lar") }
                }
            )
        }
    }
}

