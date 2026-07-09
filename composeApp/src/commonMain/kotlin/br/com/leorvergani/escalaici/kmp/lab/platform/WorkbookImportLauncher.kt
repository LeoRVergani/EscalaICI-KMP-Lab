package br.com.leorvergani.escalaici.kmp.lab.platform

import androidx.compose.runtime.Composable
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult

interface WorkbookImportLauncher {
    fun launch()
}

@Composable
expect fun rememberWorkbookImportLauncher(
    onResult: (WorkbookImportResult) -> Unit
): WorkbookImportLauncher
