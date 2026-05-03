import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import {
  evaluatePolicyDecision,
  type EmergencyUnlock,
  type PolicyDecisionInput,
  type PolicyDecisionReason
} from "./policy.js";

interface PolicyGoldenFixture {
  id: string;
  description: string;
  input: {
    currentPackage: string | null;
    ownPackage: string;
    now: string;
    creditState: {
      remainingMinutes: number;
      freeUntil: string | null;
      strictMode: boolean;
      lastUpdatedAt: string;
    };
    policyState: {
      blockedTargets: string[];
      activeWeekdays: number[];
      activeFrom: string | null;
      activeUntil: string | null;
      applyOnPublicHolidays: boolean;
      manualHolidayToday: boolean;
      manualHolidayDate: string | null;
      timezone: string;
    };
    activeEmergencyUnlocks: EmergencyUnlock[];
    isPublicHoliday: boolean;
  };
  expected: {
    allowed: boolean;
    reason: PolicyDecisionReason;
    shouldSpendCredit: boolean;
    matchedTarget: string | null;
    activeEmergencyUnlockId: string | null;
  };
}

const fixtures = JSON.parse(
  readFileSync(new URL("../../../fixtures/policy-golden.json", import.meta.url), "utf8")
) as PolicyGoldenFixture[];

describe("policy golden fixtures", () => {
  it.each(fixtures)("$id: $description", (fixture) => {
    const decision = evaluatePolicyDecision(toPolicyInput(fixture));

    expect({
      allowed: decision.allowed,
      reason: decision.reason,
      shouldSpendCredit: decision.shouldSpendCredit,
      matchedTarget: decision.matchedTarget ?? null,
      activeEmergencyUnlockId: decision.activeEmergencyUnlockId ?? null
    }).toStrictEqual(fixture.expected);
  });
});

function toPolicyInput(fixture: PolicyGoldenFixture): PolicyDecisionInput {
  const { input } = fixture;

  return {
    currentPackage: input.currentPackage,
    ownPackage: input.ownPackage,
    now: new Date(input.now),
    creditState: {
      remainingMinutes: input.creditState.remainingMinutes,
      freeUntil: input.creditState.freeUntil ?? undefined,
      strictMode: input.creditState.strictMode,
      lastUpdatedAt: input.creditState.lastUpdatedAt
    },
    policyState: {
      blockedTargets: input.policyState.blockedTargets,
      activeWeekdays: input.policyState.activeWeekdays,
      activeFrom: input.policyState.activeFrom ?? undefined,
      activeUntil: input.policyState.activeUntil ?? undefined,
      applyOnPublicHolidays: input.policyState.applyOnPublicHolidays,
      manualHolidayToday: input.policyState.manualHolidayToday,
      timezone: input.policyState.timezone
    },
    activeEmergencyUnlocks: input.activeEmergencyUnlocks,
    isPublicHoliday: input.isPublicHoliday
  };
}
