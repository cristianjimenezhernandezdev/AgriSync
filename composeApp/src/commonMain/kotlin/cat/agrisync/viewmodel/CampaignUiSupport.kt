package cat.agrisync.viewmodel

import cat.agrisync.data.PlatformDateTime

internal fun normalizedCampanyes(existing: List<Int>): List<Int> {
    val currentYear = PlatformDateTime.currentYear()
    return (existing + currentYear).distinct().sortedDescending()
}

internal fun resolveSelectedCampanya(existing: List<Int>, preferredCampanya: Int?): Int {
    val available = normalizedCampanyes(existing)
    return preferredCampanya?.takeIf { available.contains(it) }
        ?: available.firstOrNull()
        ?: PlatformDateTime.currentYear()
}
