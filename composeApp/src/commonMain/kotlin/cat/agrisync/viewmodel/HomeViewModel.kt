package cat.agrisync.viewmodel

import cat.agrisync.data.AccessRepository
import cat.agrisync.data.TitularAccessRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<TitularAccessRow> = emptyList(),
    val searchNif: String = "",
    val currentPage: Int = 0,
    val pageSize: Int = 12,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filtered: List<TitularAccessRow>
        get() {
            val query = normalizeNif(searchNif)
            if (query.isBlank()) return items
            return items.filter { normalizeNif(it.nif) .contains(query) || it.nom.contains(searchNif, ignoreCase = true) }
        }

    val totalPages: Int
        get() = if (filtered.isEmpty()) 1 else ((filtered.size - 1) / pageSize) + 1

    val pageItems: List<TitularAccessRow>
        get() {
            val start = currentPage * pageSize
            if (start >= filtered.size) return emptyList()
            val end = minOf(filtered.size, start + pageSize)
            return filtered.subList(start, end)
        }
}

internal class HomeViewModel(
    private val repository: AccessRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val rows = repository.listTitularAccess().filter { it.can_agricola || it.can_ramader }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = rows,
                        currentPage = 0
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ex.message ?: "Error carregant titulars") }
            }
        }
    }

    fun onSearchNifChange(value: String) {
        _uiState.update { it.copy(searchNif = value, currentPage = 0) }
    }

    fun nextPage() {
        _uiState.update { state ->
            val next = state.currentPage + 1
            if (next >= state.totalPages) state else state.copy(currentPage = next)
        }
    }

    fun prevPage() {
        _uiState.update { state ->
            if (state.currentPage == 0) state else state.copy(currentPage = state.currentPage - 1)
        }
    }

    fun clear() {
        scope.cancel()
    }
}

private fun normalizeNif(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return value
        .replace(" ", "")
        .replace(".", "")
        .replace("-", "")
        .uppercase()
}

