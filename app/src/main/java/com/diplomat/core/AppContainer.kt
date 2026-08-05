package com.diplomat.core

import android.content.Context
import com.diplomat.BuildConfig
import com.diplomat.data.local.DiplomatDatabase
import com.diplomat.data.remote.DiplomatApi
import com.diplomat.data.remote.HttpClientFactory
import com.diplomat.data.remote.KtorDiplomatApi
import com.diplomat.data.repository.DiplomatMessageRepository
import com.diplomat.data.repository.MessageRepository

/**
 * Minimal manual dependency-injection container. Instantiated once by
 * [com.diplomat.DiplomatApplication] and shared by ViewModels and services.
 */
class AppContainer(context: Context) {

    private val database: DiplomatDatabase = DiplomatDatabase.get(context)

    private val api: DiplomatApi = KtorDiplomatApi(
        client = HttpClientFactory.create(),
        baseUrl = BuildConfig.ANALYSIS_BASE_URL,
    )

    val messageRepository: MessageRepository =
        DiplomatMessageRepository(database.messageDao(), api)
}
