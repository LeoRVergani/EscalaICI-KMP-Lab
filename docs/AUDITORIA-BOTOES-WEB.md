# Auditoria de botões e notificações Web

Auditoria da interface de produção Web/Wasm. Estados: `FUNCIONAL`,
`DESABILITADO_COM_MOTIVO`, `OCULTO`, `EXCLUSIVO_ANDROID` e
`PENDENTE_FASE_FUTURA`.

| Tela | Controle | Função/handler | Web | Android | Decisão e validação |
|---|---|---|---|---|---|
| Global | Hoje, Escala, Importar, Alertas, Perfil | troca de aba | FUNCIONAL | FUNCIONAL | estado selecionado validado |
| Cabeçalho | Sino | central ainda inexistente | OCULTO | PENDENTE_FASE_FUTURA | removido quando não há callback real |
| Cabeçalho | Plantão | abre `PlantaoScreen` | FUNCIONAL | FUNCIONAL | navegação validada |
| Hoje | Importar escala | abre Importar | FUNCIONAL | FUNCIONAL | feedback por navegação |
| Hoje | Clima | sem fonte real | OCULTO | OCULTO | removido da produção |
| Escala | Mês anterior/próximo | altera mês dentro do período | FUNCIONAL | FUNCIONAL | limites desabilitam os botões |
| Escala | Ir para hoje | seleciona hoje quando contido | FUNCIONAL | FUNCIONAL | desabilitado com explicação fora do período |
| Escala | Ver legenda | expande/recolhe legenda | FUNCIONAL | FUNCIONAL | estado visual validado |
| Escala | Selecionar dia | atualiza detalhe | FUNCIONAL | FUNCIONAL | detalhe reage à seleção |
| Escala | Solicitar troca | depende de fluxo futuro | DESABILITADO_COM_MOTIVO | PENDENTE_FASE_FUTURA | não cria solicitação fictícia |
| Importar | Arquivo local/Escolher arquivo | abre seletor nativo | FUNCIONAL | FUNCIONAL | preview antes da ativação |
| Importar | Confirmar ano/cancelar | resolve ambiguidade | FUNCIONAL | FUNCIONAL | não apaga cache anterior |
| Importar | Usar dados importados | aplica preview | FUNCIONAL | FUNCIONAL | oculto após estado ativo |
| Importar | Remover escala | remove cache da escala | FUNCIONAL | FUNCIONAL | preserva plantão |
| Importar | Dropbox | fonte remota fora do escopo | DESABILITADO_COM_MOTIVO | PENDENTE_FASE_FUTURA | “próxima etapa” |
| Alertas | Todos/Crítico/Atenção/Info | filtra lista e contagem | FUNCIONAL | FUNCIONAL | estado selecionado e vazio tratados |
| Perfil | Sair | encerra sessão de teste | FUNCIONAL | FUNCIONAL | retorna ao login |
| Perfil | Ver minhas solicitações | abre tela local de trocas | FUNCIONAL | FUNCIONAL | navegação validada |
| Perfil | Ativar notificações Web | solicita permissão por clique | FUNCIONAL | OCULTO | nunca solicitado automaticamente |
| Perfil | Testar notificação Web | `showNotification` via SW | FUNCIONAL | OCULTO | somente com permissão concedida |
| Perfil | Toggles ilustrativos | sem persistência funcional | DESABILITADO_COM_MOTIVO | PENDENTE_FASE_FUTURA | switch sem callback |
| Perfil | Atualizar aplicativo | APK/Dropbox | OCULTO | EXCLUSIVO_ANDROID | capacidade de plataforma |
| Plantão | Voltar | fecha tela empilhada | FUNCIONAL | FUNCIONAL | navegação validada |
| Plantão | Importar relatório | seletor e parser local | FUNCIONAL | FUNCIONAL | erro/sucesso visíveis |
| Plantão | Mês anterior/próximo | altera mês | FUNCIONAL | FUNCIONAL | feedback imediato |
| Plantão | Selecionar dia | atualiza detalhe | FUNCIONAL | FUNCIONAL | ausência/presença informadas |

Total auditado: **27 grupos de controles**, abrangendo mais de 40 instâncias
renderizadas (incluindo cinco itens da navegação e filtros repetidos).

## Notificações Web

A Web verifica `Notification`, `navigator.serviceWorker` e contexto seguro.
A permissão é vinculada à origem: autorizar `localhost` não autoriza o futuro
domínio `pages.dev`. A exibição usa o service worker e uma tag estável. O clique
foca uma janela existente ou abre o aplicativo.

Nesta entrega existe ativação e notificação de teste. Como ainda não há uma
pausa programada escolhida e válida, nenhum horário é inventado e nenhum
lembrete exato é agendado. Notificações funcionam enquanto o site/PWA e o
processo do navegador estiverem ativos.

Pendência **WEB-PUSH-1**: Push API, VAPID e backend para notificações confiáveis
com o aplicativo completamente fechado. Não faz parte desta fase.

## Verificação estática

Foram pesquisados callbacks vazios, `TODO`, `NotImplementedError`, ações que
somente imprimem logs e controles demonstrativos. Controles indisponíveis foram
ocultados ou receberam `enabled = false` e motivo próximo.
