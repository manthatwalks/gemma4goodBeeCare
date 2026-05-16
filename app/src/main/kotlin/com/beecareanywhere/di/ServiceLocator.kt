package com.beecareanywhere.di

import android.app.Application
import android.content.Context
import com.beecareanywhere.data.Settings
import com.beecareanywhere.model.BeekeepingModel
import com.beecareanywhere.model.ModelRepository
import com.beecareanywhere.model.StubModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual DI registry. One process-wide instance of every collaborator.
 *
 * [init] must be called once from [com.beecareanywhere.BeeCareApp.onCreate]; collaborators that
 * need a Context resolve it from there.
 *
 * Phase 2 will swap [StubModel] for `LiteRtLmModel(context)` — that's the only line in this file
 * that needs to change.
 */
object ServiceLocator {

    private var appContext: Context? = null

    fun init(app: Application) {
        appContext = app.applicationContext
    }

    private fun context(): Context = checkNotNull(appContext) {
        "ServiceLocator.init() must be called from Application.onCreate()"
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
    }

    private val modelInstance: BeekeepingModel by lazy { StubModel() }
    private val repositoryInstance: ModelRepository by lazy {
        ModelRepository(context(), httpClient)
    }
    private val settingsInstance: Settings by lazy { Settings(context()) }

    fun provideModel(): BeekeepingModel = modelInstance
    fun provideModelRepository(): ModelRepository = repositoryInstance
    fun provideSettings(): Settings = settingsInstance

    private const val CONNECT_TIMEOUT_SEC = 30L
    private const val READ_TIMEOUT_SEC = 60L
}
