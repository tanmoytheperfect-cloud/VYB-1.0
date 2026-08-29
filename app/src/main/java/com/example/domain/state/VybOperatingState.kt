package com.example.domain.state

/**
 * Operating State Machine for VYB Operating System Pipeline.
 */
enum class VybOperatingState {
    IDLE,
    UNDERSTANDING,
    NEEDS_INFORMATION,
    PLANNING,
    WAITING_FOR_AUTHORIZATION,
    EXECUTING,
    SUCCESS,
    FAILED
}
