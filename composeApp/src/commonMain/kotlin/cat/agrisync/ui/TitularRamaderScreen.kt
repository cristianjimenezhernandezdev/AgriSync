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
import cat.agrisync.viewmodel.TitularRamaderViewModel

@Composable
internal fun TitularRamaderScreen(
    viewModel: TitularRamaderViewModel,
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
                        Text("Modul Ramader", style = MaterialTheme.typography.titleLarge)
                    }
                }
                item {
                    Text(ui.titular?.nom_rao ?: "Titular", style = MaterialTheme.typography.titleMedium)
                    Text("NIF: ${ui.titular?.nif ?: "-"}")
                }
                item { Text("Granges", style = MaterialTheme.typography.titleMedium) }
                if (ui.granges.isEmpty()) {
                    item { Text("Sense granges") }
                } else {
                    items(ui.granges, key = { it.id }) { granja ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(granja.nom ?: granja.marca_oficial)
                                Text("Marca oficial: ${granja.marca_oficial}")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { Text("Granja bestiar", style = MaterialTheme.typography.titleMedium) }
                if (ui.granjaBestiar.isEmpty()) {
                    item { Text("Sense registres") }
                } else {
                    items(ui.granjaBestiar, key = { it.id }) { gb ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(gb.granja?.nom ?: gb.granja?.marca_oficial ?: "-")
                                Text("Bestiar: ${gb.bestiar?.codi ?: "-"} · Fase: ${gb.fase_productiva?.codi ?: "-"}")
                                Text("Cens: ${gb.cens ?: 0.0}")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { Text("Entrega dejeccions", style = MaterialTheme.typography.titleMedium) }
                if (ui.entregues.isEmpty()) {
                    item { Text("Sense entregues") }
                } else {
                    items(ui.entregues, key = { it.id }) { e ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Data: ${e.data ?: "-"}")
                                Text("Quantitat: ${e.quantitat ?: 0.0}")
                                Text("Receptor: ${e.receptor_titular_id ?: "terra:${e.terra_desti_id ?: "-"}"}")
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
