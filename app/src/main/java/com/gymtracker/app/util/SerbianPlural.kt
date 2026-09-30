package com.gymtracker.app.util

/**
 * Srpska pluralizacija: vraća oblik zavisno od broja.
 * @param n broj
 * @param one oblik za 1 (npr. "trening")
 * @param few oblik za 2-4 (npr. "treninga")
 * @param many oblik za 5+ (npr. "treninga")
 */
fun pluralize(n: Int, one: String, few: String, many: String): String {
    val abs = n % 100
    return when {
        abs in 11..19 -> many
        abs % 10 == 1 -> one
        abs % 10 in 2..4 -> few
        else -> many
    }
}
