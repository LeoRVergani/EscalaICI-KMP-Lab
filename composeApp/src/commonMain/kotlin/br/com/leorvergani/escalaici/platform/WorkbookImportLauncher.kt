package br.com.leorvergani.escalaici.platform

import androidx.compose.runtime.Composable
import br.com.leorvergani.escalaici.model.WorkbookImportResult

interface WorkbookImportLauncher {
    fun launch()
}

@Composable
expect fun rememberWorkbookImportLauncher(
    onResult: (WorkbookImportResult) -> Unit
): WorkbookImportLauncher
