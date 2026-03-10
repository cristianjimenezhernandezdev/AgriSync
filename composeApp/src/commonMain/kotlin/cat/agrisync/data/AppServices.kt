package cat.agrisync.data

internal class AppServices private constructor(
    val authService: AuthService,
    val accessRepository: AccessRepository,
    val agricolaRepository: AgricolaRepository,
    val ramaderRepository: RamaderRepository,
    val oficinaRepository: OficinaRepository,
    val tecnicRepository: TecnicRepository,
    val titularManagementRepository: TitularManagementRepository
) {
    companion object {
        internal fun create(config: SupabaseConfig): AppServices {
            val httpClient = SupabaseHttpClient.create()
            val authApi = SupabaseAuthApi(httpClient, config)
            val authService = AuthService(authApi)

            // Client autenticat: usa el token JWT de l'usuari loguejat
            // Les RLS policies a Supabase controlen l'accés real
            val userRestClient = RestClient(httpClient, config) {
                authService.sessionOrNull()
            }

            // Client admin amb service_role (per operacions que necessiten bypass RLS)
            val adminConfig = SupabaseConfig(
                url = config.url,
                anonKey = config.serviceRoleKey.ifBlank { config.anonKey },
                serviceRoleKey = config.serviceRoleKey
            )
            val adminRestClient = RestClient(httpClient, adminConfig) {
                Session(
                    accessToken = config.serviceRoleKey,
                    refreshToken = "",
                    user = AuthUser(id = "service_role", email = "admin")
                )
            }

            // Repositoris usen el token de l'usuari autenticat (RLS ho controla)
            // TecnicRepository usa adminRestClient per gestió de tecnics (necessita service_role)
            return AppServices(
                authService = authService,
                accessRepository = AccessRepository(userRestClient),
                agricolaRepository = AgricolaRepository(userRestClient),
                ramaderRepository = RamaderRepository(userRestClient),
                oficinaRepository = OficinaRepository(userRestClient),
                tecnicRepository = TecnicRepository(userRestClient, httpClient, config),
                titularManagementRepository = TitularManagementRepository(userRestClient)
            )
        }
    }
}
