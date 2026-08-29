package com.example.domain.actions

/**
 * Standard Result returned from real Android action executions.
 */
data class ActionResult(
    val success: Boolean,
    val action: String,
    val message: String,
    val error: String? = null,
    val data: Any? = null,
    val isAppLaunchOnly: Boolean = false
)
