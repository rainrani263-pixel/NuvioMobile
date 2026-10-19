package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.HomeCategory
import com.nuvio.app.features.home.HomeCategoryRepository
import com.nuvio.app.features.home.isHome

/** Entry row shown on the Homescreen settings page. */
@Composable
fun HomeCategoriesEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(isTablet = false, modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Navigation categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Choose top bar categories, their order, and their catalogs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HomeCategorySettingsScreen(
    onBack: () -> Unit,
) {
    val catalogSettings by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val categoryState by HomeCategoryRepository.uiState.collectAsStateWithLifecycle()
    val catalogKeys = remember(catalogSettings.items) { catalogSettings.items.map { it.key } }
    val catalogItems = remember(catalogSettings.items) { catalogSettings.items.filter { !it.isCollection } }
    var expandedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(catalogKeys) {
        HomeCategoryRepository.ensureLoaded()
        HomeCategoryRepository.syncCatalogKeys(catalogKeys)
    }

    val categories = categoryState.categories

    NuvioScreen(modifier = Modifier.fillMaxSize()) {
        stickyHeader {
            NuvioScreenHeader(
                title = "Navigation categories",
                onBack = onBack,
            )
        }

        item(key = "categories_intro") {
            Text(
                text = "Choose which categories appear in the top bar, their order, and which catalogs each one shows. Home always shows every catalog.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item(key = "categories_list") {
            SettingsGroup(isTablet = false, modifier = Modifier.padding(horizontal = 16.dp)) {
                categories.forEachIndexed { index, category ->
                    if (index > 0) SettingsGroupDivider(isTablet = false)
                    HomeCategoryEditorRow(
                        category = category,
                        isFirst = index == 0,
                        isLast = index == categories.lastIndex,
                        onToggleExpanded = {
                            expandedId = if (expandedId == category.id) null else category.id
                        },
                        onVisibleChange = { visible -> HomeCategoryRepository.setVisible(category.id, visible) },
                        onMoveUp = { HomeCategoryRepository.move(category.id, -1) },
                        onMoveDown = { HomeCategoryRepository.move(category.id, 1) },
                    )
                    if (expandedId == category.id) {
                        CategoryCatalogPicker(
                            category = category,
                            catalogItems = catalogItems,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryEditorRow(
    category: HomeCategory,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleExpanded: () -> Unit,
    onVisibleChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onMoveUp, enabled = !isFirst) { Text("Up") }
        TextButton(onClick = onMoveDown, enabled = !isLast) { Text("Down") }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = category.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (category.isHome()) {
                    "Shows every catalog"
                } else {
                    "${category.itemKeys.size} catalogs, tap to edit"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = category.visible,
            onCheckedChange = onVisibleChange,
            enabled = !category.isHome(),
        )
    }
}

@Composable
private fun CategoryCatalogPicker(
    category: HomeCategory,
    catalogItems: List<com.nuvio.app.features.home.HomeCatalogSettingsItem>,
) {
    when {
        category.isHome() -> Text(
            text = "Home shows every enabled catalog, so there is nothing to choose here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        catalogItems.isEmpty() -> Text(
            text = "No catalogs yet. Install or enable an addon first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        else -> catalogItems.forEach { item ->
            val checked = item.key in category.itemKeys
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HomeCategoryRepository.setCatalogIncluded(category.id, item.key, !checked)
                    }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.addonName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
