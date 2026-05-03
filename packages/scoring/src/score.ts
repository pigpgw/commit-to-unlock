import { extractFeatures } from "./features.js";
import type { FeatureVector, PullRequestSignals, ScoreDecision } from "./types.js";

export function scorePullRequest(signals: PullRequestSignals): ScoreDecision {
  const features = extractFeatures(signals);
  const riskFlags = collectRiskFlags(features);

  if (features.authorIsBot) {
    return decision(signals.subjectId, 0, 0, "high", ["Bot-authored activity is excluded."], riskFlags, false);
  }

  if (features.whitespaceOnly || features.lockfileOnly) {
    return decision(signals.subjectId, 5, 0, "high", ["Change appears to be whitespace-only or lockfile-only."], riskFlags, false);
  }

  let score = 0;
  const reasons: string[] = [];

  const diffScore = scoreDiff(features);
  score += diffScore.points;
  reasons.push(diffScore.reason);

  if (features.testFiles > 0) {
    score += 15;
    reasons.push("Includes test changes.");
  } else if (features.ciPassed) {
    score += 8;
    reasons.push("CI passed, but no test file changes were detected.");
  }

  if (features.issueLinked) {
    score += 10;
    reasons.push("Links to an issue or ticket.");
  }

  if (features.approvals > 0) {
    score += 12;
    reasons.push("Includes review approval.");
  }

  if (features.reviewComments > 0 || features.discussionsResolved > 0) {
    score += Math.min(8, features.reviewComments + features.discussionsResolved * 2);
    reasons.push("Shows review discussion activity.");
  }

  if (features.sourceFiles >= 4 && features.configFiles > 0) {
    score += 8;
    reasons.push("Touches both source and configuration, suggesting cross-layer work.");
  }

  if (features.docsFiles > 0 && features.sourceFiles > 0) {
    score += 5;
    reasons.push("Updates documentation alongside code.");
  }

  if (features.revertDetected) {
    score -= 25;
    reasons.push("Recent revert risk reduces confidence.");
  }

  if (features.duplicatePatchRisk === "medium") {
    score -= 10;
  }

  if (features.duplicatePatchRisk === "high") {
    score -= 25;
  }

  if (features.generatedRatio >= 0.6) {
    score = Math.min(score, 25);
    reasons.push("Generated or vendor files dominate the change, so credit is capped.");
  }

  if (features.docsOnly) {
    score = Math.min(score, 20);
    reasons.push("Documentation-only changes receive limited credit.");
  }

  const normalizedScore = clamp(score, 0, 100);
  return decision(
    signals.subjectId,
    normalizedScore,
    creditForScore(normalizedScore),
    confidenceFor(features, riskFlags),
    reasons,
    riskFlags,
    riskFlags.includes("large_diff") || riskFlags.includes("high_duplicate_patch_risk")
  );
}

function scoreDiff(features: FeatureVector): { points: number; reason: string } {
  if (features.sourceFiles === 0) {
    return { points: 5, reason: "No source file changes detected." };
  }

  if (features.changedFiles <= 2 && features.additions + features.deletions < 40) {
    return { points: 12, reason: "Small but meaningful source change detected." };
  }

  if (features.changedFiles <= 10 && features.additions + features.deletions <= 500) {
    return { points: 25, reason: "Meaningful multi-file source change detected." };
  }

  return { points: 20, reason: "Large source change detected; review may be needed." };
}

function creditForScore(score: number): number {
  if (score < 25) return 0;
  if (score < 45) return 10;
  if (score < 65) return 25;
  if (score < 80) return 45;
  return 60;
}

function confidenceFor(features: FeatureVector, riskFlags: string[]): "low" | "medium" | "high" {
  if (riskFlags.length >= 2 || features.revertDetected) {
    return "low";
  }

  if (features.ciPassed && (features.approvals > 0 || features.testFiles > 0)) {
    return "high";
  }

  return "medium";
}

function collectRiskFlags(features: FeatureVector): string[] {
  const flags: string[] = [];

  if (features.generatedRatio >= 0.6) flags.push("generated_or_vendor_heavy");
  if (features.lockfileOnly) flags.push("lockfile_only");
  if (features.whitespaceOnly) flags.push("whitespace_only");
  if (features.changedFiles > 40 || features.additions + features.deletions > 2000) flags.push("large_diff");
  if (features.duplicatePatchRisk === "high") flags.push("high_duplicate_patch_risk");
  if (features.revertDetected) flags.push("revert_detected");

  return flags;
}

function decision(
  subjectId: string,
  score: number,
  creditMinutes: number,
  confidence: "low" | "medium" | "high",
  reasons: string[],
  riskFlags: string[],
  needsHumanReview: boolean
): ScoreDecision {
  return {
    subjectType: "pull_request",
    subjectId,
    score,
    creditMinutes,
    confidence,
    reasons,
    riskFlags,
    needsHumanReview
  };
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}
