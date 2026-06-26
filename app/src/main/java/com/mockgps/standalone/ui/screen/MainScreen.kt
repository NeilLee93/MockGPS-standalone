// app/src/main/java/com/mockgps/standalone/ui/screen/MainScreen.kt
package com.mockgps.standalone.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mockgps.standalone.R
import com.mockgps.standalone.ui.MainViewModel
import com.mockgps.standalone.ui.MockMode
import com.mockgps.standalone.ui.component.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val recents by vm.recents.collectAsState()
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteNameInput by remember { mutableStateOf("") }

    val sheetState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 160.dp,
        sheetContent = {
            BottomSheetContent(
                uiState = uiState,
                onLatChange = { vm.onMapTap(it, uiState.lon) },
                onLonChange = { vm.onMapTap(uiState.lat, it) },
                onModeChange = vm::onModeChange,
                onRadiusChange = vm::onRadiusChanged,
                onSpeedChange = vm::onSpeedChanged,
                onStart = { if (uiState.mode == MockMode.WALKER) vm.onStartWalker() else vm.onStartStatic() },
                onStop = vm::onStop
            ) {
                LocationSearchBar(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    onQueryChange = vm::onSearch,
                    onResultSelected = vm::onSearchResultSelected
                )
                Spacer(Modifier.height(8.dp))
                LocationListTabs(
                    favorites = favorites,
                    recents = recents,
                    onFavoriteTap = { vm.onLocationListItemTapped(it.lat, it.lon, it.name) },
                    onDeleteFavorite = vm::onDeleteFavorite,
                    onRecentTap = { vm.onLocationListItemTapped(it.lat, it.lon, it.name) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OsmMapView(
                lat = uiState.lat,
                lon = uiState.lon,
                walkerRadius = if (uiState.mode == MockMode.WALKER) uiState.walkerRadius else null,
                onLocationSelected = { lat, lon -> vm.onMapTap(lat, lon) },
                modifier = Modifier.fillMaxSize()
            )
            // FAB: re-center map
            FloatingActionButton(
                onClick = { /* map re-centers via lat/lon state in OsmMapView */ vm.onMapTap(uiState.lat, uiState.lon) },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) { Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.fab_recenter)) }

            // FAB: add favorite
            FloatingActionButton(
                onClick = { favoriteNameInput = ""; showAddFavoriteDialog = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) { Icon(Icons.Default.Star, contentDescription = stringResource(R.string.add_favorite_title)) }
        }
    }

    if (showAddFavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showAddFavoriteDialog = false },
            title = { Text(stringResource(R.string.add_favorite_title)) },
            text = {
                OutlinedTextField(
                    value = favoriteNameInput,
                    onValueChange = { favoriteNameInput = it },
                    label = { Text(stringResource(R.string.add_favorite_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (favoriteNameInput.isNotBlank()) vm.onAddFavorite(favoriteNameInput)
                    showAddFavoriteDialog = false
                }) { Text(stringResource(R.string.add_favorite_save)) }
            },
            dismissButton = { TextButton(onClick = { showAddFavoriteDialog = false }) { Text(stringResource(R.string.add_favorite_cancel)) } }
        )
    }
}
