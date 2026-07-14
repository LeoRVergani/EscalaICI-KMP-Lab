# ADR — Autenticação Firebase e retirada do modo de teste

- **Status:** proposta; decisão de produção pendente
- **Contexto confirmado em:** 14 de julho de 2026

## Contexto

As regras publicadas no projeto `escalaici`, banco `(default)`, não exigem
Firebase Auth. Elas liberam toda leitura e escrita até 4 de agosto de 2026. O
Dashboard usa Firebase Auth/Microsoft, mas o Android legado e o KMP não possuem
uma sessão Firebase Auth equivalente.

O diagnóstico anterior confundiu regras restritivas encontradas no código com
as regras realmente publicadas. O snapshot confirmado pelo Console passa a ser
a referência factual em `firebase/firestore.production.snapshot.rules`.

## Decisão provisória

1. Não conectar o KMP ao Firestore de produção durante o modo de teste.
2. Não alterar nem publicar regras nesta etapa.
3. Desenvolver e testar regras exclusivamente no Emulator com project ID
   `demo-escalaici-kmp`.
4. Tratar Firebase Auth do Dashboard como identidade disponível, mas não como
   proteção efetiva enquanto as regras publicadas ignorarem `request.auth`.
5. Não publicar `deny-all` nem regras autenticadas antes de mapear e testar o
   impacto no Android legado.

## Alternativas

- Manter o modo de teste: preserva funcionamento imediato, mas mantém exposição
  total e expira automaticamente.
- Publicar imediatamente regras autenticadas: reduz exposição, mas quebra o
  Android legado e pode bloquear fluxos não cobertos do Dashboard.
- Liberar leituras anônimas por coleção: evita quebra do Android, mas continua
  sem identidade confiável para limitar equipe e dados pessoais.
- Migrar clientes para Firebase Auth e então endurecer regras: caminho indicado,
  desde que validado por fases e com rollback.

## Consequências

O prazo de 4 de agosto de 2026 é operacional e de segurança. Antes dele devem
existir inventário final, testes no Emulator, decisão de autenticação para cada
cliente, projeto de desenvolvimento, plano de implantação gradual e rollback.
Até essa decisão, o KMP mantém somente arquivo local, cache e demonstração.
