package cat.agrisync.ui.navigation

/** Pantalles de l'app. */
sealed interface Screen {
    data object Login : Screen
    data object TitularsHome : Screen
    data object Profile : Screen
    data class DanPreparation(val titularId: String) : Screen
    data class TitularAgricola(val titularId: String, val canEditTitularCommon: Boolean = false) : Screen
    data class TitularRamader(val titularId: String, val canEditTitularCommon: Boolean = false) : Screen
    data object TecnicManagement : Screen
    data class TecnicDetail(val tecnicId: String) : Screen
    data object TitularManagement : Screen
    data object TerraManagement : Screen
    data object OficinaManagement : Screen
}
