import { describe, expect, it } from "vitest";
import { evaluatePolicyDecision, type PolicyDecisionInput } from "./policy.js";

const mondayAtNoon = new Date("2026-05-04T12:00:00.000Z");

const baseInput: PolicyDecisionInput = {
  currentPackage: "com.video.app",
  ownPackage: "com.commitunlock.prototype",
  now: mondayAtNoon,
  creditState: {
    remainingMinutes: 0,
    strictMode: false,
    lastUpdatedAt: mondayAtNoon.toISOString()
  },
  policyState: {
    blockedTargets: ["com.video.app"],
    activeWeekdays: [1, 2, 3, 4, 5],
    applyOnPublicHolidays: false,
    manualHolidayToday: false,
    timezone: "UTC"
  }
};

describe("evaluatePolicyDecision", () => {
  it("allows the app itself before checking policy", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      currentPackage: "com.commitunlock.prototype"
    });

    expect(decision).toMatchObject({
      allowed: true,
      reason: "own_app",
      shouldSpendCredit: false
    });
  });

  it("allows packages that are not blocked", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      currentPackage: "com.editor.app"
    });

    expect(decision.reason).toBe("target_not_blocked");
    expect(decision.allowed).toBe(true);
  });

  it("allows inactive weekdays before checking credit", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      now: new Date("2026-05-03T12:00:00.000Z")
    });

    expect(decision).toMatchObject({
      allowed: true,
      reason: "inactive_weekday",
      matchedTarget: "com.video.app"
    });
  });

  it("allows manual holidays before checking credit", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      policyState: {
        ...baseInput.policyState,
        manualHolidayToday: true
      }
    });

    expect(decision.reason).toBe("manual_holiday");
    expect(decision.allowed).toBe(true);
  });

  it("allows active free day before checking emergency unlock or credit", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      creditState: {
        ...baseInput.creditState,
        freeUntil: "2026-05-04T23:59:59.000Z"
      },
      activeEmergencyUnlocks: [
        {
          id: "unlock-1",
          durationMinutes: 15,
          reason: "Need one video",
          startedAt: "2026-05-04T11:50:00.000Z",
          expiresAt: "2026-05-04T12:05:00.000Z"
        }
      ]
    });

    expect(decision).toMatchObject({
      allowed: true,
      reason: "free_day",
      shouldSpendCredit: false
    });
  });

  it("allows active emergency unlock without spending credit", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      activeEmergencyUnlocks: [
        {
          id: "unlock-1",
          durationMinutes: 15,
          reason: "Production incident",
          startedAt: "2026-05-04T11:50:00.000Z",
          expiresAt: "2026-05-04T12:05:00.000Z"
        }
      ]
    });

    expect(decision).toMatchObject({
      allowed: true,
      reason: "emergency_unlock",
      shouldSpendCredit: false,
      activeEmergencyUnlockId: "unlock-1"
    });
  });

  it("allows and spends credit when credit is available", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      creditState: {
        ...baseInput.creditState,
        remainingMinutes: 5
      }
    });

    expect(decision).toMatchObject({
      allowed: true,
      reason: "credit_available",
      shouldSpendCredit: true,
      matchedTarget: "com.video.app"
    });
  });

  it("blocks a matched target when no higher-priority allowance applies", () => {
    const decision = evaluatePolicyDecision(baseInput);

    expect(decision).toMatchObject({
      allowed: false,
      reason: "credit_empty",
      shouldSpendCredit: false,
      matchedTarget: "com.video.app"
    });
  });

  it("supports overnight active windows", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      now: new Date("2026-05-04T23:30:00.000Z"),
      policyState: {
        ...baseInput.policyState,
        activeFrom: "22:00",
        activeUntil: "02:00"
      }
    });

    expect(decision.reason).toBe("credit_empty");
    expect(decision.allowed).toBe(false);
  });

  it("allows outside active time windows", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      now: new Date("2026-05-04T20:30:00.000Z"),
      policyState: {
        ...baseInput.policyState,
        activeFrom: "22:00",
        activeUntil: "02:00"
      }
    });

    expect(decision.reason).toBe("outside_active_time");
    expect(decision.allowed).toBe(true);
  });

  it("evaluates weekdays and time windows in the policy timezone", () => {
    const decision = evaluatePolicyDecision({
      ...baseInput,
      now: new Date("2026-05-03T15:30:00.000Z"),
      policyState: {
        ...baseInput.policyState,
        activeWeekdays: [1],
        activeFrom: "00:00",
        activeUntil: "01:00",
        timezone: "Asia/Seoul"
      }
    });

    expect(decision.reason).toBe("credit_empty");
    expect(decision.allowed).toBe(false);
  });
});
