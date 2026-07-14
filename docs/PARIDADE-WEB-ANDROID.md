# Paridade Web × Android — KMP-MVP-0

Esta matriz registra a base funcional compartilhada antes das integrações de Firebase, MSAL, OneDrive e trocas reais. Capacidades exclusivas são fornecidas pelos entrypoints por `PlatformCapabilities`; a UI comum não detecta user-agent, URL ou textos da plataforma.

| Funcionalidade | commonMain | Web | Android | Estado | Pendência |
|---|---|---|---|---|---|
| Arquivo local XLS/XLSX | Modelos, parser e tela de importação | Seletor do navegador e leitura via SheetJS | Seletor nativo e leitura local | Funcional | Validar layouts adicionais em fases próprias |
| Cache | Contrato e estado da escala | Cache local do navegador | Cache local do aplicativo | Funcional local | Sincronização remota futura |
| Firebase | Modelos documentais apenas | Não integrado | Não integrado | Pendente | Fase Firebase própria |
| OneDrive | Sem implementação | Não integrado | Não integrado | Pendente | MSAL/Graph em fase própria |
| Dropbox | Repositório experimental compartilhado | Fluxo limitado pelo navegador; controle indisponível fica desabilitado | Usado pela atualização de APK e código experimental | Parcial | Não ampliar antes de decisão arquitetural |
| Atualização do aplicativo | Contrato e resultados comuns | Oculta; PWA/site atualiza pelo host e service worker | Verificação, download e instalação de APK | Exclusiva Android | Manter validação de release |
| Notificações | Contrato de serviço e UI condicionada | Notifications API, permissão e teste | Card Web oculto | Exclusiva Web nesta fase | Notificações Android nativas futuras |
| Pausa | Regras, resumo e janela permitida | Compartilhada; aviso Web quando suportado | Compartilhada, sem agendamento nativo | Parcial | Push/agendamento fora do KMP-MVP-0 |
| Plantão | Modelos, regras e telas | Funcional com dados locais/importados | Funcional com dados locais/importados | Funcional local | Fonte universal futura |
| Alertas | Geração e filtros comuns | Funcional | Funcional | Funcional local | Dados remotos futuros |
| Trocas | Tela e estado demonstrativo existentes | Sem backend real | Sem backend real | Demonstrativo | Implementação real proibida nesta fase |
| Login | Gate e sessão de teste comuns | Login de teste; MSAL indisponível explicado | Login de teste; MSAL indisponível explicado | Parcial | MSAL/Entra em fase própria |
| PWA | Não aplicável ao núcleo comum | Manifest, service worker, cache offline e viewport dinâmico | Não aplicável | Exclusiva Web | Validar instalação e atualização em dispositivo real |

## Controles auditados

- Hoje, Escala, Importar, Alertas, Perfil e Plantão possuem navegação/callback real.
- Ações ainda indisponíveis, como solicitar troca, remover escala local e Dropbox na importação, permanecem desabilitadas com explicação.
- Atualização de APK é exibida somente quando `supportsAppUpdate` é verdadeiro.
- O card e os textos de notificações Web são exibidos somente quando `supportsWebNotifications` é verdadeiro; Android não apresenta `UNSUPPORTED` como erro.
- Login de teste e logout continuam funcionais; o botão de login corporativo conserva a mensagem de indisponibilidade e não simula MSAL.

## Riscos e próxima fase

A paridade atual é local e demonstrativa: não há identidade corporativa, fonte universal remota, sincronização ou backend de trocas. A próxima fase deve escolher uma única integração oficial prevista no roadmap e tratá-la isoladamente; Firebase/Firestore é a base de dados recomendada antes de MSAL/Graph e trocas reais.
