package com.mockgps.standalone.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mockgps.standalone.R
import com.mockgps.standalone.data.model.FavoriteEntity
import com.mockgps.standalone.data.model.RecentEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocationListTabs(
    favorites: List<FavoriteEntity>,
    recents: List<RecentEntity>,
    onFavoriteTap: (FavoriteEntity) -> Unit,
    onDeleteFavorite: (Long) -> Unit,
    onRecentTap: (RecentEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_favorites)) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.tab_recents)) })
        }
        when (selectedTab) {
            0 -> FavoritesList(favorites, onFavoriteTap, onDeleteFavorite)
            1 -> RecentsList(recents, onRecentTap)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesList(items: List<FavoriteEntity>, onTap: (FavoriteEntity) -> Unit, onDelete: (Long) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.padding(16.dp)) { Text(stringResource(R.string.favorites_empty)) }
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(items, key = { it.id }) { fav ->
            ListItem(
                headlineContent = { Text(fav.name) },
                supportingContent = { Text("%.5f, %.5f".format(fav.lat, fav.lon), style = MaterialTheme.typography.bodySmall) },
                trailingContent = {
                    IconButton(onClick = { onDelete(fav.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "刪除")
                    }
                },
                modifier = Modifier.combinedClickable(onClick = { onTap(fav) })
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentsList(items: List<RecentEntity>, onTap: (RecentEntity) -> Unit) {
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    if (items.isEmpty()) {
        Box(modifier = Modifier.padding(16.dp)) { Text(stringResource(R.string.recents_empty)) }
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(items, key = { it.id }) { recent ->
            ListItem(
                headlineContent = { Text(recent.name ?: "%.5f, %.5f".format(recent.lat, recent.lon)) },
                supportingContent = { Text(fmt.format(Date(recent.usedAt)), style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.combinedClickable(onClick = { onTap(recent) })
            )
            HorizontalDivider()
        }
    }
}
