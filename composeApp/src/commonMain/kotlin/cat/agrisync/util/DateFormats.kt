package cat.agrisync.util

private const val MILLIS_PER_DAY = 86_400_000L

internal fun normalizeDateInput(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(8)
    return buildString {
        digits.take(2).takeIf { it.isNotEmpty() }?.let { append(it) }
        digits.drop(2).take(2).takeIf { it.isNotEmpty() }?.let {
            append('/')
            append(it)
        }
        digits.drop(4).take(4).takeIf { it.isNotEmpty() }?.let {
            append('/')
            append(it)
        }
    }
}

internal fun parseEnteredDateToIso(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    parseDisplayDate(trimmed)?.let { return it.toIsoString() }
    parseIsoDate(trimmed)?.let { return it.toIsoString() }
    return null
}

internal fun formatStoredDateForInput(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return parseIsoDate(value)?.toDisplayString() ?: value
}

internal fun formatStoredDateForDisplay(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return parseIsoDate(value)?.toDisplayString() ?: value
}

internal fun enteredDateToPickerMillis(value: String): Long? {
    val parsed = parseEnteredDateToIso(value)?.let(::parseIsoDate) ?: return null
    return parsed.toEpochDay() * MILLIS_PER_DAY
}

internal fun pickerMillisToEnteredDate(millis: Long): String {
    val epochDay = millis / MILLIS_PER_DAY
    return SimpleDate.fromEpochDay(epochDay).toDisplayString()
}

private fun parseDisplayDate(value: String): SimpleDate? {
    val match = DISPLAY_DATE_REGEX.matchEntire(value) ?: return null
    val day = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val year = match.groupValues[3].toIntOrNull() ?: return null
    return SimpleDate(year, month, day).takeIf { it.isValid() }
}

private fun parseIsoDate(value: String): SimpleDate? {
    val match = ISO_DATE_REGEX.matchEntire(value) ?: return null
    val year = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val day = match.groupValues[3].toIntOrNull() ?: return null
    return SimpleDate(year, month, day).takeIf { it.isValid() }
}

private data class SimpleDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun isValid(): Boolean {
        if (year !in 1900..2100) return false
        if (month !in 1..12) return false
        val maxDay = daysInMonth(year, month)
        return day in 1..maxDay
    }

    fun toIsoString(): String = "${year.toString().padStart(4, '0')}-${month.twoDigits()}-${day.twoDigits()}"

    fun toDisplayString(): String = "${day.twoDigits()}/${month.twoDigits()}/${year.toString().padStart(4, '0')}"

    fun toEpochDay(): Long {
        val adjustedYear = year - if (month <= 2) 1 else 0
        val era = floorDiv(adjustedYear, 400)
        val yearOfEra = adjustedYear - era * 400
        val monthIndex = month + if (month > 2) -3 else 9
        val dayOfYear = (153 * monthIndex + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146097L + dayOfEra - 719468
    }

    companion object {
        fun fromEpochDay(epochDay: Long): SimpleDate {
            val shifted = epochDay + 719468
            val era = floorDiv(shifted, 146097L)
            val dayOfEra = shifted - era * 146097L
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
            val year = (yearOfEra + era * 400).toInt()
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val monthPrime = (5 * dayOfYear + 2) / 153
            val day = (dayOfYear - (153 * monthPrime + 2) / 5 + 1).toInt()
            val month = (monthPrime + if (monthPrime < 10) 3 else -9).toInt()
            val adjustedYear = year + if (month <= 2) 1 else 0
            return SimpleDate(adjustedYear, month, day)
        }
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 0
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}

private fun floorDiv(left: Int, right: Int): Int {
    var result = left / right
    if ((left xor right) < 0 && result * right != left) result--
    return result
}

private fun floorDiv(left: Long, right: Long): Long {
    var result = left / right
    if ((left xor right) < 0 && result * right != left) result--
    return result
}

private val DISPLAY_DATE_REGEX = Regex("^(\\d{2})/(\\d{2})/(\\d{4})$")
private val ISO_DATE_REGEX = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")
