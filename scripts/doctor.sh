#!/usr/bin/env bash
# Diagnóstico não destrutivo do EscalaICI-KMP-Lab.
# Não instala nada, não altera arquivos, não builda nem sobe emulador por
# conta própria — só lê o estado local e pergunta antes de rodar qualquer
# comando que consuma tempo/recursos. Seguro para rodar quantas vezes quiser.
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ok()   { printf '  [OK]    %s\n' "$1"; }
warn() { printf '  [AVISO] %s\n' "$1"; }
fail() { printf '  [FALTA] %s\n' "$1"; }
section() { printf '\n== %s ==\n' "$1"; }

section "Ferramentas"
if command -v java >/dev/null 2>&1; then
  ok "java encontrado ($(java -version 2>&1 | head -n1))"
else
  fail "java não encontrado no PATH"
fi
if [ -f local.properties ] && command grep -q '^sdk.dir=' local.properties; then
  sdk_dir="$(command grep '^sdk.dir=' local.properties | cut -d= -f2-)"
  if [ -d "$sdk_dir" ]; then
    ok "Android SDK encontrado em $sdk_dir"
  else
    fail "local.properties aponta sdk.dir=$sdk_dir, mas o diretório não existe"
  fi
else
  fail "local.properties ausente ou sem sdk.dir — normalmente gerado pelo Android Studio na primeira abertura"
fi

section "Assinatura de release (keystore.properties)"
if [ -f keystore.properties ]; then
  ok "keystore.properties encontrado"
  for key in storeFile storePassword keyAlias keyPassword; do
    value="$(command grep -E "^${key}=" keystore.properties 2>/dev/null | cut -d= -f2-)"
    if [ -n "$value" ]; then
      ok "$key preenchida (valor não exibido por segurança)"
    else
      warn "$key ausente ou vazia em keystore.properties (só é necessária para build de release assinado)"
    fi
  done
else
  warn "keystore.properties não encontrado — necessário só para build de release assinado, não para debug"
fi

section "Identidade do app (ver docs/setup/01-IDENTIDADE-OFICIAL-DO-APP.md)"
app_id="$(command grep -m1 'applicationId = ' composeApp/build.gradle.kts | command grep -oE '"[^"]+"' | tr -d '"')"
if [ -n "$app_id" ]; then
  if [[ "$app_id" == *.lab ]]; then
    warn "applicationId atual ainda é de laboratório: $app_id (ver docs/setup/01-IDENTIDADE-OFICIAL-DO-APP.md)"
  else
    ok "applicationId: $app_id"
  fi
else
  fail "não foi possível ler applicationId de composeApp/build.gradle.kts"
fi

section "Emulador Android (AVD)"
avd_dir="${HOME}/.android/avd"
if [ -d "$avd_dir" ]; then
  count="$(find "$avd_dir" -maxdepth 1 -iname '*.avd' | wc -l | tr -d ' ')"
  if [ "$count" -gt 0 ]; then
    ok "$count AVD(s) encontrado(s) em $avd_dir"
  else
    warn "nenhum AVD encontrado em $avd_dir"
  fi
else
  warn "pasta de AVDs não encontrada ($avd_dir) — nenhum emulador configurado ainda"
fi

section "Memória disponível (relevante antes de usar o emulador)"
if command -v free >/dev/null 2>&1; then
  free -h | command grep -E '^(Mem|total)'
  avail_kb="$(command grep MemAvailable /proc/meminfo 2>/dev/null | awk '{print $2}')"
  if [ -n "${avail_kb:-}" ]; then
    avail_gb=$((avail_kb / 1024 / 1024))
    if [ "$avail_gb" -lt 16 ]; then
      warn "memória disponível (~${avail_gb} GiB) abaixo do limite recomendado de 16 GiB para abrir o emulador com segurança"
    else
      ok "memória disponível (~${avail_gb} GiB) acima do limite recomendado de 16 GiB"
    fi
  fi
else
  warn "comando 'free' não disponível — não foi possível checar memória automaticamente"
fi

section "Git"
branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
ok "branch atual: $branch"
if git diff --quiet 2>/dev/null && git diff --cached --quiet 2>/dev/null; then
  ok "sem alterações não commitadas"
else
  warn "há alterações não commitadas (git status para detalhes)"
fi

section "Configuração humana pendente"
if [ -f docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md ]; then
  ok "checklist encontrado em docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md — leia-o para o que falta configurar externamente, incluindo o prazo urgente das regras do Firestore"
else
  warn "docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md não encontrado"
fi

section "Verificações de código (só rodam se você confirmar)"
read -r -p "Rodar './gradlew :composeApp:testDebugUnitTest --offline' agora (pode levar minutos)? [s/N] " resp
if [[ "$resp" =~ ^[sS]$ ]]; then
  ./gradlew :composeApp:testDebugUnitTest --offline && ok "testes JVM (Android debug) passaram" || fail "testes JVM (Android debug) falharam"
fi

printf '\nDiagnóstico concluído. Nenhum arquivo foi alterado, nenhum build/emulador foi iniciado sem sua confirmação explícita.\n'
