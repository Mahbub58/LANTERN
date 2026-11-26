package com.softvinax.lantern.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.Serializable

@Serializable
data object AskRoute

@Serializable
data class AnswerRoute(val question: String)

@Serializable
data class ReportRoute(val prefill: String = "")

@Serializable
data class ReportReviewRoute(val draftId: String)

@Serializable
data class ReportConfirmationRoute(val reportId: String)

@Serializable
data object MyReportsRoute

@Serializable
data object AlertsRoute

@Serializable
data object SettingsRoute

@Serializable
data object DebugRoute

/**
 * Reads a type-safe route argument by its parameter name.
 *
 * The route classes above remain the single source of truth for navigation. Reading the
 * raw key keeps ViewModels constructible in plain JVM unit tests, where
 * `SavedStateHandle.toRoute()` cannot decode a real navigation argument bundle.
 */
fun SavedStateHandle.routeString(name: String): String = get<String>(name).orEmpty()
