package cat.agrisync.ui.navigation

/** Pantalles de l'app. */
sealed interface Screen {
    data object Login : Screen
    data object TitularsHome : Screen
    data object Profile : Screen
    data class TitularAgricola(val titularId: String) : Screen
    data class TitularRamader(val titularId: String) : Screen
}
