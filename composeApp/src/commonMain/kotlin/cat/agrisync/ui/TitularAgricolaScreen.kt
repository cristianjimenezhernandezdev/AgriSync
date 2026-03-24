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
                            EditableTitularCard(titular) { nif, nom ->
                                viewModel.updateTitular(nif, nom)
                            }
                        }
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
                                onSave = { superficie -> viewModel.updateTerra(terra.id, superficie) },
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
                                onSave = { data, kgN, uf -> viewModel.updateAplicacio(app.id, data, kgN, uf) },
                                onDelete = { pendingDeleteAplicacioId = app.id }
                            )
                        }
                    }
                }
            }
        }

        if (showCreateTerraDialog) {
            CreateTerraDialog(
                onConfirm = { munCodi, poligon, parcela, recinte, superficie ->
                    if (viewModel.createTerra(munCodi, poligon, parcela, recinte, superficie)) {
                        showCreateTerraDialog = false
                    }
                },
                onDismiss = { showCreateTerraDialog = false }
            )
        }

        if (showCreateAplicacioDialog) {
            CreateAplicacioDialog(
                terres = ui.terres,
                onConfirm = { terraId, data, kgN, uf ->
                    if (viewModel.createAplicacio(terraId, data, kgN, uf)) {
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
private fun EditableTitularCard(titular: TitularDto, onSave: (String, String) -> Boolean) {
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
                TextButton(onClick = { editing = true }) { Text("Editar") }
            }
        }
    }
}

@Composable
private fun EditableTerraCard(
    terra: TerraDto,
    onSave: (String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var superficie by remember(terra.id, terra.superficie) { mutableStateOf((terra.superficie ?: 0.0).toString()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(terra.codi_sigpac_complet ?: terra.id, style = MaterialTheme.typography.bodyLarge)
            if (editing) {
                OutlinedTextField(value = superficie, onValueChange = { superficie = it }, label = { Text("Superficie (ha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(superficie)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { superficie = (terra.superficie ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Superficie: ${terra.superficie ?: 0.0} ha")
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
    onSave: (String, String, String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var data by remember(app.id, app.data) { mutableStateOf(app.data ?: "") }
    var kgN by remember(app.id, app.kg_n) { mutableStateOf((app.kg_n ?: 0.0).toString()) }
    var uf by remember(app.id, app.uf) { mutableStateOf((app.uf ?: 0.0).toString()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Campanya: ${app.dan?.campanya ?: "-"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (editing) {
                OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Data (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = kgN, onValueChange = { kgN = it }, label = { Text("Kg N") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uf, onValueChange = { uf = it }, label = { Text("UF") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(data, kgN, uf)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { data = app.data ?: ""; kgN = (app.kg_n ?: 0.0).toString(); uf = (app.uf ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Data: ${app.data ?: "-"}")
                Text("Kg N: ${app.kg_n ?: 0.0} · UF: ${app.uf ?: 0.0}")
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
    onConfirm: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var munCodi by remember { mutableStateOf("") }
    var poligon by remember { mutableStateOf("") }
    var parcela by remember { mutableStateOf("") }
    var recinte by remember { mutableStateOf("") }
    var superficie by remember { mutableStateOf("") }

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
                OutlinedTextField(value = superficie, onValueChange = { superficie = it }, label = { Text("Superficie (ha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(munCodi, poligon, parcela, recinte, superficie) }) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun CreateAplicacioDialog(
    terres: List<TerraDto>,
    onConfirm: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTerraId by remember(terres) { mutableStateOf(terres.firstOrNull()?.id ?: "") }
    var data by remember { mutableStateOf("") }
    var kgN by remember { mutableStateOf("") }
    var uf by remember { mutableStateOf("") }

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
                        "Selecciona la terra i informa la data i les unitats aplicades.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TerraDropdown(
                        terres = terres,
                        selectedId = selectedTerraId,
                        onSelect = { selectedTerraId = it }
                    )
                    OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Data (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = kgN, onValueChange = { kgN = it }, label = { Text("Kg N") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uf, onValueChange = { uf = it }, label = { Text("UF") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTerraId, data, kgN, uf) }, enabled = terres.isNotEmpty()) { Text("Crear") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerraDropdown(
    terres: List<TerraDto>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = terres.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.codi_sigpac_complet ?: "Selecciona una terra",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text("Terra") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            terres.forEach { terra ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(terra.codi_sigpac_complet ?: terra.id) },
                    onClick = {
                        onSelect(terra.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorBlock(error: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("< Tornar") }
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}
