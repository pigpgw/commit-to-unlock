export type ChangeKind = "added" | "modified" | "removed" | "renamed";

export interface ChangedFile {
  path: string;
  additions: number;
  deletions: number;
  changes: number;
  status: ChangeKind;
  patch?: string;
}

export interface PullRequestSignals {
  subjectId: string;
  eventType: "pull_request_opened" | "pull_request_updated" | "pull_request_merged";
  changedFiles: ChangedFile[];
  issueLinked: boolean;
  ciPassed: boolean;
  approvals: number;
  reviewComments: number;
  discussionsResolved: number;
  revertDetected: boolean;
  duplicatePatchRisk: "low" | "medium" | "high";
  authorIsBot: boolean;
}

export interface FeatureVector {
  changedFiles: number;
  additions: number;
  deletions: number;
  sourceFiles: number;
  testFiles: number;
  docsFiles: number;
  configFiles: number;
  lockFiles: number;
  generatedFiles: number;
  vendorFiles: number;
  generatedRatio: number;
  lockfileOnly: boolean;
  docsOnly: boolean;
  whitespaceOnly: boolean;
  issueLinked: boolean;
  ciPassed: boolean;
  approvals: number;
  reviewComments: number;
  discussionsResolved: number;
  revertDetected: boolean;
  duplicatePatchRisk: "low" | "medium" | "high";
  authorIsBot: boolean;
}

export interface ScoreDecision {
  subjectType: "pull_request" | "commit_batch" | "review";
  subjectId: string;
  score: number;
  creditMinutes: number;
  confidence: "low" | "medium" | "high";
  reasons: string[];
  riskFlags: string[];
  needsHumanReview: boolean;
}
