package com.nuvio.app.features.home

import kotlinx.serialization.Serializable

internal const val HOME_CATEGORY_HOME_ID = "home"

/**
 * A top-navigation category. [itemKeys] are HomeCatalogSettingsItem keys
 * ("addonId:type:catalogId" or "collection_..."). The Home category is built in:
 * it always shows every enabled catalog, so existing layouts are unchanged.
 */
@Serializable
data class HomeCategory(
    val id: String,
    val label: String,
    val visible: Boolean = true,
    val itemKeys: List<String> = emptyList(),
)

internal fun HomeCategory.isHome(): Boolean = id == HOME_CATEGORY_HOME_ID

internal fun HomeCategory.includes(catalogKey: String): Boolean =
    isHome() || catalogKey in itemKeys

internal fun defaultHomeCategories(): List<HomeCategory> = listOf(
    HomeCategory(id = HOME_CATEGORY_HOME_ID, label = "Home"),
    HomeCategory(id = "movies", label = "Movies"),
    HomeCategory(id = "tv", label = "TV Shows"),
    HomeCategory(id = "live", label = "Live TV"),
    HomeCategory(id = "anime", label = "Anime"),
    HomeCategory(id = "discover", label = "Discover"),
)

/**
 * Best-guess default category assignment for a catalog key, used once when a catalog
 * first appears. After that the user's assignments are authoritative.
 */
internal fun guessCategoryIds(catalogKey: String): List<String> {
    if (catalogKey.startsWith("collection_")) return emptyList()
    val key = catalogKey.lowercase()
    return buildList {
        if (":movie:" in key) add("movies")
        if (":series:" in key) add("tv")
        if (":tv:" in key) add("live")
        if ("anime" in key) add("anime")
    }
}
