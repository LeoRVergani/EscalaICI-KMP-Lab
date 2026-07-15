# Identidade oficial do app — auditoria e decisão pendente

Gerado durante o sprint automático noturno de 2026-07-15. Este documento
existe porque o adendo do sprint exigiu explicitamente uma auditoria de
identidade: **o `applicationId` deste projeto ainda contém `.lab`**, o que
tem consequências reais em cada integração externa (MSAL, assinatura de
build, Firebase). Nada aqui é código — é constatação de fato + uma decisão
que só Leonardo pode tomar.

## Estado atual (confirmado lendo `composeApp/build.gradle.kts` nesta sessão)

| Campo | Valor atual (KMP lab) | Valor do app oficial (EscalaSOC) |
|---|---|---|
| `applicationId` / `namespace` | `br.com.leorvergani.escalaici.kmp.lab` | `br.com.leorvergani.escalasoc` |
| Keystore de assinatura | `escalaici-kmp-lab.jks` (próprio, distinto) | keystore próprio do EscalaSOC |
| `versionCode` / `versionName` atuais | `13` / `0.6.3` | (não auditado neste sprint) |
| App Registration Entra (MSAL) | mesmo `client_id`
  (`e5b5154d-e65e-4605-b221-73d7ee570580`) e `tenant_id`
  (`d2d23346-e737-4cac-96ec-fb25e7889f01`) do app oficial, **mas sem
  plataforma Android própria cadastrada para o hash de assinatura do lab** | plataforma Android já cadastrada |
| Projeto Firebase | `escalaici` (compartilhado com o Dashboard) | não usa Firestore (usa OneDrive/Dropbox) |

## Por que isso importa (não é só nomenclatura)

O `applicationId` e a chave de assinatura entram diretamente no cálculo do
hash de assinatura que o MSAL usa para montar a `redirect URI` do fluxo de
login Android (`msauth://<applicationId>/<hash>%3D`). Como o lab tem os
dois diferentes do app oficial, **qualquer cadastro feito para o app
oficial não serve para o lab** — é sempre um cadastro adicional (ver
`docs/PENDENCIAS-EXTERNAS.md`, seção 1). Isso significa que, hoje, o KMP
lab e o EscalaSOC oficial são, do ponto de vista de qualquer provedor de
identidade externo (Microsoft, e futuramente Firebase Auth), **dois
aplicativos diferentes**, mesmo compartilhando código-fonte e propósito.

## A decisão pendente

Este projeto está em um ponto de maturidade (paridade visual/funcional
avançada com o EscalaSOC, publicação de release via Dropbox já
funcionando) em que vale perguntar: **o KMP vai continuar sendo um
laboratório paralelo para sempre, ou em algum momento substitui o
EscalaSOC como o app oficial?**

- **Se continuar como laboratório**: nenhuma ação é necessária aqui além
  de manter cadastrando cada integração externa duas vezes (uma para cada
  identidade), como já vem sendo feito. Este documento serve só para
  deixar essa duplicação explícita e rastreável.
- **Se em algum momento for promovido a app oficial**: será necessário (i)
  decidir se o `applicationId` muda para `br.com.leorvergani.escalasoc`
  (ou outro definitivo, sem `.lab`) ou se o app oficial é substituído
  mantendo o applicationId do lab; (ii) gerar/trocar de keystore de
  assinatura conforme a decisão; (iii) refazer o cadastro MSAL com o novo
  hash; (iv) migrar usuários existentes do Play Store / instalação manual,
  o que pode exigir desinstalar e reinstalar (Android não permite trocar
  `applicationId` nem assinatura de um app já instalado sem desinstalar
  antes). **Nenhum código deste sprint assume ou força essa decisão.**

## O que NÃO foi alterado neste sprint

Nenhum arquivo de configuração (`build.gradle.kts`, `keystore.properties`,
manifests) foi tocado para produzir este documento — ele é puramente uma
leitura e um registro do estado atual, seguindo a regra do sprint de nunca
alterar identidade/assinatura sem decisão humana explícita.
