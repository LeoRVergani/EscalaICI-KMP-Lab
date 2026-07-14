# Auditoria Firebase do Dashboard

## Resultado

O Dashboard usa Firebase Auth com o provedor Microsoft/Entra ID e observa a
sessão com `onAuthStateChanged`. Depois do login, consulta `system_admins`,
`teams` e `members` para decidir se a interface será liberada.

Todas as operações de dados são feitas diretamente no navegador pelo SDK Web
`firebase/firestore`. A autorização da UI não substitui Firestore Rules. Como
as regras publicadas aceitam qualquer leitura e escrita até 4 de agosto de
2026, o Dashboard funciona, mas atualmente depende de acesso que também pode
ser explorado sem passar pelo login.

## Coleções e operações observadas

| Coleções | Leituras | Escritas |
|---|---|---|
| `system_admins` | documento por e-mail | não observada no fluxo normal |
| `teams` | lista, filtros e documento | `setDoc`/merge |
| `members` | lista, filtros e documento | `setDoc`/merge |
| `schedule_periods` | lista/filtro e documento | criar/atualizar com `setDoc` |
| `schedule_assignments` | lista por equipe/período | `setDoc` e `writeBatch` |
| `oncall_periods` | lista/filtro | `setDoc` |
| `oncall_assignments` | lista/filtro | `writeBatch` |
| `source_files` | lista e documento | `setDoc` |
| `import_jobs` | lista e documento | `setDoc`/merge |
| `shift_swap_requests` | lista e documento | não observada nesse repositório |
| `escalas/{anoMes}/dias` | lista e documento | `setDoc` e `writeBatch` |
| coleções universais | lista e documento | criar, atualizar e ativar/desativar |

As coleções universais usadas são `organizations`, `org_units`,
`member_team_memberships`, `roles`, `schedule_profiles`, `activity_codes`,
`dashboard_permissions` e `app_user_preferences`.

Não foram observados `deleteDoc` nem transações no cliente auditado. As escritas
usam majoritariamente `setDoc`, inclusive com merge, timestamps de servidor e
lotes de até 500 operações.

## Dependência do acesso público

- O Dashboard já envia uma identidade Firebase Auth válida após o login.
- Suas verificações de papel residem em documentos, não em claims consistentes
  com o arquivo restritivo antigo.
- Regras autenticadas podem ser viáveis para o Dashboard, mas precisam permitir
  as leituras iniciais de `system_admins`, `teams` e `members` antes de o cliente
  saber seu papel.
- As escritas precisam distinguir administrador do sistema e administrador da
  equipe, além de impedir elevação por alteração de `adminEmails`.
- O Android legado não possui Firebase Auth e provavelmente quebrará se as
  regras propostas forem publicadas sem uma migração de autenticação.

## Testes necessários antes de produção

1. login Microsoft e leitura inicial de autorização;
2. usuário autenticado sem papel não acessar o Dashboard;
3. administrador de equipe limitado à própria equipe;
4. administrador do sistema gerenciar cadastros universais;
5. impossibilidade de promover o próprio e-mail ou trocar `teamId`;
6. importação/publicação por lotes;
7. leitura de escala e plantão pelos aplicativos;
8. comportamento offline e após expiração da sessão;
9. coleções desconhecidas e acesso anônimo negados;
10. rollback validado no Emulator e em projeto de desenvolvimento.

## Validação inicial das regras propostas

As regras propostas foram compiladas e executadas no Firestore Emulator com
project ID `demo-escalaici-kmp`. Os sete testes iniciais passaram. Isso comprova
somente a fundação das regras; ainda faltam testes de cada coleção/operação da
tabela, importação em lote, autorização inicial do Dashboard e compatibilidade
dos aplicativos leitores.
