package cat.agrisync.data

import java.io.File
import java.util.Properties

class JvmEnvConfig : EnvConfig {

    private val propertiesFileNames = listOf("agrisync.properties", "config/agrisync.properties")

    private fun addIfDirectory(target: MutableSet<File>, directory: File?) {
        val normalized = runCatching { directory?.canonicalFile }.getOrNull() ?: return
        if (normalized.exists() && normalized.isDirectory) {
            target += normalized
        }
    }

    private fun candidateDirectories(): List<File> {
        val directories = linkedSetOf<File>()

        addIfDirectory(directories, File(System.getProperty("user.dir")))

        runCatching {
            val codeSource = File(
                JvmEnvConfig::class.java.protectionDomain.codeSource.location.toURI()
            ).canonicalFile
            val codeSourceDir = codeSource.takeIf { it.isDirectory } ?: codeSource.parentFile
            addIfDirectory(directories, codeSourceDir)
            addIfDirectory(directories, codeSourceDir?.parentFile)
            addIfDirectory(directories, codeSourceDir?.parentFile?.parentFile)
        }

        runCatching {
            val command = ProcessHandle.current().info().command().orElse(null) ?: return@runCatching
            val executable = File(command).canonicalFile
            addIfDirectory(directories, executable.parentFile)
            addIfDirectory(directories, executable.parentFile?.parentFile)
        }

        System.getProperty("compose.application.resources.dir")?.let { addIfDirectory(directories, File(it)) }
        System.getenv("APP_HOME")?.let { addIfDirectory(directories, File(it)) }

        return directories.toList()
    }

    private fun loadPropertiesFile(): Properties? {
        val candidates = buildList {
            candidateDirectories().forEach { directory ->
                propertiesFileNames.forEach { add(File(directory, it)) }
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
