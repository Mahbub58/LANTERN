package com.softvinax.lantern.data.remote

/**
 * Compares card-pack versions such as "1.0.0-demo" and "1.1.0".
 * Non-numeric suffixes are ignored for ordering, so a preview bundle does not
 * look newer than a released pack with the same numbers.
 */
object VersionComparator {

    fun isNewer(candidate: String, baseline: String): Boolean {
        val candidateParts = parts(candidate)
        val baselineParts = parts(baseline)
        val size = maxOf(candidateParts.size, baselineParts.size)
        for (index in 0 until size) {
            val left = candidateParts.getOrElse(index) { 0 }
            val right = baselineParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim()
            .substringBefore('-')
            .split('.')
            .map { segment -> segment.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
