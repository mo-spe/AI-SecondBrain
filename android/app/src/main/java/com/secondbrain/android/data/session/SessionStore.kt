package com.secondbrain.android.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Persists only the token and active workspace; no server data or API key is stored on device. */
@Singleton
class SessionStore @Inject constructor(private val store: DataStore<Preferences>) {
    val workspaceFlow = store.data.map { it[WORKSPACE_ID] }
    val tokenFlow = store.data.map { it[TOKEN] }
    suspend fun token(): String? = store.data.first()[TOKEN]
    suspend fun workspaceId(): Long? = store.data.first()[WORKSPACE_ID]
    suspend fun save(token: String, workspaceId: Long? = null) = store.edit {
        it[TOKEN] = token
        if (workspaceId == null) it.remove(WORKSPACE_ID) else it[WORKSPACE_ID] = workspaceId
    }
    suspend fun setWorkspace(id: Long?) = store.edit {
        if (id == null) it.remove(WORKSPACE_ID) else it[WORKSPACE_ID] = id
    }
    suspend fun clear() = store.edit {
        it.remove(TOKEN)
        it.remove(WORKSPACE_ID)
    }

    private companion object {
        val TOKEN = stringPreferencesKey("auth_token")
        val WORKSPACE_ID = longPreferencesKey("workspace_id")
    }
}
