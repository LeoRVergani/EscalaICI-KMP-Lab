// Testes das Firestore Rules do contrato NOVO (login-based) do Escala-ICI,
// espelhadas em ../firestore.rules (origin/main commit 441c92b - FASE 15).
// Cobre exatamente o que o KMP precisa poder/nao poder ler como
// colaborador comum (nunca gestor): prompt FASE-15-FIREBASE-UNIFICADO.md
// secao 32 (usuario valido, equipe errada, rascunho nunca visivel).
import { after, before, beforeEach, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, getDocs, collection, query, where, setDoc } from "firebase/firestore";
import { readFile } from "node:fs/promises";

const projectId = "demo-escalaici-kmp";
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: await readFile(new URL("../firestore.rules", import.meta.url), "utf8"),
    },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();

    await setDoc(doc(db, "usuarios", "ana.silva"), {
      login: "ana.silva", nome: "Ana Silva", email: "ana.silva@empresa.com",
      cargo: "Analista", equipeId: "EQ_TESTE", nivelHierarquico: 6,
      turnoPadrao: "M", ativo: true,
    });
    await setDoc(doc(db, "usuarios", "marina.lima"), {
      login: "marina.lima", nome: "Marina Lima", email: "marina.lima@empresa.com",
      cargo: "Coordenadora", equipeId: "EQ_TESTE", nivelHierarquico: 4,
      turnoPadrao: "M", ativo: true,
    });
    await setDoc(doc(db, "usuarios", "carlos.souza"), {
      login: "carlos.souza", nome: "Carlos Souza", email: "carlos.souza@empresa.com",
      cargo: "Analista", equipeId: "EQ_OUTRA", nivelHierarquico: 6,
      turnoPadrao: "M", ativo: true,
    });

    await setDoc(doc(db, "tiposTurno", "EQ_TESTE_M"), {
      codigo: "M", descricao: "Manhã", categoria: "TRABALHO", equipeId: "EQ_TESTE",
      horaInicio: "07:00", horaFim: "13:00", duracaoMinutos: 360, viraDia: false,
      contaComoPlantao: false, pesoPlantao: 0, corHex: "#FFFF00", aliasesXLS: ["M"],
    });

    await setDoc(doc(db, "turnosMes", "EQ_TESTE_ana.silva_2026-08"), {
      schemaVersion: 1, usuarioUid: "ana.silva", login: "ana.silva", equipeId: "EQ_TESTE",
      competencia: "2026-08", periodoInicio: "2026-07-26", periodoFim: "2026-08-25",
      turnoPadrao: "M", status: "PUBLICADA", dias: {}, totais: {},
    });
    await setDoc(doc(db, "rascunhosTurnosMes", "EQ_TESTE_ana.silva_2026-09"), {
      schemaVersion: 1, usuarioUid: "ana.silva", login: "ana.silva", equipeId: "EQ_TESTE",
      competencia: "2026-09", periodoInicio: "2026-08-26", periodoFim: "2026-09-25",
      turnoPadrao: "M", status: "RASCUNHO", dias: {}, totais: {},
    });
    await setDoc(doc(db, "turnosMes", "EQ_OUTRA_carlos.souza_2026-08"), {
      schemaVersion: 1, usuarioUid: "carlos.souza", login: "carlos.souza", equipeId: "EQ_OUTRA",
      competencia: "2026-08", periodoInicio: "2026-07-26", periodoFim: "2026-08-25",
      turnoPadrao: "M", status: "PUBLICADA", dias: {}, totais: {},
    });
  });
});

function contextoDe(login, email) {
  return env.authenticatedContext(login, { email });
}

test("colaborador le o proprio perfil em usuarios/{login}", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertSucceeds(getDoc(doc(db, "usuarios", "ana.silva")));
});

test("colaborador le perfil de colega da mesma equipe", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertSucceeds(getDoc(doc(db, "usuarios", "marina.lima")));
});

test("colaborador NAO le perfil de usuario de outra equipe", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertFails(getDoc(doc(db, "usuarios", "carlos.souza")));
});

test("colaborador le turnosMes PUBLICADA da propria equipe", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertSucceeds(getDoc(doc(db, "turnosMes", "EQ_TESTE_ana.silva_2026-08")));
});

test("colaborador comum NUNCA le rascunhosTurnosMes (mesmo o proprio)", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertFails(getDoc(doc(db, "rascunhosTurnosMes", "EQ_TESTE_ana.silva_2026-09")));
});

test("gestor (nivelHierarquico <= 5) le rascunhosTurnosMes da propria equipe", async () => {
  const db = contextoDe("marina.lima", "marina.lima@empresa.com").firestore();
  await assertSucceeds(getDoc(doc(db, "rascunhosTurnosMes", "EQ_TESTE_ana.silva_2026-09")));
});

test("colaborador NAO le turnosMes de outra equipe", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertFails(getDoc(doc(db, "turnosMes", "EQ_OUTRA_carlos.souza_2026-08")));
});

test("consulta turnosMes por login+equipeId+status==PUBLICADA funciona sem indice composto novo", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  const consulta = query(
    collection(db, "turnosMes"),
    where("login", "==", "ana.silva"),
    where("equipeId", "==", "EQ_TESTE"),
    where("status", "==", "PUBLICADA"),
  );
  const resultado = await assertSucceeds(getDocs(consulta));
  if (resultado.size !== 1) {
    throw new Error(`esperado 1 documento, recebido ${resultado.size}`);
  }
});

test("qualquer usuario autenticado le tiposTurno", async () => {
  const db = contextoDe("ana.silva", "ana.silva@empresa.com").firestore();
  await assertSucceeds(getDoc(doc(db, "tiposTurno", "EQ_TESTE_M")));
});

test("leitura anonima (sem sessao) e sempre negada", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "usuarios", "ana.silva")));
  await assertFails(getDoc(doc(db, "turnosMes", "EQ_TESTE_ana.silva_2026-08")));
});
