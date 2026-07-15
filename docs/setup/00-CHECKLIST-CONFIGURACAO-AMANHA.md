# Checklist de configuração humana — EscalaICI-KMP-Lab

Gerado durante o sprint automático noturno de 2026-07-15, em resposta ao
ADENDO "deixar o projeto pronto para configuração humana". Este documento
é um **índice**, não substitui os specs já existentes — ele aponta para
onde cada decisão/ação externa está documentada em detalhe e resume o que
falta, em ordem de urgência real (não de dificuldade).

Convenção de status usada aqui e em todo relatório deste sprint:
`PRONTO PARA TESTE` (código existe, falta só o passo humano) ·
`PRONTO PARA CONFIGURAÇÃO` (código aceita a config, falta cadastrá-la) ·
`BLOQUEADO EXTERNAMENTE` (depende de decisão/cadastro fora deste repo) ·
`PARCIAL` · `NÃO INICIADO`. Nunca "concluído"/"funcionando" para algo que
não foi testado de ponta a ponta com a configuração real.

---

## 0.🔴 URGENTE — prazo real, não é só organização

### 0.1 Regras do Firestore do projeto `escalaici` expiram em **2026-08-04**

**Status: BLOQUEADO EXTERNAMENTE — prazo em ~3 semanas a partir de hoje (2026-07-14).**

As regras publicadas hoje no projeto `escalaici` (banco `(default)`) são
regras de modo de teste: liberam toda leitura e escrita até 4 de agosto de
2026, independente de autenticação. Depois dessa data, se nada mudar, o
Firestore passa a **negar tudo** por padrão — o que quebraria a leitura do
KMP (Android e Web) e, dependendo de como o Firebase aplicar o corte, pode
afetar o Dashboard também caso as regras publicadas não tenham sido
substituídas por regras definitivas antes disso.

Detalhe completo, decisão provisória e alternativas avaliadas:
[`docs/ADR-FIREBASE-AUTH.md`](../ADR-FIREBASE-AUTH.md).

**O que Leonardo precisa decidir/fazer, antes de 2026-08-04:**
- [ ] Decidir a política de acesso definitiva (ver alternativas na seção
      "Alternativas" do ADR) — a mais indicada ali é "migrar clientes para
      Firebase Auth e então endurecer regras", mas isso depende do item 0.2
      (Custom Token) e do item 0.3 (`user_links`) abaixo, nenhum dos dois
      pronto ainda.
- [ ] Se não houver tempo para a migração completa antes do prazo, definir
      uma prorrogação manual do modo de teste no Console do Firebase
      (Firestore → Regras → editar a data de expiração) como paliativo, e
      registrar essa decisão nesta seção com a nova data.
- [ ] Testar qualquer regra nova primeiro no Emulator (project ID
      `demo-escalaici-kmp`, já é a prática adotada — ver ADR) antes de
      publicar em produção.

Este item não pode ser resolvido por nenhuma sessão de IA: é uma decisão
de política de segurança que só o dono do projeto Firebase pode tomar.

---

## 1. Login corporativo Microsoft (MSAL) no Android do KMP lab

**Status: BLOQUEADO EXTERNAMENTE.** Passo a passo completo, com os valores
exatos de `client_id`/`tenant_id`/`package name`/signature hash e a
redirect URI pronta para colar:
[`docs/PENDENCIAS-EXTERNAS.md`](../PENDENCIAS-EXTERNAS.md), seção 1.

Resumo: falta cadastrar a plataforma Android do **lab** (applicationId
`br.com.leorvergani.escalaici.kmp.lab`, assinatura própria) no App
Registration do Entra ID que já existe para o app oficial — sem isso, o
login Microsoft real no Android nunca vai funcionar neste app, mesmo
depois de a FASE 11.3 (login MSAL real) ser implementada em código.

Ver também [`01-IDENTIDADE-OFICIAL-DO-APP.md`](01-IDENTIDADE-OFICIAL-DO-APP.md)
nesta mesma pasta — a raiz do porquê disso ser necessário é o app ainda
usar uma identidade (`applicationId`) de laboratório, não a oficial.

## 2. Firebase Auth / Custom Token para o KMP

**Status: NÃO INICIADO (decisão de arquitetura pendente).**

O KMP não tem hoje nenhuma sessão Firebase Auth própria — só leitura
anônima (ver ADR-FIREBASE-AUTH.md, decisão provisória item 1). Para ter
identidade real equivalente ao Dashboard, o caminho é: login Microsoft
(MSAL, item 1 acima) → trocar o token Microsoft por um Firebase Custom
Token via Cloud Function (nunca gerado no cliente) → autenticar no
Firebase com esse token. Isso exige:
- [ ] Plano Firebase **Blaze** ativo no projeto `escalaici` (Cloud
      Functions não roda no plano Spark gratuito).
- [ ] Implementar e implantar a Cloud Function de troca de token (ainda
      não existe neste projeto).
- [ ] Ver spec de referência:
      [`docs/spec/46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md`](../spec/46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md).

## 3. Coleção `user_links` (vínculo usuário ↔ membro/time)

**Status: NÃO INICIADO.** Depende do item 2 (precisa de uma identidade
Firebase real para vincular). Spec de referência:
[`docs/spec/47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md`](../spec/47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md).

## 4. OneDrive (importação de escala) para o KMP

**Status: NÃO INICIADO — nunca configurado para este app.** O EscalaSOC
legado usa OneDrive real (10 estratégias de busca), mas o KMP nunca teve
essa integração cadastrada. Precisa de: registro de app com escopo Graph
`Files.Read`, consentimento admin ou por usuário, e código de import ainda
não escrito no KMP. Sem estimativa de esforço de código até haver decisão
de que o KMP realmente vai importar de OneDrive (hoje o KMP só lê
Firestore/Dropbox).

## 5. Dropbox (leitura de escala) — Android e Web

**Status: PRONTO — nenhuma ação pendente conhecida.** Confirmado pelo
usuário em 2026-07-11 (nota do topo de
[`PENDENCIAS-EXTERNAS.md`](../PENDENCIAS-EXTERNAS.md)): escopo
`sharing.read` e redirect URI já cadastrados no App Console do Dropbox
(`DropboxAuthConfig.kt`), com download real da escala funcionando tanto no
Android quanto na Web via OAuth. Não confundir com o item 4 (OneDrive,
nunca configurado) nem com `docs/FONTES-UNIVERSAIS.md`, cujo texto sobre
Dropbox/OneDrive serem "apenas identificadores arquiteturais" está
desatualizado em relação a este item — precisa de uma revisão de conteúdo
nesse arquivo (não faz parte deste sprint).

## 6. Publicar atualização do APK (`EscalaICI-latest.apk` + `version.json`)

**Status: PRONTO PARA TESTE — só falta o upload manual.** Os arquivos já
estão gerados localmente (`/home/lvergani/Downloads/dropbox_update_scripts/`).
Passo a passo exato: [`docs/PENDENCIAS-EXTERNAS.md`](../PENDENCIAS-EXTERNAS.md),
seção 2. **Nenhuma sessão de IA deve tentar automatizar esse upload** — é
regra explícita já registrada naquele arquivo.

## 7. Notificações e alarmes Android (pausa de 15 minutos)

**Status: NÃO INICIADO em código** (nem `AlarmManager`, nem
`NotificationManager`, nem `WorkManager` existem hoje em nenhum lugar do
Android do KMP — confirmado por auditoria de código nesta mesma sessão).
Não depende de nenhuma configuração externa para começar a ser
implementado (é só código Android nativo), mas **validar de ponta a ponta
exige emulador ou aparelho físico** — nunca declarar "notificação validada"
sem isso ter sido de fato disparado e observado. Spec de referência:
[`docs/spec/49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md`](../spec/49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md).

---

## O que este checklist NÃO cobre ainda

O adendo original pediu um pacote de 17 documentos numerados em
`docs/setup/`. Neste sprint só foram produzidos os dois mais urgentes/
estruturais: este índice e `01-IDENTIDADE-OFICIAL-DO-APP.md`. Os demais
15 (ex.: guias passo a passo dedicados por integração, com `.example` de
cada arquivo de configuração, scripts de diagnóstico por integração,
painel de diagnóstico na UI) **não foram escritos** — ficam como trabalho
pendente, não como "concluído resumidamente". O conteúdo técnico de cada
um já existe espalhado nos specs 46/47/48/49/50/51 e nos arquivos citados
acima; falta consolidar em formato de guia passo a passo dedicado.

## Como manter isto atualizado

Sempre que um item acima for resolvido, mova a entrada para
`docs/ROADMAP.md` como `DONE` (mesma convenção já usada por
`PENDENCIAS-EXTERNAS.md`) e remova ou marque como resolvida aqui.
