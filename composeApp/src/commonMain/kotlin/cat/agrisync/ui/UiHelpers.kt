package cat.agrisync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import cat.agrisync.data.TitularCollaboratingOficinaSummary
import cat.agrisync.data.TitularCollaboratingTecnicSummary
import cat.agrisync.util.NitrogenField
import cat.agrisync.util.NitrogenTripletTexts
import cat.agrisync.util.autofillNitrogenTexts
import cat.agrisync.util.enteredDateToPickerMillis
import cat.agrisync.util.normalizeDateInput
import cat.agrisync.util.pickerMillisToEnteredDate

internal fun formatTimestamp(ts: String?): String {
    if (ts.isNullOrBlank()) return "-"
    return ts.replace("T", " ").take(16)
}

internal fun formatActorLabel(label: String?, fallbackUserId: String?): String {
    return label?.takeIf { it.isNotBlank() } ?: fallbackUserId?.takeIf { it.isNotBlank() } ?: "-"
}

@Composable
internal fun NitrogenTripletFieldGroup(
    kgN: String,
    onKgNChange: (String) -> Unit,
    volumM3: String,
    onVolumM3Change: (String) -> Unit,
    kgNPerM3: String,
    onKgNPerM3Change: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    fun applyUpdate(updated: NitrogenTripletTexts) {
        onKgNChange(updated.kgN)
        onVolumM3Change(updated.volumM3)
        onKgNPerM3Change(updated.kgNPerM3)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = kgN,
            onValueChange = {
                applyUpdate(
                    autofillNitrogenTexts(
                        NitrogenTripletTexts(kgN = kgN, volumM3 = volumM3, kgNPerM3 = kgNPerM3),
                        NitrogenField.TOTAL_KG_N,
                        it
                    )
                )
            },
            label = { Text("Kg N") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = volumM3,
                onValueChange = {
                    applyUpdate(
                        autofillNitrogenTexts(
                            NitrogenTripletTexts(kgN = kgN, volumM3 = volumM3, kgNPerM3 = kgNPerM3),
                            NitrogenField.VOLUME_M3,
                            it
                        )
                    )
                },
                label = { Text("Volum m3") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
            OutlinedTextField(
                value = kgNPerM3,
                onValueChange = {
                    applyUpdate(
                        autofillNitrogenTexts(
                            NitrogenTripletTexts(kgN = kgN, volumM3 = volumM3, kgNPerM3 = kgNPerM3),
                            NitrogenField.RATE_KG_N_M3,
                            it
                        )
                    )
                },
                label = { Text("Kg N/m3") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
        }
        Text(
            "Informa qualsevol combinacio de 2 camps. El tercer es calcula automaticament.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "dd/MM/YYYY"
) {
    var calendarExpanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { anchorHeightPx = it.height },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(normalizeDateInput(it)) },
                modifier = Modifier.weight(1f),
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true
            )
            OutlinedButton(onClick = { calendarExpanded = true }) {
                Text("Calendari")
            }
        }

        if (calendarExpanded) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = enteredDateToPickerMillis(value)
            )

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchorHeightPx + 8),
                onDismissRequest = { calendarExpanded = false }
                ,
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    modifier = Modifier.widthIn(min = 320.dp, max = 360.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = null,
                            headline = null,
                            showModeToggle = false,
                            modifier = Modifier.heightIn(max = 520.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                onValueChange("")
                                calendarExpanded = false
                            }) {
                                Text("Netejar")
                            }
                            TextButton(onClick = { calendarExpanded = false }) {
                                Text("Tancar")
                            }
                            Button(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let {
                                        onValueChange(pickerMillisToEnteredDate(it))
                                    }
                                    calendarExpanded = false
                                },
                                enabled = datePickerState.selectedDateMillis != null
                            ) {
                                Text("Aplicar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AuditInfoBlock(
    updatedAt: String?,
    updatedByLabel: String?,
    fallbackUserId: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "Ultima actualitzacio: ${formatTimestamp(updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Ultim editor: ${formatActorLabel(updatedByLabel, fallbackUserId)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun TitularCollaborationCard(
    oficines: List<TitularCollaboratingOficinaSummary>,
    tecnics: List<TitularCollaboratingTecnicSummary>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Col·laboracio", style = MaterialTheme.typography.titleSmall)
            Text(
                "Mostra quines oficines i quins tecnics tenen abast sobre aquest titular segons assignacions i comparticions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Oficines amb acces", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (oficines.isEmpty()) {
                Text(
                    "No s'han trobat oficines addicionals amb acces visible per aquest titular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                oficines.forEach { oficina ->
                    val details = buildList {
                        add("scopes: ${formatScopes(oficina.scopes)}")
                        if (oficina.hasDirectTecnics) add("tecnics assignats")
                        if (oficina.hasSharedAccess) add("oficina compartida")
                    }
                    CollaborationLine(
                        title = oficina.nom,
                        subtitle = details.joinToString(" · ")
                    )
                }
            }

            HorizontalDivider()

            Text("Tecnics que hi treballen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (tecnics.isEmpty()) {
                Text(
                    "No s'han trobat tecnics visibles per aquest titular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tecnics.forEach { tecnic ->
                    val details = buildList {
                        add(tecnic.oficinaNom)
                        tecnic.rol?.takeIf { it.isNotBlank() }?.let { add("rol: $it") }
                        add("scopes: ${formatScopes(tecnic.scopes)}")
                        tecnic.email?.takeIf { it.isNotBlank() }?.let { add(it) }
                        tecnic.telefon?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    CollaborationLine(
                        title = tecnic.nom,
                        subtitle = details.joinToString(" · ")
                    )
                }
            }
        }
    }
}

@Composable
private fun CollaborationLine(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatScopes(scopes: List<String>): String {
    if (scopes.isEmpty()) return "-"
    return scopes.joinToString(", ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SearchableSelectionField(
    items: List<T>,
    selectedItem: T?,
    onSelect: (T?) -> Unit,
    itemLabel: (T) -> String,
    itemSearchText: (T) -> String = itemLabel,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Escriu per cercar",
    emptyText: String = "No s'han trobat coincidencies",
    allowClearSelection: Boolean = false,
    clearSelectionLabel: String = "Sense seleccio",
    maxSuggestions: Int = 8
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selectedItem?.let(itemLabel).orEmpty()) }
    val selectedLabel = selectedItem?.let(itemLabel).orEmpty()

    LaunchedEffect(selectedLabel) {
        query = selectedLabel
    }

    val filtered = remember(items, query) {
        val cleanQuery = query.trim()
        val base = if (cleanQuery.isBlank()) {
            items
        } else {
            items.filter { itemSearchText(it).contains(cleanQuery, ignoreCase = true) }
        }
        base.take(maxSuggestions)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            if (allowClearSelection) {
                DropdownMenuItem(
                    text = { Text(clearSelectionLabel) },
                    onClick = {
                        query = ""
                        onSelect(null)
                        expanded = false
                    }
                )
            }

            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            emptyText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {},
                    enabled = false
                )
            } else {
                filtered.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemLabel(item)) },
                        onClick = {
                            query = itemLabel(item)
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
                if (items.size > filtered.size) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Mostrant ${filtered.size} suggerencies. Continua escrivint per afinar.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                }
            }
        }
    }
}
