package cat.agrisync.data

actual object PlatformDateTime {
    actual fun currentYear(): Int = java.time.LocalDate.now().year
}
