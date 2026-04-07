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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.TitularAccessRow
import cat.agrisync.viewmodel.HomeViewModel

@Composable
internal fun TitularsScreen(
    viewModel: HomeViewModel,
    onOpenDanPreparation: (String) -> Unit,
    onOpenAgricola: (String) -> Unit,
    onOpenRamader: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFiltered = uiState.searchNif.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchNif,
            onValueChange = viewModel::onSearchNifChange,
            label = { Text("Cercar per NIF o nom") },
            placeholder = { Text("Exemple: 40325245N o Jordi") },
            supportingText = {
                Text("La cerca es fa sobre els titulars als quals tens accés")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = {}, label = { Text("${uiState.filtered.size} visibles") })
            AssistChip(onClick = {}, label = { Text("${uiState.totalPages} pagines") })
            if (isFiltered) {
                AssistChip(onClick = {}, label = { Text("Filtre actiu") })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                MessageCard(
                    title = "No s'han pogut carregar els titulars",
                    message = uiState.error ?: "",
                    isError = true
                )
            }

            uiState.filtered.isEmpty() -> {
                val title = if (isFiltered) "Cap resultat amb aquest filtre" else "No hi ha titulars visibles"
                val message = if (isFiltered) {
                    "Canvia el text de cerca o esborra'l per tornar a veure tots els titulars accessibles."
                } else {
                    "Revisa si el teu usuari te assignacions actives o si el teu rol et permet veure titulars."
                }
                MessageCard(title = title, message = message)
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.pageItems, key = { it.titular_id }) { item ->
                        TitularCard(
                            row = item,
                            actorLabel = uiState.actorLabels[item.last_update_by],
                            onOpenDanPreparation = onOpenDanPreparation,
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
                    Text("Pag. ${uiState.currentPage + 1}/${uiState.totalPages}")
                    TextButton(onClick = viewModel::nextPage, enabled = uiState.currentPage + 1 < uiState.totalPages) {
                        Text("Seguent >")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String, isError: Boolean = false) {
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            }
        }
    }
}

@Composable
private fun TitularCard(
    row: TitularAccessRow,
    actorLabel: String?,
    onOpenDanPreparation: (String) -> Unit,
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
            AuditInfoBlock(
                updatedAt = row.last_update_at,
                updatedByLabel = actorLabel,
                fallbackUserId = row.last_update_by
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { onOpenDanPreparation(row.titular_id) }, label = { Text("Preparar DAN") })
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
