package com.ivelosi.dnc.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ivelosi.dnc.ui.screen.call.CallDestination
import com.ivelosi.dnc.ui.screen.call.CallScreen
import com.ivelosi.dnc.ui.screen.call.CallState
import com.ivelosi.dnc.ui.screen.call.CallViewModel
import com.ivelosi.dnc.ui.screen.call.CallViewModelFactory
import com.ivelosi.dnc.ui.screen.chat.ChatDestination
import com.ivelosi.dnc.ui.screen.chat.ChatScreen
import com.ivelosi.dnc.ui.screen.chat.ChatViewModel
import com.ivelosi.dnc.ui.screen.chat.ChatViewModelFactory
import com.ivelosi.dnc.ui.screen.home.HomeDestination
import com.ivelosi.dnc.ui.screen.home.HomeScreen
import com.ivelosi.dnc.ui.screen.home.HomeViewModel
import com.ivelosi.dnc.ui.screen.home.HomeViewModelFactory
import com.ivelosi.dnc.ui.screen.info.InfoDestination
import com.ivelosi.dnc.ui.screen.info.InfoScreen
import com.ivelosi.dnc.ui.screen.info.InfoViewModel
import com.ivelosi.dnc.ui.screen.info.InfoViewModelFactory
import com.ivelosi.dnc.ui.screen.settings.SettingsDestination
import com.ivelosi.dnc.ui.screen.settings.SettingsScreen
import com.ivelosi.dnc.ui.screen.settings.SettingsViewModel
import com.ivelosi.dnc.ui.screen.settings.SettingsViewModelFactory

@Composable
fun NavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
        enterTransition = {
            EnterTransition.None
            // slideInHorizontally(animationSpec = tween(500))
        },
        exitTransition = {
            ExitTransition.None
        },
        modifier = modifier
    ) {
        composable(route = HomeDestination.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())

            HomeScreen(
                homeViewModel = homeViewModel,
                navController = navController,
                onChatClick = { navController.navigate("${ChatDestination.route}/${it.accountId}") },
                onSettingsButtonClick = { navController.navigate(SettingsDestination.route) },
            )
        }

        composable(
            route = ChatDestination.routeWithArgs,
            arguments = listOf(navArgument(ChatDestination.accountIdArg) {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong(ChatDestination.accountIdArg)
            accountId?.let {
                val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(accountId))

                ChatScreen(
                    accountId = accountId,
                    chatViewModel = chatViewModel,
                    navController = navController,
                    onInfoButtonClick = { navController.navigate("${InfoDestination.route}/${it}") },
                )
            }
        }

        composable(
            route = InfoDestination.routeWithArgs,
            arguments = listOf(navArgument(InfoDestination.accountIdArg) {
                type = NavType.LongType
            })
        ){backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong(InfoDestination.accountIdArg)
            accountId?.let {
                val infoViewModel: InfoViewModel = viewModel(factory = InfoViewModelFactory(accountId))

                InfoScreen(
                    infoViewModel,
                    navController = navController,
                )
            }
        }

        composable(
            route = CallDestination.routeWithArgs,
            arguments = listOf(navArgument(CallDestination.accountIdArg) {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong(CallDestination.accountIdArg)
            val callState = backStackEntry.arguments?.getString(CallDestination.callStateArg)?.let { CallState.valueOf(it) }

            if(accountId != null && callState != null) {
                BackHandler(true) {

                }

                val callViewModel: CallViewModel = viewModel(factory = CallViewModelFactory(accountId, callState))

                CallScreen(
                    callViewModel = callViewModel,
                    navController = navController
                )
            }
        }

        composable(
            route = SettingsDestination.route
        ) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory())

            SettingsScreen(
                settingsViewModel = settingsViewModel,
                navController = navController,
                onSettingsButtonClick = { navController.navigate(SettingsDestination.route) },
            )
        }
    }
}