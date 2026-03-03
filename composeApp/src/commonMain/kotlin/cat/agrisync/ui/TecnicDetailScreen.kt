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
import cat.agrisync.data.TitularDto
import cat.agrisync.viewmodel.TecnicDetailViewModel

@Composable
internal fun TecnicDetailScreen(
    viewModel: TecnicDetailViewModel,
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
                                }
                            }
                        }
                    }

                    // Assignacions
                    item {
                        Text("Assignacions de titulars", style = MaterialTheme.typography.titleMedium)
                    }

                    if (ui.assignacions.isEmpty()) {
                        item { Text("Cap titular assignat", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(ui.assignacions, key = { it.id }) { assig ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(assig.titular?.nom_rao ?: assig.titular_id, style = MaterialTheme.typography.bodyLarge)
                                        Text("NIF: ${assig.titular?.nif ?: "-"} · Scope: ${assig.scope}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.deleteAssignacio(assig.id) }) {
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
                                Text("Titular:", style = MaterialTheme.typography.labelMedium)
                                TitularDropdown(
                                    titulars = ui.allTitulars,
                                    selectedId = ui.newTitularId,
                                    onSelect = viewModel::onNewTitular
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitularDropdown(
    titulars: List<TitularDto>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = titulars.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.nom_rao} (${it.nif ?: "-"})" } ?: "Selecciona titular",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            titulars.forEach { t ->
                DropdownMenuItem(
                    text = { Text("${t.nom_rao} (${t.nif ?: "-"})") },
                    onClick = { onSelect(t.id); expanded = false }
                )
            }
        }
    }
}

