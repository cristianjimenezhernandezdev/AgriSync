package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.EntregaDejeccioDto
import cat.agrisync.data.GranjaBestiarDto
import cat.agrisync.data.GranjaDto
import cat.agrisync.data.TitularDto
import cat.agrisync.viewmodel.TitularRamaderViewModel

@Composable
internal fun TitularRamaderScreen(
    viewModel: TitularRamaderViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()

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
            ui.error != null -> RamaderErrorBlock(ui.error ?: "Error", onBack)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) { Text("< Tornar") }
                            Text("Modul Ramader", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    // Titular editable
                    item {
                        ui.titular?.let { titular ->
                            EditableRamaderTitularCard(titular) { nif, nom ->
                                viewModel.updateTitular(nif, nom)
                            }
                        }
                    }

                    // Granges editables
                    item { Text("Granges", style = MaterialTheme.typography.titleMedium) }
                    if (ui.granges.isEmpty()) {
                        item { Text("Sense granges", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(ui.granges, key = { it.id }) { granja ->
                            EditableGranjaCard(granja) { nom, marca ->
                                viewModel.updateGranja(granja.id, nom, marca)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    // Granja bestiar editable
                    item { Text("Granja bestiar", style = MaterialTheme.typography.titleMedium) }
                    if (ui.granjaBestiar.isEmpty()) {
                        item { Text("Sense registres", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(ui.granjaBestiar, key = { it.id }) { gb ->
                            EditableGranjaBestiarCard(gb) { cens ->
                                viewModel.updateGranjaBestiar(gb.id, cens)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    // Entregues editables
                    item { Text("Entrega dejeccions", style = MaterialTheme.typography.titleMedium) }
                    if (ui.entregues.isEmpty()) {
                        item { Text("Sense entregues", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(ui.entregues, key = { it.id }) { e ->
                            EditableEntregaCard(e) { data, quantitat ->
                                viewModel.updateEntrega(e.id, data, quantitat)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableRamaderTitularCard(titular: TitularDto, onSave: (String, String) -> Unit) {
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
private fun EditableGranjaCard(granja: GranjaDto, onSave: (String, String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var nom by remember(granja.id, granja.nom) { mutableStateOf(granja.nom ?: "") }
    var marca by remember(granja.id, granja.marca_oficial) { mutableStateOf(granja.marca_oficial) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (editing) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom granja") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca oficial") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(nom, marca); editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { nom = granja.nom ?: ""; marca = granja.marca_oficial; editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text(granja.nom ?: granja.marca_oficial, style = MaterialTheme.typography.bodyLarge)
                Text("Marca oficial: ${granja.marca_oficial}")
                TextButton(onClick = { editing = true }) { Text("Editar") }
            }
        }
    }
}

@Composable
private fun EditableGranjaBestiarCard(gb: GranjaBestiarDto, onSave: (Double) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var cens by remember(gb.id, gb.cens) { mutableStateOf((gb.cens ?: 0.0).toString()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(gb.granja?.nom ?: gb.granja?.marca_oficial ?: "-", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (editing) {
                Text("Bestiar: ${gb.bestiar?.codi ?: "-"} · Fase: ${gb.fase_productiva?.codi ?: "-"}")
                OutlinedTextField(value = cens, onValueChange = { cens = it }, label = { Text("Cens") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { cens.toDoubleOrNull()?.let { onSave(it) }; editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { cens = (gb.cens ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Bestiar: ${gb.bestiar?.codi ?: "-"} · Fase: ${gb.fase_productiva?.codi ?: "-"}")
                Text("Cens: ${gb.cens ?: 0.0}")
                TextButton(onClick = { editing = true }) { Text("Editar") }
            }
        }
    }
}

@Composable
private fun EditableEntregaCard(e: EntregaDejeccioDto, onSave: (String, Double) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var data by remember(e.id, e.data) { mutableStateOf(e.data ?: "") }
    var quantitat by remember(e.id, e.quantitat) { mutableStateOf((e.quantitat ?: 0.0).toString()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (editing) {
                OutlinedTextField(value = data, onValueChange = { data = it }, label = { Text("Data (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = quantitat, onValueChange = { quantitat = it }, label = { Text("Quantitat") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Receptor: ${e.receptor_titular_id ?: "terra:${e.terra_desti_id ?: "-"}"}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val q = quantitat.toDoubleOrNull() ?: 0.0
                        onSave(data, q)
                        editing = false
                    }) { Text("Guardar") }
                    OutlinedButton(onClick = { data = e.data ?: ""; quantitat = (e.quantitat ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Data: ${e.data ?: "-"}")
                Text("Quantitat: ${e.quantitat ?: 0.0}")
                Text("Receptor: ${e.receptor_titular_id ?: "terra:${e.terra_desti_id ?: "-"}"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { editing = true }) { Text("Editar") }
            }
        }
    }
}

@Composable
private fun RamaderErrorBlock(error: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("< Tornar") }
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}
