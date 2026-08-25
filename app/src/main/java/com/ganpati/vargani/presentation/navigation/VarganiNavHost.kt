package com.ganpati.vargani.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ganpati.vargani.presentation.auth.LoginRoute
import com.ganpati.vargani.presentation.auth.OtpRoute
import com.ganpati.vargani.presentation.auth.SignUpRoute
import com.ganpati.vargani.presentation.backup.BackupRoute
import com.ganpati.vargani.presentation.dashboard.DashboardRoute
import com.ganpati.vargani.presentation.donation.DonationFormRoute
import com.ganpati.vargani.presentation.donation.DonationDetailsRoute
import com.ganpati.vargani.presentation.donor.DonorListRoute
import com.ganpati.vargani.presentation.expense.ExpenseDetailsRoute
import com.ganpati.vargani.presentation.expense.ExpenseFormRoute
import com.ganpati.vargani.presentation.profile.AmountBreakdownRoute
import com.ganpati.vargani.presentation.profile.ProfileRoute
import com.ganpati.vargani.presentation.receipt.ReceiptPreviewRoute
import com.ganpati.vargani.presentation.reports.ReportsRoute
import com.ganpati.vargani.presentation.settings.SettingsRoute
import com.ganpati.vargani.presentation.splash.SplashRoute
import com.ganpati.vargani.presentation.users.ManageUsersRoute
sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Login : Route("auth/login")
    data object SignUp : Route("auth/signup")
    data object Otp : Route("auth/otp")
    data object Dashboard : Route("dashboard")
    data object Donors : Route("donors")
    data object Reports : Route("reports")
    data object Settings : Route("settings")
    data object Backup : Route("backup")
    data object ManageUsers : Route("settings/users")
    data object Profile : Route("profile")
    data object AmountBreakdown : Route("profile/breakdown")
    data object AddDonation : Route("donation/add")
    data object EditDonation : Route("donation/edit/{donationId}") {
        fun create(donationId: Long) = "donation/edit/$donationId"
    }
    data object DonationDetails : Route("donation/details/{donationId}") {
        fun create(donationId: Long) = "donation/details/$donationId"
    }
    data object Receipt : Route("receipt/{donationId}") {
        fun create(donationId: Long) = "receipt/$donationId"
    }
    data object AddExpense : Route("expense/add")
    data object EditExpense : Route("expense/edit/{expenseId}") {
        fun create(expenseId: Long) = "expense/edit/$expenseId"
    }
    data object ExpenseDetails : Route("expense/details/{expenseId}") {
        fun create(expenseId: Long) = "expense/details/$expenseId"
    }
}

private const val ANIM_MS = 380

@Composable
fun VarganiNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Route.Splash.path
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(ANIM_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(ANIM_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    initialOffset = { it / 5 },
                )
        },
        exitTransition = {
            fadeOut(tween(ANIM_MS / 2)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(ANIM_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                targetOffset = { it / 8 },
            )
        },
        popEnterTransition = {
            fadeIn(tween(ANIM_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(ANIM_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    initialOffset = { it / 5 },
                )
        },
        popExitTransition = {
            fadeOut(tween(ANIM_MS / 2)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(ANIM_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                targetOffset = { it / 8 },
            )
        }
    ) {
        composable(Route.Splash.path) {
            SplashRoute(
                onLoggedIn = {
                    navController.navigate(Route.Dashboard.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
                onNeedAuth = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.Login.path) {
            LoginRoute(
                onLoggedIn = {
                    navController.navigate(Route.Dashboard.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Route.SignUp.path) },
            )
        }
        composable(Route.SignUp.path) {
            SignUpRoute(
                onBack = { navController.popBackStack() },
                onLoggedIn = {
                    navController.navigate(Route.Dashboard.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.Otp.path) {
            OtpRoute(
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(Route.Dashboard.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.Dashboard.path) {
            DashboardRoute(
                onAddDonation = { navController.navigate(Route.AddDonation.path) },
                onAddExpense = { navController.navigate(Route.AddExpense.path) },
                onOpenDonors = { navController.navigate(Route.Donors.path) },
                onOpenManageUsers = { navController.navigate(Route.ManageUsers.path) },
                onOpenSettings = { navController.navigate(Route.Settings.path) },
                onOpenProfile = { navController.navigate(Route.Profile.path) },
                onOpenDonation = { id -> navController.navigate(Route.DonationDetails.create(id)) },
                onOpenExpense = { id -> navController.navigate(Route.ExpenseDetails.create(id)) },
                onOpenHistory = { navController.navigate(Route.AmountBreakdown.path) },
                onExport = { navController.navigate(Route.Reports.path) },
            )
        }
        composable(Route.Donors.path) {
            DonorListRoute(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Route.AddDonation.path) },
                onOpen = { id -> navController.navigate(Route.DonationDetails.create(id)) },
                onEdit = { id -> navController.navigate(Route.EditDonation.create(id)) }
            )
        }
        composable(Route.Reports.path) {
            ReportsRoute(onBack = { navController.popBackStack() })
        }
        composable(Route.Backup.path) {
            BackupRoute(onBack = { navController.popBackStack() })
        }
        composable(Route.Settings.path) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onOpenProfile = { navController.navigate(Route.Profile.path) },
                onLoggedOut = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.ManageUsers.path) {
            ManageUsersRoute(onBack = { navController.popBackStack() })
        }
        composable(Route.Profile.path) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.AmountBreakdown.path) {
            AmountBreakdownRoute(
                onBack = { navController.popBackStack() },
                onOpenDonation = { id -> navController.navigate(Route.DonationDetails.create(id)) },
                onOpenExpense = { id -> navController.navigate(Route.ExpenseDetails.create(id)) },
            )
        }
        composable(Route.AddDonation.path) {
            DonationFormRoute(
                donationId = null,
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    navController.navigate(Route.DonationDetails.create(id)) {
                        popUpTo(Route.Dashboard.path)
                    }
                }
            )
        }
        composable(
            route = Route.EditDonation.path,
            arguments = listOf(navArgument("donationId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("donationId") ?: return@composable
            DonationFormRoute(
                donationId = id,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Route.DonationDetails.path,
            arguments = listOf(navArgument("donationId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("donationId") ?: return@composable
            DonationDetailsRoute(
                donationId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Route.EditDonation.create(id)) },
                onReceipt = { navController.navigate(Route.Receipt.create(id)) },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(
            route = Route.Receipt.path,
            arguments = listOf(navArgument("donationId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("donationId") ?: return@composable
            ReceiptPreviewRoute(
                donationId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.AddExpense.path) {
            ExpenseFormRoute(
                expenseId = null,
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    navController.navigate(Route.ExpenseDetails.create(id)) {
                        popUpTo(Route.Dashboard.path)
                    }
                },
            )
        }
        composable(
            route = Route.EditExpense.path,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("expenseId") ?: return@composable
            ExpenseFormRoute(
                expenseId = id,
                onBack = { navController.popBackStack() },
                onSaved = { _ -> navController.popBackStack() },
            )
        }
        composable(
            route = Route.ExpenseDetails.path,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("expenseId") ?: return@composable
            ExpenseDetailsRoute(
                expenseId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Route.EditExpense.create(id)) },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
