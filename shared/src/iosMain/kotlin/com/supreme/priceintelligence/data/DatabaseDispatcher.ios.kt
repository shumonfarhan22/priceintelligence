package com.supreme.priceintelligence.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

// Dispatchers.IO exists on Native but is marked internal in this project's
// coroutines version, so it can't be referenced directly here. This creates
// a small dedicated thread pool instead — same goal (keep blocking database
// work off the pool Compose uses for UI work), different API to get there.
@OptIn(DelicateCoroutinesApi::class)
internal actual val databaseDispatcher: CoroutineDispatcher =
    newFixedThreadPoolContext(4, "DatabaseIO")