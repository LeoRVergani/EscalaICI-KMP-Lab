import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
} from "firebase/firestore";
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
    await setDoc(doc(db, "system_admins", "admin@ici.tec.br"), { active: true });

    await setDoc(doc(db, "teams", "soc"), {
      teamId: "soc",
      active: true,
      adminEmails: ["team.admin@ici.tec.br"],
    });
    await setDoc(doc(db, "teams", "n1"), {
      teamId: "n1",
      active: true,
      adminEmails: ["n1.admin@ici.tec.br"],
    });
    await setDoc(doc(db, "teams", "descontinuada"), {
      teamId: "descontinuada",
      active: false,
      adminEmails: [],
    });

    await setDoc(doc(db, "members", "member-1"), {
      memberId: "member-1",
      teamId: "soc",
      active: true,
      displayName: "Colaborador Um",
    });
    await setDoc(doc(db, "members", "member-inativo"), {
      memberId: "member-inativo",
      teamId: "soc",
      active: false,
      displayName: "Ex-colaborador",
    });

    await setDoc(doc(db, "schedule_periods", "period-1"), {
      periodId: "period-1",
      teamId: "soc",
      active: true,
    });
    await setDoc(doc(db, "schedule_periods", "period-rascunho"), {
      periodId: "period-rascunho",
      teamId: "soc",
      active: false,
    });

    await setDoc(doc(db, "schedule_assignments", "assignment-1"), {
      assignmentId: "assignment-1",
      teamId: "soc",
      periodId: "period-1",
    });

    await setDoc(doc(db, "oncall_periods", "oncall-period-1"), {
      periodId: "oncall-period-1",
      teamId: "soc",
      active: true,
    });
    await setDoc(doc(db, "oncall_assignments", "oncall-assignment-1"), {
      onCallId: "oncall-assignment-1",
      teamId: "soc",
      active: true,
    });
    await setDoc(doc(db, "oncall_assignments", "oncall-assignment-inativo"), {
      onCallId: "oncall-assignment-inativo",
      teamId: "soc",
      active: false,
    });

    await setDoc(doc(db, "source_files", "file-1"), { teamId: "soc" });
    await setDoc(doc(db, "import_jobs", "job-1"), { teamId: "soc" });
    await setDoc(doc(db, "shift_swap_requests", "swap-1"), { teamId: "soc" });
    await setDoc(doc(db, "work_patterns", "pattern-1"), { teamId: "soc" });

    await setDoc(doc(db, "user_links", "uid-reader"), {
      tenantId: "tenant-1",
      objectId: "obj-1",
      memberId: "member-1",
      teamIds: ["soc"],
      active: true,
    });
  });
});

after(async () => {
  await env.cleanup();
});

// --- 1-3: nenhuma escrita anônima em nenhuma coleção ---

test("anonimo nao pode criar documentos", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(setDoc(doc(db, "schedule_assignments", "assignment-novo"), {
    assignmentId: "assignment-novo",
    teamId: "soc",
  }));
});

test("anonimo nao pode atualizar documentos", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(updateDoc(doc(db, "schedule_periods", "period-1"), { active: false }));
});

test("anonimo nao pode excluir documentos", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(deleteDoc(doc(db, "members", "member-1")));
});

// --- 4: leitura anonima somente nas coleções explicitamente temporárias ---

test("anonimo le teams/members/periodos/assignments ativos (minimo do KMP)", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "teams", "soc")));
  await assertSucceeds(getDoc(doc(db, "members", "member-1")));
  await assertSucceeds(getDoc(doc(db, "schedule_periods", "period-1")));
  await assertSucceeds(getDoc(doc(db, "schedule_assignments", "assignment-1")));
  await assertSucceeds(getDoc(doc(db, "oncall_periods", "oncall-period-1")));
  await assertSucceeds(getDoc(doc(db, "oncall_assignments", "oncall-assignment-1")));
});

test("anonimo nao le coleções fora da lista temporária", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "work_patterns", "pattern-1")));
  await assertFails(getDoc(doc(db, "shift_swap_requests", "swap-1")));
  await assertFails(getDoc(doc(db, "escalas", "2026-07")));
});

// --- 5-8: coleções administrativas/sensíveis nunca públicas ---

test("anonimo nao pode ler system_admins", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "system_admins", "admin@ici.tec.br")));
});

test("anonimo nao pode ler user_links", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "user_links", "uid-reader")));
});

test("anonimo nao pode ler source_files", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "source_files", "file-1")));
});

test("anonimo nao pode ler import_jobs", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "import_jobs", "job-1")));
});

// --- 9: documentos não publicados nunca anônimos, mesmo em coleção temporária ---

// --- 9: risco residual documentado — não é possível bloquear documentos
// inativos/não publicados nas 6 coleções de leitura anônima mínima sem
// quebrar a listagem sem filtro que o KMP faz hoje (ver comentário em
// firestore.rules e spec 51). Este teste documenta e prova o comportamento
// atual — igual ao já exposto hoje pelo modo de teste totalmente aberto, não
// ampliado por esta regra transitória — em vez de afirmar silenciosamente
// que está bloqueado.
test("risco residual documentado: anonimo ainda le time/membro/periodo/plantao inativos (ver spec 51)", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "teams", "descontinuada")));
  await assertSucceeds(getDoc(doc(db, "members", "member-inativo")));
  await assertSucceeds(getDoc(doc(db, "schedule_periods", "period-rascunho")));
  await assertSucceeds(getDoc(doc(db, "oncall_assignments", "oncall-assignment-inativo")));
});

// --- 10: autenticado sem permissão não pode escrever ---

test("autenticado sem papel nao pode escrever em nenhuma coleção operacional", async () => {
  const db = env.authenticatedContext("reader", { email: "reader@ici.tec.br" }).firestore();
  await assertFails(setDoc(doc(db, "schedule_assignments", "assignment-x"), {
    assignmentId: "assignment-x",
    teamId: "soc",
  }));
  await assertFails(updateDoc(doc(db, "members", "member-1"), { displayName: "Alterado" }));
});

test("autenticado sem papel nao cria nem exclui teams", async () => {
  const db = env.authenticatedContext("reader", { email: "reader@ici.tec.br" }).firestore();
  await assertFails(setDoc(doc(db, "teams", "reader-team"), {
    teamId: "reader-team",
    active: true,
    adminEmails: [],
  }));
  await assertFails(deleteDoc(doc(db, "teams", "soc")));
});

// --- 11: administrador autenticado executa as operações administrativas previstas ---

test("administrador do sistema gerencia cadastro universal e coleções operacionais", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "organizations", "ici"), {
    organizationId: "ici",
    active: true,
  }));
  await assertSucceeds(updateDoc(doc(db, "members", "member-1"), { displayName: "Atualizado pelo admin" }));
  await assertSucceeds(setDoc(doc(db, "user_links", "uid-novo"), {
    tenantId: "tenant-1",
    objectId: "obj-2",
    memberId: "member-1",
    teamIds: ["soc"],
    active: true,
  }));
});

test("administrador do sistema exclui cadastro universal", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "organizations", "org-delete"), {
    organizationId: "org-delete",
    active: true,
  }));
  await assertSucceeds(deleteDoc(doc(db, "organizations", "org-delete")));
});

test("administrador do sistema cria preferencia de usuario global", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "app_user_preferences", "pref-1"), {
    userId: "reader",
    theme: "light",
  }));
});

// --- 12: coordenador não pode modificar outro time ---

test("administrador de uma equipe nao escreve em coleções de outra equipe", async () => {
  const db = env.authenticatedContext("team-admin-soc", { email: "team.admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "schedule_assignments", "assignment-soc"), {
    assignmentId: "assignment-soc",
    teamId: "soc",
  }));
  await assertFails(setDoc(doc(db, "schedule_assignments", "assignment-n1"), {
    assignmentId: "assignment-n1",
    teamId: "n1",
  }));
  await assertFails(updateDoc(doc(db, "teams", "n1"), { adminEmails: ["team.admin@ici.tec.br"] }));
});

test("administrador de equipe nao pode trocar teamId nem conceder admin", async () => {
  const db = env.authenticatedContext("team-admin", { email: "team.admin@ici.tec.br" }).firestore();
  await assertFails(updateDoc(doc(db, "schedule_periods", "period-1"), { teamId: "n1" }));
  await assertFails(updateDoc(doc(db, "teams", "soc"), {
    adminEmails: ["team.admin@ici.tec.br", "attacker@ici.tec.br"],
  }));
});

// --- 13: coleções desconhecidas são negadas (leitura e escrita) ---

test("colecao desconhecida nega leitura mesmo autenticado", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertFails(getDoc(doc(db, "unknown_collection", "item")));
});

test("colecao desconhecida nega leitura anonima", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "unknown_collection", "item")));
});

test("colecao desconhecida nega escrita mesmo para administrador do sistema", async () => {
  const db = env.authenticatedContext("system-admin", { email: "admin@ici.tec.br" }).firestore();
  await assertFails(setDoc(doc(db, "unknown_collection", "item"), { active: true }));
});

// --- 14: regras não dependem apenas de campos enviados pelo cliente ---

test("teamId declarado pelo cliente nao concede autorizacao sem vinculo real em adminEmails", async () => {
  const db = env.authenticatedContext("outsider", { email: "outsider@ici.tec.br" }).firestore();
  await assertFails(setDoc(doc(db, "schedule_assignments", "assignment-forjado"), {
    assignmentId: "assignment-forjado",
    teamId: "soc",
  }));
});

// --- 15: leitura mínima do KMP continua possível, inclusive em listagem sem filtro ---

test("listagem anonima sem filtro (como o KMP faz hoje) continua funcionando para teams", async () => {
  const db = env.unauthenticatedContext().firestore();
  const snapshot = await assertSucceeds(getDocs(collection(db, "teams")));
  const ids = snapshot.docs.map((d) => d.id).sort();
  // Inclui "descontinuada" (active:false) — risco residual documentado acima
  // e em spec 51: a listagem sem `where` não pode ser filtrada por regra sem
  // ser rejeitada por inteiro (testado neste Emulator).
  assert.deepEqual(ids, ["descontinuada", "n1", "soc"]);
});

test("listagem anonima sem filtro continua funcionando para members", async () => {
  const db = env.unauthenticatedContext().firestore();
  const snapshot = await assertSucceeds(getDocs(collection(db, "members")));
  const ids = snapshot.docs.map((d) => d.id).sort();
  assert.deepEqual(ids, ["member-1", "member-inativo"]);
});

// --- 16: escrita usada pelo Dashboard continua possível para o perfil autorizado ---

test("administrador de equipe publica periodo e assignments da propria equipe", async () => {
  const db = env.authenticatedContext("team-admin", { email: "team.admin@ici.tec.br" }).firestore();
  await assertSucceeds(setDoc(doc(db, "schedule_periods", "period-novo"), {
    periodId: "period-novo",
    teamId: "soc",
    active: true,
  }));
  await assertSucceeds(setDoc(doc(db, "members", "member-novo"), {
    memberId: "member-novo",
    teamId: "soc",
    active: true,
  }));
});

test("usuario le somente o proprio user_links por firebaseUid", async () => {
  const own = env.authenticatedContext("uid-reader", { email: "reader@ici.tec.br" }).firestore();
  await assertSucceeds(getDoc(doc(own, "user_links", "uid-reader")));
  const other = env.authenticatedContext("uid-outro", { email: "outro@ici.tec.br" }).firestore();
  await assertFails(getDoc(doc(other, "user_links", "uid-reader")));
});
