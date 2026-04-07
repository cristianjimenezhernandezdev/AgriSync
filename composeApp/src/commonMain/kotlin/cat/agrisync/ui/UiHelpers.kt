package cat.agrisync.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun formatTimestamp(ts: String?): String {
    if (ts.isNullOrBlank()) return "-"
    return ts.replace("T", " ").take(16)
}

internal fun formatActorLabel(label: String?, fallbackUserId: String?): String {
    return label?.takeIf { it.isNotBlank() } ?: fallbackUserId?.takeIf { it.isNotBlank() } ?: "-"
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
