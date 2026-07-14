# SPEC 51 — Endurecimento transitório das regras do Firestore

**Status:** proposta testada no Emulator; não implantada em produção
**Escopo:** `firebase/firestore.rules` + testes do Emulator (`firebase/test/firestore.rules.test.mjs`)
**Fase:** FASE 14a.1 (correção emergencial, entre FASE 14a e FASE 14d)

> **AVISO: A regra transitória não substitui Firebase Authentication no KMP.**
> Ela apenas elimina a escrita anônima e mantém, de forma explicitamente
> documentada, a mesma leitura anônima mínima que o app já usa hoje. A
> solução definitiva (MSAL real + `user_links` + Firebase Auth no KMP) é a
> FASE 14d, já desenhada nas specs 46-48.

## 1. Risco identificado

A auditoria da FASE 14a confirmou que:

- as regras hoje publicadas no projeto `escalaici` (banco `(default)`)
  liberam **leitura e escrita totalmente livres, sem exigir autenticação**,
  até uma data-limite;
- o KMP não tem Firebase Authentication e lê o Firestore de forma
  inteiramente anônima;
- o Dashboard tem Firebase Auth (Microsoft/Entra via `OAuthProvider`
  nativo), mas as regras publicadas não usam essa identidade para nada;
- depois da data-limite, a regra temporária muda para negar tudo por
  padrão, o que pode interromper o Dashboard sem aviso se nada for
  republicado antes.

**Risco mais grave, não aceitável manter**: qualquer pessoa com a URL do
projeto pode hoje **escrever, alterar ou apagar** qualquer documento de
qualquer coleção, sem login algum. Esta spec resolve isso primeiro.

## 2. Regra publicada encontrada (evidência)

Snapshot confirmado pelo Console, preservado em
`firebase/firestore.production.snapshot.rules` (não alterado por esta fase):

```
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.time < timestamp.date(2026, 8, 4);
    }
  }
}
```

## 3. Data de expiração da regra temporária

**4 de agosto de 2026.** A auditoria desta fase ocorreu em 14 de julho de
2026 — restam aproximadamente 3 semanas até a expiração no momento em que
esta spec foi escrita.

## 4. Matriz de acesso

Legenda de colunas: **KMP anônimo** = acesso que o app KMP tem hoje sem
nenhuma sessão; **Dashboard autenticado** = qualquer usuário com login
Microsoft/Firebase válido, sem papel especial; **Administrador** =
`system_admins/{email}` com `active != false`; **Coordenador de equipe** =
e-mail presente em `teams/{teamId}.adminEmails`; **Usuário comum** = membro
sem nenhum dos papéis acima (o modelo atual do Dashboard não concede
escrita a este papel em nenhuma coleção — só leitura, quando autenticado).

| Coleção | Operação | KMP anônimo | Dashboard autenticado (sem papel) | Administrador | Coordenador de equipe | Usuário comum | Dado público/sensível | Regra transitória (esta spec) | Regra definitiva prevista (FASE 14d) |
|---|---|---|---|---|---|---|---|---|---|
| `teams` | read | ✅ (hoje, mantido) | ✅ | ✅ | ✅ | ✅ | Sensível parcial (nomes de equipe) | `signedIn() \|\| true` | `signedIn()` apenas (sem exceção anônima) |
| `teams` | create/update/delete | ❌ | ❌ | ✅ | update parcial (própria equipe, sem trocar `teamId`/`adminEmails`) | ❌ | — | igual à definitiva | igual |
| `members` | read | ✅ (hoje, mantido) | ✅ | ✅ | ✅ | ✅ | **Sensível** (pode conter `email`) | `signedIn() \|\| true` | `signedIn()` apenas |
| `members` | create/update/delete | ❌ | ❌ | ✅ | ✅ (própria equipe) | ❌ | — | igual | igual |
| `schedule_periods` | read | ✅ (hoje, mantido) | ✅ | ✅ | ✅ | ✅ | Operacional | `signedIn() \|\| true` | `signedIn()` apenas |
| `schedule_periods` | create/update/delete | ❌ | ❌ | ✅ | ✅ (própria equipe) | ❌ | — | igual | igual |
| `schedule_assignments` | read | ✅ (hoje, mantido) | ✅ | ✅ | ✅ | ✅ | Operacional (associa `memberId`) | `signedIn() \|\| true` | `signedIn()` apenas |
| `schedule_assignments` | create/update/delete | ❌ | ❌ | ✅ | ✅ (própria equipe) | ❌ | — | igual | igual |
| `oncall_periods` | read | ✅ (hoje, mantido) | ✅ | ✅ | ✅ | ✅ | Operacional | `signedIn() \|\| true` | `signedIn()` apenas |
| `oncall_periods` | create/update/delete | ❌ | ❌ | ✅ | ✅ (própria equipe) | ❌ | — | igual | igual |
| `oncall_assignments` | read | ✅ (hoje, mantido) | ✅ | ✅ | ✅ | ✅ | Operacional | `signedIn() \|\| true` | `signedIn()` apenas |
| `oncall_assignments` | create/update/delete | ❌ | ❌ | ✅ | ✅ (própria equipe) | ❌ | — | igual | igual |
| `shift_swap_requests` | read/write | ❌ (não usado pelo KMP hoje) | read ✅ | ✅ | write ✅ (própria equipe) | read ✅ | Operacional | `signedIn()` apenas | igual |
| `system_admins` | read | ❌ | ✅ | ✅ | ✅ | ✅ | **Nunca público** | `signedIn()` apenas | igual |
| `system_admins` | write | ❌ | ❌ | ✅ | ❌ | ❌ | — | igual | igual |
| `source_files` | read/write | ❌ (não usado pelo KMP hoje) | read ✅ | ✅ | write ✅ (própria equipe) | read ✅ | **Nunca público** | `signedIn()` apenas | igual |
| `import_jobs` | read/write | ❌ (não usado pelo KMP hoje) | read ✅ | ✅ | write ✅ (própria equipe) | read ✅ | **Nunca público** | `signedIn()` apenas | igual |
| `user_links` | read | ❌ | próprio vínculo ✅ | ✅ | ❌ | próprio vínculo ✅ | **Nunca público** (contém `tenantId`/`objectId`) | `signedIn() && (isSystemAdmin() \|\| own uid)` | igual, já preparado nesta fase para a FASE 14c |
| `user_links` | write | ❌ | ❌ | ✅ | ❌ | ❌ | — | igual | igual |
| `app_user_preferences` | read/write | ❌ | read ✅ | ✅ | — | read ✅ | Baixo risco | `signedIn()` apenas (leitura); `isSystemAdmin()` (escrita) | avaliar autoatendimento do próprio usuário na FASE 14d |
| demais coleções universais (`organizations`, `org_units`, `member_team_memberships`, `roles`, `schedule_profiles`, `activity_codes`, `dashboard_permissions`) | read/write | ❌ | read ✅ | ✅ | ❌ | read ✅ | Cadastro administrativo | `signedIn()` (leitura); `isSystemAdmin()` (escrita) | igual |
| `work_patterns`, `shift_definitions` | read/write | ❌ (não usado pelo KMP hoje) | read ✅ | ✅ | write ✅ (própria equipe) | read ✅ | Operacional | `signedIn()` apenas | igual |
| `escalas/{anoMes}` (+ `dias`) | read/write | ❌ | read ✅ | ✅ | — | read ✅ | Histórico do protótipo | `signedIn()` (leitura); `isSystemAdmin()` (escrita) — herdado, não alterado nesta fase | reavaliar necessidade na FASE 14d |
| coleção desconhecida/inexistente | read/write | ❌ | ❌ | ❌ | ❌ | ❌ | — | negado por padrão (`match /{document=**} { allow read, write: if false; }`) | igual |

## 5. Leituras anônimas temporárias mantidas

Exatamente 6 coleções, e nenhuma outra: `teams`, `members`,
`schedule_periods`, `schedule_assignments`, `oncall_periods`,
`oncall_assignments`. Este é o conjunto exato que
`FirestoreRestGateway.kt` lê hoje sem nenhuma sessão — confirmado por
leitura direta do código (`loadTeam`, `loadActiveSchedulePeriod`,
`loadScheduleAssignments`, `loadMembers`, `loadActiveOnCallPeriod`,
`loadOnCallAssignments`), não presumido.

## 6. Riscos residuais (não resolvidos nesta fase, documentados explicitamente)

1. **Limite técnico do Firestore para listagens sem filtro.** O cliente KMP
   lê essas 6 coleções via `GET .../documents/{collection}?pageSize=1000`
   — uma listagem sem `where`. Testado neste Emulator: uma regra que só
   libere leitura de documentos `active == true` faz o Firestore
   **rejeitar a listagem inteira** com `permission-denied` para
   requisições anônimas, em vez de apenas ocultar os documentos que não
   passam, porque a consulta do cliente não tem um `where` correspondente
   que prove a condição para o conjunto todo. Como esta fase não pode
   alterar Kotlin, a única forma de manter a leitura anônima mínima
   funcionando é liberá-la de forma incondicional para estas 6 coleções —
   idêntico ao acesso que já existe hoje, sem ampliá-lo. **Isso significa
   que documentos inativos/rascunho (equipes desativadas, membros
   inativos, períodos não publicados) continuam visíveis a qualquer
   leitor anônimo.** Esta troca foi apresentada explicitamente ao usuário
   durante esta sessão, que confirmou manter a exposição de leitura no
   nível atual em troca de eliminar 100% da escrita anônima. Resolução
   completa depende da FASE 14e (consulta do KMP ganhar `where` real) ou
   da FASE 14d (KMP com Firebase Auth, sem depender mais de leitura
   anônima).
2. **`members` pode conter `email`.** Firestore Rules não fazem redação por
   campo — um documento é liberado ou negado por inteiro. Se um documento
   de `members` tiver o campo opcional `email` (confirmado como campo
   existente no schema, ainda que não usado pela UI do KMP hoje —
   `FirebaseMemberDto` não o inclui), ele é exposto por completo a
   qualquer leitor anônimo que acesse a API REST diretamente (não só pelo
   app KMP). Este risco já existe hoje sob o modo de teste totalmente
   aberto; esta regra transitória não o amplia, mas também não o fecha.
3. **`isTeamAdmin()` verifica somente `teams.adminEmails`.** O modelo atual
   do Dashboard também tem o conceito de `responsibleEmail`/
   `responsibleTitle` (responsável pela equipe), que pode ou não ser
   equivalente a "coordenador" para fins de escrita. A regra transitória
   preserva a lógica já existente no rascunho anterior
   (`adminEmails` apenas) sem expandir o escopo de quem pode escrever —
   reconciliar isso com o modelo completo do Dashboard fica para a FASE
   14d, para não introduzir uma nova via de autorização sem validação
   completa.
4. **`schedule_assignments` não tem campo `active` próprio.** Mesmo que a
   listagem funcionasse com filtro, não haveria como distinguir um
   assignment de rascunho de um publicado neste nível — a distinção só
   existe no `schedule_periods` pai.

## 7. Coleções completamente bloqueadas ao público (sem exceção)

`system_admins`, `user_links`, `source_files`, `import_jobs`,
`shift_swap_requests`, `work_patterns`, `shift_definitions`, todas as
coleções universais (`organizations`, `org_units`,
`member_team_memberships`, `roles`, `schedule_profiles`, `activity_codes`,
`dashboard_permissions`, `app_user_preferences`), `escalas` (+ `dias`), e
qualquer coleção não listada explicitamente na regra (negação padrão).

## 8. Diferença entre regra transitória e definitiva

A regra transitória (esta spec) e a regra definitiva (FASE 14d) têm a
**mesma estrutura de autorização de escrita** (`isSystemAdmin()`/
`isTeamAdmin()`, negação padrão, proteção contra troca de `teamId`/
`adminEmails`). A única diferença real é a leitura das 6 coleções
operacionais: a transitória tem `signedIn() || true` (leitura anônima
mantida, ver seção 6); a definitiva remove esse `|| true`, exigindo
`signedIn()` para tudo, sem exceção — o que só pode ser publicado com
segurança depois que o KMP tiver uma sessão Firebase Auth real (spec 46) e
`user_links` (spec 47) implementados, para que a autenticação não quebre o
app.

## 9. Impacto no KMP

- **Leitura**: nenhuma mudança de comportamento — as 6 coleções que o app
  lê hoje continuam acessíveis exatamente como estão, incluindo documentos
  inativos (seção 6).
- **Escrita**: o KMP não escreve no Firestore hoje (confirmado — o
  contrato `FirebaseScheduleGateway` não expõe `create`/`update`/`delete`),
  então não há nenhum impacto de escrita a considerar.
- Nenhuma alteração de código Kotlin foi feita ou é necessária para esta
  fase.

## 10. Impacto no Dashboard

- Todas as escritas confirmadas por auditoria de código (`setDoc` em
  `sourceFilesRepository.ts`, `importJobsRepository.ts`,
  `membersRepository.ts`, `scheduleAssignmentsRepository.ts` [+
  `writeBatch`], `universalRepository.ts`, `schedulePeriodsRepository.ts`,
  `dashboardRepository.ts` [+ `writeBatch`], `teamsRepository.ts`,
  `onCallRepository.ts` [+ `writeBatch`]) continuam funcionando para
  usuários autenticados com papel de administrador do sistema ou
  administrador da própria equipe — mesma autorização que o rascunho de
  regras já testado no Emulator antes desta fase previa.
- Usuários autenticados sem nenhum papel (`system_admins`/`adminEmails`)
  passam a ler as 6 coleções operacionais normalmente (via `signedIn()`),
  mas não conseguem escrever em nada — comportamento já esperado pelo
  modelo de autorização do Dashboard, que só libera edição para papéis
  reconhecidos.
- **Atenção**: como o rascunho de regras já existente antes desta fase
  também exigia `signedIn()` para leitura de `system_admins`/`teams`/
  `members` (necessário para o Dashboard descobrir o papel do usuário logo
  após o login), essa leitura inicial continua funcionando sem mudança.

## 11. Rollback

Se a regra transitória causar qualquer interrupção inesperada no Dashboard
ou no KMP após implantação:

1. reverter o deploy com o comando:
   ```bash
   firebase deploy --only firestore:rules --project escalaici
   ```
   usando a versão anterior do arquivo (disponível no histórico do Git —
   `git show <commit-anterior>:firebase/firestore.rules` ou, em último
   caso, republicar manualmente o snapshot de
   `firebase/firestore.production.snapshot.rules` até uma nova correção
   ser testada);
2. o Firebase Console também mantém histórico de versões de Rules
   publicadas (Firestore → Regras → Histórico), permitindo reverter por lá
   sem precisar de acesso a este repositório;
3. **nenhuma migração de dados está envolvida** — regras não alteram
   documentos existentes, então o rollback é seguro e imediato.

## 12. Plano de implantação manual (ação humana, não executada por esta sessão)

```bash
cd firebase
npm run test:rules   # confirmar que os 22 testes continuam passando
firebase deploy --only firestore:rules --project escalaici
```

Antes de rodar o deploy real, confirme manualmente:

- que está autenticado na conta correta do Firebase (`firebase login:list`);
- que o projeto de destino é `escalaici` (produção), não
  `demo-escalaici-kmp` (Emulator);
- que este é o único artefato sendo publicado (`firestore.rules`), sem
  Functions, sem Cloudflare, sem nenhum outro deploy simultâneo.

## 13. Plano da FASE 14d

1. Implementar a ponte MSAL → Firebase Auth (spec 46) para o KMP Android
   primeiro, depois Web.
2. Implementar `user_links` de verdade (spec 47), populado pelo fluxo de
   vínculo administrativo no Dashboard.
3. Alterar `FirestoreRestGateway.kt` para autenticar (`signInWithCustomToken`)
   antes de ler, e adicionar `where` real às consultas de `schedule_periods`/
   `oncall_periods` por `active == true` (spec 48), o que finalmente permite
   remover a exceção `|| true` das 6 coleções sem quebrar a listagem.
4. Publicar a regra definitiva (sem `|| true` em nenhuma coleção),
   validada primeiro no Emulator e depois num projeto Firebase de
   desenvolvimento, antes de produção.
5. Reavaliar `isTeamAdmin()` para incluir `responsibleEmail`, se confirmado
   necessário pelo modelo real do Dashboard.

## 14. Critérios de aceite

1. Nenhuma escrita anônima é permitida em nenhuma coleção (testado).
2. As 6 coleções operacionais continuam legíveis anonimamente, sem
   ampliar o que já é lido hoje (testado).
3. `system_admins`, `user_links`, `source_files`, `import_jobs` nunca são
   públicos (testado).
4. Coleções desconhecidas são negadas tanto para leitura quanto para
   escrita, autenticado ou não (testado — corrige uma lacuna real
   encontrada no rascunho anterior, que permitia leitura de qualquer nome
   de coleção para qualquer usuário autenticado).
5. Um coordenador de equipe não consegue escrever em outra equipe nem
   trocar `teamId`/`adminEmails` (testado).
6. Um usuário autenticado sem papel não escreve em nada (testado).
7. Um administrador do sistema continua conseguindo gerenciar cadastros
   universais e coleções operacionais (testado).
8. Todos os 22 testes do Emulator passam
   (`cd firebase && npm run test:rules`).
9. Nenhum deploy real foi executado por esta sessão.

## 15. Checklist de implantação (antes do deploy manual)

- [ ] `npm run test:rules` passando localmente (25/25).
- [ ] Revisão independente desta spec e das regras concluída (seção
      "Revisão independente" do relatório final desta fase).
- [ ] Confirmação de que nenhuma credencial ou arquivo grande foi
      adicionado.
- [ ] Confirmação de que `firebase/firestore.production.snapshot.rules`
      permanece inalterado como evidência histórica.
- [ ] Verificação manual, nos dados reais de produção, de que nenhum
      documento em `teams` tem `responsibleEmail` que não esteja também
      presente em `adminEmails`. Como `isTeamAdmin()` só reconhece
      `adminEmails`, qualquer responsável fora dessa lista teria escritas
      bloqueadas sem aviso após o deploy. Se algum time real estiver nessa
      situação, avisar essas pessoas ou adicioná-las a `adminEmails` antes
      do deploy (ação humana, fora do escopo desta correção).
- [ ] Decisão humana explícita de prosseguir com o deploy, ciente dos
      riscos residuais da seção 6.

## 16. Checklist de validação após implantação

- [ ] Dashboard: login Microsoft continua funcionando; leitura inicial de
      `system_admins`/`teams`/`members` continua liberando a interface;
      criação/edição de equipe, membro, período, assignment continua
      funcionando para o papel correto; usuário sem papel não consegue
      editar nada.
- [ ] KMP (Android e Web): abas Hoje/Escala/Plantão continuam carregando
      dados normalmente, sem nenhuma mensagem nova de erro de permissão.
- [ ] Nenhum log inesperado de `PERMISSION_DENIED` no Firebase Console para
      tráfego legítimo do Dashboard ou do KMP nas primeiras 24-48h.
- [ ] Se qualquer um dos itens acima falhar: seguir o rollback da seção 11
      imediatamente.

## 17. Limitações que permanecem até a FASE 14d

- Leitura anônima das 6 coleções operacionais continua no nível atual,
  incluindo documentos inativos/rascunho (seção 6, item 1).
- Campo `email` em `members`, se presente, continua exposto a leitores
  anônimos que acessem a API REST diretamente (seção 6, item 2).
- `isTeamAdmin()` não reconhece `responsibleEmail`, só `adminEmails`
  (seção 6, item 3).
- Nenhuma autenticação real existe ainda no KMP — esta regra é
  estritamente uma redução de risco de escrita, não uma solução de
  identidade.
