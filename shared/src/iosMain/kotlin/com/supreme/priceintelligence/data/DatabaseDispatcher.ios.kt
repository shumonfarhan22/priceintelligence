package com.supreme.priceintelligence.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// The iPhone worker pool keeps database work away from the main UI thread
// without reserving four permanent threads for this app.
internal actual val databaseDispatcher: CoroutineDispatcher =
    Dispatchers.Default