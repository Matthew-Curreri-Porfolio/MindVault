package com.mindvault.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindvault.ai.ui.detail.EntryDetailRoute
import com.mindvault.ai.ui.entries.EntriesRoute
import com.mindvault.ai.ui.insights.InsightsRoute
import com.mindvault.ai.ui.home.HomeRoute
import com.mindvault.ai.ui.theme.MindVaultTheme

class MainActivity : ComponentActivity() {
    private val reqPerms =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MindVaultTheme {
                MindVaultApp()
            }
        }
    }
}

@Composable
private fun MindVaultApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination.isBottomDestination()
    val snackbarHostState = remember { SnackbarHostState() }

    val view = LocalView.current
    val colorScheme = MaterialTheme.colorScheme
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
            }
            val controller = WindowCompat.getInsetsController(window, view)
            val isLight = ColorUtils.calculateLuminance(colorScheme.surface.toArgb()) > 0.5
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
    }

    Scaffold(
        topBar = {
            MindVaultTopBar(
                currentDestination = currentDestination,
                showBack = !showBottomBar,
                onNavigateUp = { navController.navigateUp() }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                MindVaultBottomBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        MindVaultNavHost(
            navController = navController,
            contentPadding = padding,
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MindVaultTopBar(
    currentDestination: NavDestination?,
    showBack: Boolean,
    onNavigateUp: () -> Unit
) {
    val routeKey = currentDestination?.route?.substringBefore('/')
    val title = when (routeKey) {
        MindVaultDestination.Home.route -> "Home"
        MindVaultDestination.Entries.route -> "Entries"
        MindVaultDestination.Insights.route -> "Insights"
        MindVaultDestination.Settings.route -> "Settings"
        MindVaultDestination.EntryDetail.route -> "Entry"
        else -> "MindVault"
    }

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    )
}

@Composable
private fun MindVaultBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    NavigationBar {
        MindVaultDestination.bottomDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun MindVaultNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = MindVaultDestination.Home.route,
        modifier = Modifier.padding(contentPadding)
    ) {
        composable(MindVaultDestination.Home.route) {
            HomeRoute(snackbarHostState = snackbarHostState)
        }
        composable(MindVaultDestination.Entries.route) {
            EntriesRoute(
                onOpenEntry = { entryId ->
                    navController.navigate(MindVaultDestination.EntryDetail.createRoute(entryId))
                }
            )
        }
        composable(MindVaultDestination.Insights.route) {
            InsightsRoute(
                contentPadding = PaddingValues(0.dp),
                snackbarHostState = snackbarHostState
            )
        }
        composable(MindVaultDestination.Settings.route) {
            com.mindvault.ai.ui.settings.SettingsRoute(
                contentPadding = PaddingValues(0.dp),
                snackbarHostState = snackbarHostState
            )
        }
        composable(
            route = MindVaultDestination.EntryDetail.routeWithArg,
            arguments = listOf(navArgument(MindVaultDestination.EntryDetail.KEY_ENTRY_ID) {
                defaultValue = ""
            })
        ) { navBackStackEntry ->
            val entryId = navBackStackEntry.arguments?.getString(MindVaultDestination.EntryDetail.KEY_ENTRY_ID).orEmpty()
            EntryDetailRoute(
                entryId = entryId,
                contentPadding = PaddingValues(0.dp),
                snackbarHostState = snackbarHostState,
                onEntryDeleted = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private sealed class MindVaultDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : MindVaultDestination("home", "Home", Icons.Outlined.Mic)
    object Entries : MindVaultDestination("entries", "Entries", Icons.Outlined.CalendarViewMonth)
    object Insights : MindVaultDestination("insights", "Insights", Icons.Outlined.Analytics)
    object Settings : MindVaultDestination("settings", "Settings", Icons.Outlined.Settings)
    object EntryDetail : MindVaultDestination("entry_detail", "Entry", Icons.Outlined.CalendarViewMonth) {
        const val KEY_ENTRY_ID = "entryId"
        val routeWithArg: String = "$route/{$KEY_ENTRY_ID}"
        fun createRoute(id: String) = "$route/$id"
    }

    companion object {
        val bottomDestinations = listOf(Home, Entries, Insights, Settings)
    }
}

@Composable
private fun InsightsPlaceholder() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Weekly mood chart coming soon", style = MaterialTheme.typography.titleLarge)
        Text(text = "Insights and streaks will live here.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsPlaceholder() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)
        Text(text = "Account & Sync", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Privacy", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Models", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Storage", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EntryDetailPlaceholder(entryId: String) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Entry Detail", style = MaterialTheme.typography.titleLarge)
        Text(text = "Entry ID: $entryId", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Summary, transcript, and playback UI will land here.", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun NavDestination?.isBottomDestination(): Boolean {
    val routeKey = this?.route?.substringBefore('/')
    return MindVaultDestination.bottomDestinations.any { it.route == routeKey }
}
