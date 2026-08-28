package com.supreme.priceintelligence.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogException
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitPickerException
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class InventoryBackupActions(
    val exportBackup: () -> Unit,
    val importBackup: () -> Unit
)

@Composable
internal fun rememberInventoryBackupActions(
    viewModel: InventoryViewModel,
    onImportCompleted: () -> Unit = {}
): InventoryBackupActions {
    val coroutineScope = rememberCoroutineScope()

    val currentOnImportCompleted by
        rememberUpdatedState(onImportCompleted)

    var pendingBackupJson by remember {
        mutableStateOf<String?>(null)
    }

    val backupSaver = rememberFileSaverLauncher(
        dialogSettings =
            FileKitDialogSettings.createDefault(),
        onError = {
                failure: FileKitDialogException ->

            pendingBackupJson = null

            viewModel.reportBackupError(
                failure.message
                    ?: "The backup file could not be saved"
            )
        },
        onResult = { file: PlatformFile? ->
            val backupJson = pendingBackupJson
            pendingBackupJson = null

            if (
                file != null &&
                backupJson != null
            ) {
                coroutineScope.launch {
                    try {
                        file.writeString(backupJson)
                        viewModel.reportBackupSaved()
                    } catch (
                        error: CancellationException
                    ) {
                        throw error
                    } catch (_: Exception) {
                        viewModel.reportBackupError(
                            "The backup file could not be written"
                        )
                    }
                }
            }
        }
    )

    val backupPicker = rememberFilePickerLauncher(
        type =
            FileKitType.File(
                extensions = listOf("json")
            ),
        onError = {
                failure: FileKitPickerException ->

            viewModel.reportBackupError(
                failure.message
                    ?: "The backup file could not be opened"
            )
        },
        onResult = { file: PlatformFile? ->
            if (file != null) {
                coroutineScope.launch {
                    try {
                        viewModel.restoreBackupJson(
                            file.readString()
                        )

                        currentOnImportCompleted()
                    } catch (
                        error: CancellationException
                    ) {
                        throw error
                    } catch (
                        error: IllegalArgumentException
                    ) {
                        viewModel.reportBackupError(
                            error.message
                                ?: "This is not a valid Price Intelligence backup"
                        )
                    } catch (_: Exception) {
                        viewModel.reportBackupError(
                            "The backup file could not be restored"
                        )
                    }
                }
            }
        }
    )

    return InventoryBackupActions(
        exportBackup = {
            coroutineScope.launch {
                try {
                    pendingBackupJson =
                        viewModel.createBackupJson()

                    backupSaver.launch(
                        suggestedName =
                            "price-intelligence-backup",
                        defaultExtension = "json",
                        allowedExtensions =
                            setOf("json")
                    )
                } catch (
                    error: CancellationException
                ) {
                    throw error
                } catch (_: Exception) {
                    pendingBackupJson = null

                    viewModel.reportBackupError(
                        "The backup could not be prepared"
                    )
                }
            }
        },
        importBackup = {
            backupPicker.launch()
        }
    )
}