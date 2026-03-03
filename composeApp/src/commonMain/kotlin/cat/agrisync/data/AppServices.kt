package cat.agrisync.data

internal class AppServices private constructor(
    val authService: AuthService,
    val accessRepository: AccessRepository,
    val agricolaRepository: AgricolaRepository,
    val ramaderRepository: RamaderRepository,
    val oficinaRepository: OficinaRepository
) {
    companion object {
        internal fun create(config: SupabaseConfig): AppServices {
            val httpClient = SupabaseHttpClient.create()
            val authApi = SupabaseAuthApi(httpClient, config)
            val authService = AuthService(authApi)
            val restClient = RestClient(httpClient, config) { authService.sessionOrNull() }
            return AppServices(
                authService = authService,
                accessRepository = AccessRepository(restClient),
                agricolaRepository = AgricolaRepository(restClient),
                ramaderRepository = RamaderRepository(restClient),
                oficinaRepository = OficinaRepository(restClient)
            )
        }
    }
}
