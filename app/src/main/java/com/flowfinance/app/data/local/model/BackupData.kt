package com.flowfinance.app.data.local.model

import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.preferences.UserData
import com.google.gson.annotations.SerializedName

data class BackupFile(
    @SerializedName("metadata")
    val metadata: BackupMetadata,
    @SerializedName("encryptedData")
    val encryptedData: String
)

data class BackupMetadata(
    @SerializedName("appVersion")
    val appVersion: String,
    @SerializedName("dbVersion")
    val dbVersion: Int,
    @SerializedName("createdAt")
    val createdAt: String
)

data class BackupPayload(
    @SerializedName("transactions")
    val transactions: List<Transaction>,
    @SerializedName("categories")
    val categories: List<Category>,
    @SerializedName("userData")
    val userData: UserData? = null
)
