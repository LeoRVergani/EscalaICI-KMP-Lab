#!/usr/bin/env sh
# Semeia o Firebase Emulator (Auth + Firestore) com os mesmos dados
# ficticios usados em firebase/test/firestore.rules.test.mjs, para o
# teste de integracao Kotlin real (FirebaseIntegrationTest.kt) exercitar
# login -> usuarios/{login} -> turnosMes PUBLICADA -> tiposTurno ->
# EscalaIciScheduleMapper contra os emuladores. So funciona com os
# emuladores ja rodando (ex.: dentro de `firebase emulators:exec`).
#
# FASE 16 (Trocas reais) estende este seed com:
# - carlos.souza@empresa.com, mesma equipe EQ_TESTE de ana.silva, turno
#   "T" (Tarde) no mesmo dia - par valido para trocar turno com ana.silva.
# - mariana.rocha@empresa.com, equipe EQ_OUTRA - usada so para confirmar
#   que colaborador de outra equipe nao le/lista trocasEscala de EQ_TESTE.
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
HOJE1=$(date -d "+1 day" +%Y-%m-%d)
HOJE2=$(date -d "+2 day" +%Y-%m-%d)
HOJE3=$(date -d "+3 day" +%Y-%m-%d)
# 4 dias (nao so HOJE) - o teste de Trocas precisa de uma data por cenario
# (aceitar/recusar/cancelar/negacao-cross-equipe) sem colidir com a checagem
# de solicitacao duplicada (mesma solicitante+data ativa).
echo "[seed] escrevendo turnosMes/EQ_TESTE_ana.silva_2026-08 (periodo cobre ${HOJE}..${HOJE3})..."
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
        \"${HOJE}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"M\"}}}},
        \"${HOJE1}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"M\"}}}},
        \"${HOJE2}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"M\"}}}},
        \"${HOJE3}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"M\"}}}}
      }}}
    }
  }" > /dev/null

echo "[seed] criando usuario Auth carlos.souza@empresa.com..."
curl -s -X POST "http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=any" \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos.souza@empresa.com","password":"TesteEmulator123!","returnSecureToken":true}' > /dev/null

echo "[seed] escrevendo usuarios/carlos.souza (mesma equipe EQ_TESTE)..."
curl -s -X PATCH "${FIRESTORE_BASE}/usuarios/carlos.souza" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "login": {"stringValue": "carlos.souza"},
      "nome": {"stringValue": "Carlos Souza"},
      "email": {"stringValue": "carlos.souza@empresa.com"},
      "cargo": {"stringValue": "Analista"},
      "equipeId": {"stringValue": "EQ_TESTE"},
      "nivelHierarquico": {"integerValue": "6"},
      "turnoPadrao": {"stringValue": "T"},
      "ativo": {"booleanValue": true}
    }
  }' > /dev/null

echo "[seed] escrevendo tiposTurno/EQ_TESTE_T..."
curl -s -X PATCH "${FIRESTORE_BASE}/tiposTurno/EQ_TESTE_T" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "codigo": {"stringValue": "T"},
      "descricao": {"stringValue": "Tarde"},
      "categoria": {"stringValue": "TRABALHO"},
      "equipeId": {"stringValue": "EQ_TESTE"},
      "horaInicio": {"stringValue": "13:00"},
      "horaFim": {"stringValue": "19:00"},
      "duracaoMinutos": {"integerValue": "360"},
      "viraDia": {"booleanValue": false},
      "contaComoPlantao": {"booleanValue": false},
      "pesoPlantao": {"integerValue": "0"},
      "corHex": {"stringValue": "#FF8800"},
      "aliasesXLS": {"arrayValue": {"values": [{"stringValue": "T"}]}}
    }
  }' > /dev/null

echo "[seed] escrevendo turnosMes/EQ_TESTE_carlos.souza_2026-08 (periodo cobre ${HOJE}..${HOJE3})..."
curl -s -X PATCH "${FIRESTORE_BASE}/turnosMes/EQ_TESTE_carlos.souza_2026-08" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d "{
    \"fields\": {
      \"schemaVersion\": {\"integerValue\": \"1\"},
      \"usuarioUid\": {\"stringValue\": \"carlos.souza\"},
      \"login\": {\"stringValue\": \"carlos.souza\"},
      \"equipeId\": {\"stringValue\": \"EQ_TESTE\"},
      \"competencia\": {\"stringValue\": \"2026-08\"},
      \"periodoInicio\": {\"stringValue\": \"2020-01-01\"},
      \"periodoFim\": {\"stringValue\": \"2030-12-31\"},
      \"turnoPadrao\": {\"stringValue\": \"T\"},
      \"status\": {\"stringValue\": \"PUBLICADA\"},
      \"dias\": {\"mapValue\": {\"fields\": {
        \"${HOJE}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"T\"}}}},
        \"${HOJE1}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"T\"}}}},
        \"${HOJE2}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"T\"}}}},
        \"${HOJE3}\": {\"mapValue\": {\"fields\": {\"c\": {\"stringValue\": \"T\"}}}}
      }}}
    }
  }" > /dev/null

echo "[seed] criando usuario Auth mariana.rocha@empresa.com (equipe diferente)..."
curl -s -X POST "http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=any" \
  -H "Content-Type: application/json" \
  -d '{"email":"mariana.rocha@empresa.com","password":"TesteEmulator123!","returnSecureToken":true}' > /dev/null

echo "[seed] escrevendo usuarios/mariana.rocha (equipe EQ_OUTRA)..."
curl -s -X PATCH "${FIRESTORE_BASE}/usuarios/mariana.rocha" \
  -H "Authorization: Bearer owner" -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "login": {"stringValue": "mariana.rocha"},
      "nome": {"stringValue": "Mariana Rocha"},
      "email": {"stringValue": "mariana.rocha@empresa.com"},
      "cargo": {"stringValue": "Analista"},
      "equipeId": {"stringValue": "EQ_OUTRA"},
      "nivelHierarquico": {"integerValue": "6"},
      "turnoPadrao": {"stringValue": "M"},
      "ativo": {"booleanValue": true}
    }
  }' > /dev/null

echo "[seed] concluido."
