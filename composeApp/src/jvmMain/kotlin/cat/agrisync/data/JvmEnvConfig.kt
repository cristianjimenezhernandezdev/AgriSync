package cat.agrisync.data

class JvmEnvConfig : EnvConfig {

    private fun resolve(envName: String): String? {
        System.getProperty(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return null
    }

    override fun load(): SupabaseConfig? {
        val url = resolve("SUPABASE_URL") ?: return null
        val anonKey = resolve("SUPABASE_ANON_KEY") ?: return null
        val serviceRoleKey = resolve("SUPABASE_SERVICE_ROLE_KEY") ?: return null
        return SupabaseConfig(
            url = url,
            anonKey = anonKey,
            serviceRoleKey = serviceRoleKey
        )
    }

    override fun missingMessage(): String {
        val missing = buildList {
            if (resolve("SUPABASE_URL") == null) add("SUPABASE_URL")
            if (resolve("SUPABASE_ANON_KEY") == null) add("SUPABASE_ANON_KEY")
            if (resolve("SUPABASE_SERVICE_ROLE_KEY") == null) add("SUPABASE_SERVICE_ROLE_KEY")
        }
        return if (missing.isEmpty()) {
            "Configuracio carregada correctament"
        } else {
            "Falten variables d'entorn: ${missing.joinToString(", ")}"
        }
    }
}
