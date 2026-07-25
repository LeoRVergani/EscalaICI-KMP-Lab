# Spec 72 — Design system Compose do Escala ICI (contrato, sem mudança de código)

**Status:** documentação apenas — nenhum arquivo de código alterado nesta frente.
**Escopo:** `EscalaICI-KMP-Lab` (Android + Web/Wasm), `commonMain`.
**Contexto:** espelha, no lado Compose, o mesmo objetivo do design system do dashboard
(`escala-dashboard/docs/spec/13-DASHBOARD-DESIGN-SYSTEM-COMPONENTES-PADRAO.md`, Frente B1) —
formalizar o que já deve ser reaproveitado, para que qualquer tela nova componha a partir dos
tokens/componentes existentes em vez de inventar cor, forma ou padrão de card/banner próprio.

## 1. O que já existe (auditado nesta frente, não criado)

| Peça | Arquivo | Papel |
|---|---|---|
| `LabColors` | `ui/theme/LabColors.kt` | **Fonte única de cor.** Porte literal da paleta do app Android real (`SocDarkColorScheme`). Todo `background`/`surface`/`primary`/`secondary`/`tertiary`/`error`/`orange`/`yellow`/`red`/`purple`/`cyan`/`gray` vive aqui. |
| `LabTheme` | `ui/theme/LabTheme.kt` | Mapeia `LabColors` para o `ColorScheme` do Material 3 (`darkColorScheme`) — app é dark-only, sem tema claro. |
| `LabShapes` | `ui/theme/LabShapes.kt` | Fonte única de raio de canto: `cardLarge` (20dp), `cardMedium` (16dp), `cardSmall` (12dp), `button`/`chip` (12dp), `navPill` (14dp). |
| `LabTypography` | `ui/theme/LabTypography.kt` | `Typography` do Material 3 (porte de `SocTypography`) — pesos/tamanhos de headline/title/body/label. |
| `LabCard` | `ui/components/LabCard.kt` | Componente de card/container reutilizável (`HeroCard` com gradiente + variantes de card com borda/ícone) — é o que `ViewAsBanner` (FASE 14J.1) e os cards de "Hoje"/Alertas já usam. |
| `LabAlerts` | `model/LabAlerts.kt` | **Não é um componente de UI** — é o modelo de domínio (`LabAlert` + `GenerateLabAlerts`) que calcula alertas de folga/6x1/inconsistência/erro de importação a partir de `ScheduleSummary`. A apresentação visual desses alertas é `PremiumAlertCard` (privado, em `ui/AlertsTab.kt`). |

## 2. Achado a corrigir num checkpoint futuro (fora de escopo agora)

`PremiumAlertCard` (`ui/AlertsTab.kt`) declara suas próprias cores de severidade
(`AlertCriticalColor = Color(0xFFEF4444)`, `AlertWarningColor = Color(0xFFF59E0B)`,
`AlertInfoColor = Color(0xFF3B82F6)`) em vez de usar `LabColors.error`/`LabColors.orange`/
`LabColors.primary` — os valores hexadecimais coincidem por acaso, não por referência ao token.
Exatamente o tipo de "cor hardcoded quando já existe token equivalente" que este contrato proíbe
para código novo (seção 4). Corrigir isso é um item pequeno e isolado para uma fase futura, não
implementado aqui.

## 3. Padrão de modal

**Não existe hoje.** Nenhuma tela usa `AlertDialog`/`Dialog`/`ModalBottomSheet` do Compose —
navegação para telas secundárias (ex.: seletor "Visualizar como colaborador" da FASE 14J.1) usa
`StackedScreen` como estado local empilhável, substituindo o conteúdo da tela em vez de flutuar
por cima. Se uma necessidade real de modal/confirmação flutuante surgir, ela deve nascer como um
componente único e reutilizável (equivalente ao `AppDialog`/`AppConfirm` do dashboard) em
`ui/components/`, não como um `AlertDialog` ad-hoc por tela.

## 4. Padrão de banner

`ViewAsBanner` (`ui/AdminViewAsMemberScreen.kt`, FASE 14J.1) é o exemplo canônico: `LabCard` com
`borderColor`/`iconTint` vindos de `LabColors` (nunca `Color(0x...)` literal), título em
`MaterialTheme.typography.titleSmall`, corpo em `bodySmall` com `LabColors.onSurfaceMuted`. Todo
banner persistente novo deve seguir essa mesma composição: `LabCard` + cor nomeada de
`LabColors`, nunca uma superfície/cor construída do zero.

## 5. Safe area, foco e navegação

- **Safe area**: já tratado por `WindowInsets`/padding padrão do Compose Material 3 nas telas
  existentes (Hoje/Escala/Alertas/Perfil) — nenhuma convenção nova a criar, só manter o padrão já
  usado (nenhuma tela deve aplicar padding de status/navigation bar manualmente quando o
  Scaffold/insets padrão já resolve).
- **Foco/navegação por teclado**: relevante principalmente no alvo Web/Wasm (teclado físico é o
  caso comum; Android depende mais de touch/TalkBack). Componentes novos que introduzirem
  interação (ex.: um futuro modal) devem seguir o mesmo padrão de acessibilidade já usado pelos
  botões/ícones existentes (`contentDescription` em todo `Icon` interativo, já praticado em
  `ViewAsBanner`/`AlertsTab`).

## 6. Android e Web/Wasm

Nenhuma diferença de design system entre os dois alvos — `LabColors`/`LabTheme`/`LabShapes`/
`LabTypography`/`LabCard` vivem em `commonMain`, compartilhados via expect/actual apenas onde a
plataforma exige (ex.: `ResolutionDiagnostics`, não em UI/tema). Um componente visual não deve
ter versão Android e versão Web divergente — se uma diferença de plataforma for genuinamente
necessária, ela é tratada dentro do componente compartilhado (parâmetro/condicional), não
duplicando o componente por alvo.

## 7. Sobre um futuro `LabButton`

Hoje botões usam `Button`/`OutlinedButton`/`TextButton` do Compose Material diretamente,
estilizados via `MaterialTheme`/`LabColors` (ex.: `TextButton` em `ViewAsBanner`). Isso é
aceitável e não será trocado por um wrapper `LabButton` agora — diferente do dashboard React
(que não tinha nenhuma camada de tema automática e por isso precisava de `AppButton` para não
repetir `className="btn ..."` em cada tela), o Compose já centraliza aparência de botão via
`MaterialTheme`/`LabTheme` sem precisar de um componente próprio. Um `LabButton` só deve ser
criado se/quando variantes de botão começarem a divergir de verdade na prática (cor, forma ou
comportamento inconsistente entre telas) — não preventivamente.

## 8. Regra geral (proibição)

Nenhuma tela nova deve declarar `Color(0x......)`, `RoundedCornerShape(...)`, ou tamanho de fonte
literal quando `LabColors`/`LabShapes`/`LabTypography` já cobre o caso. Card/container novo
reaproveita `LabCard`; alerta/banner novo segue o padrão da seção 4. Divergência real de
plataforma (Android vs. Web/Wasm) é resolvida dentro do componente compartilhado, nunca com uma
segunda implementação visual paralela.

## Critérios de aceite

1. Nenhum arquivo de código foi alterado nesta frente — só este documento.
2. `LabColors`/`LabTheme`/`LabShapes`/`LabTypography`/`LabCard`/`LabAlerts` documentados com seus
   arquivos e papéis reais (verificados por leitura direta do código, não por suposição).
3. Achado do `PremiumAlertCard` (cores hardcoded coincidentes) registrado como dívida futura,
   não corrigido aqui.
4. Decisão sobre `LabButton` registrada explicitamente (não criar agora).
