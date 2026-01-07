package com.flowfinance.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")
    private val REMINDER_INTERVAL_DAYS_KEY = intPreferencesKey("reminder_interval_days")
    private val REMINDER_ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    val userData: Flow<UserData> = context.dataStore.data
        .map { preferences ->
            UserData(
                userName = preferences[USER_NAME_KEY] ?: "Usuário",
                currency = preferences[CURRENCY_KEY] ?: "BRL",
                isDarkTheme = preferences[IS_DARK_THEME_KEY] ?: false,
                reminderHour = preferences[REMINDER_HOUR_KEY] ?: 9,
                reminderMinute = preferences[REMINDER_MINUTE_KEY] ?: 0,
                reminderIntervalDays = preferences[REMINDER_INTERVAL_DAYS_KEY] ?: 7,
                isReminderEnabled = preferences[REMINDER_ENABLED_KEY] ?: true,
                language = preferences[LANGUAGE_KEY] ?: ""
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
    
    suspend fun setReminderSettings(hour: Int, minute: Int, intervalDays: Int, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_HOUR_KEY] = hour
            preferences[REMINDER_MINUTE_KEY] = minute
            preferences[REMINDER_INTERVAL_DAYS_KEY] = intervalDays
            preferences[REMINDER_ENABLED_KEY] = isEnabled
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
}

data class UserData(
    val userName: String,
    val currency: String,
    val isDarkTheme: Boolean,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderIntervalDays: Int = 7,
    val isReminderEnabled: Boolean = true,
    val language: String = ""
)
