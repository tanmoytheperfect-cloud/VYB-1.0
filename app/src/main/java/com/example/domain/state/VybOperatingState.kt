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

/**
 * Real-time state machine for Ask VYB runtime lifecycle (Part 14):
 * IDLE -> SUBMITTING -> THINKING -> RESPONDING -> SUCCESS / ERROR.
 */
enum class AskVybState {
    IDLE,
    SUBMITTING,
    THINKING,
    RESPONDING,
    SUCCESS,
    ERROR
}
