import { after, before, beforeEach, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  setDoc,
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
    await setDoc(doc(db, "workspaces", "demo-v1"), {
      workspaceId: "demo-v1",
      publicationRevision: 1,
      status: "ACTIVE",
    });
    await setDoc(doc(db, "workspaces/demo-v1/revisions/1/members/member-demo-1"), {
      workspaceId: "demo-v1",
      publicationRevision: 1,
    });
    await setDoc(doc(db, "workspaces", "ici-dev"), {
      workspaceId: "ici-dev",
      publicationRevision: 2,
      status: "ACTIVE",
    });
    await setDoc(doc(db, "workspaces/ici-dev/revisions/2/members/member-corp-1"), {
      workspaceId: "ici-dev",
      publicationRevision: 2,
    });
    await setDoc(doc(db, "workspaces", "outro"), {
      workspaceId: "outro",
      publicationRevision: 1,
    });
    await setDoc(doc(db, "system_admins", "admin@example.invalid"), { active: true });
  });
});

after(async () => {
  await env.cleanup();
});

test("leitura do ponteiro permitido", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "workspaces", "demo-v1")));
  await assertSucceeds(getDoc(doc(db, "workspaces", "ici-dev")));
});

test("leitura de snapshot permitido", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "workspaces/demo-v1/revisions/1/members/member-demo-1")));
  await assertSucceeds(getDoc(doc(db, "workspaces/ici-dev/revisions/2/members/member-corp-1")));
});

test("escrita negada", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(setDoc(doc(db, "workspaces/demo-v1/revisions/1/members/member-new"), {
    workspaceId: "demo-v1",
    publicationRevision: 1,
  }));
});

test("outro workspace negado", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "workspaces", "outro")));
  await assertFails(getDoc(doc(db, "workspaces/outro/revisions/1/members/member-1")));
});

test("colecao administrativa negada", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "system_admins", "admin@example.invalid")));
  await assertFails(getDoc(doc(db, "user_links", "uid-1")));
  await assertFails(getDoc(doc(db, "source_files", "file-1")));
});

test("colecoes raiz operacionais nao sao liberadas genericamente", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "members", "member-1")));
  await assertFails(getDoc(doc(db, "teams", "team-1")));
});
