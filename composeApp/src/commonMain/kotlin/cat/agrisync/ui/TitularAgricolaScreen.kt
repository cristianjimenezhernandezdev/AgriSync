package cat.agrisync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.AplicacioFertilitzantDto
import cat.agrisync.data.TerraDto
import cat.agrisync.data.TitularDto
import cat.agrisync.viewmodel.TitularAgricolaViewModel

@Composable
internal fun TitularAgricolaScreen(
    viewModel: TitularAgricolaViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    var showCreateTerraDialog by remember { mutableStateOf(false) }
    var showCreateAplicacioDialog by remember { mutableStateOf(false) }
    var pendingDeleteTerraId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteAplicacioId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.saveMessage) {
        ui.saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.error != null -> ErrorBlock(ui.error ?: "Error", onBack)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) { Text("< Tornar") }
                            Text("Modul Agricola", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    item {
                        ui.titular?.let { titular ->
                            EditableTitularCard(
                                titular = titular,
                                actorLabel = ui.actorLabels[titular.updated_by]
                            ) { nif, nom ->
                                viewModel.updateTitular(nif, nom)
                            }
                        }
                    }

                    item {
                        CampaignSelectorCard(
                            selectedCampanya = ui.selectedCampanya,
                            availableCampanyes = ui.availableCampanyes,
                            onSelect = viewModel::onSelectCampanya
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Terres",
                            description = "Parcel·les i recintes associats al titular.",
                            actionLabel = "+ Nova Terra",
                            onAction = { showCreateTerraDialog = true }
                        )
                    }
                    if (ui.terres.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "Encara no hi ha terres",
                                message = "Dona d'alta una terra des d'aquest mateix mòdul per començar a treballar la part agrícola."
                            )
                        }
                    } else {
                        items(ui.terres, key = { it.id }) { terra ->
                            EditableTerraCard(
                                terra = terra,
                                appliedKgN = ui.appliedKgNForTerra(terra.id),
                                selectedCampanya = ui.selectedCampanya,
                                actorLabel = ui.actorLabels[terra.updated_by],
                                onSave = { superficie, zona, municipiLiteral, usSigpac, cultiu ->
                                    viewModel.updateTerra(terra.id, superficie, zona, municipiLiteral, usSigpac, cultiu)
                                },
                                onDelete = { pendingDeleteTerraId = terra.id }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        SectionHeader(
                            title = "Aplicacions fertilitzants",
                            description = "Registres d'aplicació de nitrogen vinculats a la DAN.",
                            actionLabel = "+ Nova Aplicacio",
                            onAction = { showCreateAplicacioDialog = true }
                        )
                    }
                    if (ui.aplicacions.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "Encara no hi ha aplicacions",
                                message = "Quan tinguis terres disponibles, pots registrar des d'aquí les aplicacions de fertilitzants del titular."
                            )
                        }
                    } else {
                        items(ui.aplicacions, key = { it.id }) { app ->
                            EditableAplicacioCard(
                                app = app,
                                terres = ui.terres,
                                selectedCampanya = ui.selectedCampanya,
                                currentAppliedKgNForTerra = ui.appliedKgNForTerra(app.terra_id ?: ""),
                                actorLabel = ui.actorLabels[app.updated_by],
                                onSave = { data, kgN, tipus, procedencia, volum, kgNM3 ->
                                    viewModel.updateAplicacio(app.id, data, kgN, tipus, procedencia, volum, kgNM3)
                                },
                                onDelete = { pendingDeleteAplicacioId = app.id }
                            )
                        }
                    }
                }
            }
        }

        if (showCreateTerraDialog) {
            CreateTerraDialog(
                onConfirm = { munCodi, poligon, parcela, recinte, superficie, zona, municipiLiteral, usSigpac, cultiu ->
                    if (viewModel.createTerra(munCodi, poligon, parcela, recinte, superficie, zona, municipiLiteral, usSigpac, cultiu)) {
                        showCreateTerraDialog = false
                    }
                },
                onDismiss = { showCreateTerraDialog = false }
            )
        }

        if (showCreateAplicacioDialog) {
            CreateAplicacioDialog(
                terres = ui.terres,
                selectedCampanya = ui.selectedCampanya,
                currentAppliedKgNByTerra = { terraId -> ui.appliedKgNForTerra(terraId) },
                onConfirm = { terraId, data, kgN, tipus, procedencia, volum, kgNM3 ->
                    if (viewModel.createAplicacio(terraId, data, kgN, tipus, procedencia, volum, kgNM3)) {
                        showCreateAplicacioDialog = false
                    }
                },
                onDismiss = { showCreateAplicacioDialog = false }
            )
        }

        val terraToDelete = ui.terres.find { it.id == pendingDeleteTerraId }
        if (terraToDelete != null) {
            ConfirmDeleteDialog(
                title = "Eliminar terra",
                message = "S'eliminara la terra '${terraToDelete.codi_sigpac_complet ?: terraToDelete.id}'. Aquesta accio es destructiva.",
                onConfirm = {
                    viewModel.deleteTerra(terraToDelete.id)
                    pendingDeleteTerraId = null
                },
                onDismiss = { pendingDeleteTerraId = null }
            )
        }

        val aplicacioToDelete = ui.aplicacions.find { it.id == pendingDeleteAplicacioId }
        if (aplicacioToDelete != null) {
            ConfirmDeleteDialog(
                title = "Eliminar aplicacio",
                message = "S'eliminara l'aplicacio del dia '${aplicacioToDelete.data ?: "-"}'. Aquesta accio es destructiva.",
                onConfirm = {
                    viewModel.deleteAplicacio(aplicacioToDelete.id)
                    pendingDeleteAplicacioId = null
                },
                onDismiss = { pendingDeleteAplicacioId = null }
            )
        }
    }
}

@Composable
private fun CampaignSelectorCard(
    selectedCampanya: Int,
    availableCampanyes: List<Int>,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Campanya de treball", style = MaterialTheme.typography.titleSmall)
            Text(
                "Les aplicacions i els calculs es mostren per la campanya seleccionada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableCampanyes.forEach { campanya ->
                    FilterChip(
                        selected = campanya == selectedCampanya,
                        onClick = { onSelect(campanya) },
                        label = { Text(campanya.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, description: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun EmptySectionCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditableTitularCard(
    titular: TitularDto,
    actorLabel: String?,
    onSave: (String, String) -> Boolean
) {
    var editing by remember { mutableStateOf(false) }
    var nif by remember(titular.id) { mutableStateOf(titular.nif ?: "") }
    var nom by remember(titular.id) { mutableStateOf(titular.nom_rao) }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Titular", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (editing) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom / Rao social") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nif, onValueChange = { nif = it }, label = { Text("NIF") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(nif, nom)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { nif = titular.nif ?: ""; nom = titular.nom_rao; editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text(nom, style = MaterialTheme.typography.titleMedium)
                Text("NIF: $nif")
                AuditInfoBlock(
                    updatedAt = titular.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = titular.updated_by
                )
                TextButton(onClick = { editing = true }) { Text("Editar") }
            }
        }
    }
}

@Composable
private fun EditableTerraCard(
    terra: TerraDto,
    appliedKgN: Double,
    selectedCampanya: Int,
    actorLabel: String?,
    onSave: (String, String, String, String, String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var superficie by remember(terra.id, terra.superficie) { mutableStateOf((terra.superficie ?: 0.0).toString()) }
    var zona by remember(terra.id, terra.zona) { mutableStateOf(terra.zona) }
    var municipiLiteral by remember(terra.id, terra.municipi_literal) { mutableStateOf(terra.municipi_literal ?: "") }
    var usSigpac by remember(terra.id, terra.us_sigpac) { mutableStateOf(terra.us_sigpac ?: "") }
    var cultiu by remember(terra.id, terra.cultiu) { mutableStateOf(terra.cultiu ?: "") }
    val limitKgN = terra.limit_kg_n_ha ?: if (terra.zona == "ZV") 170.0 else 190.0
    val kgNPerHa = terra.superficie?.takeIf { it > 0.0 }?.let { appliedKgN / it }
    val allowedKgN = terra.superficie?.let { it * limitKgN }
    val excessKgN = allowedKgN?.let { appliedKgN - it }?.takeIf { it > 0.0 }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(terra.codi_sigpac_complet ?: terra.id, style = MaterialTheme.typography.bodyLarge)
            if (editing) {
                OutlinedTextField(value = superficie, onValueChange = { superficie = it }, label = { Text("Superficie (ha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = municipiLiteral, onValueChange = { municipiLiteral = it }, label = { Text("Municipi literal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = usSigpac, onValueChange = { usSigpac = it }, label = { Text("Us SIGPAC") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cultiu, onValueChange = { cultiu = it }, label = { Text("Cultiu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ZonaSelector(zona = zona, onZonaChange = { zona = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(superficie, zona, municipiLiteral, usSigpac, cultiu)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = {
                        superficie = (terra.superficie ?: 0.0).toString()
                        zona = terra.zona
                        municipiLiteral = terra.municipi_literal ?: ""
                        usSigpac = terra.us_sigpac ?: ""
                        cultiu = terra.cultiu ?: ""
                        editing = false
                    }) { Text("Cancel·lar") }
                }
            } else {
                Text("Superficie: ${terra.superficie ?: 0.0} ha")
                Text("Zona: ${terra.zona} · Limit: ${formatKgN(limitKgN)} kg N/ha/any")
                Text("Municipi: ${terra.municipi_literal ?: "-"}")
                Text("Us SIGPAC: ${terra.us_sigpac ?: "-"} · Cultiu: ${terra.cultiu ?: "-"}")
                Text("Aplicat campanya: ${formatKgN(appliedKgN)} kg N")
                Text("Maxim admissible campanya: ${formatKgN(allowedKgN)} kg N")
                Text("Kg N/ha campanya: ${formatKgN(kgNPerHa)}")
                if (excessKgN != null) {
                    Text(
                        "Avis campanya $selectedCampanya: aquesta terra supera el limit anual en ${formatKgN(excessKgN)} kg N.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("Situacio campanya $selectedCampanya: dins del limit anual.")
                }
                AuditInfoBlock(
                    updatedAt = terra.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = terra.updated_by
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = true }) { Text("Editar") }
                    TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun EditableAplicacioCard(
    app: AplicacioFertilitzantDto,
    terres: List<TerraDto>,
    selectedCampanya: Int,
    currentAppliedKgNForTerra: Double,
    actorLabel: String?,
    onSave: (String, String, String, String, String, String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var data by remember(app.id, app.data) { mutableStateOf(app.data ?: "") }
    var kgN by remember(app.id, app.kg_n) { mutableStateOf((app.kg_n ?: 0.0).toString()) }
    var tipusFertilitzant by remember(app.id, app.tipus_fertilitzant) { mutableStateOf(app.tipus_fertilitzant ?: "") }
    var procedencia by remember(app.id, app.procedencia) { mutableStateOf(app.procedencia ?: "") }
    var volumM3 by remember(app.id, app.volum_m3) { mutableStateOf(app.volum_m3?.toString() ?: "") }
    var kgNM3 by remember(app.id, app.kg_n_m3) { mutableStateOf(app.kg_n_m3?.toString() ?: "") }
    val terra = terres.find { it.id == app.terra_id }
    val limitKgNHa = terra?.limit_kg_n_ha ?: if (terra?.zona == "ZV") 170.0 else 190.0
    val allowedKgN = terra?.superficie?.let { it * limitKgNHa }
    val editedKgN = kgN.toDoubleOrNull() ?: 0.0
    val projectedAppliedKgN = currentAppliedKgNForTerra - (app.kg_n ?: 0.0) + editedKgN
    val projectedExcessKgN = allowedKgN?.let { projectedAppliedKgN - it }?.takeIf { it > 0.0 }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Campanya: ${app.dan?.campanya ?: "-"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (editing) {
                OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Data (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tipusFertilitzant, onValueChange = { tipusFertilitzant = it }, label = { Text("Tipus fertilitzant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = procedencia, onValueChange = { procedencia = it }, label = { Text("Procedencia") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = kgN, onValueChange = { kgN = it }, label = { Text("Kg N") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = volumM3, onValueChange = { volumM3 = it }, label = { Text("Volum m3") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = kgNM3, onValueChange = { kgNM3 = it }, label = { Text("Kg N/m3") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (projectedExcessKgN != null) {
                    HorizontalDivider()
                    Text(
                        "Avis campanya $selectedCampanya: si guardes aquest valor, la terra quedara ${formatKgN(projectedExcessKgN)} kg N per sobre del limit anual.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(data, kgN, tipusFertilitzant, procedencia, volumM3, kgNM3)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = {
                        data = app.data ?: ""
                        kgN = (app.kg_n ?: 0.0).toString()
                        tipusFertilitzant = app.tipus_fertilitzant ?: ""
                        procedencia = app.procedencia ?: ""
                        volumM3 = app.volum_m3?.toString() ?: ""
                        kgNM3 = app.kg_n_m3?.toString() ?: ""
                        editing = false
                    }) { Text("Cancel·lar") }
                }
            } else {
                Text("Data: ${app.data ?: "-"}")
                Text("Kg N: ${app.kg_n ?: 0.0} · Volum m3: ${app.volum_m3 ?: "-"}")
                Text("Tipus: ${app.tipus_fertilitzant ?: "-"} · Procedencia: ${app.procedencia ?: "-"}")
                Text("Kg N/m3: ${app.kg_n_m3 ?: "-"}")
                if (allowedKgN != null && projectedExcessKgN != null) {
                    Text(
                        "Avis campanya $selectedCampanya: terra per sobre del limit anual en ${formatKgN(projectedExcessKgN)} kg N.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                AuditInfoBlock(
                    updatedAt = app.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = app.updated_by
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = true }) { Text("Editar") }
                    TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun CreateTerraDialog(
    onConfirm: (String, String, String, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var munCodi by remember { mutableStateOf("") }
    var poligon by remember { mutableStateOf("") }
    var parcela by remember { mutableStateOf("") }
    var recinte by remember { mutableStateOf("") }
    var superficie by remember { mutableStateOf("") }
    var zona by remember { mutableStateOf("ZNV") }
    var municipiLiteral by remember { mutableStateOf("") }
    var usSigpac by remember { mutableStateOf("") }
    var cultiu by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova terra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Introdueix la identificació SIGPAC bàsica i la superfície de la nova terra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(value = munCodi, onValueChange = { munCodi = it }, label = { Text("Codi municipal (5 digits)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = poligon, onValueChange = { poligon = it }, label = { Text("Poligon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = parcela, onValueChange = { parcela = it }, label = { Text("Parcela") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = recinte, onValueChange = { recinte = it }, label = { Text("Recinte") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = municipiLiteral, onValueChange = { municipiLiteral = it }, label = { Text("Municipi literal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = usSigpac, onValueChange = { usSigpac = it }, label = { Text("Us SIGPAC") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cultiu, onValueChange = { cultiu = it }, label = { Text("Cultiu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = superficie, onValueChange = { superficie = it }, label = { Text("Superficie (ha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ZonaSelector(zona = zona, onZonaChange = { zona = it })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(munCodi, poligon, parcela, recinte, superficie, zona, municipiLiteral, usSigpac, cultiu) }) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun ZonaSelector(
    zona: String,
    onZonaChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Zona nitrogen", style = MaterialTheme.typography.labelMedium)
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

private fun formatKgN(value: Double?): String {
    if (value == null) return "-"
    val rounded = kotlin.math.round(value * 100) / 100
    return rounded.toString().replace('.', ',')
}

@Composable
private fun CreateAplicacioDialog(
    terres: List<TerraDto>,
    selectedCampanya: Int,
    currentAppliedKgNByTerra: (String) -> Double,
    onConfirm: (String, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTerraId by remember(terres) { mutableStateOf(terres.firstOrNull()?.id ?: "") }
    var data by remember { mutableStateOf("") }
    var kgN by remember { mutableStateOf("") }
    var tipusFertilitzant by remember { mutableStateOf("") }
    var procedencia by remember { mutableStateOf("") }
    var volumM3 by remember { mutableStateOf("") }
    var kgNM3 by remember { mutableStateOf("") }
    val selectedTerra = terres.find { it.id == selectedTerraId }
    val limitKgNHa = selectedTerra?.limit_kg_n_ha ?: if (selectedTerra?.zona == "ZV") 170.0 else 190.0
    val allowedKgN = selectedTerra?.superficie?.let { it * limitKgNHa }
    val enteredKgN = kgN.toDoubleOrNull() ?: 0.0
    val projectedAppliedKgN = if (selectedTerraId.isBlank()) 0.0 else currentAppliedKgNByTerra(selectedTerraId) + enteredKgN
    val projectedExcessKgN = allowedKgN?.let { projectedAppliedKgN - it }?.takeIf { it > 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova aplicacio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (terres.isEmpty()) {
                    Text(
                        "Abans de crear una aplicacio has de donar d'alta almenys una terra per aquest titular.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Selecciona la terra i informa la data i les quantitats aplicades.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Campanya activa: $selectedCampanya",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TerraDropdown(
                        terres = terres,
                        selectedId = selectedTerraId,
                        onSelect = { selectedTerraId = it }
                    )
                    OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Data (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tipusFertilitzant, onValueChange = { tipusFertilitzant = it }, label = { Text("Tipus fertilitzant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = procedencia, onValueChange = { procedencia = it }, label = { Text("Procedencia") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = kgN, onValueChange = { kgN = it }, label = { Text("Kg N") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = volumM3, onValueChange = { volumM3 = it }, label = { Text("Volum m3") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = kgNM3, onValueChange = { kgNM3 = it }, label = { Text("Kg N/m3") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    if (projectedExcessKgN != null) {
                        HorizontalDivider()
                        Text(
                            "Avis campanya $selectedCampanya: aquesta entrada deixara la terra ${formatKgN(projectedExcessKgN)} kg N per sobre del limit anual.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTerraId, data, kgN, tipusFertilitzant, procedencia, volumM3, kgNM3) },
                enabled = terres.isNotEmpty()
            ) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Eliminar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun TerraDropdown(
    terres: List<TerraDto>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    SearchableSelectionField(
        items = terres,
        selectedItem = terres.find { it.id == selectedId },
        onSelect = { terra -> onSelect(terra?.id ?: "") },
        itemLabel = { it.codi_sigpac_complet ?: it.id },
        itemSearchText = { it.codi_sigpac_complet ?: it.id },
        label = "Terra",
        placeholder = "Cerca per codi SIGPAC"
    )
}

@Composable
private fun ErrorBlock(error: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("< Tornar") }
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}
