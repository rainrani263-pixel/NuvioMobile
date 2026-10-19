package com.nuvio.app.features.home

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object HomeCatalogSettingsStorage {
    private const val payloadKey = "catalog_settings_payload"
    private const val categoriesPayloadKey = "category_settings_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun loadCategoriesPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(categoriesPayloadKey))

    actual fun saveCategoriesPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(categoriesPayloadKey))
    }
}
