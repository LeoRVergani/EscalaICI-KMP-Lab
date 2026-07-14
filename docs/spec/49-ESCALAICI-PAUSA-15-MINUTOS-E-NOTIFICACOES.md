# SPEC 49 — Pausa de 15 minutos e notificações

**Status:** proposta; nenhuma linha de código funcional criada por esta spec
**Escopo:** Escala ICI KMP (Android + Web/PWA)
**Fase:** FASE 14a (documentação) — implementação prevista para FASE 14f
**Depende de:** SPEC 48 (a pausa só pode ser calculada com escala/turno real sincronizado)

## 1. Diagnóstico que motiva esta spec

O controle de pausa no KMP hoje é **puramente decorativo**: `VisualToggle`
(`ui/ProfileTab.kt:262-268`) sempre renderiza `Switch(..., enabled = false)`,
valor fixo no código-fonte, sem nenhuma lógica de habilitação — o texto/janela
calculados por `pauseFor()` (`model/TemporalRules.kt:36-47`, offsets 120-285min
a partir do início do turno) só afetam o que é **exibido**, nunca o toggle em
si. Não há agendamento real, não há notificação real, não há persistência de
preferência funcional.

O app legado `EscalaSOC` tem uma implementação real de referência
(`PauseWindow.kt`, `NotificationScheduler.kt`, `PauseSchedule.kt`), com
limitações conhecidas e **não corrigidas** que esta spec decide explicitamente
não repetir: uso de `AlarmManager` inexato (nunca `setExactAndAllowWhileIdle`),
ausência de `BroadcastReceiver` para `BOOT_COMPLETED` (alarmes perdidos no
reboot até o app reabrir) e ausência de receiver para mudança de fuso horário
(`ACTION_TIMEZONE_CHANGED`).

## 2. Janela permitida por turno

Reaproveitar a fórmula já portada e testada (`TemporalRules.kt:36-47`, fiel ao
`PauseWindow.kt` do EscalaSOC): janela = início do turno + 120min até início
do turno + 285min, limitada ao fim do turno. Duração da pausa em si: 15
minutos fixos (`PauseDurationMinutes`), consistente com o nome da feature e
com o app legado.

## 3. Turno cruzando meia-noite

A janela de pausa (2h-4h45 após o início) nunca ultrapassa a duração normal de
um turno antes da virada do dia civil (confirmado pelo EscalaSOC: turno Noite
19:00 + 285min = 23:45, ainda no mesmo dia) — **a lógica de cálculo da janela
em si não precisa tratar cruzamento de meia-noite**. O que precisa: ao
persistir o horário absoluto de disparo do alarme/notificação, usar a mesma
data do turno (não a data corrente do dispositivo), e ao calcular o fim do
turno para exibição, aplicar `crossesMidnight` (já existe como conceito no
domínio, `ShiftType`) somente para decidir se a data de referência é `date`
ou `date.plusDays(1)`.

## 4. Seleção, duração, persistência, remarcação, cancelamento

- Diferente do EscalaSOC (que tem só 1 pausa configurável apesar de sugerir 6
  horários), o Escala ICI deve permitir ao usuário **escolher e persistir**
  um horário dentre as sugestões (a cada 30min dentro da janela), não apenas
  visualizar sugestões — esse é o ganho funcional real desta fase sobre o
  legado.
- Duração fixa de 15 minutos, não configurável pelo usuário (mesma regra do
  legado, evita erro operacional de marcar pausa maior que o permitido).
- Persistência: local (preferências do dispositivo, criptografado — ver
  seção de segurança) e, quando útil para auditoria/coordenação, refletida em
  `app_user_preferences` (Firestore, coleção já prevista na spec 35) — decisão
  de sincronizar via Firestore ou manter só local cabe à implementação real
  na FASE 14f; esta spec exige que, se sincronizado, a leitura nunca bloqueie
  a marcação local (offline-first).
- Remarcação: usuário pode alterar o horário escolhido enquanto a pausa não
  tiver iniciado; após iniciada, não é remarcável (só cancelável).
- Cancelamento: usuário pode cancelar a pausa agendada antes do horário de
  início; alarmes/notificações pendentes daquela pausa são desfeitos.

## 5. Mudança na escala

- Se a escala do usuário mudar (nova publicação, troca de turno) depois de
  uma pausa já agendada para um turno que deixou de existir/mudou de horário:
  a pausa agendada deve ser **invalidada automaticamente** e o usuário
  notificado de que precisa reagendar — nunca manter um alarme apontando
  para um horário que não corresponde mais ao turno real (mesmo princípio de
  "nunca mostrar dado obsoleto como se fosse real" da SPEC 48).

## 6. Reboot do dispositivo

- **Corrigir a lacuna conhecida do legado**: implementar
  `BroadcastReceiver` para `BOOT_COMPLETED` (com a permissão
  `RECEIVE_BOOT_COMPLETED` no manifest) que reagenda todos os alarmes
  pendentes (pausa + lembretes de turno) a partir do cache/Firestore local,
  sem depender de o usuário reabrir o app manualmente.
- Alternativa/complemento avaliável na implementação real (FASE 14f):
  `WorkManager` com trabalho periódico de reconciliação de agendamentos,
  reduzindo a dependência de um único `BroadcastReceiver` de boot.

## 7. Alteração de fuso e hora

- **Corrigir a lacuna conhecida do legado**: implementar `BroadcastReceiver`
  para `ACTION_TIMEZONE_CHANGED` e `ACTION_TIME_CHANGED`, recalculando todos
  os `epochMilli` de disparo pendentes a partir do fuso horário atual — nunca
  deixar um alarme já agendado disparar na hora errada após mudança de fuso.

## 8. Mecanismo Android

- **Preferir `AlarmManager.setExactAndAllowWhileIdle` (ou `setAlarmClock`
  quando apropriado)** em vez do `AlarmManager.set()` inexato usado pelo
  legado — pausa e fim de turno são eventos sensíveis a horário, disparo
  aproximado pode fazer o lembrete perder o sentido (ex.: avisar da janela de
  pausa depois que ela já fechou).
- Isso exige a permissão `SCHEDULE_EXACT_ALARM` (Android 12+) — ver seção 9.
- **Fallback com `WorkManager`**: para reconciliação periódica (detectar
  alarmes perdidos, reagendar após boot/mudança de fuso caso o
  `BroadcastReceiver` correspondente falhe por algum motivo), não como
  mecanismo primário de disparo pontual (`WorkManager` não garante
  pontualidade fina o suficiente para uma janela de 15 minutos).

## 9. Permissões

- `POST_NOTIFICATIONS` (Android 13+) — solicitada em runtime, mas **com
  verificação de "já concedida" antes de disparar novamente** (o legado
  solicita incondicionalmente a cada abertura do app,
  `MainActivity.onCreate()` — esta spec corrige isso: só solicitar se ainda
  não concedida).
- `SCHEDULE_EXACT_ALARM` (ou `USE_EXACT_ALARM`, conforme política vigente da
  Play Store para o tipo de app) — necessária pelo uso de alarmes exatos
  (seção 8); a tela de configurações deve explicar por que a permissão é
  pedida (lembrete de pausa e fim de turno no horário certo) e oferecer
  atalho para a tela de sistema se negada.
- `RECEIVE_BOOT_COMPLETED` — nova em relação ao legado (seção 6).

## 10. Notification API Web / Service Worker

- Web/PWA usa a `Notification API` do navegador + `service-worker.js` já
  existente (`docs` confirmam PWA real com service worker, FASE 9g) para
  notificações locais agendadas via `setTimeout`/`TimestampTrigger` (Web
  Periodic Background Sync/Notification Triggers, onde suportado pelo
  navegador) — sem depender de um servidor de push próprio nesta fase.
- **Limitação sem Push real**: sem um backend de Web Push (VAPID + endpoint),
  notificações Web só disparam enquanto a aba/service worker estiver ativo
  no navegador (ou dentro da janela de tolerância do sistema operacional para
  service workers) — não há garantia de entrega com o app completamente
  fechado, diferente do Android com `AlarmManager`. Esta limitação deve ser
  comunicada explicitamente ao usuário Web (ex.: "notificações funcionam
  melhor com o app aberto ou instalado como PWA"), nunca prometida como
  equivalente ao Android nesta fase.
- Push real (Web Push com VAPID) fica registrado como possível fase futura,
  fora do escopo desta spec.

## 11. Estados desabilitados e motivos explícitos

Diferente do legado (toggle sempre `enabled=false` sem explicação), todo
controle desabilitado deve ter um motivo visível:

| Estado desabilitado | Motivo mostrado ao usuário |
|---|---|
| Sem turno válido no momento (nenhum `CURRENT`/`UPCOMING`) | "Nenhum turno ativo para agendar pausa." |
| Fora da janela permitida (turno existe, mas fora de 120-285min do início) | "A pausa só pode ser marcada entre {janela}." |
| Permissão de notificação negada | "Ative as notificações para agendar lembretes de pausa." |
| Permissão de alarme exato negada (Android) | "Ative alarmes e lembretes para horários precisos." |
| Sem sincronização de escala (`NO_ACTIVE_PERIOD`/`NO_ASSIGNMENTS`, spec 48) | "Não é possível calcular a pausa sem uma escala publicada." |

Nunca um controle desabilitado sem nenhuma das mensagens acima — essa é a
correção central desta spec sobre o comportamento atual.

## 12. Critérios de aceite

1. O toggle de pausa reflete estado real (habilitado apenas quando há turno
   válido e permissões concedidas), nunca um valor fixo no código.
2. Toda vez que o controle estiver desabilitado, existe um motivo explícito
   visível ao usuário (seção 11).
3. Alarmes sobrevivem a reboot do dispositivo (`BOOT_COMPLETED` implementado).
4. Alarmes se recalculam corretamente após mudança de fuso horário.
5. Mudança na escala publicada invalida automaticamente pausas agendadas
   para turnos que não existem mais.
6. Web comunica explicitamente a limitação de notificações sem Push real.
7. Nenhuma notificação de pausa/turno usa a mensagem genérica de erro da
   SPEC 48 — falhas de agendamento têm mensagem própria.
