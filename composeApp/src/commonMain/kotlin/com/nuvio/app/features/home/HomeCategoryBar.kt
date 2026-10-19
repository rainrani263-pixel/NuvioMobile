package com.nuvio.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Horizontal category strip shown at the top of the home screen. */
@Composable
internal fun HomeCategoryBar(
    categories: List<HomeCategory>,
    activeCategoryId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = categories, key = { it.id }) { category ->
            FilterChip(
                selected = category.id == activeCategoryId,
                onClick = { onSelect(category.id) },
                label = { Text(category.label) },
            )
        }
    }
}
