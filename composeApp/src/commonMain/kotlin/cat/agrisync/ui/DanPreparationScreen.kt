package cat.agrisync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cat.agrisync.data.DanPreparationAplicacioDto
import cat.agrisync.data.DanPreparationEntregaDto
import cat.agrisync.data.DanPreparationTerraDto
import cat.agrisync.data.GranjaBestiarDto
import cat.agrisync.data.GranjaDto
import cat.agrisync.viewmodel.DanPreparationViewModel
import cat.agrisync.viewmodel.DanPreparationUiState
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.round

@Composable
internal fun DanPreparationScreen(
    viewModel: DanPreparationViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.error != null -> DanPreparationErrorBlock(ui.error ?: "Error carregant el resum DAN", onBack)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onBack) { Text("< Tornar") }
                            Text("Preparar DAN", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    item {
                        InfoCard(
                            title = "Pantalla de preparacio",
                            body = "Aquest resum agrupa les dades que ja tens entrades a AgriSync i les presenta d'una manera mes facil de traslladar a l'aplicatiu extern de la DAN."
                        )
                    }

                    item {
                        TitularSummaryCard(ui)
                    }

                    item {
                        PreparationActionsCard(
                            onCopySummary = {
                                clipboardManager.setText(AnnotatedString(ui.buildClipboardSummary()))
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("Resum DAN copiat al porta-retalls")
                                }
                            },
                            onCopyChecklist = {
                                clipboardManager.setText(AnnotatedString(ui.buildClipboardChecklist()))
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("Checklist DAN copiada al porta-retalls")
                                }
                            }
                        )
                    }

                    item {
                        CampaignSelectorCard(
                            selectedCampanya = ui.selectedCampanya,
                            availableCampanyes = ui.availableCampanyes,
                            onSelect = { campanya ->
                                ui.titular?.id?.let { viewModel.onSelectCampanya(it, campanya) }
                            }
                        )
                    }

                    item {
                        SummaryMetrics(ui)
                    }

                    item {
                        AutomaticChecklistCard(ui)
                    }

                    item {
                        SectionTitle(
                            title = "Dades agricoles per trasllat",
                            description = "Parcel·les i aplicacions fertilitzants que s'assemblen als camps vistos als PDFs reals de DAN."
                        )
                    }

                    if (ui.terres.isEmpty() && ui.aplicacions.isEmpty()) {
                        item {
                            EmptyPreparationCard(
                                title = "Sense dades agricoles",
                                body = "Aquest titular no te terres ni aplicacions fertilitzants registrades ara mateix."
                            )
                        }
                    } else {
                        if (ui.terres.isNotEmpty()) {
                            item { SubsectionTitle("Terres / recintes") }
                            items(ui.terres, key = { it.id }) { terra ->
                                TerraPreparationCard(
                                    terra = terra,
                                    appliedKgN = ui.totalKgNByTerra(terra.id),
                                    kgNPerHa = ui.kgNPerHaByTerra(terra.id),
                                    allowedKgN = ui.allowedKgNByTerra(terra.id),
                                    excessKgN = ui.excessKgNByTerra(terra.id)
                                )
                            }
                        }

                        if (ui.aplicacions.isNotEmpty()) {
                            item { SubsectionTitle("Aplicacions fertilitzants") }
                            items(ui.aplicacions, key = { it.id }) { aplicacio ->
                                AplicacioPreparationCard(aplicacio = aplicacio)
                            }
                        }
                    }

                    item {
                        SectionTitle(
                            title = "Dades ramaderes per trasllat",
                            description = "Informacio de granges, censos i entregues que ajuda a completar la DAN ramadera o el resum de gestio."
                        )
                    }

                    item {
                        if (ui.granges.isEmpty() && ui.granjaBestiar.isEmpty() && ui.entregues.isEmpty()) {
                            EmptyPreparationCard(
                                title = "Sense dades ramaderes",
                                body = "Aquest titular no te granges, censos ni entregues registrades ara mateix."
                            )
                        }
                    }
                    if (ui.granges.isNotEmpty() || ui.granjaBestiar.isNotEmpty() || ui.entregues.isNotEmpty()) {
                        if (ui.granges.isNotEmpty()) {
                            item { SubsectionTitle("Granges") }
                            items(ui.granges, key = { it.id }) { granja ->
                                GranjaPreparationCard(granja)
                            }
                        }

                        if (ui.granjaBestiar.isNotEmpty()) {
                            item { SubsectionTitle("Cens per bestiar i fase") }
                            items(ui.granjaBestiar, key = { it.id }) { registre ->
                                GranjaBestiarPreparationCard(registre)
                            }
                        }

                        if (ui.entregues.isNotEmpty()) {
                            item { SubsectionTitle("Entregues de dejeccions") }
                            items(ui.entregues, key = { it.id }) { entrega ->
                                EntregaPreparationCard(entrega)
                            }
                        }
                    }

                    item {
                        SectionTitle(
                            title = "Camps a revisar manualment",
                            description = "Als PDFs reals hi ha camps finals que el MVP encara no calcula o no desa explicitament."
                        )
                    }

                    item {
                        ManualReviewCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun TitularSummaryCard(ui: DanPreparationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(ui.titular?.nom_rao ?: "Titular", style = MaterialTheme.typography.titleMedium)
            Text("NIF: ${ui.titular?.nif ?: "-"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${ui.availableCampanyes.size} campanyes detectades") })
                AssistChip(
                    onClick = {},
                    label = { Text(if (ui.selectedCampanya == 0) "Sense campanya" else "Treballant ${ui.selectedCampanya}") }
                )
            }
            Text(
                "Aquesta pantalla prioritza la lectura i la transcripcio. Les dades es mostren en el mateix llenguatge funcional que apareix als resums de DAN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreparationActionsCard(
    onCopySummary: () -> Unit,
    onCopyChecklist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Sortida rapida", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Pots copiar un resum estructurat o nomes la checklist final per enganxar-la a notes, correus o al flux extern de preparacio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopySummary) { Text("Copiar resum") }
                OutlinedButton(onClick = onCopyChecklist) { Text("Copiar checklist") }
            }
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Campanya de resum", style = MaterialTheme.typography.titleSmall)
            Text(
                "Els totals de kg N, volum m3 i entregues es calculen per la campanya seleccionada.",
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
private fun SummaryMetrics(ui: DanPreparationUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricsRow(
            leftLabel = "Terres",
            leftValue = ui.terres.size.toString(),
            rightLabel = "Total ha",
            rightValue = formatDecimal(ui.totalHectares)
        )
        MetricsRow(
            leftLabel = "Aplicacions",
            leftValue = ui.aplicacions.size.toString(),
            rightLabel = "Kg N total",
            rightValue = formatDecimal(ui.totalKgN)
        )
        MetricsRow(
            leftLabel = "Volum total m3",
            leftValue = formatDecimal(ui.totalVolumM3),
            rightLabel = "Kg N/ha",
            rightValue = formatDecimal(ui.kgNPerHa)
        )
        MetricsRow(
            leftLabel = "Granges",
            leftValue = ui.granges.size.toString(),
            rightLabel = "Cens total",
            rightValue = formatDecimal(ui.totalCens)
        )
        MetricsRow(
            leftLabel = "Entregues",
            leftValue = ui.entregues.size.toString(),
            rightLabel = "Total entregat",
            rightValue = formatDecimal(ui.totalQuantitatEntregada)
        )
        MetricsRow(leftLabel = "Checklist", leftValue = "1 bloc final", rightLabel = "Campanya", rightValue = ui.selectedCampanya.toString())
    }
}

@Composable
private fun AutomaticChecklistCard(ui: DanPreparationUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Checklist automatica", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            ui.automaticChecklistItems().forEach { item ->
                Text("- $item", color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
private fun MetricsRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            label = leftLabel,
            value = leftValue
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            label = rightLabel,
            value = rightValue
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubsectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun TerraPreparationCard(
    terra: DanPreparationTerraDto,
    appliedKgN: Double,
    kgNPerHa: Double?,
    allowedKgN: Double?,
    excessKgN: Double?
) {
    val limitKgN = terra.limit_kg_n_ha ?: if (terra.zona == "ZV") 170.0 else 190.0
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Recinte: ${terra.codi_sigpac_complet ?: terra.id}", style = MaterialTheme.typography.titleSmall)
            PreparationFieldRow("Municipi", terra.mun_codi ?: "-")
            PreparationFieldRow("Municipi literal", terra.municipi_literal ?: "-")
            PreparationFieldRow("Poligon", terra.poligon?.toString() ?: "-")
            PreparationFieldRow("Parcela", terra.parcela?.toString() ?: "-")
            PreparationFieldRow("Recinte", terra.recinte?.toString() ?: "-")
            PreparationFieldRow("Us SIGPAC", terra.us_sigpac ?: "-")
            PreparationFieldRow("Cultiu", terra.cultiu ?: "-")
            PreparationFieldRow("ha", formatDecimal(terra.superficie))
            PreparationFieldRow("Zona", terra.zona)
            PreparationFieldRow("Limit kg N/ha", formatDecimal(limitKgN))
            PreparationFieldRow("Kg N aplicats", formatDecimal(appliedKgN))
            PreparationFieldRow("Kg N/ha campanya", formatDecimal(kgNPerHa))
            PreparationFieldRow("Kg N totals permesos", formatDecimal(allowedKgN))
            PreparationFieldRow("Excés sobre límit", formatDecimal(excessKgN))
        }
    }
}

@Composable
private fun AplicacioPreparationCard(
    aplicacio: DanPreparationAplicacioDto
) {
    val terra = aplicacio.terra
    val hectares = terra?.superficie
    val kgNPerHa = if ((hectares ?: 0.0) > 0.0) (aplicacio.kg_n ?: 0.0) / (hectares ?: 1.0) else null

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Aplicacio ${aplicacio.dan?.campanya ?: "-"}", style = MaterialTheme.typography.titleSmall)
            PreparationFieldRow("Mun.", terra?.mun_codi ?: "-")
            PreparationFieldRow("Pol.", terra?.poligon?.toString() ?: "-")
            PreparationFieldRow("Par.", terra?.parcela?.toString() ?: "-")
            PreparationFieldRow("Rec.", terra?.recinte?.toString() ?: "-")
            PreparationFieldRow("ha", formatDecimal(terra?.superficie))
            PreparationFieldRow("Data", aplicacio.data ?: "-")
            PreparationFieldRow("Dia", dayFromDate(aplicacio.data))
            PreparationFieldRow("Mes", monthName(aplicacio.data))
            PreparationFieldRow("Tipus fertilitzant", aplicacio.tipus_fertilitzant ?: "-")
            PreparationFieldRow("Procedencia", aplicacio.procedencia ?: "-")
            PreparationFieldRow("Volum m3", formatDecimal(aplicacio.volum_m3))
            PreparationFieldRow("Kg N/m3", formatDecimal(aplicacio.kg_n_m3))
            PreparationFieldRow("kg N", formatDecimal(aplicacio.kg_n))
            PreparationFieldRow("kg N/ha", formatDecimal(kgNPerHa))
            PreparationFieldRow("Campanya", aplicacio.dan?.campanya?.toString() ?: "-")
        }
    }
}

@Composable
private fun GranjaPreparationCard(granja: GranjaDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(granja.nom ?: granja.marca_oficial, style = MaterialTheme.typography.titleSmall)
            PreparationFieldRow("Marca oficial", granja.marca_oficial)
            PreparationFieldRow("Nom granja", granja.nom ?: "-")
        }
    }
}

@Composable
private fun GranjaBestiarPreparationCard(registre: GranjaBestiarDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(registre.granja?.nom ?: registre.granja?.marca_oficial ?: "Registre de bestiar", style = MaterialTheme.typography.titleSmall)
            PreparationFieldRow("Marca oficial", registre.granja?.marca_oficial ?: "-")
            PreparationFieldRow("Bestiar", registre.bestiar?.codi ?: "-")
            PreparationFieldRow("Descripcio", registre.bestiar?.descripcio ?: "-")
            PreparationFieldRow("Fase", registre.fase_productiva?.codi ?: "-")
            PreparationFieldRow("Fase descripcio", registre.fase_productiva?.descripcio ?: "-")
            PreparationFieldRow("Cens", formatDecimal(registre.cens))
        }
    }
}

@Composable
private fun EntregaPreparationCard(entrega: DanPreparationEntregaDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Entrega ${entrega.dan?.campanya ?: "-"}", style = MaterialTheme.typography.titleSmall)
            PreparationFieldRow("Data", entrega.data ?: "-")
            PreparationFieldRow("Dia", dayFromDate(entrega.data))
            PreparationFieldRow("Mes", monthName(entrega.data))
            PreparationFieldRow("Quantitat", formatDecimal(entrega.quantitat))
            PreparationFieldRow("Granja origen", entrega.granja_origen?.marca_oficial ?: "-")
            PreparationFieldRow("Nom origen", entrega.granja_origen?.nom ?: "-")
            PreparationFieldRow("Receptor titular", entrega.receptor_titular?.nom_rao ?: "-")
            PreparationFieldRow("NIF receptor", entrega.receptor_titular?.nif ?: "-")
            PreparationFieldRow("Terra desti", entrega.terra_desti?.codi_sigpac_complet ?: "-")
            PreparationFieldRow("Campanya", entrega.dan?.campanya?.toString() ?: "-")
        }
    }
}

@Composable
private fun ManualReviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Comprovacions finals", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("Agricola: revisar S/R i qualsevol camp normatiu extern que encara no estigui modelat al sistema.", color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("Ramadera: revisar estat de lliurament, persona que presenta, nitrogen total a gestionar, balanc i estoc final.", color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("Si algun d'aquests camps es necessita de manera recurrent, ja tenim una base clara per una iteracio posterior mes orientada a la DAN final.", color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun EmptyPreparationCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PreparationFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(110.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DanPreparationErrorBlock(error: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("< Tornar") }
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}

private fun dayFromDate(date: String?): String = date?.split("-")?.getOrNull(2) ?: "-"

private fun monthName(date: String?): String {
    val month = date?.split("-")?.getOrNull(1) ?: return "-"
    return when (month) {
        "01" -> "Gener"
        "02" -> "Febrer"
        "03" -> "Marc"
        "04" -> "Abril"
        "05" -> "Maig"
        "06" -> "Juny"
        "07" -> "Juliol"
        "08" -> "Agost"
        "09" -> "Setembre"
        "10" -> "Octubre"
        "11" -> "Novembre"
        "12" -> "Desembre"
        else -> "-"
    }
}

private fun formatDecimal(value: Double?, decimals: Int = 2): String {
    if (value == null) return "-"
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    return rounded.toString().replace('.', ',')
}
