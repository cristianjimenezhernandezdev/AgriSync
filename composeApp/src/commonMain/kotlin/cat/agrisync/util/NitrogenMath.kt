package cat.agrisync.util

import kotlin.math.abs
import kotlin.math.round

private const val NITROGEN_TOLERANCE = 0.0001

internal enum class NitrogenField {
    TOTAL_KG_N,
    VOLUME_M3,
    RATE_KG_N_M3
}

internal data class NitrogenTriplet(
    val kgN: Double,
    val volumM3: Double,
    val kgNPerM3: Double
)

internal data class NitrogenTripletTexts(
    val kgN: String,
    val volumM3: String,
    val kgNPerM3: String
)

internal data class NitrogenValidationResult(
    val values: NitrogenTriplet? = null,
    val errorMessage: String? = null
)

internal fun parseDecimalInput(value: String): Double? {
    val clean = value.trim().replace(',', '.')
    if (clean.isBlank()) return null
    return clean.toDoubleOrNull()
}

internal fun formatDecimalInput(value: Double, decimals: Int = 4): String {
    val factor = powerOfTen(decimals)
    val rounded = round(value * factor) / factor
    val raw = rounded.toString()
    return if (raw.endsWith(".0")) raw.dropLast(2) else raw
}

internal fun autofillNitrogenTexts(
    current: NitrogenTripletTexts,
    changedField: NitrogenField,
    newValue: String
): NitrogenTripletTexts {
    val updated = when (changedField) {
        NitrogenField.TOTAL_KG_N -> current.copy(kgN = newValue)
        NitrogenField.VOLUME_M3 -> current.copy(volumM3 = newValue)
        NitrogenField.RATE_KG_N_M3 -> current.copy(kgNPerM3 = newValue)
    }

    val kgN = parseDecimalInput(updated.kgN)
    val volumM3 = parseDecimalInput(updated.volumM3)
    val kgNPerM3 = parseDecimalInput(updated.kgNPerM3)

    val present = listOf(kgN, volumM3, kgNPerM3).count { it != null }
    if (present < 2) return updated

    return when {
        kgN == null && volumM3 != null && kgNPerM3 != null -> {
            updated.copy(kgN = formatDecimalInput(volumM3 * kgNPerM3, decimals = 2))
        }
        volumM3 == null && kgN != null && kgNPerM3 != null && kgNPerM3 > 0.0 -> {
            updated.copy(volumM3 = formatDecimalInput(kgN / kgNPerM3, decimals = 2))
        }
        kgNPerM3 == null && kgN != null && volumM3 != null && volumM3 > 0.0 -> {
            updated.copy(kgNPerM3 = formatDecimalInput(kgN / volumM3, decimals = 4))
        }
        else -> updated
    }
}

internal fun validateAndResolveNitrogenTriplet(
    kgNText: String,
    volumM3Text: String,
    kgNPerM3Text: String
): NitrogenValidationResult {
    val kgN = parseDecimalInput(kgNText)
    val volumM3 = parseDecimalInput(volumM3Text)
    val kgNPerM3 = parseDecimalInput(kgNPerM3Text)

    if (kgNText.isNotBlank() && kgN == null) {
        return NitrogenValidationResult(errorMessage = "Kg N ha de ser un nombre valid")
    }
    if (volumM3Text.isNotBlank() && volumM3 == null) {
        return NitrogenValidationResult(errorMessage = "El volum m3 ha de ser un nombre valid")
    }
    if (kgNPerM3Text.isNotBlank() && kgNPerM3 == null) {
        return NitrogenValidationResult(errorMessage = "El kg N/m3 ha de ser un nombre valid")
    }

    if ((kgN ?: 0.0) < 0.0 || (volumM3 ?: 0.0) < 0.0 || (kgNPerM3 ?: 0.0) < 0.0) {
        return NitrogenValidationResult(errorMessage = "Kg N, volum m3 i kg N/m3 no poden ser negatius")
    }

    val present = listOf(kgN, volumM3, kgNPerM3).count { it != null }
    if (present < 2) {
        return NitrogenValidationResult(errorMessage = "Has d'informar almenys 2 dels camps: Kg N, volum m3 o kg N/m3")
    }

    val resolved = when {
        kgN != null && volumM3 != null && kgNPerM3 == null -> {
            if (volumM3 <= 0.0) return NitrogenValidationResult(errorMessage = "El volum m3 ha de ser superior a 0 per calcular kg N/m3")
            NitrogenTriplet(kgN = kgN, volumM3 = volumM3, kgNPerM3 = kgN / volumM3)
        }
        kgN != null && volumM3 == null && kgNPerM3 != null -> {
            if (kgNPerM3 <= 0.0) return NitrogenValidationResult(errorMessage = "El kg N/m3 ha de ser superior a 0 per calcular el volum m3")
            NitrogenTriplet(kgN = kgN, volumM3 = kgN / kgNPerM3, kgNPerM3 = kgNPerM3)
        }
        kgN == null && volumM3 != null && kgNPerM3 != null -> {
            NitrogenTriplet(kgN = volumM3 * kgNPerM3, volumM3 = volumM3, kgNPerM3 = kgNPerM3)
        }
        kgN != null && volumM3 != null && kgNPerM3 != null -> {
            val expectedKgN = volumM3 * kgNPerM3
            if (abs(expectedKgN - kgN) > NITROGEN_TOLERANCE) {
                return NitrogenValidationResult(errorMessage = "Kg N ha de ser igual a volum m3 x kg N/m3")
            }
            NitrogenTriplet(kgN = kgN, volumM3 = volumM3, kgNPerM3 = kgNPerM3)
        }
        else -> return NitrogenValidationResult(errorMessage = "No s'han pogut resoldre els valors de nitrogen")
    }

    return NitrogenValidationResult(values = resolved)
}

private fun powerOfTen(decimals: Int): Double {
    var result = 1.0
    repeat(decimals) { result *= 10.0 }
    return result
}
