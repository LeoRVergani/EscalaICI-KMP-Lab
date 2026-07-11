package br.com.leorvergani.escalaici.kmp.lab.platform

import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult

/**
 * Le bytes de uma planilha .xls/.xlsx real ja em memoria (ex.: baixada via
 * Ktor) e devolve o mesmo modelo comum usado pelo seletor de arquivo local
 * (`WorkbookImportLauncher`). Android usa Apache POI; Web/Wasm usa SheetJS
 * via interop com JS (mesma lib ja usada pelo seletor de arquivo local).
 */
expect suspend fun readWorkbookFromBytes(fileName: String, bytes: ByteArray): WorkbookImportResult
