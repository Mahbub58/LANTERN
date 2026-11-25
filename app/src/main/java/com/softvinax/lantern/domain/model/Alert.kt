package com.softvinax.lantern.domain.model

enum class AlertLevel {
    INFO,
    WARNING,
    URGENT,
}

data class Alert(
    val id: String,
    val level: AlertLevel,
    val title: LocalizedText,
    val message: LocalizedText,
    val region: String,
    /** Epoch millis when the responder published the alert. */
    val issuedAt: Long,
)
