# Identidade oficial do app — auditoria

Gerado durante o sprint automático noturno de 2026-07-15. Este documento
existe porque o adendo do sprint exigiu explicitamente uma auditoria de
identidade. Nada aqui é código — é constatação de fato sobre a identidade
técnica do app e suas consequências em integrações externas (MSAL,
assinatura de build, Firebase).

## Estado atual

| Campo | Valor atual (Escala ICI) | Valor do app oficial legado (EscalaSOC) |
|---|---|---|
| `applicationId` / `namespace` | `br.com.leorvergani.escalaici` | `br.com.leorvergani.escalasoc` |
| Keystore de assinatura | `escalaici-kmp-lab.jks` (próprio, distinto) | keystore próprio do EscalaSOC |
| `versionCode` / `versionName` atuais | `13` / `0.6.3` | (não auditado neste sprint) |
| App Registration Entra (MSAL) | mesmo `client_id`
  (`e5b5154d-e65e-4605-b221-73d7ee570580`) e `tenant_id`
  (`d2d23346-e737-4cac-96ec-fb25e7889f01`) do app oficial, **mas sem
  plataforma Android própria cadastrada para o package e hash de assinatura atuais do Escala ICI** | plataforma Android já cadastrada |
| Projeto Firebase | `escalaici` (compartilhado com o Dashboard) | não usa Firestore (usa OneDrive/Dropbox) |

## Por que isso importa (não é só nomenclatura)

O `applicationId` e a chave de assinatura entram diretamente no cálculo do
hash de assinatura que o MSAL usa para montar a `redirect URI` do fluxo de
login Android (`msauth://<applicationId>/<hash>%3D`). Como o Escala ICI tem
package e assinatura próprios, **qualquer cadastro feito para o EscalaSOC
não serve para ele** — é sempre um cadastro adicional (ver
`docs/PENDENCIAS-EXTERNAS.md`, seção 1). Isso significa que, hoje, o Escala
ICI e o EscalaSOC são, do ponto de vista de qualquer provedor de identidade
externo (Microsoft, e futuramente Firebase Auth), **dois aplicativos
diferentes**, mesmo compartilhando propósito.

## Decisões futuras

Este projeto está em um ponto de maturidade (paridade visual/funcional
avançada com o EscalaSOC, publicação de release via Dropbox já funcionando)
em que a identidade oficial do Escala ICI já foi definida como
`br.com.leorvergani.escalaici`. Ainda restam decisões futuras de migração,
loja e convivência com o EscalaSOC legado.

- **MSAL/Entra**: cadastrar o package atual
  `br.com.leorvergani.escalaici` e recalcular o hash de assinatura para a
  redirect URI Android.
- **Migração de usuários**: se a estratégia final envolver substituir o
  EscalaSOC instalado, documentar o caminho de instalação/migração, pois o
  Android não permite trocar `applicationId` nem assinatura de um app já
  instalado sem desinstalar antes.
- **Loja e release**: decidir quando o Escala ICI passa a ser distribuído
  como app oficial em vez de apenas por APK/manual.

## O que NÃO foi alterado neste sprint

Nenhum arquivo de configuração (`build.gradle.kts`, `keystore.properties`,
manifests) foi tocado para produzir este documento — ele é puramente uma
leitura e um registro do estado atual, seguindo a regra do sprint de nunca
alterar identidade/assinatura sem decisão humana explícita.
