package br.com.leorvergani.escalaici.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wrapper de tela padrao do laboratorio, equivalente ao `ScreenContainer`
 * do app real: `LazyColumn` com o mesmo `contentPadding`. O app real nao
 * tem um titulo generico de pagina alem do `PremiumHeader` (adicionado
 * como primeiro item pelo proprio conteudo da aba) — por isso `PageList`
 * nao recebe mais `title`/`subtitle`.
 */
@Composable
internal fun PageList(
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}
