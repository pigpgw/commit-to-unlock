import { describe, expect, it } from "vitest";
import { scorePullRequest } from "./score.js";
import type { PullRequestSignals } from "./types.js";

const baseSignals: PullRequestSignals = {
  subjectId: "github:repo:1",
  eventType: "pull_request_merged",
  changedFiles: [
    {
      path: "src/auth/session.ts",
      additions: 120,
      deletions: 30,
      changes: 150,
      status: "modified",
      patch: "@@ -1 +1 @@\n-export const old = true\n+export const refreshed = true"
    },
    {
      path: "src/auth/session.test.ts",
      additions: 80,
      deletions: 2,
      changes: 82,
      status: "added",
      patch: "@@ -0,0 +1 @@\n+expect(refresh()).toBeTruthy()"
    },
    {
      path: "README.md",
      additions: 12,
      deletions: 0,
      changes: 12,
      status: "modified",
      patch: "@@ -1 +1 @@\n+Updated auth docs"
    }
  ],
  issueLinked: true,
  ciPassed: true,
  approvals: 1,
  reviewComments: 4,
  discussionsResolved: 1,
  revertDetected: false,
  duplicatePatchRisk: "low",
  authorIsBot: false
};

describe("scorePullRequest", () => {
  it("rewards reviewed source changes with tests and CI", () => {
    const decision = scorePullRequest(baseSignals);

    expect(decision.score).toBeGreaterThanOrEqual(65);
    expect(decision.creditMinutes).toBe(45);
    expect(decision.confidence).toBe("high");
    expect(decision.riskFlags).toEqual([]);
  });

  it("rejects bot-authored activity", () => {
    const decision = scorePullRequest({ ...baseSignals, authorIsBot: true });

    expect(decision.score).toBe(0);
    expect(decision.creditMinutes).toBe(0);
  });

  it("caps generated-heavy changes", () => {
    const decision = scorePullRequest({
      ...baseSignals,
      changedFiles: [
        {
          path: "src/generated/client.gen.ts",
          additions: 2000,
          deletions: 0,
          changes: 2000,
          status: "modified",
          patch: "@@ -0,0 +1 @@\n+generated"
        },
        {
          path: "vendor/sdk/index.ts",
          additions: 1000,
          deletions: 0,
          changes: 1000,
          status: "modified",
          patch: "@@ -0,0 +1 @@\n+vendor"
        }
      ]
    });

    expect(decision.score).toBeLessThanOrEqual(25);
    expect(decision.creditMinutes).toBeLessThanOrEqual(10);
    expect(decision.riskFlags).toContain("generated_or_vendor_heavy");
  });

  it("does not reward lockfile-only changes", () => {
    const decision = scorePullRequest({
      ...baseSignals,
      changedFiles: [
        {
          path: "pnpm-lock.yaml",
          additions: 10,
          deletions: 10,
          changes: 20,
          status: "modified",
          patch: "@@ -1 +1 @@\n-lock\n+lock"
        }
      ]
    });

    expect(decision.creditMinutes).toBe(0);
    expect(decision.riskFlags).toContain("lockfile_only");
  });
});
