#!/usr/bin/env bash
# Diagnóstico de assinatura — SOMENTE LEITURA.
#
# Imprime SHA-1, SHA-256 e o signature hash Base64 usado pelo MSAL
# (msauth://<applicationId>/<hash>) para uma keystore local. Não grava
# nenhum arquivo, não imprime senha, não modifica a keystore.
#
# Uso:
#   KEYSTORE_PATH=/caminho/para/arquivo.jks \
#   KEYSTORE_ALIAS=meu-alias \
#   KEYSTORE_PASSWORD='minha-senha' \
#     ./scripts/print-signing-info.sh
#
# Também aceita debug.keystore padrão do Android se KEYSTORE_PATH não for
# definida (senha/alias padrão "android", conforme convenção do SDK).
#
# Variáveis:
#   KEYSTORE_PATH      caminho do arquivo .jks/.keystore (obrigatório para
#                       release; opcional para debug — default abaixo)
#   KEYSTORE_ALIAS      alias da chave (obrigatório)
#   KEYSTORE_PASSWORD   senha da keystore (obrigatória; NUNCA hardcode isto
#                       em um script versionado — passe via variável de
#                       ambiente na hora de rodar)
#
# A senha nunca é impressa por este script. Ela é usada apenas como
# argumento local para o `keytool`, que a lê da variável de ambiente.

set -euo pipefail

DEFAULT_DEBUG_KEYSTORE="${HOME}/.android/debug.keystore"

KEYSTORE_PATH="${KEYSTORE_PATH:-}"
KEYSTORE_ALIAS="${KEYSTORE_ALIAS:-}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}"

if [[ -z "${KEYSTORE_PATH}" ]]; then
  if [[ -f "${DEFAULT_DEBUG_KEYSTORE}" ]]; then
    echo "KEYSTORE_PATH não definida — usando debug.keystore padrão: ${DEFAULT_DEBUG_KEYSTORE}"
    KEYSTORE_PATH="${DEFAULT_DEBUG_KEYSTORE}"
    KEYSTORE_ALIAS="${KEYSTORE_ALIAS:-androiddebugkey}"
    KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-android}"
  else
    echo "ERRO: KEYSTORE_PATH não definida e ${DEFAULT_DEBUG_KEYSTORE} não existe." >&2
    echo "Defina KEYSTORE_PATH apontando para a keystore de release (ex.: escalaici-kmp-lab.jks)." >&2
    exit 1
  fi
fi

if [[ ! -f "${KEYSTORE_PATH}" ]]; then
  echo "ERRO: arquivo de keystore não encontrado em: ${KEYSTORE_PATH}" >&2
  exit 1
fi

if [[ -z "${KEYSTORE_ALIAS}" ]]; then
  echo "ERRO: KEYSTORE_ALIAS não definida. Veja o alias real em keystore.properties (fora do Git)." >&2
  exit 1
fi

if [[ -z "${KEYSTORE_PASSWORD}" ]]; then
  echo "ERRO: KEYSTORE_PASSWORD não definida. Passe via variável de ambiente, não como argumento de linha de comando." >&2
  exit 1
fi

if ! command -v keytool >/dev/null 2>&1; then
  echo "ERRO: 'keytool' não encontrado no PATH. Use um JDK (ex.: JAVA_HOME=/usr/lib/jvm/java-21-openjdk)." >&2
  exit 1
fi

echo "== Keystore: ${KEYSTORE_PATH}"
echo "== Alias:    ${KEYSTORE_ALIAS}"
echo

FINGERPRINTS="$(keytool -list -v \
  -keystore "${KEYSTORE_PATH}" \
  -alias "${KEYSTORE_ALIAS}" \
  -storepass "${KEYSTORE_PASSWORD}" 2>/dev/null || true)"

if [[ -z "${FINGERPRINTS}" ]]; then
  echo "ERRO: não foi possível ler a keystore. Verifique KEYSTORE_PATH/KEYSTORE_ALIAS/KEYSTORE_PASSWORD." >&2
  exit 1
fi

SHA1_LINE="$(echo "${FINGERPRINTS}" | grep -i 'SHA1:' | head -1)"
SHA256_LINE="$(echo "${FINGERPRINTS}" | grep -i 'SHA256:' | head -1)"

echo "-- SHA-1:"
echo "   ${SHA1_LINE:-(não encontrado — verifique a saída do keytool acima)}"
echo
echo "-- SHA-256:"
echo "   ${SHA256_LINE:-(não encontrado — verifique a saída do keytool acima)}"
echo

echo "-- Signature hash Base64 (para redirect URI MSAL: msauth://<applicationId>/<hash>):"
SIG_HASH="$(keytool -exportcert \
  -alias "${KEYSTORE_ALIAS}" \
  -keystore "${KEYSTORE_PATH}" \
  -storepass "${KEYSTORE_PASSWORD}" 2>/dev/null \
  | openssl sha1 -binary \
  | openssl base64 || true)"

if [[ -z "${SIG_HASH}" ]]; then
  echo "   ERRO: não foi possível gerar o hash (verifique openssl instalado e credenciais)." >&2
else
  echo "   ${SIG_HASH}"
  echo
  echo "   Redirect URI Android completa (URL-encode o '=' final como %3D ao"
  echo "   cadastrar no Entra, se o portal pedir o valor já pronto):"
  echo "   msauth://<ANDROID_PACKAGE_NAME>/$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1], safe=''))" "${SIG_HASH}" 2>/dev/null || echo "${SIG_HASH}")"
fi

echo
echo "Nenhuma senha foi impressa. Nenhum arquivo foi criado ou modificado."
