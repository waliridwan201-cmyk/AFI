const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_afi_owner";
const BOB_UID = "bob_other_user";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

const pastTs = () => new Date(Date.now() - 2000);
const futureTs = () => new Date(Date.now() + 60000);

test("Unauthenticated user cannot read or write any AFI collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/sales`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/products`).doc("prod_1").set({
      userId: ALICE_UID,
      name: "Kerupuk AFI",
      unit: "Bal",
      buyPrice: 20000,
      sellPrice: 25000,
      stock: 10,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${ALICE_UID}/products/prod_1`).set({
      userId: ALICE_UID,
      name: "Bawang Goreng",
      unit: "Kg",
      buyPrice: 40000,
      sellPrice: 55000,
      stock: 20,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/products/prod_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/products/prod_1`).update({
      sellPrice: 1000,
      updatedAt: pastTs(),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/products/prod_1`).delete());
});

test("StoreSettings create, update, and reject ghost/future fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/store_settings/default`);
  const ts = pastTs();

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      storeName: "Pabrik & Toko AFI",
      ownerName: "Ibu Afi",
      phone: "08123456789",
      address: "Jl. Industri No. 12",
      receiptFooter: "Terima kasih",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    ref.update({
      storeName: "AFI Sentosa",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field rejected
  await assertFails(
    ref.update({
      storeName: "AFI Hack",
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/store_settings/future_cfg`).set({
      userId: ALICE_UID,
      storeName: "Future Store",
      createdAt: futureTs(),
      updatedAt: futureTs(),
    })
  );
});

test("Products CRUD and @DocumentId id-exclusion enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection(`users/${ALICE_UID}/products`);
  const ts = pastTs();

  await assertSucceeds(
    col.doc("prod_a").set({
      userId: ALICE_UID,
      name: "Kerupuk Udang 500g",
      sku: "AFI-001",
      category: "Makanan Ringan",
      unit: "Pak",
      buyPrice: 12000,
      sellPrice: 15000,
      stock: 50,
      minStock: 10,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject including "id" key in body
  await assertFails(
    col.doc("prod_b").set({
      id: "prod_b",
      userId: ALICE_UID,
      name: "Kerupuk Ikan",
      unit: "Pak",
      buyPrice: 10000,
      sellPrice: 13000,
      stock: 30,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial stock update
  await assertSucceeds(
    col.doc("prod_a").update({
      stock: 45,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative stock
  await assertFails(
    col.doc("prod_a").update({
      stock: -5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(col.get());
});

test("Factory JobTypes, Employees, and Payslips CRUD + paid toggle", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTs();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/job_types/job_penakar`).set({
      userId: ALICE_UID,
      code: "PENAKAR",
      name: "Penakar",
      unit: "Takar",
      ratePerUnit: 500,
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/job_types/job_penakar`).update({
      ratePerUnit: 650,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/employees/emp_1`).set({
      userId: ALICE_UID,
      name: "Siti Aminah",
      phone: "0812000111",
      defaultJobTypeId: "job_penakar",
      defaultJobName: "Penakar",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/payslips/slip_1`).set({
      userId: ALICE_UID,
      employeeId: "emp_1",
      employeeName: "Siti Aminah",
      periodLabel: "Minggu 4 Nov 2026",
      items: [
        {
          jobName: "Penakar",
          unit: "Takar",
          quantity: 100,
          ratePerUnit: 650,
          subtotal: 65000,
        },
      ],
      bonus: 10000,
      deduction: 0,
      totalAmount: 75000,
      paid: false,
      notes: "Mingguan",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Mark paid
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/payslips/slip_1`).update({
      paid: true,
      cashBoxId: "cash_main",
      paidAtMillis: Date.now() - 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("CashBox, CashFlow, Sales, Purchases, Returns, Expenses, and DebtRecords", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTs();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/cash_boxes/cash_main`).set({
      userId: ALICE_UID,
      name: "Cash Box Utama",
      description: "Kas tunai toko & pabrik",
      balance: 5000000,
      primaryBox: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/cash_flows/flow_1`).set({
      userId: ALICE_UID,
      flowType: "IN",
      category: "PENJUALAN",
      amount: 150000,
      cashBoxId: "cash_main",
      cashBoxName: "Cash Box Utama",
      description: "Penjualan INV-001",
      referenceId: "sale_1",
      transactionDateMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // CashFlow update must fail (immutable ledger)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/cash_flows/flow_1`).update({
      amount: 999999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/sales/sale_1`).set({
      userId: ALICE_UID,
      invoiceNo: "INV-001",
      customerName: "Pak Budi (Keliling)",
      merchantId: "merch_1",
      items: [
        {
          productId: "prod_a",
          productName: "Kerupuk Udang",
          unit: "Pak",
          quantity: 10,
          buyPrice: 12000,
          sellPrice: 15000,
          subtotal: 150000,
          profit: 30000,
        },
      ],
      totalAmount: 150000,
      paidAmount: 100000,
      remainingDebt: 50000,
      totalCost: 120000,
      totalProfit: 30000,
      lossAmount: 0,
      paymentStatus: "PIUTANG",
      cashBoxId: "cash_main",
      notes: "Cicilan awal",
      transactionDateMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/purchases/pur_1`).set({
      userId: ALICE_UID,
      invoiceNo: "PB-001",
      supplierName: "CV Tepung Jaya",
      items: [
        {
          productId: "prod_a",
          productName: "Kerupuk Udang",
          unit: "Pak",
          quantity: 50,
          buyPrice: 12000,
          subtotal: 600000,
        },
      ],
      totalAmount: 600000,
      paidAmount: 0,
      remainingDebt: 600000,
      paymentStatus: "HUTANG",
      notes: "Tempo 7 hari",
      transactionDateMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/purchase_returns/ret_1`).set({
      userId: ALICE_UID,
      purchaseId: "pur_1",
      supplierName: "CV Tepung Jaya",
      productId: "prod_a",
      productName: "Kerupuk Udang",
      quantity: 5,
      unitPrice: 12000,
      totalRefund: 60000,
      refundMethod: "POTONG_HUTANG",
      reason: "Kemasan rusak",
      transactionDateMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).set({
      userId: ALICE_UID,
      title: "Listrik Pabrik",
      category: "Utilitas",
      amount: 250000,
      cashBoxId: "cash_main",
      cashBoxName: "Cash Box Utama",
      notes: "Token listrik",
      transactionDateMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/debt_records/debt_1`).set({
      userId: ALICE_UID,
      debtType: "PIUTANG",
      partyId: "merch_1",
      partyName: "Pak Budi (Keliling)",
      referenceId: "sale_1",
      invoiceNo: "INV-001",
      totalAmount: 150000,
      paidAmount: 100000,
      remainingAmount: 50000,
      status: "CICILAN",
      transactionDateMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/debt_records/debt_1`).update({
      paidAmount: 150000,
      remainingAmount: 0,
      status: "LUNAS",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
