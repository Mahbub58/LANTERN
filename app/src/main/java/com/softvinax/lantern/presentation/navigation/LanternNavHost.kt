package com.softvinax.lantern.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.softvinax.lantern.presentation.alerts.AlertsScreenRoute
import com.softvinax.lantern.presentation.answer.AnswerScreenRoute
import com.softvinax.lantern.presentation.ask.AskScreenRoute
import com.softvinax.lantern.presentation.components.LanternTab
import com.softvinax.lantern.presentation.debug.DebugScreenRoute
import com.softvinax.lantern.presentation.report.MyReportsScreenRoute
import com.softvinax.lantern.presentation.report.ReportConfirmationScreenRoute
import com.softvinax.lantern.presentation.report.ReportReviewScreenRoute
import com.softvinax.lantern.presentation.report.ReportScreenRoute
import com.softvinax.lantern.presentation.settings.SettingsScreenRoute

@Composable
fun LanternNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AskRoute,
    ) {
        composable<AskRoute> {
            AskScreenRoute(
                onOpenAnswer = { question -> navController.navigate(AnswerRoute(question)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenDebug = { navController.navigate(DebugRoute) },
                onOpenMyReports = { navController.navigate(MyReportsRoute) },
                onTabSelected = navController::navigateToTab,
            )
        }

        composable<AnswerRoute> {
            AnswerScreenRoute(
                onBack = { navController.popBackStack() },
                onAskAnother = { navController.popBackStack() },
                onFileReport = { question ->
                    navController.navigate(ReportRoute(prefill = question)) {
                        popUpTo<AskRoute> { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ReportRoute> {
            ReportScreenRoute(
                onOpenReview = { draftId -> navController.navigate(ReportReviewRoute(draftId)) },
                onOpenMyReports = { navController.navigate(MyReportsRoute) },
                onTabSelected = navController::navigateToTab,
            )
        }

        composable<ReportReviewRoute> {
            ReportReviewScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenConfirmation = { reportId ->
                    navController.navigate(ReportConfirmationRoute(reportId)) {
                        popUpTo<ReportRoute> { inclusive = true }
                    }
                },
                onStartNewReport = {
                    navController.navigate(ReportRoute()) {
                        popUpTo<ReportRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ReportConfirmationRoute> {
            ReportConfirmationScreenRoute(
                onViewReports = { navController.navigate(MyReportsRoute) },
                onDone = {
                    navController.navigate(AskRoute) {
                        popUpTo<AskRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNewReport = {
                    navController.navigate(ReportRoute()) {
                        popUpTo<AskRoute> { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<MyReportsRoute> {
            MyReportsScreenRoute(
                onBack = { navController.popBackStack() },
                onFileReport = {
                    navController.navigate(ReportRoute()) {
                        popUpTo<AskRoute> { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<AlertsRoute> {
            AlertsScreenRoute(onTabSelected = navController::navigateToTab)
        }

        composable<SettingsRoute> {
            SettingsScreenRoute(onBack = { navController.popBackStack() })
        }

        composable<DebugRoute> {
            DebugScreenRoute(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateToTab(tab: LanternTab) {
    when (tab) {
        LanternTab.ASK -> navigate(AskRoute) {
            popUpTo<AskRoute> { inclusive = true }
            launchSingleTop = true
        }

        LanternTab.REPORT -> navigate(ReportRoute()) {
            popUpTo<AskRoute> { inclusive = false }
            launchSingleTop = true
        }

        LanternTab.ALERTS -> navigate(AlertsRoute) {
            popUpTo<AskRoute> { inclusive = false }
            launchSingleTop = true
        }
    }
}
