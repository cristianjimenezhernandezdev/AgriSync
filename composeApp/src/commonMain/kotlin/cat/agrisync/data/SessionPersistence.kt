package cat.agrisync.data

expect object SessionPersistence {
    fun save(session: StoredSession)
    fun load(): StoredSession?
    fun clear()
}

