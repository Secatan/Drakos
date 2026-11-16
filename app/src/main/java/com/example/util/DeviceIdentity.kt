package com.example.util

import android.content.Context
import android.os.Build
import java.util.UUID

object DeviceIdentity {
    private const val PREFS_NAME = "drakos_device_identity"
    private const val KEY_DEVICE_ID = "terminal_device_id"

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val shortCode = UUID.randomUUID().toString()
            .replace("-", "")
            .take(8)
            .uppercase()
        val generated = "DRK-$shortCode"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun getDeviceModelLabel(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val combined = listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
        return combined.ifBlank { "Terminal Android Drakos" }
    }
}
