package com.nuvio.app.features.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class HomeCategoryUiState(
    val categories: List<HomeCategory> = defaultHomeCategories(),
    val activeCategoryId: String = HOME_CATEGORY_HOME_ID,
) {
    /** Categories shown in the top bar, in the user's order. Home is always visible. */
    val navigableCategories: List<HomeCategory>
        get() = categories.filter { it.visible || it.isHome() }

    /** Falls back to Home if the active category was hidden. */
    val effectiveActiveCategoryId: String
        get() = navigableCategories.firstOrNull { it.id == activeCategoryId }?.id
            ?: HOME_CATEGORY_HOME_ID

    val activeCategory: HomeCategory
        get() = categories.firstOrNull { it.id == effectiveActiveCategoryId }
            ?: HomeCategory(id = HOME_CATEGORY_HOME_ID, label = "Home")
}

@Serializable
private data class StoredHomeCategoryPayload(
    val categories: List<HomeCategory> = emptyList(),
    val knownKeys: List<String> = emptyList(),
)

object HomeCategoryRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(HomeCategoryUiState())
    val uiState: StateFlow<HomeCategoryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var knownKeys: Set<String> = emptySet()

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        val stored = HomeCatalogSettingsStorage.loadCategoriesPayload()?.trim().orEmpty()
        val payload = if (stored.isEmpty()) {
            null
        } else {
            runCatching { json.decodeFromString<StoredHomeCategoryPayload>(stored) }.getOrNull()
        }
        if (payload == null || payload.categories.isEmpty()) {
            knownKeys = emptySet()
            _uiState.value = HomeCategoryUiState()
            return
        }
        knownKeys = payload.knownKeys.toSet()
        _uiState.value = HomeCategoryUiState(categories = normalize(payload.categories))
    }

    fun onProfileChanged() = reset()

    fun clearLocalState() = reset()

    private fun reset() {
        hasLoaded = false
        knownKeys = emptySet()
        _uiState.value = HomeCategoryUiState()
    }

    /**
     * Called whenever the catalog list changes. Catalogs seen for the first time get
     * assigned to categories by [guessCategoryIds]; known catalogs keep the user's choices.
     */
    fun syncCatalogKeys(catalogKeys: List<String>) {
        ensureLoaded()
        val newKeys = catalogKeys.filter { it !in knownKeys }
        if (newKeys.isEmpty()) return
        val updated = _uiState.value.categories.map { category ->
            if (category.isHome()) return@map category
            val additions = newKeys.filter { key ->
                category.id in guessCategoryIds(key) && key !in category.itemKeys
            }
            if (additions.isEmpty()) category else category.copy(itemKeys = category.itemKeys + additions)
        }
        knownKeys = knownKeys + newKeys
        _uiState.value = _uiState.value.copy(categories = updated)
        persist()
    }

    fun setActiveCategory(categoryId: String) {
        ensureLoaded()
        if (_uiState.value.categories.none { it.id == categoryId }) return
        _uiState.value = _uiState.value.copy(activeCategoryId = categoryId)
    }

    fun setVisible(categoryId: String, visible: Boolean) {
        ensureLoaded()
        updateCategories { categories ->
            categories.map { category ->
                when {
                    category.id != categoryId -> category
                    category.isHome() -> category.copy(visible = true)
                    else -> category.copy(visible = visible)
                }
            }
        }
    }

    fun move(categoryId: String, delta: Int) {
        ensureLoaded()
        updateCategories { categories ->
            val from = categories.indexOfFirst { it.id == categoryId }
            val to = from + delta
            if (from < 0 || to !in categories.indices) {
                categories
            } else {
                categories.toMutableList().apply { add(to, removeAt(from)) }
            }
        }
    }

    fun setCatalogIncluded(categoryId: String, catalogKey: String, included: Boolean) {
        ensureLoaded()
        updateCategories { categories ->
            categories.map { category ->
                if (category.id != categoryId || category.isHome()) {
                    category
                } else {
                    category.copy(
                        itemKeys = if (included) {
                            (category.itemKeys + catalogKey).distinct()
                        } else {
                            category.itemKeys - catalogKey
                        },
                    )
                }
            }
        }
    }

    fun resetToDefaults(catalogKeys: List<String>) {
        ensureLoaded()
        knownKeys = emptySet()
        _uiState.value = _uiState.value.copy(categories = defaultHomeCategories())
        syncCatalogKeys(catalogKeys)
        persist()
    }

    private fun updateCategories(transform: (List<HomeCategory>) -> List<HomeCategory>) {
        _uiState.value = _uiState.value.copy(categories = transform(_uiState.value.categories))
        persist()
    }

    private fun persist() {
        val payload = StoredHomeCategoryPayload(
            categories = _uiState.value.categories,
            knownKeys = knownKeys.sorted(),
        )
        HomeCatalogSettingsStorage.saveCategoriesPayload(json.encodeToString(payload))
    }

    private fun normalize(stored: List<HomeCategory>): List<HomeCategory> {
        val unique = stored.distinctBy { it.id }.map { category ->
            if (category.isHome()) category.copy(visible = true) else category
        }
        val withHome = if (unique.any { it.isHome() }) {
            unique
        } else {
            listOf(HomeCategory(id = HOME_CATEGORY_HOME_ID, label = "Home")) + unique
        }
        val missingDefaults = defaultHomeCategories().filter { default ->
            withHome.none { it.id == default.id }
        }
        return withHome + missingDefaults
    }
}
