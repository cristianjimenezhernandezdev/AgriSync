package cat.agrisync.viewmodel

import cat.agrisync.data.DanPreparationAplicacioDto
import cat.agrisync.data.DanPreparationEntregaDto
import cat.agrisync.data.DanPreparationGranjaBalanceDto
import cat.agrisync.data.DanPreparationRepository
import cat.agrisync.data.DanPreparationTerraDto
import cat.agrisync.data.GranjaBestiarDto
import cat.agrisync.data.GranjaDto
import cat.agrisync.data.TitularDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DanPreparationUiState(
    val titular: TitularDto? = null,
    val terres: List<DanPreparationTerraDto> = emptyList(),
    val aplicacions: List<DanPreparationAplicacioDto> = emptyList(),
    val granges: List<GranjaDto> = emptyList(),
    val granjaBestiar: List<GranjaBestiarDto> = emptyList(),
    val granjaBalances: List<DanPreparationGranjaBalanceDto> = emptyList(),
    val entregues: List<DanPreparationEntregaDto> = emptyList(),
    val availableCampanyes: List<Int> = emptyList(),
    val selectedCampanya: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalHectares: Double
        get() = terres.sumOf { it.superficie ?: 0.0 }

    val totalKgN: Double
        get() = aplicacions.sumOf { it.kg_n ?: 0.0 }

    val totalVolumM3: Double
        get() = aplicacions.sumOf { it.volum_m3 ?: 0.0 }

    val totalCens: Double
        get() = granjaBestiar.sumOf { it.cens ?: 0.0 }

    val totalVolumEntregat: Double
        get() = entregues.sumOf { it.volum_m3 ?: 0.0 }

    val totalKgNEntregat: Double
        get() = entregues.sumOf { it.kg_n ?: 0.0 }

    val kgNPerHa: Double?
        get() = totalHectares.takeIf { it > 0 }?.let { totalKgN / it }

    fun totalKgNByTerra(terraId: String): Double {
        return aplicacions.filter { it.terra?.id == terraId }.sumOf { it.kg_n ?: 0.0 }
    }

    fun allowedKgNByTerra(terraId: String): Double? {
        val terra = terres.firstOrNull { it.id == terraId } ?: return null
        val hectares = terra.superficie ?: return null
        val limitKgNHa = terra.limit_kg_n_ha ?: if (terra.zona == "ZV") 170.0 else 190.0
        return hectares * limitKgNHa
    }

    fun kgNPerHaByTerra(terraId: String): Double? {
        val terra = terres.firstOrNull { it.id == terraId } ?: return null
        val hectares = terra.superficie ?: return null
        if (hectares <= 0.0) return null
        return totalKgNByTerra(terraId) / hectares
    }

    fun excessKgNByTerra(terraId: String): Double? {
        val allowed = allowedKgNByTerra(terraId) ?: return null
        val applied = totalKgNByTerra(terraId)
        val excess = applied - allowed
        return excess.takeIf { it > 0.0 }
    }

    fun justifiedKgNByGranja(granjaId: String): Double {
        return entregues.filter { it.granja_origen?.id == granjaId }.sumOf { it.kg_n ?: 0.0 }
    }

    fun balanceByGranja(granjaId: String): DanPreparationGranjaBalanceDto? {
        return granjaBalances.firstOrNull { it.granja_id == granjaId }
    }

    fun calculatedFinalKgNByGranja(granjaId: String): Double? {
        val balance = balanceByGranja(granjaId) ?: return null
        return (balance.estoc_inicial_kg_n ?: 0.0) + (balance.kg_n_generat ?: 0.0) - justifiedKgNByGranja(granjaId)
    }

    fun deviationKgNByGranja(granjaId: String): Double? {
        val balance = balanceByGranja(granjaId) ?: return null
        val calculatedFinal = calculatedFinalKgNByGranja(granjaId) ?: return null
        val declaredFinal = balance.estoc_final_declarat_kg_n ?: return null
        return calculatedFinal - declaredFinal
    }

    fun automaticChecklistItems(): List<String> {
        val items = mutableListOf<String>()

        val terresSenseContext = terres.count {
            it.municipi_literal.isNullOrBlank() || it.us_sigpac.isNullOrBlank() || it.cultiu.isNullOrBlank()
        }
        if (terresSenseContext > 0) {
            items += "$terresSenseContext terres amb municipi, us SIGPAC o cultiu pendents de completar."
        }

        val terresSenseSuperficie = terres.count { (it.superficie ?: 0.0) <= 0.0 }
        if (terresSenseSuperficie > 0) {
            items += "$terresSenseSuperficie terres sense superficie valida."
        }

        val aplicacionsSenseOrigen = aplicacions.count {
            it.tipus_fertilitzant.isNullOrBlank() || it.procedencia.isNullOrBlank()
        }
        if (aplicacionsSenseOrigen > 0) {
            items += "$aplicacionsSenseOrigen aplicacions sense tipus de fertilitzant o procedencia informada."
        }

        val aplicacionsSenseVolum = aplicacions.count { it.volum_m3 == null || it.kg_n_m3 == null }
        if (aplicacionsSenseVolum > 0) {
            items += "$aplicacionsSenseVolum aplicacions sense volum m3 o kg N/m3."
        }

        if (granges.isNotEmpty() && granjaBestiar.isEmpty()) {
            items += "Hi ha granges registrades pero no hi ha cens carregat per bestiar i fase."
        }

        val grangesSenseBalanc = granges.count { balanceByGranja(it.id) == null }
        if (grangesSenseBalanc > 0) {
            items += "$grangesSenseBalanc granges sense balanc de nitrogen de campanya."
        }

        if (granges.isNotEmpty() && entregues.isEmpty()) {
            items += "Hi ha granges registrades pero no hi ha entregues de dejeccions a la campanya ${selectedCampanyaLabel()}."
        }

        if (entregues.any { it.terra_desti == null }) {
            items += "Hi ha entregues sense terra de desti resolta."
        }

        val balancesWithDeviation = granges.count { (deviationKgNByGranja(it.id) ?: 0.0).let { deviation -> kotlin.math.abs(deviation) > 0.01 } }
        if (balancesWithDeviation > 0) {
            items += "$balancesWithDeviation granges amb desviacio entre estoc final declarat i estoc final calculat."
        }

        if (items.isEmpty()) {
            items += "No s'han detectat buits basics automatics per a la campanya ${selectedCampanyaLabel()}."
        }

        items += "Revisar manualment els camps finals de DAN que encara no modela el MVP: balanc ramader, estat inicial/final de fossa i camps normatius externs."
        return items
    }

    fun buildClipboardSummary(): String = buildString {
        appendLine("AgriSync - Resum DAN")
        appendLine("Titular: ${titular?.nom_rao ?: "-"}")
        appendLine("NIF: ${titular?.nif ?: "-"}")
        appendLine("Telefon: ${titular?.telefon ?: "-"}")
        appendLine("Email: ${titular?.email ?: "-"}")
        appendLine("Adreca: ${titular?.adreca ?: "-"}")
        appendLine("Codi postal: ${titular?.codi_postal ?: "-"}")
        appendLine("Campanya: ${selectedCampanyaLabel()}")
        appendLine()
        appendLine("Totals")
        appendLine("- Terres: ${terres.size}")
        appendLine("- Total ha: ${formatForExport(totalHectares)}")
        appendLine("- Aplicacions: ${aplicacions.size}")
        appendLine("- Kg N total: ${formatForExport(totalKgN)}")
        appendLine("- Volum total m3: ${formatForExport(totalVolumM3)}")
        appendLine("- Kg N/ha: ${formatForExport(kgNPerHa)}")
        appendLine("- Granges: ${granges.size}")
        appendLine("- Cens total: ${formatForExport(totalCens)}")
        appendLine("- Entregues: ${entregues.size}")
        appendLine("- Volum total entregat: ${formatForExport(totalVolumEntregat)}")
        appendLine("- Kg N total entregat: ${formatForExport(totalKgNEntregat)}")
        appendLine()

        appendLine("Terres")
        if (terres.isEmpty()) {
            appendLine("- Sense terres registrades")
        } else {
            terres.forEach { terra ->
                appendLine(
                    "- ${terra.codi_sigpac_complet ?: terra.id} | ha=${formatForExport(terra.superficie)} | zona=${terra.zona} | cultiu=${terra.cultiu ?: "-"} | us=${terra.us_sigpac ?: "-"} | kgN aplicats=${formatForExport(totalKgNByTerra(terra.id))} | kgN permesos=${formatForExport(allowedKgNByTerra(terra.id))} | exces=${formatForExport(excessKgNByTerra(terra.id))}"
                )
            }
        }
        appendLine()

        appendLine("Aplicacions")
        if (aplicacions.isEmpty()) {
            appendLine("- Sense aplicacions registrades a la campanya ${selectedCampanyaLabel()}")
        } else {
            aplicacions.forEach { aplicacio ->
                appendLine(
                    "- ${aplicacio.data ?: "-"} | terra=${aplicacio.terra?.codi_sigpac_complet ?: aplicacio.terra?.id ?: "-"} | tipus=${aplicacio.tipus_fertilitzant ?: "-"} | procedencia=${aplicacio.procedencia ?: "-"} | volum m3=${formatForExport(aplicacio.volum_m3)} | kg N/m3=${formatForExport(aplicacio.kg_n_m3)} | kg N=${formatForExport(aplicacio.kg_n)}"
                )
            }
        }
        appendLine()

        appendLine("Granges")
        if (granges.isEmpty()) {
            appendLine("- Sense granges registrades")
        } else {
            granges.forEach { granja ->
                appendLine("- ${granja.marca_oficial} | ${granja.nom ?: "-"}")
            }
        }
        appendLine()

        appendLine("Balanc nitrogen per granja")
        if (granges.isEmpty()) {
            appendLine("- Sense granges registrades")
        } else {
            granges.forEach { granja ->
                val balance = balanceByGranja(granja.id)
                appendLine(
                    "- ${granja.marca_oficial} | inicial=${formatForExport(balance?.estoc_inicial_kg_n)} | generat=${formatForExport(balance?.kg_n_generat)} | justificat=${formatForExport(justifiedKgNByGranja(granja.id))} | final declarat=${formatForExport(balance?.estoc_final_declarat_kg_n)} | final calculat=${formatForExport(calculatedFinalKgNByGranja(granja.id))} | desviacio=${formatForExport(deviationKgNByGranja(granja.id))}"
                )
            }
        }
        appendLine()

        appendLine("Cens per bestiar i fase")
        if (granjaBestiar.isEmpty()) {
            appendLine("- Sense cens registrat")
        } else {
            granjaBestiar.forEach { registre ->
                appendLine(
                    "- granja=${registre.granja?.marca_oficial ?: "-"} | bestiar=${registre.bestiar?.codi ?: "-"} | fase=${registre.fase_productiva?.codi ?: "-"} | cens=${formatForExport(registre.cens)}"
                )
            }
        }
        appendLine()

        appendLine("Entregues")
        if (entregues.isEmpty()) {
            appendLine("- Sense entregues registrades a la campanya ${selectedCampanyaLabel()}")
        } else {
            entregues.forEach { entrega ->
                appendLine(
                    "- ${entrega.data ?: "-"} | origen=${entrega.granja_origen?.marca_oficial ?: "-"} | tipus=${entrega.tipus_fertilitzant ?: "-"} | volum m3=${formatForExport(entrega.volum_m3)} | kg N/m3=${formatForExport(entrega.kg_n_m3)} | kg N=${formatForExport(entrega.kg_n)} | terra desti=${entrega.terra_desti?.codi_sigpac_complet ?: "-"}"
                )
            }
        }
        appendLine()

        appendLine("Checklist automatica")
        automaticChecklistItems().forEach { item ->
            appendLine("- $item")
        }
    }

    fun buildClipboardChecklist(): String = buildString {
        appendLine("AgriSync - Checklist DAN")
        appendLine("Titular: ${titular?.nom_rao ?: "-"}")
        appendLine("NIF: ${titular?.nif ?: "-"}")
        appendLine("Telefon: ${titular?.telefon ?: "-"}")
        appendLine("Codi postal: ${titular?.codi_postal ?: "-"}")
        appendLine("Campanya: ${selectedCampanyaLabel()}")
        appendLine()
        automaticChecklistItems().forEach { item ->
            appendLine("- $item")
        }
    }

    private fun selectedCampanyaLabel(): String =
        selectedCampanya.takeIf { it > 0 }?.toString() ?: "sense campanya"

    private fun formatForExport(value: Double?): String {
        if (value == null) return "-"
        val rounded = kotlin.math.round(value * 100) / 100
        return rounded.toString().replace('.', ',')
    }
}

internal class DanPreparationViewModel(
    private val repository: DanPreparationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(DanPreparationUiState())
    val uiState: StateFlow<DanPreparationUiState> = _uiState.asStateFlow()

    fun load(titularId: String, preferredCampanya: Int? = null) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val titular = repository.getTitular(titularId)
                val terres = repository.listTerres(titularId)
                val existingCampanyes = repository.listCampanyesByTitular(titularId)
                val selectedCampanya = resolveSelectedCampanya(existingCampanyes, preferredCampanya)
                val aplicacions = repository.listAplicacionsByTitular(titularId, selectedCampanya)
                val granges = repository.listGranges(titularId)
                val granjaBestiar = repository.listGranjaBestiar(titularId)
                val granjaBalances = repository.listGranjaCampanyaBalances(titularId, selectedCampanya)
                val entregues = repository.listEntreguesByTitular(titularId, selectedCampanya)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        terres = terres,
                        aplicacions = aplicacions,
                        granges = granges,
                        granjaBestiar = granjaBestiar,
                        granjaBalances = granjaBalances,
                        entregues = entregues,
                        availableCampanyes = normalizedCampanyes(existingCampanyes),
                        selectedCampanya = selectedCampanya
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapDanPreparationError(ex.message)) }
            }
        }
    }

    fun onSelectCampanya(titularId: String, campanya: Int) {
        load(titularId, campanya)
    }

    fun clear() {
        scope.cancel()
    }
}

private fun mapDanPreparationError(message: String?): String {
    val msg = message ?: return "Error carregant el resum DAN"
    return when {
        msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
        msg.contains("403") -> "No tens permis per consultar aquest resum DAN (403)."
        msg.contains("42703") && msg.contains("entrega_id") ->
            "La base de dades actual no te el camp antic `entrega_id`. El resum DAN s'ha d'obrir amb el mode compatible."
        msg.contains("PGRST200") || msg.contains("Could not find a relationship between") ->
            "No s'ha pogut carregar una relacio de dades del resum DAN. Cal revisar la configuracio de Supabase."
        else -> msg
    }
}
