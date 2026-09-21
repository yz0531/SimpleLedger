package com.example.simpleledger.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.simpleledger.AppContainer
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.domain.model.LedgerSkin
import com.example.simpleledger.ui.backup.NutstoreBackupScreen
import com.example.simpleledger.ui.editor.EditorScreen
import com.example.simpleledger.ui.home.HomeScreen
import com.example.simpleledger.ui.recurring.RecurringEditorScreen
import com.example.simpleledger.ui.recurring.RecurringScreen
import com.example.simpleledger.ui.settings.SettingsScreen
import com.example.simpleledger.ui.statistics.StatisticsScreen
import com.example.simpleledger.ui.theme.SkinBackground
import com.example.simpleledger.ui.transfer.TransferScreen
import kotlinx.coroutines.CancellationException

private const val LEDGER_APP_LOG_TAG = "LedgerApp"

private object Routes {
    const val HOME = "home"
    const val RECURRING = "recurring"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val EDITOR = "editor"
    const val EDITOR_WITH_ID = "editor/{transactionId}"
    const val RECURRING_EDITOR = "recurring-editor"
    const val RECURRING_EDITOR_WITH_ID = "recurring-editor/{ruleId}"
    const val TRANSFER = "transfer"
    const val NUTSTORE_BACKUP = "nutstore-backup"

    fun editor(transactionId: String): String = "editor/${Uri.encode(transactionId)}"
    fun recurringEditor(ruleId: String): String = "recurring-editor/${Uri.encode(ruleId)}"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "账本", Icons.AutoMirrored.Rounded.ReceiptLong),
    TopLevelDestination(Routes.RECURRING, "周期", Icons.Rounded.EventRepeat),
    TopLevelDestination(Routes.STATISTICS, "统计", Icons.Rounded.BarChart),
    TopLevelDestination(Routes.SETTINGS, "设置", Icons.Rounded.Settings),
)

@Composable
fun LedgerApp(
    container: AppContainer,
    skin: LedgerSkin,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val transactions by container.repository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val mode by container.preferences.mode.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }
    val lifecycleOwner = LocalLifecycleOwner.current
    var foregroundGeneration by remember {
        mutableIntStateOf(
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) 1 else 0,
        )
    }

    LaunchedEffect(mode) {
        if (mode != LedgerMode.EXPENSE_ONLY) return@LaunchedEffect
        try {
            container.recurringRepository.disableIncomeRules()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(LEDGER_APP_LOG_TAG, "Failed to enforce expense-only recurring rules", exception)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) foregroundGeneration += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(foregroundGeneration) {
        if (foregroundGeneration == 0) return@LaunchedEffect
        try {
            val foregroundMode = container.preferences.currentMode
            if (foregroundMode == LedgerMode.EXPENSE_ONLY) {
                container.recurringRepository.disableIncomeRules()
            }
            container.recurringProcessor.processDue(
                includeIncome = foregroundMode == LedgerMode.INCOME_AND_EXPENSE,
            )
            container.nutstoreBackupManager.automaticBackupIfChanged()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(LEDGER_APP_LOG_TAG, "Foreground maintenance failed", exception)
        }
    }

    SkinBackground(skin = skin, modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    ) {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navController.navigateTopLevel(destination.route) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val navigationModifier = if (showBottomBar) {
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            } else {
                Modifier.fillMaxSize()
            }

            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = navigationModifier,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        transactions = transactions,
                        mode = mode,
                        onAdd = { navController.navigate(Routes.EDITOR) },
                        onEdit = { id -> navController.navigate(Routes.editor(id)) },
                    )
                }
                composable(Routes.RECURRING) {
                    RecurringScreen(
                        repository = container.recurringRepository,
                        mode = mode,
                        onAdd = { navController.navigate(Routes.RECURRING_EDITOR) },
                        onEdit = { id -> navController.navigate(Routes.recurringEditor(id)) },
                    )
                }
                composable(Routes.STATISTICS) {
                    StatisticsScreen(transactions = transactions)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        mode = mode,
                        skin = skin,
                        onModeChanged = container.preferences::setMode,
                        onSkinChanged = container.preferences::setSkin,
                        onRecurring = { navController.navigateTopLevel(Routes.RECURRING) },
                        onTransfer = { navController.navigate(Routes.TRANSFER) },
                        onNutstoreBackup = { navController.navigate(Routes.NUTSTORE_BACKUP) },
                    )
                }
                composable(Routes.EDITOR) {
                    EditorScreen(
                        repository = container.repository,
                        transactionId = null,
                        mode = mode,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.EDITOR_WITH_ID,
                    arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
                ) { entry ->
                    EditorScreen(
                        repository = container.repository,
                        transactionId = entry.arguments?.getString("transactionId"),
                        mode = mode,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(Routes.RECURRING_EDITOR) {
                    RecurringEditorScreen(
                        repository = container.recurringRepository,
                        ruleId = null,
                        mode = mode,
                        processDue = {
                            container.recurringProcessor.processDue(
                                includeIncome = mode == LedgerMode.INCOME_AND_EXPENSE,
                            )
                        },
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.RECURRING_EDITOR_WITH_ID,
                    arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
                ) { entry ->
                    RecurringEditorScreen(
                        repository = container.recurringRepository,
                        ruleId = entry.arguments?.getString("ruleId"),
                        mode = mode,
                        processDue = {
                            container.recurringProcessor.processDue(
                                includeIncome = mode == LedgerMode.INCOME_AND_EXPENSE,
                            )
                        },
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(Routes.TRANSFER) {
                    TransferScreen(
                        transferManager = container.transferManager,
                        onImportCompleted = {
                            if (mode == LedgerMode.EXPENSE_ONLY) {
                                container.recurringRepository.disableIncomeRules()
                            }
                            container.recurringProcessor.processDue(
                                includeIncome = mode == LedgerMode.INCOME_AND_EXPENSE,
                            )
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.NUTSTORE_BACKUP) {
                    NutstoreBackupScreen(
                        manager = container.nutstoreBackupManager,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
