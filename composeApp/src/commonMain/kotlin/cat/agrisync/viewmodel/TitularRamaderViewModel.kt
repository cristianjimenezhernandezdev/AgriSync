package cat.agrisync.viewmodel

import cat.agrisync.data.*
import cat.agrisync.util.parseDecimalInput
import cat.agrisync.util.parseEnteredDateToIso
import cat.agrisync.util.validateAndResolveNitrogenTriplet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TitularRamaderUiState(
    val titular: TitularDto? = null,
    val collaboratingTecnics: List<TitularCollaboratingTecnicSummary> = emptyList(),
    val collaboratingOficines: List<TitularCollaboratingOficinaSummary> = emptyList(),
    val granges: List<GranjaDto> = emptyList(),
    val granjaBestiar: List<GranjaBestiarDto> = emptyList(),
    val granjaCampanyaBalances: List<GranjaCampanyaBalanceDto> = emptyList(),
    val entregues: List<EntregaDejeccioDto> = emptyList(),
    val terres: List<TerraDto> = emptyList(),
    val receptorTitulars: List<TitularDto> = emptyList(),
    val receptorTerres: List<TerraDto> = emptyList(),
    val availableCampanyes: List<Int> = emptyList(),
    val selectedCampanya: Int = 0,
    val bestiars: List<BestiarDto> = emptyList(),
    val fasesProductives: List<FaseProductivaDto> = emptyList(),
    val actorLabels: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
)

internal class TitularRamaderViewModel(
    private val repository: RamaderRepository,
    private val auditRepository: AuditRepository,
    private val tecnicRepository: TecnicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularRamaderUiState())
    val uiState: StateFlow<TitularRamaderUiState> = _uiState.asStateFlow()
    private var currentTitularId: String = ""

    fun load(titularId: String, preferredCampanya: Int? = null) {
        currentTitularId = titularId
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveMessage = null) }
            try {
                val titular = repository.getTitular(titularId)
                val collaboratingTecnicsRaw = tecnicRepository.listCollaboratingTecnicsByTitular(titularId)
                val collaboratingOficinesRaw = tecnicRepository.listCollaboratingOficinesByTitular(titularId)
                val granges = repository.listGranges(titularId)
                val gb = repository.listGranjaBestiar(titularId)
                val existingCampanyes = repository.listCampanyesByTitular(titularId)
                val selectedCampanya = resolveSelectedCampanya(existingCampanyes, preferredCampanya)
                val balances = repository.listGranjaCampanyaBalances(titularId, selectedCampanya)
                val entregues = repository.listEntreguesByTitular(titularId, selectedCampanya)
                val terres = repository.listTerres(titularId)
                val receptorTitulars = repository.listAccessibleTitulars()
                val receptorTerres = repository.listAccessibleTerres()
                val bestiars = repository.listBestiarCatalog()
                val fases = repository.listFaseProductivaCatalog()
                val actorLabels = resolveActorLabels(titular, granges, gb, balances, entregues, terres)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        collaboratingTecnics = collaboratingTecnicsRaw.toTitularCollaboratingTecnicSummaries(),
                        collaboratingOficines = buildTitularCollaboratingOficinaSummaries(
                            collaboratingTecnicsRaw,
                            collaboratingOficinesRaw
                        ),
                        granges = granges,
                        granjaBestiar = gb,
                        granjaCampanyaBalances = balances,
                        entregues = entregues,
                        terres = terres,
                        receptorTitulars = receptorTitulars,
                        receptorTerres = receptorTerres,
                        availableCampanyes = normalizedCampanyes(existingCampanyes),
                        selectedCampanya = selectedCampanya,
                        bestiars = bestiars,
                        fasesProductives = fases,
                        actorLabels = actorLabels
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapHttpError(ex.message)) }
            }
        }
    }

    fun updateTitular(nif: String, nom: String, telefon: String, email: String, adreca: String, codiPostal: String): Boolean {
        if (nom.isBlank()) {
            _uiState.update { it.copy(saveMessage = "El nom del titular es obligatori") }
            return false
        }
        val cleanCodiPostal = codiPostal.trim()
        if (cleanCodiPostal.isNotBlank() && !cleanCodiPostal.matches(Regex("^\\d{5}$"))) {
            _uiState.update { it.copy(saveMessage = "El codi postal ha de tenir 5 digits") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateTitular(
                    currentTitularId,
                    TitularUpdateRequest(
                        nif = nif.trim().ifBlank { null },
                        nom_rao = nom.trim(),
                        telefon = telefon.trim().ifBlank { null },
                        email = email.trim().ifBlank { null },
                        adreca = adreca.trim().ifBlank { null },
                        codi_postal = cleanCodiPostal.ifBlank { null }
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                _uiState.update { it.copy(titular = updated, actorLabels = actorLabels, saveMessage = "Titular guardat") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun updateGranja(granjaId: String, nom: String, marca: String): Boolean {
        if (marca.isBlank()) {
            _uiState.update { it.copy(saveMessage = "La marca oficial es obligatoria") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateGranja(
                    granjaId,
                    GranjaUpdateRequest(nom = nom.trim().ifBlank { null }, marca_oficial = marca.trim())
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                _uiState.update { st ->
                    st.copy(
                        granges = st.granges.map { if (it.id == granjaId) updated else it },
                        actorLabels = actorLabels,
                        saveMessage = "Granja guardada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createGranja(nom: String, marca: String): Boolean {
        val cleanMarca = marca.trim()
        val cleanNom = nom.trim()
        if (cleanMarca.isBlank()) {
            _uiState.update { it.copy(saveMessage = "La marca oficial es obligatoria") }
            return false
        }
        scope.launch {
            try {
                val created = repository.createGranja(
                    GranjaCreateRequest(
                        titular_id = currentTitularId,
                        marca_oficial = cleanMarca,
                        nom = cleanNom.ifBlank { null }
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, created.updated_by)
                _uiState.update { st ->
                    st.copy(
                        granges = listOf(created) + st.granges,
                        actorLabels = actorLabels,
                        saveMessage = "Granja creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteGranja(granjaId: String) {
        scope.launch {
            try {
                repository.deleteGranja(granjaId)
                _uiState.update { st ->
                    st.copy(
                        granges = st.granges.filterNot { it.id == granjaId },
                        granjaBestiar = st.granjaBestiar.filterNot { it.granja?.id == granjaId },
                        granjaCampanyaBalances = st.granjaCampanyaBalances.filterNot { it.granja_id == granjaId },
                        entregues = st.entregues.filterNot { it.granja_origen_id == granjaId },
                        saveMessage = "Granja eliminada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun onSelectCampanya(campanya: Int) {
        if (currentTitularId.isBlank()) return
        load(currentTitularId, campanya)
    }

    fun updateGranjaBestiar(id: String, censText: String): Boolean {
        val cens = censText.toDoubleOrNull()
        if (cens == null) {
            _uiState.update { it.copy(saveMessage = "El cens ha de ser un nombre valid") }
            return false
        }
        if (cens < 0) {
            _uiState.update { it.copy(saveMessage = "El cens no pot ser negatiu") }
            return false
        }
        scope.launch {
            try {
                val result = repository.updateGranjaBestiar(id, GranjaBestiarUpdateRequest(cens = cens))
                if (result.isNotEmpty()) {
                    val updated = result.first()
                    val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                    _uiState.update { st ->
                        st.copy(
                            granjaBestiar = st.granjaBestiar.map { if (it.id == id) updated else it },
                            actorLabels = actorLabels,
                            saveMessage = "Bestiar guardat"
                        )
                    }
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createGranjaBestiar(granjaId: String, bestiarId: String, faseId: String, censText: String): Boolean {
        val cens = censText.toDoubleOrNull()
        if (granjaId.isBlank() || bestiarId.isBlank() || faseId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar granja, bestiar i fase") }
            return false
        }
        if (cens == null) {
            _uiState.update { it.copy(saveMessage = "El cens ha de ser un nombre valid") }
            return false
        }
        if (cens < 0) {
            _uiState.update { it.copy(saveMessage = "El cens no pot ser negatiu") }
            return false
        }
        scope.launch {
            try {
                val created = repository.createGranjaBestiar(
                    GranjaBestiarCreateRequest(
                        granja_id = granjaId,
                        bestiar_id = bestiarId,
                        fase_productiva_id = faseId,
                        cens = cens
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, created.updated_by)
                _uiState.update { st ->
                    st.copy(
                        granjaBestiar = listOf(created) + st.granjaBestiar,
                        actorLabels = actorLabels,
                        saveMessage = "Registre de bestiar creat correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteGranjaBestiar(id: String) {
        scope.launch {
            try {
                repository.deleteGranjaBestiar(id)
                _uiState.update { st ->
                    st.copy(
                        granjaBestiar = st.granjaBestiar.filterNot { it.id == id },
                        saveMessage = "Registre de bestiar eliminat"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun updateEntrega(
        id: String,
        data: String,
        terraDestiId: String,
        tipusFertilitzant: String,
        volumText: String,
        kgNM3Text: String,
        kgNText: String
    ): Boolean {
        val cleanData = parseEnteredDateToIso(data)
        if (cleanData == null) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format dd/MM/YYYY") }
            return false
        }
        if (terraDestiId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una terra de desti") }
            return false
        }
        val nutrients = validateAndResolveNitrogenTriplet(
            kgNText = kgNText,
            volumM3Text = volumText,
            kgNPerM3Text = kgNM3Text
        )
        if (nutrients.errorMessage != null) {
            _uiState.update { it.copy(saveMessage = nutrients.errorMessage) }
            return false
        }
        val resolved = nutrients.values ?: return false
        scope.launch {
            try {
                val result = repository.updateEntrega(
                    id,
                    EntregaUpdateRequest(
                        data = cleanData,
                        terra_desti_id = terraDestiId,
                        tipus_fertilitzant = tipusFertilitzant.trim().ifBlank { null },
                        volum_m3 = resolved.volumM3,
                        kg_n_m3 = resolved.kgNPerM3,
                        kg_n = resolved.kgN
                    )
                )
                if (result.isNotEmpty()) {
                    val updated = result.first()
                    val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                    _uiState.update { st ->
                        st.copy(
                            entregues = st.entregues.map { if (it.id == id) updated else it },
                            actorLabels = actorLabels,
                            saveMessage = "Entrega guardada"
                        )
                    }
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createEntrega(
        granjaOrigenId: String,
        data: String,
        terraDestiId: String,
        tipusFertilitzant: String,
        volumText: String,
        kgNM3Text: String,
        kgNText: String
    ): Boolean {
        val cleanData = parseEnteredDateToIso(data)
        if (granjaOrigenId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una granja d'origen") }
            return false
        }
        if (cleanData == null) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format dd/MM/YYYY") }
            return false
        }
        if (terraDestiId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una terra de desti") }
            return false
        }
        val nutrients = validateAndResolveNitrogenTriplet(
            kgNText = kgNText,
            volumM3Text = volumText,
            kgNPerM3Text = kgNM3Text
        )
        if (nutrients.errorMessage != null) {
            _uiState.update { it.copy(saveMessage = nutrients.errorMessage) }
            return false
        }
        val resolved = nutrients.values ?: return false

        scope.launch {
            try {
                val created = repository.createEntrega(
                    titularId = currentTitularId,
                    campanya = _uiState.value.selectedCampanya,
                    granjaOrigenId = granjaOrigenId,
                    data = cleanData,
                    terraDestiId = terraDestiId,
                    tipusFertilitzant = tipusFertilitzant.trim().ifBlank { null },
                    volumM3 = resolved.volumM3,
                    kgNM3 = resolved.kgNPerM3,
                    kgN = resolved.kgN
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, created.updated_by)
                _uiState.update { st ->
                    st.copy(
                        entregues = listOf(created) + st.entregues,
                        actorLabels = actorLabels,
                        saveMessage = "Entrega creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun saveGranjaCampanyaBalance(
        granjaId: String,
        balanceId: String?,
        estocInicialText: String,
        kgNGeneratText: String,
        estocFinalText: String
    ): Boolean {
        if (granjaId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una granja") }
            return false
        }

        val estocInicialResult = parseOptionalKgN(estocInicialText, "L'estoc inicial")
        if (!estocInicialResult.isValid) return false
        val kgNGeneratResult = parseOptionalKgN(kgNGeneratText, "El nitrogen generat")
        if (!kgNGeneratResult.isValid) return false
        val estocFinalResult = parseOptionalKgN(estocFinalText, "L'estoc final declarat")
        if (!estocFinalResult.isValid) return false

        scope.launch {
            try {
                val saved = if (balanceId.isNullOrBlank()) {
                    repository.createGranjaCampanyaBalance(
                        titularId = currentTitularId,
                        campanya = _uiState.value.selectedCampanya,
                        granjaId = granjaId,
                        estocInicialKgN = estocInicialResult.value,
                        kgNGenerat = kgNGeneratResult.value,
                        estocFinalDeclaratKgN = estocFinalResult.value
                    )
                } else {
                    repository.updateGranjaCampanyaBalance(
                        balanceId,
                        GranjaCampanyaBalanceUpdateRequest(
                            estoc_inicial_kg_n = estocInicialResult.value,
                            kg_n_generat = kgNGeneratResult.value,
                            estoc_final_declarat_kg_n = estocFinalResult.value
                        )
                    )
                }
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, saved.updated_by)
                _uiState.update { st ->
                    val updatedBalances = if (balanceId.isNullOrBlank()) {
                        listOf(saved) + st.granjaCampanyaBalances.filterNot { it.granja_id == granjaId }
                    } else {
                        st.granjaCampanyaBalances.map { if (it.id == balanceId) saved else it }
                    }
                    st.copy(
                        granjaCampanyaBalances = updatedBalances,
                        actorLabels = actorLabels,
                        saveMessage = "Balanç de granja guardat"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteEntrega(id: String) {
        scope.launch {
            try {
                repository.deleteEntrega(id)
                _uiState.update { st ->
                    st.copy(
                        entregues = st.entregues.filterNot { it.id == id },
                        saveMessage = "Entrega eliminada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    fun clear() {
        scope.cancel()
    }

    private fun mapHttpError(message: String?): String {
        val msg = message ?: return "Error desconegut"
        return when {
            msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
            msg.contains("403") -> "No tens permis per aquest titular (403)."
            else -> msg
        }
    }

    private suspend fun resolveActorLabels(
        titular: TitularDto?,
        granges: List<GranjaDto>,
        granjaBestiar: List<GranjaBestiarDto>,
        granjaCampanyaBalances: List<GranjaCampanyaBalanceDto>,
        entregues: List<EntregaDejeccioDto>,
        terres: List<TerraDto>
    ): Map<String, String> {
        return auditRepository.resolveActorLabels(
            buildList {
                add(titular?.updated_by)
                granges.forEach { add(it.updated_by) }
                granjaBestiar.forEach { add(it.updated_by) }
                granjaCampanyaBalances.forEach { add(it.updated_by) }
                entregues.forEach { add(it.updated_by) }
                terres.forEach { add(it.updated_by) }
            }
        )
    }

    private suspend fun updateActorLabels(
        current: Map<String, String>,
        userId: String?
    ): Map<String, String> {
        val cleanUserId = userId?.takeIf { it.isNotBlank() } ?: return current
        if (current.containsKey(cleanUserId)) return current
        val label = auditRepository.resolveActorLabel(cleanUserId) ?: return current
        return current + (cleanUserId to label)
    }

    private fun parseOptionalKgN(value: String, fieldName: String): OptionalNumberParseResult {
        if (value.isBlank()) return OptionalNumberParseResult(isValid = true, value = null)
        val parsed = parseDecimalInput(value)
        if (parsed == null) {
            _uiState.update { it.copy(saveMessage = "$fieldName ha de ser un nombre valid") }
            return OptionalNumberParseResult(isValid = false, value = null)
        }
        if (parsed < 0.0) {
            _uiState.update { it.copy(saveMessage = "$fieldName no pot ser negatiu") }
            return OptionalNumberParseResult(isValid = false, value = null)
        }
        return OptionalNumberParseResult(isValid = true, value = parsed)
    }
}

private data class OptionalNumberParseResult(
    val isValid: Boolean,
    val value: Double?
)
