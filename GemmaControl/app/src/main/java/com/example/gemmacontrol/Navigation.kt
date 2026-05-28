package com.example.gemmacontrol

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.gemmacontrol.ui.main.MainScreen
import com.example.gemmacontrol.ui.main.SetupScreen
import com.example.gemmacontrol.ui.main.SetupViewModel
import com.example.gemmacontrol.ui.main.StoredInboxScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current

    // Determine starting destination: skip Setup if already fully configured
    val setupViewModel = remember { SetupViewModel(context.applicationContext as android.app.Application) }
    val startDest = if (setupViewModel.isSetupComplete(context)) Main else Setup

    val backStack = rememberNavBackStack(startDest)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Setup> {
                SetupScreen(
                    viewModel = setupViewModel,
                    onSetupComplete = {
                        // Replace the Setup entry with Main (no back arrow to Setup)
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    }
                )
            }
            entry<Main> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) }
                )
            }
            entry<StoredInbox> {
                StoredInboxScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

