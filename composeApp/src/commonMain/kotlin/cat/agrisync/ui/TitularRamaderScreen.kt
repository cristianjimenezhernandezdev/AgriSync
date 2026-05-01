package cat.agrisync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import cat.agrisync.data.BestiarDto
import cat.agrisync.data.EntregaDejeccioDto
import cat.agrisync.data.FaseProductivaDto
import cat.agrisync.data.GranjaCampanyaBalanceDto
import cat.agrisync.data.GranjaBestiarDto
import cat.agrisync.data.GranjaDto
import cat.agrisync.data.TerraDto
import cat.agrisync.data.TitularDto
import cat.agrisync.util.formatStoredDateForDisplay
import cat.agrisync.util.formatStoredDateForInput
import cat.agrisync.viewmodel.TitularRamaderViewModel

@Composable
internal fun TitularRamaderScreen(
    viewModel: TitularRamaderViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    var showCreateGranjaDialog by remember { mutableStateOf(false) }
    var showCreateGranjaBestiarDialog by remember { mutableStateOf(false) }
    var showCreateEntregaDialog by remember { mutableStateOf(false) }
    var pendingDeleteGranjaId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteGranjaBestiarId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteEntregaId by remember { mutableStateOf<String?>(null) }

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
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) { Text("< Tornar") }
                            Text("Modul Ramader", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    item {
                        ui.titular?.let { titular ->
                            EditableRamaderTitularCard(
                                titular = titular,
                                actorLabel = ui.actorLabels[titular.updated_by]
                            ) { nif, nom, telefon, email, adreca, codiPostal ->
                                viewModel.updateTitular(nif, nom, telefon, email, adreca, codiPostal)
                            }
                        }
                    }

                    item {
                        TitularCollaborationCard(
                            oficines = ui.collaboratingOficines,
                            tecnics = ui.collaboratingTecnics
                        )
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
                            title = "Granges",
                            description = "Explotacions ramaderes vinculades al titular.",
                            actionLabel = "+ Nova Granja",
                            onAction = { showCreateGranjaDialog = true }
                        )
                    }
                    if (ui.granges.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "Encara no hi ha granges",
                                message = "Crea una granja per poder afegir bestiar i registrar entregues de dejeccions."
                            )
                        }
                    } else {
                        items(ui.granges, key = { it.id }) { granja ->
                            EditableGranjaCard(
                                granja = granja,
                                actorLabel = ui.actorLabels[granja.updated_by],
                                onSave = { nom, marca -> viewModel.updateGranja(granja.id, nom, marca) },
                                onDelete = { pendingDeleteGranjaId = granja.id }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        SectionHeader(
                            title = "Granja bestiar",
                            description = "Relació entre granja, tipus de bestiar, fase productiva i cens.",
                            actionLabel = "+ Nou Registre",
                            onAction = { showCreateGranjaBestiarDialog = true }
                        )
                    }
                    if (ui.granjaBestiar.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "Encara no hi ha registres de bestiar",
                                message = "Quan tinguis una granja creada, pots afegir-hi aquí les línies de cens per bestiar i fase productiva."
                            )
                        }
                    } else {
                        items(ui.granjaBestiar, key = { it.id }) { gb ->
                            EditableGranjaBestiarCard(
                                gb = gb,
                                actorLabel = ui.actorLabels[gb.updated_by],
                                onSave = { cens -> viewModel.updateGranjaBestiar(gb.id, cens) },
                                onDelete = { pendingDeleteGranjaBestiarId = gb.id }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Balanç nitrogen campanya", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Per cada granja informa estoc inicial, nitrogen generat i estoc final declarat. El programa calcula el nitrogen justificat per entregues.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!ui.isGranjaCampanyaBalanceAvailable) {
                        item {
                            EmptySectionCard(
                                title = "Balanç no disponible",
                                message = "Aquesta base de dades encara no te activada la taula de balanç de granja per campanya. Pots treballar granges, bestiar i entregues igualment."
                            )
                        }
                    } else if (ui.granges.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "Sense balanç ramader",
                                message = "Primer cal crear una granja per poder portar el balanç de nitrogen de campanya."
                            )
                        }
                    } else {
                        items(ui.granges, key = { "balance-${it.id}" }) { granja ->
                            EditableGranjaCampanyaBalanceCard(
                                granja = granja,
                                balance = ui.granjaCampanyaBalances.find { it.granja_id == granja.id },
                                justifiedKgN = ui.entregues.filter { it.granja_origen_id == granja.id }.sumOf { it.kg_n ?: 0.0 },
                                actorLabel = ui.granjaCampanyaBalances.find { it.granja_id == granja.id }?.updated_by?.let { ui.actorLabels[it] },
                                onSave = { balanceId, estocInicial, generat, estocFinal ->
                                    viewModel.saveGranjaCampanyaBalance(granja.id, balanceId, estocInicial, generat, estocFinal)
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        SectionHeader(
                            title = "Entregues a terra",
                            description = "Cada entrega justifica automàticament una aplicació agrícola sobre la terra de destí.",
                            actionLabel = "+ Nova Entrega",
                            onAction = { showCreateEntregaDialog = true }
                        )
                    }
                    if (ui.entregues.isEmpty()) {
                        item {
                            EmptySectionCard(
                                title = "Encara no hi ha entregues",
                                message = "Des d'aquí pots justificar on va cada purí o fertilitzant orgànic de les granges."
                            )
                        }
                    } else {
                        items(ui.entregues, key = { it.id }) { e ->
                            EditableEntregaCard(
                                e = e,
                                terres = ui.receptorTerres,
                                actorLabel = ui.actorLabels[e.updated_by],
                                onSave = { data, terraDestiId, tipusFertilitzant, volum, kgNM3, kgN ->
                                    viewModel.updateEntrega(e.id, data, terraDestiId, tipusFertilitzant, volum, kgNM3, kgN)
                                },
                                onDelete = { pendingDeleteEntregaId = e.id }
                            )
                        }
                    }
                }
            }
        }

        if (showCreateGranjaDialog) {
            CreateGranjaDialog(
                onConfirm = { nom, marca ->
                    if (viewModel.createGranja(nom, marca)) {
                        showCreateGranjaDialog = false
                    }
                },
                onDismiss = { showCreateGranjaDialog = false }
            )
        }

        if (showCreateGranjaBestiarDialog) {
            CreateGranjaBestiarDialog(
                granges = ui.granges,
                bestiars = ui.bestiars,
                fases = ui.fasesProductives,
                onConfirm = { granjaId, bestiarId, faseId, cens ->
                    if (viewModel.createGranjaBestiar(granjaId, bestiarId, faseId, cens)) {
                        showCreateGranjaBestiarDialog = false
                    }
                },
                onDismiss = { showCreateGranjaBestiarDialog = false }
            )
        }

        if (showCreateEntregaDialog) {
            CreateEntregaDialog(
                granges = ui.granges,
                terres = ui.receptorTerres,
                selectedCampanya = ui.selectedCampanya,
                onConfirm = { granjaId, data, terraDestiId, tipusFertilitzant, volum, kgNM3, kgN ->
                    if (viewModel.createEntrega(granjaId, data, terraDestiId, tipusFertilitzant, volum, kgNM3, kgN)) {
                        showCreateEntregaDialog = false
                    }
                },
                onDismiss = { showCreateEntregaDialog = false }
            )
        }

        val granjaToDelete = ui.granges.find { it.id == pendingDeleteGranjaId }
        if (granjaToDelete != null) {
            ConfirmDeleteDialog(
                title = "Eliminar granja",
                message = "S'eliminara la granja '${granjaToDelete.nom ?: granjaToDelete.marca_oficial}'. Si te entregues associades, la base de dades pot impedir l'operacio.",
                onConfirm = {
                    viewModel.deleteGranja(granjaToDelete.id)
                    pendingDeleteGranjaId = null
                },
                onDismiss = { pendingDeleteGranjaId = null }
            )
        }

        val granjaBestiarToDelete = ui.granjaBestiar.find { it.id == pendingDeleteGranjaBestiarId }
        if (granjaBestiarToDelete != null) {
            ConfirmDeleteDialog(
                title = "Eliminar registre de bestiar",
                message = "S'eliminara aquest registre de bestiar de la granja. Aquesta accio es destructiva.",
                onConfirm = {
                    viewModel.deleteGranjaBestiar(granjaBestiarToDelete.id)
                    pendingDeleteGranjaBestiarId = null
                },
                onDismiss = { pendingDeleteGranjaBestiarId = null }
            )
        }

        val entregaToDelete = ui.entregues.find { it.id == pendingDeleteEntregaId }
        if (entregaToDelete != null) {
            ConfirmDeleteDialog(
                title = "Eliminar entrega",
                message = "S'eliminara l'entrega del dia '${formatStoredDateForDisplay(entregaToDelete.data)}'. Aquesta accio es destructiva.",
                onConfirm = {
                    viewModel.deleteEntrega(entregaToDelete.id)
                    pendingDeleteEntregaId = null
                },
                onDismiss = { pendingDeleteEntregaId = null }
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
                "Les entregues es mostren i es creen dins la campanya seleccionada.",
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
private fun EditableRamaderTitularCard(
    titular: TitularDto,
    actorLabel: String?,
    onSave: (String, String, String, String, String, String) -> Boolean
) {
    var editing by remember { mutableStateOf(false) }
    var nif by remember(titular.id) { mutableStateOf(titular.nif ?: "") }
    var nom by remember(titular.id) { mutableStateOf(titular.nom_rao) }
    var telefon by remember(titular.id) { mutableStateOf(titular.telefon ?: "") }
    var email by remember(titular.id) { mutableStateOf(titular.email ?: "") }
    var adreca by remember(titular.id) { mutableStateOf(titular.adreca ?: "") }
    var codiPostal by remember(titular.id) { mutableStateOf(titular.codi_postal ?: "") }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Titular", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (editing) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom / Rao social") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nif, onValueChange = { nif = it }, label = { Text("NIF") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefon, onValueChange = { telefon = it }, label = { Text("Telefon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = adreca, onValueChange = { adreca = it }, label = { Text("Adreca") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = codiPostal, onValueChange = { codiPostal = it }, label = { Text("Codi postal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(nif, nom, telefon, email, adreca, codiPostal)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = {
                        nif = titular.nif ?: ""
                        nom = titular.nom_rao
                        telefon = titular.telefon ?: ""
                        email = titular.email ?: ""
                        adreca = titular.adreca ?: ""
                        codiPostal = titular.codi_postal ?: ""
                        editing = false
                    }) { Text("Cancel·lar") }
                }
            } else {
                Text(nom, style = MaterialTheme.typography.titleMedium)
                Text("NIF: $nif")
                Text("Telefon: ${telefon.ifBlank { "-" }}")
                Text("Email: ${email.ifBlank { "-" }}")
                Text("Adreca: ${adreca.ifBlank { "-" }}")
                Text("CP: ${codiPostal.ifBlank { "-" }}")
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
private fun EditableGranjaCard(
    granja: GranjaDto,
    actorLabel: String?,
    onSave: (String, String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var nom by remember(granja.id, granja.nom) { mutableStateOf(granja.nom ?: "") }
    var marca by remember(granja.id, granja.marca_oficial) { mutableStateOf(granja.marca_oficial) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (editing) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom granja") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca oficial") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(nom, marca)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { nom = granja.nom ?: ""; marca = granja.marca_oficial; editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text(granja.nom ?: granja.marca_oficial, style = MaterialTheme.typography.bodyLarge)
                Text("Marca oficial: ${granja.marca_oficial}")
                AuditInfoBlock(
                    updatedAt = granja.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = granja.updated_by
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
private fun EditableGranjaBestiarCard(
    gb: GranjaBestiarDto,
    actorLabel: String?,
    onSave: (String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var cens by remember(gb.id, gb.cens) { mutableStateOf((gb.cens ?: 0.0).toString()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(gb.granja?.nom ?: gb.granja?.marca_oficial ?: "-", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (editing) {
                Text("Bestiar: ${gb.bestiar?.codi ?: "-"} · Fase: ${gb.fase_productiva?.codi ?: "-"}")
                OutlinedTextField(value = cens, onValueChange = { cens = it }, label = { Text("Cens") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(cens)) editing = false }) { Text("Guardar") }
                    OutlinedButton(onClick = { cens = (gb.cens ?: 0.0).toString(); editing = false }) { Text("Cancel·lar") }
                }
            } else {
                Text("Bestiar: ${gb.bestiar?.codi ?: "-"} · Fase: ${gb.fase_productiva?.codi ?: "-"}")
                Text("Cens: ${gb.cens ?: 0.0}")
                AuditInfoBlock(
                    updatedAt = gb.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = gb.updated_by
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
private fun EditableGranjaCampanyaBalanceCard(
    granja: GranjaDto,
    balance: GranjaCampanyaBalanceDto?,
    justifiedKgN: Double,
    actorLabel: String?,
    onSave: (String?, String, String, String) -> Boolean
) {
    var editing by remember(balance?.id, granja.id) { mutableStateOf(balance == null) }
    var estocInicial by remember(balance?.id, balance?.estoc_inicial_kg_n) { mutableStateOf(balance?.estoc_inicial_kg_n?.toString() ?: "") }
    var kgNGenerat by remember(balance?.id, balance?.kg_n_generat) { mutableStateOf(balance?.kg_n_generat?.toString() ?: "") }
    var estocFinal by remember(balance?.id, balance?.estoc_final_declarat_kg_n) { mutableStateOf(balance?.estoc_final_declarat_kg_n?.toString() ?: "") }

    val initialValue = balance?.estoc_inicial_kg_n ?: estocInicial.replace(',', '.').toDoubleOrNull() ?: 0.0
    val generatedValue = balance?.kg_n_generat ?: kgNGenerat.replace(',', '.').toDoubleOrNull() ?: 0.0
    val declaredFinalValue = balance?.estoc_final_declarat_kg_n ?: estocFinal.replace(',', '.').toDoubleOrNull() ?: 0.0
    val calculatedFinal = initialValue + generatedValue - justifiedKgN
    val deviation = calculatedFinal - declaredFinalValue

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(granja.nom ?: granja.marca_oficial, style = MaterialTheme.typography.bodyLarge)
            Text("Marca oficial: ${granja.marca_oficial}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (editing) {
                OutlinedTextField(value = estocInicial, onValueChange = { estocInicial = it }, label = { Text("Estoc inicial Kg N") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kgNGenerat, onValueChange = { kgNGenerat = it }, label = { Text("Kg N generat campanya") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estocFinal, onValueChange = { estocFinal = it }, label = { Text("Estoc final declarat Kg N") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Kg N justificat per entregues: ${formatBalanceNumber(justifiedKgN)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (onSave(balance?.id, estocInicial, kgNGenerat, estocFinal)) editing = false }) { Text("Guardar") }
                    if (balance != null) {
                        OutlinedButton(onClick = {
                            estocInicial = balance.estoc_inicial_kg_n?.toString() ?: ""
                            kgNGenerat = balance.kg_n_generat?.toString() ?: ""
                            estocFinal = balance.estoc_final_declarat_kg_n?.toString() ?: ""
                            editing = false
                        }) { Text("Cancel·lar") }
                    }
                }
            } else {
                Text("Estoc inicial Kg N: ${formatBalanceNumber(balance?.estoc_inicial_kg_n)}")
                Text("Kg N generat campanya: ${formatBalanceNumber(balance?.kg_n_generat)}")
                Text("Kg N justificat per entregues: ${formatBalanceNumber(justifiedKgN)}")
                Text("Estoc final declarat Kg N: ${formatBalanceNumber(balance?.estoc_final_declarat_kg_n)}")
                Text("Estoc final calculat Kg N: ${formatBalanceNumber(calculatedFinal)}")
                Text(
                    "Desviacio balanc: ${formatBalanceNumber(deviation)}",
                    color = if (kotlin.math.abs(deviation) > 0.01) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                AuditInfoBlock(
                    updatedAt = balance?.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = balance?.updated_by
                )
                TextButton(onClick = { editing = true }) { Text(if (balance == null) "Configurar" else "Editar") }
            }
        }
    }
}

@Composable
private fun EditableEntregaCard(
    e: EntregaDejeccioDto,
    terres: List<TerraDto>,
    actorLabel: String?,
    onSave: (String, String, String, String, String, String) -> Boolean,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var data by remember(e.id, e.data) { mutableStateOf(formatStoredDateForInput(e.data)) }
    var selectedTerraId by remember(e.id, e.terra_desti_id) { mutableStateOf(e.terra_desti_id ?: "") }
    var tipusFertilitzant by remember(e.id, e.tipus_fertilitzant) { mutableStateOf(e.tipus_fertilitzant ?: "Puri") }
    var volumM3 by remember(e.id, e.volum_m3) { mutableStateOf(e.volum_m3?.toString() ?: "") }
    var kgNM3 by remember(e.id, e.kg_n_m3) { mutableStateOf(e.kg_n_m3?.toString() ?: "") }
    var kgN by remember(e.id, e.kg_n) { mutableStateOf(e.kg_n?.toString() ?: "") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (editing) {
                DateInputField(
                    value = data,
                    onValueChange = { data = it },
                    label = "Data (dd/MM/YYYY)",
                    modifier = Modifier.fillMaxWidth()
                )
                TerraDropdown(terres = terres, selectedId = selectedTerraId, onSelect = { selectedTerraId = it }, label = "Terra desti")
                OutlinedTextField(value = tipusFertilitzant, onValueChange = { tipusFertilitzant = it }, label = { Text("Tipus fertilitzant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NitrogenTripletFieldGroup(
                    kgN = kgN,
                    onKgNChange = { kgN = it },
                    volumM3 = volumM3,
                    onVolumM3Change = { volumM3 = it },
                    kgNPerM3 = kgNM3,
                    onKgNPerM3Change = { kgNM3 = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (onSave(data, selectedTerraId, tipusFertilitzant, volumM3, kgNM3, kgN)) editing = false
                    }) { Text("Guardar") }
                    OutlinedButton(onClick = {
                        data = formatStoredDateForInput(e.data)
                        selectedTerraId = e.terra_desti_id ?: ""
                        tipusFertilitzant = e.tipus_fertilitzant ?: "Puri"
                        volumM3 = e.volum_m3?.toString() ?: ""
                        kgNM3 = e.kg_n_m3?.toString() ?: ""
                        kgN = e.kg_n?.toString() ?: ""
                        editing = false
                    }) { Text("Cancel·lar") }
                }
            } else {
                Text("Data: ${formatStoredDateForDisplay(e.data)}")
                Text("Tipus: ${e.tipus_fertilitzant ?: "-"}")
                Text("Kg N: ${e.kg_n ?: 0.0} · Volum m3: ${e.volum_m3 ?: "-"} · Kg N/m3: ${e.kg_n_m3 ?: "-"}")
                Text(
                    "Terra desti: ${formatEntregaReceptor(e)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Justifica automàticament una aplicacio agrícola sobre aquesta terra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                AuditInfoBlock(
                    updatedAt = e.updated_at,
                    updatedByLabel = actorLabel,
                    fallbackUserId = e.updated_by
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
private fun CreateGranjaDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova granja") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Identifica la granja pel seu nom i per la marca oficial.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom granja") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca oficial") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nom, marca) }) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun CreateGranjaBestiarDialog(
    granges: List<GranjaDto>,
    bestiars: List<BestiarDto>,
    fases: List<FaseProductivaDto>,
    onConfirm: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGranjaId by remember(granges) { mutableStateOf(granges.firstOrNull()?.id ?: "") }
    var selectedBestiarId by remember(bestiars) { mutableStateOf(bestiars.firstOrNull()?.id ?: "") }
    var selectedFaseId by remember(fases) { mutableStateOf(fases.firstOrNull()?.id ?: "") }
    var cens by remember { mutableStateOf("") }
    val canCreate = granges.isNotEmpty() && bestiars.isNotEmpty() && fases.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nou registre de bestiar") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!canCreate) {
                    item {
                        Text(
                            "Per crear aquest registre necessites almenys una granja i catalegs de bestiar i fase productiva disponibles.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        Text(
                            "Selecciona la granja, el tipus de bestiar, la fase productiva i el cens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        GranjaDropdown(granges = granges, selectedId = selectedGranjaId, onSelect = { selectedGranjaId = it }, label = "Granja")
                    }
                    item {
                        BestiarDropdown(bestiars = bestiars, selectedId = selectedBestiarId, onSelect = { selectedBestiarId = it })
                    }
                    item {
                        FaseDropdown(fases = fases, selectedId = selectedFaseId, onSelect = { selectedFaseId = it })
                    }
                    item {
                        OutlinedTextField(value = cens, onValueChange = { cens = it }, label = { Text("Cens") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedGranjaId, selectedBestiarId, selectedFaseId, cens) }, enabled = canCreate) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") }
        }
    )
}

@Composable
private fun CreateEntregaDialog(
    granges: List<GranjaDto>,
    terres: List<TerraDto>,
    selectedCampanya: Int,
    onConfirm: (String, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGranjaId by remember(granges) { mutableStateOf(granges.firstOrNull()?.id ?: "") }
    var selectedTerraId by remember(terres) { mutableStateOf(terres.firstOrNull()?.id ?: "") }
    var data by remember { mutableStateOf("") }
    var tipusFertilitzant by remember { mutableStateOf("Puri") }
    var volumM3 by remember { mutableStateOf("") }
    var kgNM3 by remember { mutableStateOf("") }
    var kgN by remember { mutableStateOf("") }
    val canCreate = granges.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova entrega") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!canCreate) {
                    item {
                        Text(
                            "Per crear una entrega necessites almenys una granja d'origen.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        Text(
                            "Registra la sortida de dejeccions indicant origen, terra de destí i càlcul de nitrogen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Text(
                            "Campanya activa: $selectedCampanya",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        GranjaDropdown(granges = granges, selectedId = selectedGranjaId, onSelect = { selectedGranjaId = it }, label = "Granja d'origen")
                    }
                    item {
                        DateInputField(
                            value = data,
                            onValueChange = { data = it },
                            label = "Data (dd/MM/YYYY)",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (terres.isEmpty()) {
                        item {
                            Text("No tens cap terra accessible per seleccionar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        item {
                            TerraDropdown(terres = terres, selectedId = selectedTerraId, onSelect = { selectedTerraId = it }, label = "Terra desti")
                        }
                    }
                    item {
                        OutlinedTextField(value = tipusFertilitzant, onValueChange = { tipusFertilitzant = it }, label = { Text("Tipus fertilitzant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        NitrogenTripletFieldGroup(
                            kgN = kgN,
                            onKgNChange = { kgN = it },
                            volumM3 = volumM3,
                            onVolumM3Change = { volumM3 = it },
                            kgNPerM3 = kgNM3,
                            onKgNPerM3Change = { kgNM3 = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedGranjaId, data, selectedTerraId, tipusFertilitzant, volumM3, kgNM3, kgN) },
                enabled = canCreate && terres.isNotEmpty() && selectedTerraId.isNotBlank()
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
        confirmButton = { Button(onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel·lar") } }
    )
}

@Composable
private fun GranjaDropdown(
    granges: List<GranjaDto>,
    selectedId: String,
    onSelect: (String) -> Unit,
    label: String
) {
    SearchableSelectionField(
        items = granges,
        selectedItem = granges.find { it.id == selectedId },
        onSelect = { granja -> onSelect(granja?.id ?: "") },
        itemLabel = { "${it.nom ?: it.marca_oficial} (${it.marca_oficial})" },
        itemSearchText = { "${it.nom ?: ""} ${it.marca_oficial}" },
        label = label,
        placeholder = "Cerca una granja"
    )
}

@Composable
private fun BestiarDropdown(
    bestiars: List<BestiarDto>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    SearchableSelectionField(
        items = bestiars,
        selectedItem = bestiars.find { it.id == selectedId },
        onSelect = { bestiar -> onSelect(bestiar?.id ?: "") },
        itemLabel = { "${it.codi} - ${it.descripcio ?: ""}".trim() },
        itemSearchText = { "${it.codi} ${it.descripcio ?: ""}" },
        label = "Bestiar",
        placeholder = "Cerca per codi o descripcio"
    )
}

@Composable
private fun FaseDropdown(
    fases: List<FaseProductivaDto>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    SearchableSelectionField(
        items = fases,
        selectedItem = fases.find { it.id == selectedId },
        onSelect = { fase -> onSelect(fase?.id ?: "") },
        itemLabel = { "${it.codi} - ${it.descripcio ?: ""}".trim() },
        itemSearchText = { "${it.codi} ${it.descripcio ?: ""}" },
        label = "Fase productiva",
        placeholder = "Cerca una fase"
    )
}

@Composable
private fun TerraDropdown(
    terres: List<TerraDto>,
    selectedId: String,
    onSelect: (String) -> Unit,
    label: String
) {
    SearchableSelectionField(
        items = terres,
        selectedItem = terres.find { it.id == selectedId },
        onSelect = { terra -> onSelect(terra?.id ?: "") },
        itemLabel = { "${it.codi_sigpac_complet ?: it.id} · ${it.titular?.nom_rao ?: "-"}" },
        itemSearchText = { "${it.codi_sigpac_complet ?: it.id} ${it.titular?.nom_rao ?: ""} ${it.titular?.nif ?: ""} ${it.titular?.telefon ?: ""} ${it.titular?.codi_postal ?: ""}" },
        label = label,
        placeholder = "Cerca per codi SIGPAC"
    )
}

private fun formatEntregaReceptor(entrega: EntregaDejeccioDto): String {
    entrega.terra_desti?.let { terra ->
        val codi = terra.codi_sigpac_complet ?: terra.id
        val titular = terra.titular?.nom_rao ?: "-"
        return "$codi · $titular"
    }
    return "terra:${entrega.terra_desti_id ?: "-"}"
}

private fun formatBalanceNumber(value: Double?): String {
    if (value == null) return "-"
    val rounded = kotlin.math.round(value * 100) / 100
    return rounded.toString().replace('.', ',')
}

@Composable
private fun RamaderErrorBlock(error: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("< Tornar") }
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}
