package com.beecareanywhere.di

import com.beecareanywhere.model.BeekeepingModel
import com.beecareanywhere.model.StubModel

/**
 * Manual DI registry. One process-wide instance of every collaborator.
 *
 * Phase 2 will swap [StubModel] for `LiteRtLmModel(application)` — that's a one-line change here,
 * and the only code path that touches a concrete model class.
 */
object ServiceLocator {

    private val modelInstance: BeekeepingModel by lazy { StubModel() }

    fun provideModel(): BeekeepingModel = modelInstance
}
