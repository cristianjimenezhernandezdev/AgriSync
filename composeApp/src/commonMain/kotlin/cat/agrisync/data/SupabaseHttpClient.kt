package cat.agrisync.data

import io.ktor.client.HttpClient

expect object SupabaseHttpClient {
    fun create(): HttpClient
}

