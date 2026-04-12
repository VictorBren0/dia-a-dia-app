package dev.victorbreno.diaadia.services

import android.content.Context
import dev.victorbreno.diaadia.data.DiaryProfile
import dev.victorbreno.diaadia.data.ReflectionEntry
import org.json.JSONArray
import org.json.JSONObject

object LocalStorageService {
    private const val PREFS_NAME = "diaadia_local_storage"
    private const val KEY_CURRENT_USER = "current_user"
    private const val KEY_UID = "uid"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_PHOTO_URL = "photoUrl"
    private const val KEY_COVER_PHOTO_BASE64 = "coverPhotoBase64"
    private const val KEY_TODAY_FOCUS = "todayFocus"
    private const val KEY_DAILY_REFLECTION = "dailyReflection"
    private const val KEY_REFLECTION_DATE = "reflectionDate"
    private const val KEY_ID = "id"
    private const val KEY_TEXT = "text"
    private const val KEY_PHOTO_BASE64 = "photoBase64"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_LOCATION_NAME = "locationName"
    private const val KEY_CREATED_AT = "createdAt"
    private const val KEY_FORMATTED_DATE = "formattedDate"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun profileKey(uid: String) = "profile_$uid"

    private fun reflectionsKey(uid: String) = "reflections_$uid"

    fun saveUserSession(context: Context, uid: String, displayName: String, email: String) {
        if (uid.isBlank()) return

        val json = JSONObject()
            .put(KEY_UID, uid)
            .put(KEY_DISPLAY_NAME, displayName)
            .put(KEY_EMAIL, email)

        preferences(context).edit()
            .putString(KEY_CURRENT_USER, json.toString())
            .apply()
    }

    fun getCurrentUserId(context: Context): String? {
        val stored = preferences(context).getString(KEY_CURRENT_USER, null) ?: return null
        return runCatching {
            JSONObject(stored).optString(KEY_UID).takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun clearUserSession(context: Context) {
        preferences(context).edit()
            .remove(KEY_CURRENT_USER)
            .apply()
    }

    fun saveProfile(context: Context, profile: DiaryProfile) {
        if (profile.uid.isBlank()) return

        preferences(context).edit()
            .putString(profileKey(profile.uid), profileToJson(profile).toString())
            .apply()

        saveUserSession(context, profile.uid, profile.displayName, profile.email)
    }

    fun getProfile(context: Context, uid: String? = getCurrentUserId(context)): DiaryProfile? {
        val validUid = uid?.takeIf { it.isNotBlank() } ?: return null
        val stored = preferences(context).getString(profileKey(validUid), null) ?: return null

        return runCatching {
            jsonToProfile(JSONObject(stored))
        }.getOrNull()
    }

    fun saveReflections(context: Context, uid: String, entries: List<ReflectionEntry>) {
        if (uid.isBlank()) return

        val orderedEntries = entries.sortedByDescending { it.createdAt }
        val jsonArray = JSONArray()
        orderedEntries.forEach { entry ->
            jsonArray.put(reflectionToJson(entry))
        }

        preferences(context).edit()
            .putString(reflectionsKey(uid), jsonArray.toString())
            .apply()
    }

    fun saveReflection(context: Context, uid: String, entry: ReflectionEntry) {
        if (uid.isBlank()) return

        val updatedEntries = (getReflections(context, uid) + entry)
            .distinctBy { reflection -> reflection.id.ifBlank { "${reflection.createdAt}-${reflection.text}" } }
            .sortedByDescending { it.createdAt }

        saveReflections(context, uid, updatedEntries)
    }

    fun getReflections(context: Context, uid: String? = getCurrentUserId(context)): List<ReflectionEntry> {
        val validUid = uid?.takeIf { it.isNotBlank() } ?: return emptyList()
        val stored = preferences(context).getString(reflectionsKey(validUid), null) ?: return emptyList()

        return runCatching {
            val jsonArray = JSONArray(stored)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(jsonToReflection(item))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun profileToJson(profile: DiaryProfile): JSONObject {
        return JSONObject()
            .put(KEY_UID, profile.uid)
            .put(KEY_DISPLAY_NAME, profile.displayName)
            .put(KEY_EMAIL, profile.email)
            .put(KEY_PHONE, profile.phone)
            .put(KEY_PHOTO_URL, profile.photoUrl)
            .put(KEY_COVER_PHOTO_BASE64, profile.coverPhotoBase64)
            .put(KEY_TODAY_FOCUS, profile.todayFocus)
            .put(KEY_DAILY_REFLECTION, profile.dailyReflection)
            .put(KEY_REFLECTION_DATE, profile.reflectionDate)
    }

    private fun jsonToProfile(json: JSONObject): DiaryProfile {
        return DiaryProfile(
            uid = json.optString(KEY_UID),
            displayName = json.optString(KEY_DISPLAY_NAME),
            email = json.optString(KEY_EMAIL),
            phone = json.optString(KEY_PHONE),
            photoUrl = json.optString(KEY_PHOTO_URL),
            coverPhotoBase64 = json.optString(KEY_COVER_PHOTO_BASE64),
            todayFocus = json.optString(KEY_TODAY_FOCUS),
            dailyReflection = json.optString(KEY_DAILY_REFLECTION),
            reflectionDate = json.optString(KEY_REFLECTION_DATE)
        )
    }

    private fun reflectionToJson(entry: ReflectionEntry): JSONObject {
        return JSONObject()
            .put(KEY_ID, entry.id)
            .put(KEY_TEXT, entry.text)
            .put(KEY_PHOTO_BASE64, entry.photoBase64)
            .put(KEY_LATITUDE, entry.latitude)
            .put(KEY_LONGITUDE, entry.longitude)
            .put(KEY_LOCATION_NAME, entry.locationName)
            .put(KEY_CREATED_AT, entry.createdAt)
            .put(KEY_FORMATTED_DATE, entry.formattedDate)
    }

    private fun jsonToReflection(json: JSONObject): ReflectionEntry {
        return ReflectionEntry(
            id = json.optString(KEY_ID),
            text = json.optString(KEY_TEXT),
            photoBase64 = json.optString(KEY_PHOTO_BASE64),
            latitude = json.optDouble(KEY_LATITUDE, 0.0),
            longitude = json.optDouble(KEY_LONGITUDE, 0.0),
            locationName = json.optString(KEY_LOCATION_NAME),
            createdAt = json.optLong(KEY_CREATED_AT),
            formattedDate = json.optString(KEY_FORMATTED_DATE)
        )
    }
}