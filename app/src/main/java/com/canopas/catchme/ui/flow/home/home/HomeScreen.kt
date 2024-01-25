package com.canopas.catchme.ui.flow.home.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.canopas.catchme.R
import com.canopas.catchme.data.utils.isBackgroundLocationPermissionGranted
import com.canopas.catchme.ui.component.CheckBackgroundLocationPermission
import com.canopas.catchme.ui.flow.home.activity.ActivityScreen
import com.canopas.catchme.ui.flow.home.home.component.SpaceSelectionMenu
import com.canopas.catchme.ui.flow.home.home.component.SpaceSelectionPopup
import com.canopas.catchme.ui.flow.home.map.MapScreen
import com.canopas.catchme.ui.flow.home.places.PlacesScreen
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!context.isBackgroundLocationPermissionGranted) {
            viewModel.shouldAskForBackgroundLocationPermission(true)
        } else {
            viewModel.startTracking()
        }
    }

    PermissionChecker()

    AppNavigator(navController = navController, viewModel.navActions)

    Scaffold(
        containerColor = AppTheme.colorScheme.surface,
        content = {
            Box(
                modifier = Modifier
                    .padding(it)
            ) {
                HomeScreenContent(navController)
                HomeTopBar()
            }
        },
        bottomBar = {
            HomeBottomBar(navController)
        }
    )
}

@Composable
fun HomeTopBar() {
    var enableLocation by remember { mutableStateOf(true) }
    var toggleSpaceSelection by remember { mutableStateOf(false) }

    SpaceSelectionPopup(show = toggleSpaceSelection)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MapControl(icon = R.drawable.ic_settings, modifier = Modifier) {
        }

        SpaceSelectionMenu(modifier = Modifier.weight(1f)) {
            toggleSpaceSelection = !toggleSpaceSelection
        }

        MapControl(icon = R.drawable.ic_messages, modifier = Modifier) {
        }

        MapControl(
            icon = if (enableLocation) R.drawable.ic_location_on else R.drawable.ic_location_off,
            modifier = Modifier
        ) {
            enableLocation = !enableLocation
        }
    }
}

@Composable
private fun MapControl(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    SmallFloatingActionButton(
        modifier = modifier,
        onClick = { onClick() },
        containerColor = AppTheme.colorScheme.surface,
        contentColor = AppTheme.colorScheme.primary
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PermissionChecker() {
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val state by viewModel.state.collectAsState()

    if (state.shouldAskForBackgroundLocationPermission) {
        CheckBackgroundLocationPermission(onDismiss = {
            viewModel.shouldAskForBackgroundLocationPermission(false)
        }, onGranted = {
            viewModel.startTracking()
        })
    }
}

@Composable
fun HomeScreenContent(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.map.path
    ) {
        composable(AppDestinations.map.path) {
            MapScreen()
        }

        composable(AppDestinations.places.path) {
            PlacesScreen()
        }

        composable(AppDestinations.activity.path) {
            ActivityScreen()
        }
    }
}

@Composable
fun HomeBottomBar(navController: NavHostController) {
    val viewModel = hiltViewModel<HomeScreenViewModel>()
    val state by viewModel.state.collectAsState()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationBar(
        contentColor = AppTheme.colorScheme.primary,
        containerColor = AppTheme.colorScheme.surface
    ) {
        NavItem(
            HomeTab.Main,
            state.currentTab == 0
        ) {
            navigateTo(AppDestinations.map.path)
            viewModel.onTabChange(0)
        }

        NavItem(
            HomeTab.Places,
            state.currentTab == 1
        ) {
            navigateTo(AppDestinations.places.path)
            viewModel.onTabChange(1)
        }

        NavItem(
            HomeTab.Activities,
            state.currentTab == 2
        ) {
            navigateTo(AppDestinations.activity.path)
            viewModel.onTabChange(2)
        }
    }
}

@Composable
private fun RowScope.NavItem(
    screen: HomeTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (screen.route) {
        HomeTab.Main.route -> stringResource(R.string.home_tab_label_home)
        HomeTab.Places.route -> stringResource(R.string.home_tab_label_places)
        HomeTab.Activities.route -> stringResource(R.string.home_tab_label_activities)
        else -> stringResource(R.string.home_tab_label_home)
    }

    NavigationBarItem(
        icon = {
            Icon(
                painter = painterResource(id = if (isSelected) screen.resourceIdFilled else screen.resourceIdLine),
                null,
                modifier = Modifier.size(24.dp)
            )
        },
        selected = isSelected,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = AppTheme.colorScheme.containerNormalOnSurface,
            selectedIconColor = AppTheme.colorScheme.primary,
            selectedTextColor = AppTheme.colorScheme.primary,
            unselectedIconColor = AppTheme.colorScheme.textDisabled,
            unselectedTextColor = AppTheme.colorScheme.textDisabled
        ),
        alwaysShowLabel = true,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = AppTheme.appTypography.label3,
                fontSize = 10.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

sealed class HomeTab(
    val route: String,
    @DrawableRes val resourceIdLine: Int,
    @DrawableRes val resourceIdFilled: Int
) {
    object Main : HomeTab(
        "Main",
        R.drawable.ic_tab_home_outlined,
        R.drawable.ic_tab_home_filled
    )

    object Places : HomeTab(
        "Places",
        R.drawable.ic_tab_places_outlined,
        R.drawable.ic_tab_places_filled
    )

    object Activities : HomeTab(
        "Activities",
        R.drawable.ic_tab_activities_outlined,
        R.drawable.ic_tab_activities_filled
    )
}