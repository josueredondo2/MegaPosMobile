package com.devlosoft.megaposmobile.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devlosoft.megaposmobile.core.common.Constants
import com.devlosoft.megaposmobile.domain.model.ProcessPermission
import com.devlosoft.megaposmobile.domain.model.ScreenAccess
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
        private val KEY_USER_CODE = stringPreferencesKey(Constants.KEY_USER_CODE)
        private val KEY_USER_NAME = stringPreferencesKey(Constants.KEY_USER_NAME)
        private val KEY_SESSION_ID = stringPreferencesKey(Constants.KEY_SESSION_ID)
        private val KEY_STATION_ID = stringPreferencesKey(Constants.KEY_STATION_ID)
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey(Constants.KEY_IS_LOGGED_IN)
        private val KEY_SERVER_URL = stringPreferencesKey(Constants.KEY_SERVER_URL)
        private val KEY_USER_PERMISSIONS = stringPreferencesKey(Constants.KEY_USER_PERMISSIONS)
    }

    suspend fun saveSession(
        accessToken: String,
        userCode: String? = null,
        userName: String? = null,
        sessionId: String? = null
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
            preferences[KEY_IS_LOGGED_IN] = true
            userCode?.let { preferences[KEY_USER_CODE] = it }
            userName?.let { preferences[KEY_USER_NAME] = it }
            sessionId?.let { preferences[KEY_SESSION_ID] = it }
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_USER_CODE)
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_SESSION_ID)
            preferences.remove(KEY_STATION_ID)
            preferences.remove(KEY_USER_PERMISSIONS)
            preferences[KEY_IS_LOGGED_IN] = false
        }
    }

    suspend fun saveStationInfo(sessionId: String, stationId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SESSION_ID] = sessionId
            preferences[KEY_STATION_ID] = stationId
        }
    }

    fun getAccessToken(): Flow<String?> = dataStore.data.map { it[KEY_ACCESS_TOKEN] }

    fun getUserCode(): Flow<String?> = dataStore.data.map { it[KEY_USER_CODE] }

    fun getUserName(): Flow<String?> = dataStore.data.map { it[KEY_USER_NAME] }

    fun getSessionId(): Flow<String?> = dataStore.data.map { it[KEY_SESSION_ID] }

    fun getStationId(): Flow<String?> = dataStore.data.map { it[KEY_STATION_ID] }

    fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }

    suspend fun saveServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SERVER_URL] = url
        }
    }

    fun getServerUrl(): Flow<String?> = dataStore.data.map { it[KEY_SERVER_URL] }

    suspend fun saveUserPermissions(permissions: UserPermissions) {
        val json = gson.toJson(permissions)
        dataStore.edit { preferences ->
            preferences[KEY_USER_PERMISSIONS] = json
        }
    }

    fun getUserPermissions(): Flow<UserPermissions?> = dataStore.data.map { preferences ->
        val json = preferences[KEY_USER_PERMISSIONS]
        if (json != null) {
            try {
                gson.fromJson(json, UserPermissions::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun getUserPermissionsSync(): UserPermissions? {
        return getUserPermissions().first()
    }

    /**
     * Check if user has access to a specific process
     */
    suspend fun hasProcessAccess(processKey: String): Boolean {
        val permissions = getUserPermissionsSync()
        return permissions?.hasAccess(processKey) ?: false
    }

    /**
     * Check if user can use (access + show) a specific process
     */
    suspend fun canUseProcess(processKey: String): Boolean {
        val permissions = getUserPermissionsSync()
        return permissions?.canUse(processKey) ?: false
    }
}
