package com.supreme.priceintelligence.data

import kotlinx.coroutines.CoroutineDispatcher

// Dispatchers.IO exists on Android/JVM (the right choice for blocking
// database work there) but is not public on iOS/Kotlin Native — that's what
// broke the iOS build. Each platform supplies its own correct equivalent.
internal expect val databaseDispatcher: CoroutineDispatcher