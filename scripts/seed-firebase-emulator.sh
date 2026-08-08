#!/usr/bin/env sh
# Semeia o Firebase Emulator (Auth + Firestore) com os mesmos dados
# ficticios usados em firebase/test/firestore.rules.test.mjs, para o
# teste de integracao Kotlin real (FirebaseIntegrationTest.kt) exercitar
# login -> usuarios/{login} -> turnosMes PUBLICADA -> tiposTurno ->
# EscalaIciScheduleMapper contra os emuladores. So funciona com os
# emuladores ja rodando (ex.: dentro de `firebase emulators:exec`).
set -eu

AUTH_HOST="127.0.0.1:9099"
FIRESTORE_HOST="127.0.0.1:8080"
PROJECT_ID="demo-escalaici-kmp"

echo "[seed] criando usuario Auth ana.silva@empresa.com..."
curl -s -X POST "http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=any" \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.silva@empresa.com","password":"TesteEmulator123!","returnSecureToken":true}' > /dev/null

FIRESTORE_BASE="http://${FIRESTORE_HOST}/v1/projects/${PROJECT_ID}/databases/(default)/documents"

echo "[seed] escrevendo usuarios/ana.silva..."
curl -s -X PATCH "${FIRESTORE_BASE}/usuarios/ana.silva" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "login": {"stringValue": "ana.silva"},
      "nome": {"stringValue": "Ana Silva"},
      "email": {"stringValue": "ana.silva@empresa.com"},
      "cargo": {"stringValue": "Analista"},
      "equipeId": {"stringValue": "EQ_TESTE"},
      "nivelHierarquico": {"integerValue": "6"},
      "turnoPadrao": {"stringValue": "M"},
      "ativo": {"booleanValue": true}
    }
  }' > /dev/null

echo "[seed] escrevendo tiposTurno/EQ_TESTE_M..."
curl -s -X PATCH "${FIRESTORE_BASE}/tiposTurno/EQ_TESTE_M" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "codigo": {"stringValue": "M"},
      "descricao": {"stringValue": "Manhã"},
      "categoria": {"stringValue": "TRABALHO"},
      "equipeId": {"stringValue": "EQ_TESTE"},
      "horaInicio": {"stringValue": "07:00"},
      "horaFim": {"stringValue": "13:00"},
      "duracaoMinutos": {"integerValue": "360"},
      "viraDia": {"booleanValue": false},
      "contaComoPlantao": {"booleanValue": false},
      "pesoPlantao": {"integerValue": "0"},
      "corHex": {"stringValue": "#FFFF00"},
      "aliasesXLS": {"arrayValue": {"values": [{"stringValue": "M"}]}}
    }
  }' > /dev/null

HOJE=$(date +%Y-%m-%d)
echo "[seed] escrevendo turnosMes/EQ_TESTE_ana.silva_2026-08 (periodo cobre ${HOJE})..."
curl -s -X PATCH "${FIRESTORE_BASE}/turnosMes/EQ_TESTE_ana.silva_2026-08" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d "{
    \"fields\": {
      \"schemaVersion\": {\"integerValue\": \"1\"},
      \"usuarioUid\": {\"stringValue\": \"ana.silva\"},
      \"login\": {\"stringValue\": \"ana.silva\"},
      \"equipeId\": {\"stringValue\": \"EQ_TESTE\"},
      \"competencia\": {\"stringValue\": \"2026-08\"},
      \"periodoInicio\": {\"stringValue\": \"2020-01-01\"},
      \"periodoFim\": {\"stringValue\": \"2030-12-31\"},
      \"turnoPadrao\": {\"stringValue\": \"M\"},
      \"status\": {\"stringValue\": \"PUBLICADA\"},
      \"dias\": {\"mapValue\": {\"fields\": {
        \"${HOJE}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"M\"}}}}
      }}}
    }
  }" > /dev/null

echo "[seed] concluido."
