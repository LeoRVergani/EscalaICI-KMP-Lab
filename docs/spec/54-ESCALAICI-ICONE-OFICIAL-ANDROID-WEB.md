# SPEC 54 — Ícone oficial do Escala ICI no Android e Web/PWA

**Fase:** 14b-1a  
**Versão inicial:** `versionCode 15` / `versionName 0.7.1`

**Correção 14b-1b:** `versionCode 17` / `versionName 0.7.3`
**Nome visível:** Escala ICI

## 1. Objetivo e fonte

Esta fase aplica o ícone oficial composto por calendário, grade mensal e
relógio sobreposto, com acabamento em roxo, violeta, lilás e branco. A única
fonte autorizada é:

`branding/input/escala-ici-icon-transparent.png`

O arquivo original é preservado. A auditoria técnica constatou que, apesar do
nome, ele era PNG RGB sem canal alpha real e continha fundo quadriculado. O
processamento remove somente esse checker, preservando desenho, sombras e
degradês, e grava a cópia transparente em:

`branding/generated/escala-ici-icon-foreground.png`

O PNG gerado é a fonte de foreground; não recebe fundo branco, moldura, texto,
letras ou cantos arredondados desenhados.

## 2. Transparência e área segura

O foreground gerado possui transparência real, incluindo pixels totalmente
transparentes. O símbolo fica centralizado, com margem para que calendário,
relógio e sombras permaneçam dentro da zona segura em máscaras circulares,
quadradas arredondadas e squircle. O relógio não encosta na borda.

O fundo do launcher nunca é incorporado ao foreground. A composição usa uma
camada roxa/violeta separada, coerente com a paleta do aplicativo. Essa
separação também evita halo branco e permite que o Android aplique sua máscara
sem recortar o desenho.

## 3. Android

### Adaptive icon

Os recursos `mipmap-anydpi-v26/ic_launcher.xml` e
`ic_launcher_round.xml` compõem:

- foreground: símbolo oficial transparente e com margem segura;
- background: camada roxa/violeta separada;
- round icon: a mesma composição adaptativa, sujeita à máscara do launcher.

O `AndroidManifest.xml` continua apontando para `@mipmap/ic_launcher` e
`@mipmap/ic_launcher_round`, sem apontar diretamente para um PNG de alta
resolução.

### Launchers legados

As variantes `ic_launcher` e `ic_launcher_round` são fornecidas nas densidades
mdpi, hdpi, xhdpi, xxhdpi e xxxhdpi. Elas preservam proporção e margem do
símbolo sobre fundo roxo coerente, sem esticar ou cortar calendário, relógio ou
sombras.

### Splash

O splash usa o símbolo transparente centralizado e controla o fundo pelo tema.
Não há quadrado branco, checker, texto, “KMP”, “Mock” ou “Lab”. O recurso não
recria a tela inicial: altera somente a identidade visual de abertura.

## 4. Web/PWA

O pacote Web contém ícones normais de 192×192 e 512×512, favicon e variantes
maskable de 192×192 e 512×512. Os ícones normais podem preservar transparência;
os maskable usam fundo roxo preenchido e escala segura para máscaras
agressivas.

O `manifest.json` mantém `name` e `short_name` como **Escala ICI**. O
`index.html` referencia o favicon oficial e o ícone Apple/PWA apropriado. O
service worker inclui os novos arquivos no app shell e deve ter sua chave de
cache incrementada quando necessário para não servir a marca anterior.

## 5. Nome, versão e release

A versão anterior era `14` / `0.7.0`; a versão desta fase é `15` / `0.7.1`.
Os valores são mantidos em sincronia em `composeApp/build.gradle.kts`,
`AppVersion` e `version.json.example`.

O nome público do APK é `EscalaICI-latest.apk`. O esquema compartilhado do
manifesto de atualização permanece inalterado (`kmpVersionCode`,
`kmpVersionName`, `kmpApkUrl` e `kmpChangelog`) e o upload continua manual.
Changelog desta versão: “Novo ícone oficial do Escala ICI aplicado no Android,
splash e Web/PWA.”

## 6. Validação

Validações executadas no fechamento da fase:

- conferir PNG, dimensões, alpha real e pixels transparentes;
- inspecionar previews em 48, 72, 96, 144 e 192 px, em máscaras circular e
  quadrada arredondada, sobre fundos claro e escuro;
- testes unitários Android, `assembleDebug`, `assembleRelease` e
  `wasmJsBrowserDistribution`: `BUILD SUCCESSFUL`;
- `wasmJsTest`: `BUILD SUCCESSFUL` com o Chrome local;
- validar no APK o package `br.com.leorvergani.escalaici`, versão `15` /
  `0.7.1`, label “Escala ICI” e referências de launcher;
- `aapt2 dump badging`: package `br.com.leorvergani.escalaici`, versionCode
  `15`, versionName `0.7.1`, label “Escala ICI” e launcher adaptive presentes;
- previews temporários validados em 48, 72, 96, 144 e 192 px, máscaras circular
  e quadrada arredondada, fundos claro e escuro: relógio e calendário legíveis,
  sem corte, checker ou halo branco;
- nenhum preview temporário, APK ou `version.json` real foi versionado.

O APK release interno foi gerado em
`composeApp/build/outputs/apk/release/composeApp-release.apk`. A pasta externa
documentada `/home/lvergani/Downloads/dropbox_update_scripts` não estava
disponível neste host Windows (WSL sem distribuição), e não foi encontrada uma
pasta Windows equivalente configurada. Por isso `EscalaICI-latest.apk` e o
`version.json` real externos não foram criados/atualizados: nenhum diretório foi
inventado e nenhum upload ou chamada à API do Dropbox foi realizado.

## 7. Limitações

- A qualidade final em launchers de fabricantes diferentes exige inspeção em
  dispositivo ou emulador; ela não é inferida apenas pelo build.
- Máscaras PWA variam entre navegadores e sistemas operacionais.
- O detalhe do desenho pode perder legibilidade em tamanhos muito pequenos;
  ajustes permitidos se limitam a escala, margem e nitidez, sem redesenho.
- Não há upload, deploy ou publicação automática nesta fase.

## 8. Substituição futura

Para trocar o ícone futuramente:

1. substitua somente a fonte aprovada em `branding/input/`, preservando o
   original recebido;
2. verifique formato, proporção, alpha e fundos incorporados antes de gerar;
3. regenere o foreground transparente com área segura, sem fundo embutido;
4. regenere adaptive/round/legados Android e os ícones normal/maskable Web;
5. atualize favicon, manifestos e cache do service worker;
6. incremente a versão e repita builds, inspeção de APK e previews visuais.

Qualquer redesenho do calendário ou relógio, troca de paleta ou inclusão de
texto exige autorização explícita.

## 9. Correção de safe zone — FASE 14b-1b

### Causa

O foreground e o splash reutilizavam o mesmo símbolo com ocupação aproximada
de 68% do canvas. Embora o bounding box estivesse matematicamente centralizado,
essa escala ficava no limite superior da zona segura e sofria recorte adicional
pelas máscaras do launcher e pelo splash do Android 12+.

### Correção

O gerador determinístico `scripts/generate-icon-assets.py` centraliza a arte
pelo bounding box real do canal alpha e aplica escalas específicas, sem
redesenhar ou alterar cores:

- fonte processada: aproximadamente 60% do canvas 1024×1024;
- adaptive foreground: aproximadamente 40% do canvas 432×432, que resulta em
  cerca de 60% visual após a ampliação interna do launcher;
- splash: aproximadamente 36% do canvas 432×432 para tolerar a máscara e a
  ampliação do Android 12+;
- ícones legacy/round: aproximadamente 60% em mdpi a xxxhdpi.

O background `#2F145C`, os adaptive XMLs e o nome visível “Escala ICI” foram
preservados. Web/PWA não precisou ser alterado.

O primeiro ajuste de 58%/48% ainda apareceu cortado em aparelho real porque a
prévia local aplicava somente a máscara, sem simular o zoom interno do launcher.
O print do dispositivo foi adotado como evidência e motivou a compensação acima.

### Arquivos e validação

Foram regenerados a fonte processada, foreground adaptive, splash e launchers
legacy/round. A validação inclui alpha real, bounding boxes centralizados,
previews de 48/72/96/144/192 px, máscaras circular/rounded, testes unitários,
builds debug/release e `aapt2 dump badging` do APK final.
