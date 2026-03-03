package cat.agrisync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.agrisync.data.AppServices
import cat.agrisync.data.AuthState
import cat.agrisync.data.EnvConfig
import cat.agrisync.data.OficinaDto
import cat.agrisync.ui.LoginScreen
import cat.agrisync.ui.ProfileScreen
import cat.agrisync.ui.TitularAgricolaScreen
import cat.agrisync.ui.TitularRamaderScreen
import cat.agrisync.ui.TitularsScreen
import cat.agrisync.ui.navigation.Screen
import cat.agrisync.viewmodel.HomeViewModel
import cat.agrisync.viewmodel.LoginViewModel
import cat.agrisync.viewmodel.TitularAgricolaViewModel
import cat.agrisync.viewmodel.TitularRamaderViewModel

@Composable
fun App(envConfig: EnvConfig) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val config = remember { envConfig.load() }
            if (config == null) {
                MissingConfigScreen(envConfig.missingMessage())
                return@Surface
            }

            val services = remember { AppServices.create(config) }
            val authState by services.authService.state.collectAsState()

            // LoginViewModel persistent — no es destrueix quan authState canvia
            val loginVm = remember { LoginViewModel(services.authService) }
            DisposableEffect(Unit) { onDispose { loginVm.clear() } }

            LaunchedEffect(Unit) {
                services.authService.initialize()
            }

            when (authState) {
                is AuthState.Initializing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                AuthState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.padding(8.dp))
                            Text("Iniciant sessio...")
                        }
                    }
                }

                is AuthState.Authenticated -> {
                    val data = authState as AuthState.Authenticated
                    AuthenticatedContent(services, data)
                }

                is AuthState.Error -> {
                    val err = authState as AuthState.Error
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text(err.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        LoginScreen(loginVm)
                    }
                }

                AuthState.Unauthenticated -> {
                    LoginScreen(loginVm)
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedContent(services: AppServices, data: AuthState.Authenticated) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TitularsHome) }
    var oficina by remember { mutableStateOf<OficinaDto?>(null) }

    LaunchedEffect(data.tecnic.oficina_id) {
        oficina = runCatching { services.oficinaRepository.getById(data.tecnic.oficina_id) }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("AgriSync", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { currentScreen = Screen.TitularsHome }) { Text("Titulars") }
                    TextButton(onClick = { currentScreen = Screen.Profile }) { Text("Perfil") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(data.tecnic.nom)
                    Button(onClick = services.authService::signOut) { Text("Logout") }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (val screen = currentScreen) {
                Screen.TitularsHome -> {
                    val vm = remember { HomeViewModel(services.accessRepository) }
                    DisposableEffect(Unit) { onDispose { vm.clear() } }
                    LaunchedEffect(Unit) { vm.load() }
                    TitularsScreen(
                        viewModel = vm,
                        onOpenAgricola = { currentScreen = Screen.TitularAgricola(it) },
                        onOpenRamader = { currentScreen = Screen.TitularRamader(it) }
                    )
                }

                Screen.Profile -> {
                    ProfileScreen(
                        tecnic = data.tecnic,
                        oficina = oficina,
                        onBack = { currentScreen = Screen.TitularsHome }
                    )
                }

                is Screen.TitularAgricola -> {
                    val vm = remember(screen.titularId) { TitularAgricolaViewModel(services.agricolaRepository) }
                    DisposableEffect(screen.titularId) { onDispose { vm.clear() } }
                    LaunchedEffect(screen.titularId) { vm.load(screen.titularId) }
                    TitularAgricolaScreen(
                        viewModel = vm,
                        onBack = { currentScreen = Screen.TitularsHome }
                    )
                }

                is Screen.TitularRamader -> {
                    val vm = remember(screen.titularId) { TitularRamaderViewModel(services.ramaderRepository) }
                    DisposableEffect(screen.titularId) { onDispose { vm.clear() } }
                    LaunchedEffect(screen.titularId) { vm.load(screen.titularId) }
                    TitularRamaderScreen(
                        viewModel = vm,
                        onBack = { currentScreen = Screen.TitularsHome }
                    )
                }

                Screen.Login -> Unit
            }
        }
    }
}

@Composable
private fun MissingConfigScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Configuracio incompleta", style = MaterialTheme.typography.headlineSmall)
        Text(message)
    }
}
