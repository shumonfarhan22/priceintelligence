package com.supreme.priceintelligence

import androidx.compose.ui.window.ComposeUIViewController
import com.supreme.priceintelligence.data.getDatabaseBuilder

fun MainViewController() = ComposeUIViewController { App(databaseBuilder = getDatabaseBuilder()) }