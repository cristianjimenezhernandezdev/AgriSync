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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.viewmodel.TitularAgricolaViewModel

@Composable
internal fun TitularAgricolaScreen(
    viewModel: TitularAgricolaViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()

    when {
        ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ui.error != null -> ErrorBlock(ui.error ?: "Error", onBack)
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onBack) { Text("< Tornar") }
                        Text("Modul Agricola", style = MaterialTheme.typography.titleLarge)
                    }
                }
                item {
                    Text(ui.titular?.nom_rao ?: "Titular", style = MaterialTheme.typography.titleMedium)
                    Text("NIF: ${ui.titular?.nif ?: "-"}")
                }
                item { Text("Terres", style = MaterialTheme.typography.titleMedium) }
                if (ui.terres.isEmpty()) {
                    item { Text("Sense terres") }
                } else {
                    items(ui.terres, key = { it.id }) { terra ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(terra.codi_sigpac_complet ?: terra.id)
                                Text("Superficie: ${terra.superficie ?: 0.0}")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item { Text("Aplicacions fertilitzants", style = MaterialTheme.typography.titleMedium) }
                if (ui.aplicacions.isEmpty()) {
                    item { Text("Sense aplicacions") }
                } else {
                    items(ui.aplicacions, key = { it.id }) { app ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Data: ${app.data ?: "-"}")
                                Text("Kg N: ${app.kg_n ?: 0.0} · UF: ${app.uf ?: 0.0}")
                                Text("Campanya: ${app.dan?.campanya ?: "-"}")
                            }
                        }
                    }
                }
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

