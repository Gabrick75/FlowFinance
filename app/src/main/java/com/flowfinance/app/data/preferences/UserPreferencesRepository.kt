package com.flowfinance.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val CURRENCY_KEY = stringPreferencesKey("currency")
    private val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")

    val userData: Flow<UserData> = context.dataStore.data
        .map { preferences ->
            UserData(
                userName = preferences[USER_NAME_KEY] ?: "Usuário",
                currency = preferences[CURRENCY_KEY] ?: "BRL",
                isDarkTheme = preferences[IS_DARK_THEME_KEY] ?: false
            )
        }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    suspend fun setDarkTheme(isDarkTheme: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_THEME_KEY] = isDarkTheme
        }
    }
}

data class UserData(
    val userName: String,
    val currency: String,
    val isDarkTheme: Boolean
)
