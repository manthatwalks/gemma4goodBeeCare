package com.beecareanywhere.model

sealed interface ModelState {
    data object Idle : ModelState
    data object Loading : ModelState
    data object Ready : ModelState
    data class Error(val message: String, val cause: Throwable? = null) : ModelState
}
