package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.TerraFullDto
import cat.agrisync.data.TitularDto
import cat.agrisync.viewmodel.TerraManagementViewModel

@Composable
internal fun TerraManagementScreen(
    viewModel: TerraManagementViewModel,
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
                    Text("Gestio de Terres", style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = { viewModel.showCreateDialog() }) {
                    Text("+ Nova Terra")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Buscador + Filtre per titular
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ui.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    label = { Text("Cercar per SIGPAC, nom, NIF, telefon o CP") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                // Filtre per titular
                SearchableSelectionField(
                    items = ui.titulars,
                    selectedItem = ui.titulars.find { it.id == ui.filterTitularId },
                    onSelect = { titular -> viewModel.onFilterTitular(titular?.id) },
                    itemLabel = { "${it.nom_rao} (${it.nif ?: "-"})" },
                    itemSearchText = { "${it.nom_rao} ${it.nif ?: ""} ${it.telefon ?: ""} ${it.email ?: ""} ${it.codi_postal ?: ""}" },
                    label = "Filtrar per titular",
                    placeholder = "Tots els titulars",
                    allowClearSelection = true,
                    clearSelectionLabel = "Tots els titulars",
                    modifier = Modifier.width(280.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ui.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                }
                ui.filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hi ha terres", style = MaterialTheme.typography.bodyLarge)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ui.pageItems, key = { it.id }) { terra ->
                            TerraManagementCard(
                                terra = terra,
                                titulars = ui.titulars,
                                isEditingThis = ui.editingTerra?.id == terra.id,
                                editTitularId = ui.editTitularId,
                                editMunicipiLiteral = ui.editMunicipiLiteral,
                                editUsSigpac = ui.editUsSigpac,
                                editCultiu = ui.editCultiu,
                                editSuperficie = ui.editSuperficie,
                                editZona = ui.editZona,
                                isEditing = ui.isEditing,
                                onStartEdit = { viewModel.startEdit(terra) },
                                onCancelEdit = viewModel::cancelEdit,
                                onEditTitularId = viewModel::onEditTitularId,
                                onEditMunicipiLiteral = viewModel::onEditMunicipiLiteral,
                                onEditUsSigpac = viewModel::onEditUsSigpac,
                                onEditCultiu = viewModel::onEditCultiu,
                                onEditSuperficie = viewModel::onEditSuperficie,
                                onEditZona = viewModel::onEditZona,
                                onSave = viewModel::saveEdit,
                                onDelete = { viewModel.deleteTerra(terra.id) }
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

        // Dialog crear terra
        if (ui.showCreateDialog) {
            CreateTerraDialog(
                titulars = ui.titulars,
                titularId = ui.newTitularId,
                munCodi = ui.newMunCodi,
                poligon = ui.newPoligon,
                parcela = ui.newParcela,
                recinte = ui.newRecinte,
                municipiLiteral = ui.newMunicipiLiteral,
                usSigpac = ui.newUsSigpac,
                cultiu = ui.newCultiu,
                superficie = ui.newSuperficie,
                zona = ui.newZona,
                isCreating = ui.isCreating,
                onTitularIdChange = viewModel::onNewTitularId,
                onMunCodiChange = viewModel::onNewMunCodi,
                onPoligonChange = viewModel::onNewPoligon,
                onParcelaChange = viewModel::onNewParcela,
                onRecinteChange = viewModel::onNewRecinte,
                onMunicipiLiteralChange = viewModel::onNewMunicipiLiteral,
                onUsSigpacChange = viewModel::onNewUsSigpac,
                onCultiuChange = viewModel::onNewCultiu,
                onSuperficieChange = viewModel::onNewSuperficie,
                onZonaChange = viewModel::onNewZona,
                onConfirm = viewModel::createTerra,
                onDismiss = viewModel::hideCreateDialog
            )
        }
    }
}

@Composable
private fun TerraManagementCard(
    terra: TerraFullDto,
    titulars: List<TitularDto>,
    isEditingThis: Boolean,
    editTitularId: String,
    editMunicipiLiteral: String,
    editUsSigpac: String,
    editCultiu: String,
    editSuperficie: String,
    editZona: String,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditTitularId: (String) -> Unit,
    onEditMunicipiLiteral: (String) -> Unit,
    onEditUsSigpac: (String) -> Unit,
    onEditCultiu: (String) -> Unit,
    onEditSuperficie: (String) -> Unit,
    onEditZona: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isEditingThis) {
                Text("Editant terra", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    "SIGPAC: ${terra.codi_sigpac_complet ?: "${terra.mun_codi}:${terra.poligon}:${terra.parcela}:${terra.recinte}"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Selector titular
                SearchableSelectionField(
                    items = titulars,
                    selectedItem = titulars.find { it.id == editTitularId },
                    onSelect = { titular -> onEditTitularId(titular?.id ?: "") },
                    itemLabel = { "${it.nom_rao} (${it.nif ?: "-"})" },
                    itemSearchText = { "${it.nom_rao} ${it.nif ?: ""} ${it.telefon ?: ""} ${it.email ?: ""} ${it.codi_postal ?: ""}" },
                    label = "Titular",
                    placeholder = "Cerca per nom, NIF, telefon o CP",
                    allowClearSelection = true,
                    clearSelectionLabel = "Sense titular"
                )
                OutlinedTextField(
                    value = editMunicipiLiteral,
                    onValueChange = onEditMunicipiLiteral,
                    label = { Text("Municipi literal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editUsSigpac,
                    onValueChange = onEditUsSigpac,
                    label = { Text("Us SIGPAC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editCultiu,
                    onValueChange = onEditCultiu,
                    label = { Text("Cultiu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editSuperficie,
                    onValueChange = onEditSuperficie,
                    label = { Text("Superficie (ha)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ZonaSelector(zona = editZona, onZonaChange = onEditZona)
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
                        Text(
                            terra.codi_sigpac_complet ?: "${terra.mun_codi}:${terra.poligon}:${terra.parcela}:${terra.recinte}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Superficie: ${terra.superficie ?: 0.0} ha",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Zona: ${terra.zona} · Limit: ${formatLimit(terra.limit_kg_n_ha ?: if (terra.zona == "ZV") 170.0 else 190.0)} kg N/ha/any",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Municipi: ${terra.municipi_literal ?: "-"} · Us SIGPAC: ${terra.us_sigpac ?: "-"} · Cultiu: ${terra.cultiu ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val titularNom = terra.titular?.nom_rao ?: "Sense titular"
                        val titularNif = terra.titular?.nif ?: ""
                        Text(
                            "Titular: $titularNom ${if (titularNif.isNotBlank()) "($titularNif)" else ""}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            text = { Text("Segur que vols eliminar la terra \"${terra.codi_sigpac_complet ?: terra.id}\"?") },
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
private fun CreateTerraDialog(
    titulars: List<TitularDto>,
    titularId: String,
    munCodi: String,
    poligon: String,
    parcela: String,
    recinte: String,
    municipiLiteral: String,
    usSigpac: String,
    cultiu: String,
    superficie: String,
    zona: String,
    isCreating: Boolean,
    onTitularIdChange: (String) -> Unit,
    onMunCodiChange: (String) -> Unit,
    onPoligonChange: (String) -> Unit,
    onParcelaChange: (String) -> Unit,
    onRecinteChange: (String) -> Unit,
    onMunicipiLiteralChange: (String) -> Unit,
    onUsSigpacChange: (String) -> Unit,
    onCultiuChange: (String) -> Unit,
    onSuperficieChange: (String) -> Unit,
    onZonaChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nova terra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Selector titular
                SearchableSelectionField(
                    items = titulars,
                    selectedItem = titulars.find { it.id == titularId },
                    onSelect = { titular -> onTitularIdChange(titular?.id ?: "") },
                    itemLabel = { "${it.nom_rao} (${it.nif ?: "-"})" },
                    itemSearchText = { "${it.nom_rao} ${it.nif ?: ""} ${it.telefon ?: ""} ${it.email ?: ""} ${it.codi_postal ?: ""}" },
                    label = "Titular",
                    placeholder = "Cerca per nom, NIF, telefon o CP",
                    allowClearSelection = true,
                    clearSelectionLabel = "Sense titular"
                )

                Text("Dades SIGPAC", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = munCodi,
                    onValueChange = onMunCodiChange,
                    label = { Text("Codi municipal (5 digits, ex: 17071)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = poligon,
                        onValueChange = onPoligonChange,
                        label = { Text("Poligon") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = parcela,
                        onValueChange = onParcelaChange,
                        label = { Text("Parcela") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = recinte,
                        onValueChange = onRecinteChange,
                        label = { Text("Recinte") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = municipiLiteral,
                    onValueChange = onMunicipiLiteralChange,
                    label = { Text("Municipi literal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = usSigpac,
                    onValueChange = onUsSigpacChange,
                    label = { Text("Us SIGPAC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cultiu,
                    onValueChange = onCultiuChange,
                    label = { Text("Cultiu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = superficie,
                    onValueChange = onSuperficieChange,
                    label = { Text("Superficie (ha)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ZonaSelector(zona = zona, onZonaChange = onZonaChange)

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

@Composable
private fun ZonaSelector(
    zona: String,
    onZonaChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Zona nitrogen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = zona == "ZNV",
                onClick = { onZonaChange("ZNV") },
                label = { Text("ZNV · 190") }
            )
            FilterChip(
                selected = zona == "ZV",
                onClick = { onZonaChange("ZV") },
                label = { Text("ZV · 170") }
            )
        }
    }
}

private fun formatLimit(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return rounded.toString().replace('.', ',')
}

