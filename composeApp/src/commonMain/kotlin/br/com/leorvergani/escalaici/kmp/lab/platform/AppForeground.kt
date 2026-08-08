package br.com.leorvergani.escalaici.kmp.lab.platform

import androidx.compose.runtime.Composable

/**
 * Observa o app voltando ao foreground (Android: `ProcessLifecycleOwner`;
 * Wasm: `document.visibilitychange`) e chama `onForeground` - usado por
 * Trocas (FASE 16, seção 21) para atualizar notificações/pendências sem
 * polling. Cada `actual` usa `DisposableEffect` para registrar e cancelar o
 * observer/listener de forma segura - nunca deixa um observer duplicado
 * entre recomposições (ajuste obrigatório da FASE 16, aprovação do usuário).
 */
@Composable
expect fun ObserveAppForeground(onForeground: () -> Unit)
