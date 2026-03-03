package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    // Snackbar per missatges de guardat
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
                    // Header
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) { Text("< Tornar") }
                            Text("Modul Agricola", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    // Titular editable
                    item {
                        ui.titular?.let { titular ->
                            EditableTitularCard(titular) { nif, nom ->
                                viewModel.updateTitular(nif, nom)
                            }
                        }
                    }

                    // Terres editables
                    item { Text("Terres", style = MaterialTheme.typography.titleMedium) }
                    if (ui.terres.isEmpty()) {
                        item { Text("Sense terres", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(ui.terres, key = { it.id }) { terra ->
                            EditableTerraCard(terra) { superficie ->
                                viewModel.updateTerra(terra.id, superficie)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    // Aplicacions editables
                    item { Text("Aplicacions fertilitzants", style = MaterialTheme.typography.titleMedium) }
                    if (ui.aplicacions.isEmpty()) {
                        item { Text("Sense aplicacions", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(ui.aplicacions, key = { it.id }) { app ->
                            EditableAplicacioCard(app) { data, kgN, uf ->
                                viewModel.updateAplicacio(app.id, data, kgN, uf)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableTitularCard(titular: TitularDto, onSave: (String, String) -> Unit) {
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
                    Button(onClick = { onSave(nif, nom); editing = false }) { Text("Guardar") }
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
private fun EditableTerraCard(terra: TerraDto, onSave: (Double) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var superficie by remember(terra.id, terra.superficie) { mutableStateOf((terra.superficie ?: 0.0).toString()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(terra.codi_sigpac_complet ?: terra.id, style = MaterialTheme.typography.bodyLarge)
            if (editing) {
                OutlinedTextField(value = superficie, onValueChange = { superficie = it }, label = { Text("Superficie (ha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { superficie.toDoubleOrNull()?.let { onSave(it) }; editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { superficie = (terra.superficie ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Superficie: ${terra.superficie ?: 0.0} ha")
                TextButton(onClick = { editing = true }) { Text("Editar") }
            }
        }
    }
}

@Composable
private fun EditableAplicacioCard(app: AplicacioFertilitzantDto, onSave: (String, Double, Double) -> Unit) {
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
                    Button(onClick = {
                        val kgVal = kgN.toDoubleOrNull() ?: 0.0
                        val ufVal = uf.toDoubleOrNull() ?: 0.0
                        onSave(data, kgVal, ufVal)
                        editing = false
                    }) { Text("Guardar") }
                    OutlinedButton(onClick = { data = app.data ?: ""; kgN = (app.kg_n ?: 0.0).toString(); uf = (app.uf ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Data: ${app.data ?: "-"}")
                Text("Kg N: ${app.kg_n ?: 0.0} · UF: ${app.uf ?: 0.0}")
                TextButton(onClick = { editing = true }) { Text("Editar") }
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
