import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";
import { readFile } from "node:fs/promises";

const projectId = "demo-escalaici-kmp";
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: await readFile(new URL("../firestore.rules", import.meta.url), "utf8"),
    },
  });
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "system_admins", "admin@ici.tec.br"), {
      active: true,
    });
    await setDoc(doc(db, "teams", "soc"), {
      teamId: "soc",
      active: true,
      adminEmails: ["team.admin@ici.tec.br"],
    });
    await setDoc(doc(db, "schedule_periods", "period-1"), {
      periodId: "period-1",
      teamId: "soc",
      active: true,
    });
  });
});

after(async () => {
  await env.cleanup();
});

test("nega leitura anonima", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "schedule_periods", "period-1")));
});

test("permite leitura para usuario autenticado", async () => {
  const db = env.authenticatedContext("reader", { email: "reader@ici.tec.br" }).firestore();
  await assertSucceeds(getDoc(doc(db, "schedule_periods", "period-1")));
});

test("nega escrita para leitor autenticado", async () => {
  const db = env.authenticatedContext("reader", { email: "reader@ici.tec.br" }).firestore();
  await assertFails(setDoc(doc(db, "schedule_assignments", "assignment-1"), {
    assignmentId: "assignment-1",
    teamId: "soc",
  }));
});

test("permite escrita da equipe para administrador da equipe", async () => {
  const db = env.authenticatedContext("team-admin", { email: "team.admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "schedule_assignments", "assignment-1"), {
    assignmentId: "assignment-1",
    teamId: "soc",
  }));
});

test("administrador de equipe nao pode trocar teamId nem conceder admin", async () => {
  const db = env.authenticatedContext("team-admin", { email: "team.admin@ici.tec.br" }).firestore();
  await assertFails(updateDoc(doc(db, "schedule_periods", "period-1"), { teamId: "outro" }));
  await assertFails(updateDoc(doc(db, "teams", "soc"), {
    adminEmails: ["team.admin@ici.tec.br", "attacker@ici.tec.br"],
  }));
});

test("administrador do sistema gerencia cadastro universal", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "organizations", "ici"), {
    organizationId: "ici",
    active: true,
  }));
});

test("colecao desconhecida permanece negada", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertFails(setDoc(doc(db, "unknown_collection", "item"), { active: true }));
});
