package cat.agrisync.data

import java.io.File
import java.util.Properties

class JvmEnvConfig : EnvConfig {

    private val propertiesFileNames = listOf("agrisync.properties", "config/agrisync.properties")

    private fun loadPropertiesFile(): Properties? {
        val workingDir = File(System.getProperty("user.dir"))
        val candidates = buildList {
            propertiesFileNames.forEach { add(File(workingDir, it)) }
            runCatching {
                val jarDir = File(
                    JvmEnvConfig::class.java.protectionDomain.codeSource.location.toURI()
                ).parentFile
                propertiesFileNames.forEach { add(File(jarDir, it)) }
            }
        }

        val file = candidates.firstOrNull { it.exists() && it.isFile } ?: return null
        return Properties().apply {
            file.inputStream().use { load(it) }
        }
    }

    private fun resolve(envName: String, props: Properties?): String? {
        System.getProperty(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        props?.getProperty(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return null
    }

    override fun load(): SupabaseConfig? {
        val props = loadPropertiesFile()
        val url = resolve("SUPABASE_URL", props) ?: return null
        val anonKey = resolve("SUPABASE_ANON_KEY", props) ?: return null
        val serviceRoleKey = resolve("SUPABASE_SERVICE_ROLE_KEY", props) ?: return null
        return SupabaseConfig(
            url = url,
            anonKey = anonKey,
            serviceRoleKey = serviceRoleKey
        )
    }

    override fun missingMessage(): String {
        val props = loadPropertiesFile()
        val missing = buildList {
            if (resolve("SUPABASE_URL", props) == null) add("SUPABASE_URL")
            if (resolve("SUPABASE_ANON_KEY", props) == null) add("SUPABASE_ANON_KEY")
            if (resolve("SUPABASE_SERVICE_ROLE_KEY", props) == null) add("SUPABASE_SERVICE_ROLE_KEY")
        }
        return if (missing.isEmpty()) {
            "Configuracio carregada correctament"
        } else {
            "Falten dades de configuracio: ${missing.joinToString(", ")}. Pots definir-les com variables d'entorn o al fitxer agrisync.properties."
        }
    }
}
