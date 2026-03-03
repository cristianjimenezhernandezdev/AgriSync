package cat.agrisync.data

import kotlinx.serialization.json.Json

object SupabaseJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

