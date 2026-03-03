package cat.agrisync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.OficinaDto
import cat.agrisync.data.TecnicDto

@Composable
internal fun ProfileScreen(
    tecnic: TecnicDto,
    oficina: OficinaDto?,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("El meu perfil", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Tornar") }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileRow("Nom", tecnic.nom)
                ProfileRow("Email", tecnic.email ?: "—")
                ProfileRow("Rol", tecnic.rol ?: "tecnic")
                ProfileRow("Oficina", oficina?.nom ?: tecnic.oficina_id)
                ProfileRow("Actiu", if (tecnic.actiu) "Si" else "No")
                ProfileRow("User ID", tecnic.user_id ?: "—")
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

