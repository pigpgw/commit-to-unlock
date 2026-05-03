import { classifyPath } from "./file-classifier.js";
import type { FeatureVector, PullRequestSignals } from "./types.js";

export function extractFeatures(signals: PullRequestSignals): FeatureVector {
  const totals = signals.changedFiles.reduce(
    (acc, file) => {
      const classification = classifyPath(file.path);
      acc.additions += file.additions;
      acc.deletions += file.deletions;
      acc.sourceFiles += classification.isSource ? 1 : 0;
      acc.testFiles += classification.isTest ? 1 : 0;
      acc.docsFiles += classification.isDocs ? 1 : 0;
      acc.configFiles += classification.isConfig ? 1 : 0;
      acc.lockFiles += classification.isLockfile ? 1 : 0;
      acc.generatedFiles += classification.isGenerated ? 1 : 0;
      acc.vendorFiles += classification.isVendor ? 1 : 0;
      return acc;
    },
    {
      additions: 0,
      deletions: 0,
      sourceFiles: 0,
      testFiles: 0,
      docsFiles: 0,
      configFiles: 0,
      lockFiles: 0,
      generatedFiles: 0,
      vendorFiles: 0
    }
  );

  const changedFiles = signals.changedFiles.length;
  const nonWhitespaceChanges = signals.changedFiles.some((file) =>
    hasNonWhitespacePatch(file.patch)
  );
  const generatedOrVendor = totals.generatedFiles + totals.vendorFiles;

  return {
    changedFiles,
    additions: totals.additions,
    deletions: totals.deletions,
    sourceFiles: totals.sourceFiles,
    testFiles: totals.testFiles,
    docsFiles: totals.docsFiles,
    configFiles: totals.configFiles,
    lockFiles: totals.lockFiles,
    generatedFiles: totals.generatedFiles,
    vendorFiles: totals.vendorFiles,
    generatedRatio: changedFiles === 0 ? 0 : generatedOrVendor / changedFiles,
    lockfileOnly: changedFiles > 0 && totals.lockFiles === changedFiles,
    docsOnly: changedFiles > 0 && totals.docsFiles === changedFiles,
    whitespaceOnly: changedFiles > 0 && !nonWhitespaceChanges,
    issueLinked: signals.issueLinked,
    ciPassed: signals.ciPassed,
    approvals: signals.approvals,
    reviewComments: signals.reviewComments,
    discussionsResolved: signals.discussionsResolved,
    revertDetected: signals.revertDetected,
    duplicatePatchRisk: signals.duplicatePatchRisk,
    authorIsBot: signals.authorIsBot
  };
}

function hasNonWhitespacePatch(patch: string | undefined): boolean {
  if (!patch) {
    return true;
  }

  return patch
    .split("\n")
    .filter((line) => line.startsWith("+") || line.startsWith("-"))
    .some((line) => line.slice(1).trim().length > 0);
}
