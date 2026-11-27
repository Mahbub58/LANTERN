package com.mahbub.lantern.domain.model

enum class AlertLevel {
    INFO,
    WARNING,
    URGENT,
}

/** Which Firestore feed an alert came from. */
enum class AlertKind {
    ALERT,
    WARNING,
}

data class Alert(
    val id: String,
    val level: AlertLevel,
    val title: LocalizedText,
    val message: LocalizedText,
    val region: String,
    /** Epoch millis when the responder published the alert. */
    val issuedAt: Long,
    /** [AlertKind.ALERT] for `alerts`, [AlertKind.WARNING] for `warnings`. */
    val kind: AlertKind = AlertKind.ALERT,
)
