package com.supreme.priceintelligence.dashboard

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object PriceRefreshCoordinator {
    private val executionMutex = Mutex()
    private val priorityMutex = Mutex()
    private var userPriorityRequests = 0

    suspend fun <T> runUserPriority(
        block: suspend () -> T
    ): T {
        priorityMutex.withLock {
            userPriorityRequests += 1
        }

        return try {
            executionMutex.withLock {
                block()
            }
        } finally {
            priorityMutex.withLock {
                userPriorityRequests =
                    (userPriorityRequests - 1)
                        .coerceAtLeast(0)
            }
        }
    }

    suspend fun <T> runAutomatic(
        block: suspend () -> T
    ): T? {
        if (hasWaitingUserRequest()) {
            return null
        }

        return executionMutex.withLock {
            if (hasWaitingUserRequest()) {
                null
            } else {
                block()
            }
        }
    }

    private suspend fun hasWaitingUserRequest(): Boolean =
        priorityMutex.withLock {
            userPriorityRequests > 0
        }
}