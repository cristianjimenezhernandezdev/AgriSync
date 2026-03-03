package cat.agrisync.data

import java.util.prefs.Preferences

actual object SessionPersistence {
    private val prefs: Preferences = Preferences.userRoot().node("cat.agrisync.session")
    private const val KEY = "stored_session"

    actual fun save(session: StoredSession) {
        val raw = SupabaseJson.instance.encodeToString(StoredSession.serializer(), session)
        prefs.put(KEY, raw)
    }

    actual fun load(): StoredSession? {
        val raw = prefs.get(KEY, null) ?: return null
        return runCatching {
            SupabaseJson.instance.decodeFromString(StoredSession.serializer(), raw)
        }.getOrNull()
    }

    actual fun clear() {
        prefs.remove(KEY)
    }
}

