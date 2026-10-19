package com.nuvio.app.features.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCategoryTest {

    @Test
    fun guessesDefaultCategoriesFromCatalogKeys() {
        assertEquals(listOf("movies"), guessCategoryIds("tmdb:movie:top"))
        assertEquals(listOf("tv"), guessCategoryIds("tmdb:series:popular"))
        assertEquals(listOf("live"), guessCategoryIds("iptv:tv:channels"))
        assertTrue("anime" in guessCategoryIds("kitsu:series:anime-trending"))
        assertTrue(guessCategoryIds("collection_123").isEmpty())
    }

    @Test
    fun homeIncludesEverythingAndOtherCategoriesOnlyTheirOwnKeys() {
        val home = defaultHomeCategories().first()
        val movies = HomeCategory(id = "movies", label = "Movies", itemKeys = listOf("a:movie:b"))
        assertTrue(home.includes("anything:at:all"))
        assertTrue(movies.includes("a:movie:b"))
        assertFalse(movies.includes("x:series:y"))
    }

    @Test
    fun activeCategoryFallsBackToHomeWhenHidden() {
        val state = HomeCategoryUiState(
            categories = listOf(
                HomeCategory(id = HOME_CATEGORY_HOME_ID, label = "Home"),
                HomeCategory(id = "movies", label = "Movies", visible = false),
            ),
            activeCategoryId = "movies",
        )
        assertEquals(HOME_CATEGORY_HOME_ID, state.effectiveActiveCategoryId)
        assertEquals(listOf(HOME_CATEGORY_HOME_ID), state.navigableCategories.map { it.id })
    }
}
