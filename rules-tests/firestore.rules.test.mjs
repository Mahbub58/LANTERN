import { after, before, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

const CITIZEN = "citizen-1";
const OTHER = "citizen-2";

let testEnv;

function report(uid, overrides = {}) {
  return {
    uid,
    description: "Flood water entered the house, two people are trapped.",
    language: "EN",
    type: "FLOOD",
    urgency: 5,
    peopleCount: 2,
    needs: ["RESCUE", "WATER"],
    placeDescription: "Near the market",
    location: null,
    status: "NEW",
    createdAt: 1_700_000_000_000,
    updatedAt: 1_700_000_000_000,
    sentAt: 1_700_000_000_000,
    ...overrides,
  };
}

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-lantern",
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: readFileSync("firestore.rules", "utf8"),
    },
  });
});

after(async () => {
  await testEnv?.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "reports/own-1"), report(CITIZEN));
    await setDoc(doc(db, "reports/other-1"), report(OTHER));
    await setDoc(doc(db, "alerts/a1"), {
      level: "URGENT",
      title: { en: "Flood", ur: "سیلاب", roman: "Selab" },
      message: { en: "Move up", ur: "اوپر جائیں", roman: "Upar jayen" },
      region: "Karachi",
      issuedAt: 1_700_000_000_000,
    });
    await setDoc(doc(db, "responders/r1"), { name: "Desk", phone: "1122" });
    await setDoc(doc(db, "config/safety_cards"), {
      version: "1.1.0",
      json: "{}",
    });
  });
});

describe("reports", () => {
  it("allows a signed-in citizen to create a report with their own uid", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertSucceeds(setDoc(doc(db, "reports/new-1"), report(CITIZEN)));
  });

  it("denies a report created with someone else's uid", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(setDoc(doc(db, "reports/forged"), report(OTHER)));
  });

  it("denies an invalid urgency value", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(
      setDoc(doc(db, "reports/bad-urgency"), report(CITIZEN, { urgency: 9 })),
    );
  });

  it("denies a report with unexpected fields", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(
      setDoc(
        doc(db, "reports/bad-fields"),
        report(CITIZEN, { admin: true }),
      ),
    );
  });

  it("allows a citizen to read their own report", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertSucceeds(getDoc(doc(db, "reports/own-1")));
  });

  it("denies reading another citizen's report", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(getDoc(doc(db, "reports/other-1")));
  });

  it("allows the own-report query but denies an unfiltered query", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertSucceeds(
      getDocs(query(collection(db, "reports"), where("uid", "==", CITIZEN))),
    );
    await assertFails(getDocs(collection(db, "reports")));
  });

  it("denies updating and deleting a report", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(updateDoc(doc(db, "reports/own-1"), { urgency: 4 }));
    await assertFails(deleteDoc(doc(db, "reports/own-1")));
  });

  it("denies signed-out reads and writes", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "reports/own-1")));
    await assertFails(setDoc(doc(db, "reports/anon"), report("anonymous")));
  });
});

describe("alerts", () => {
  it("allows a signed-in citizen to read alerts", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertSucceeds(getDoc(doc(db, "alerts/a1")));
    await assertSucceeds(getDocs(collection(db, "alerts")));
  });

  it("denies signed-out alert reads", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "alerts/a1")));
  });

  it("denies citizen alert writes", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(setDoc(doc(db, "alerts/forged"), { level: "INFO" }));
  });
});

describe("responders", () => {
  it("denies responder reads to signed-in citizens", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(getDoc(doc(db, "responders/r1")));
  });

  it("denies responder reads to signed-out clients", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "responders/r1")));
  });
});

describe("config/safety_cards", () => {
  it("allows a signed-in citizen to read the card pack", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertSucceeds(getDoc(doc(db, "config/safety_cards")));
  });

  it("denies signed-out card-pack reads", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "config/safety_cards")));
  });

  it("denies citizen card-pack writes", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(
      setDoc(doc(db, "config/safety_cards"), { version: "9.9.9", json: "{}" }),
    );
  });

  it("denies other config documents", async () => {
    const db = testEnv.authenticatedContext(CITIZEN).firestore();
    await assertFails(getDoc(doc(db, "config/private")));
  });
});
