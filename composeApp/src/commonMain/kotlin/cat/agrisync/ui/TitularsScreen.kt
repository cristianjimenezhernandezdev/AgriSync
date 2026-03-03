package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.TitularAccessRow
import cat.agrisync.viewmodel.HomeViewModel

@Composable
internal fun TitularsScreen(
    viewModel: HomeViewModel,
    onOpenAgricola: (String) -> Unit,
    onOpenRamader: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchNif,
                onValueChange = viewModel::onSearchNifChange,
                label = { Text("Cercar per NIF o nom") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }

            uiState.filtered.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hi ha titulars assignats", style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.pageItems, key = { it.titular_id }) { item ->
                        TitularCard(
                            row = item,
                            onOpenAgricola = onOpenAgricola,
                            onOpenRamader = onOpenRamader
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = viewModel::prevPage, enabled = uiState.currentPage > 0) {
                        Text("< Anterior")
                    }
                    Text(" Pag. ${uiState.currentPage + 1}/${uiState.totalPages} ")
                    TextButton(onClick = viewModel::nextPage, enabled = uiState.currentPage + 1 < uiState.totalPages) {
                        Text("Seguent >")
                    }
                }
            }
        }
    }
}

@Composable
private fun TitularCard(
    row: TitularAccessRow,
    onOpenAgricola: (String) -> Unit,
    onOpenRamader: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.nom, style = MaterialTheme.typography.titleMedium)
            Text("NIF: ${row.nif ?: "-"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Ultima edicio: ${formatTimestamp(row.last_update_at)} · ${row.last_update_by ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (row.can_agricola) {
                    AssistChip(onClick = { onOpenAgricola(row.titular_id) }, label = { Text("Agricola") })
                }
                if (row.can_ramader) {
                    AssistChip(onClick = { onOpenRamader(row.titular_id) }, label = { Text("Ramader") })
                }
            }
        }
    }
}

private fun formatTimestamp(ts: String?): String {
    if (ts.isNullOrBlank()) return "-"
    return ts.replace("T", " ").take(16)
}
