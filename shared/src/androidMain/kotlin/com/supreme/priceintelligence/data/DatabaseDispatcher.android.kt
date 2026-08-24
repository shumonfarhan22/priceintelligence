package com.supreme.priceintelligence.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO