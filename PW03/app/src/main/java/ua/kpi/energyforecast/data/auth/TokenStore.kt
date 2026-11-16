package ua.kpi.energyforecast.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Один DataStore для цього файла протягом роботи процесу.
private val Context.authDataStore by preferencesDataStore(
    name = "api_session"
)

interface TokenStore {
    suspend fun read(): String?
    suspend fun write(token: String)
    suspend fun clear()
}

/** Пароль не зберігається. Записується лише отриманий токен. */
class PreferencesTokenStore(
    private val dataStore: DataStore<Preferences>
) : TokenStore {
    constructor(context: Context) : this(context.applicationContext.authDataStore)

    override suspend fun read(): String? {
        return dataStore.data.first()[TOKEN]
    }

    override suspend fun write(token: String) {
        require(token.isNotBlank())

        dataStore.edit { preferences ->
            preferences[TOKEN] = token
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN)
        }
    }

    companion object {
        private val TOKEN = stringPreferencesKey("access_token")
    }
}
