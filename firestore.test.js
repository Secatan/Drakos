const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kiosco_1";
const BOB_UID = "bob_kiosco_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. UNAUTHENTICATED ACCESS REJECTION ---
test("Unauthenticated caller cannot read or list sales, sessions, or user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sales").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cash_sessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("settings").doc("company").get());
});

// --- 2. USER PROFILE PII & OWNER ISOLATION ---
test("Owner can create and update own workspace profile; cross-user get/write is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Roberto Kioscos",
      email: "roberto@kioscopos.ar",
      chainName: "Cadena KioscoPOS 10",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's profile (PII email isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice cannot inject ghost field (Shadow Update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Roberto Admin",
      isAdmin: true,
      updatedAt: pastTimestamp(1000),
    })
  );

  // Valid update passes
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      chainName: "KioscoPOS Buenos Aires S.A.",
      updatedAt: pastTimestamp(1000),
    })
  );
});

// --- 3. SALES CRUD, CROSS-USER ISOLATION & TERMINAL STATE LOCKING ---
test("Owner can create sale and void once, cannot re-open a VOIDED sale, Bob is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();
  const saleRef = aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_101");

  await assertSucceeds(
    saleRef.set({
      branchId: 1,
      branchName: "Sucursal 01 - Centro",
      receiptNumber: "TK-001-101",
      totalAmount: 3600.0,
      paymentMethod: "CASH",
      status: "COMPLETED",
      cashierName: "Lucia",
      localSaleId: 101,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sales").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_101").get());

  // Void sale transition COMPLETED -> VOIDED succeeds
  await assertSucceeds(
    saleRef.update({
      status: "VOIDED",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Terminal state lock: VOIDED -> COMPLETED must fail
  await assertFails(
    saleRef.update({
      status: "COMPLETED",
      updatedAt: pastTimestamp(500),
    })
  );
});

// --- 4. TRANSFERS STATE MACHINE & SAME-BRANCH GUARD ---
test("Transfer forbids same source/target branch and enforces REQUESTED -> DISPATCHED -> RECEIVED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();
  const trfCol = aliceDb.collection("users").doc(ALICE_UID).collection("transfers");

  // Same branch must fail
  await assertFails(
    trfCol.doc("trf_bad").set({
      sourceBranchId: 2,
      targetBranchId: 2,
      productSummary: "Coca 500ml",
      quantity: 12,
      status: "REQUESTED",
      notes: "",
      localTransferId: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const trfRef = trfCol.doc("trf_1");
  await assertSucceeds(
    trfRef.set({
      sourceBranchId: 1,
      targetBranchId: 3,
      productSummary: "Alfajor Havanna x 12",
      quantity: 12,
      status: "REQUESTED",
      notes: "Reposicion urgente",
      localTransferId: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // State shortcut REQUESTED -> RECEIVED must fail
  await assertFails(
    trfRef.update({
      status: "RECEIVED",
      updatedAt: pastTimestamp(1000),
    })
  );

  await assertSucceeds(
    trfRef.update({
      status: "DISPATCHED",
      updatedAt: pastTimestamp(1000),
    })
  );

  await assertSucceeds(
    trfRef.update({
      status: "RECEIVED",
      updatedAt: pastTimestamp(500),
    })
  );

  // Terminal state lock after RECEIVED
  await assertFails(
    trfRef.update({
      status: "DISPATCHED",
      updatedAt: pastTimestamp(100),
    })
  );
});

// --- 5. EXPENSES IMMUTABILITY & ID POISONING ---
test("Expenses reject negative amounts, reject updates (immutable), and reject invalid doc IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();
  const expCol = aliceDb.collection("users").doc(ALICE_UID).collection("expenses");

  await assertFails(
    expCol.doc("bad$id!").set({
      branchId: 1,
      category: "VARIOS",
      amount: 1200,
      description: "Bolsas",
      paymentSource: "CASH_DRAWER",
      localExpenseId: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    expCol.doc("exp_neg").set({
      branchId: 1,
      category: "VARIOS",
      amount: -50,
      description: "Invalido",
      paymentSource: "CASH_DRAWER",
      localExpenseId: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const expRef = expCol.doc("exp_1");
  await assertSucceeds(
    expRef.set({
      branchId: 1,
      category: "SERVICIOS",
      amount: 18500.5,
      description: "Factura Edenor",
      paymentSource: "BANK",
      localExpenseId: 3,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Immutable update check
  await assertFails(
    expRef.update({
      amount: 100.0,
      updatedAt: pastTimestamp(500),
    })
  );
});

// --- 6. STAFF PROFILES & AUTHORIZED DEVICES ---
test("Staff profiles & devices: owner can sync valid SHA-256 PIN hashes; cross-user & malformed hash fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();
  const validHash = "a".repeat(64);
  const staffRef = aliceDb.collection("users").doc(ALICE_UID).collection("staff_profiles").doc("staff_cajero1");

  // Unhashed plaintext PIN must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("staff_profiles").doc("staff_bad").set({
      username: "cajero_bad",
      fullName: "Mal",
      pinHash: "1234",
      role: "EMPLOYEE",
      assignedBranchId: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid SHA-256 profile passes
  await assertSucceeds(
    staffRef.set({
      username: "cajero_norte",
      fullName: "Matias Cajero",
      pinHash: validHash,
      role: "EMPLOYEE",
      assignedBranchId: 2,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user cannot read or write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("staff_profiles").get());

  // Cannot mutate immutable username
  await assertFails(
    staffRef.update({
      username: "otro_user",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Devices collection check
  const devRef = aliceDb.collection("users").doc(ALICE_UID).collection("devices").doc("dev_INVITE_DRK_4821");
  await assertSucceeds(
    devRef.set({
      deviceId: "INVITE-DRK-4821",
      deviceName: "Celular Caja 2",
      assignedBranchId: 2,
      status: "PENDING",
      pairingCode: "DRK-4821",
      approvedByUserName: "Roberto Dueño",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("devices").get());
});

